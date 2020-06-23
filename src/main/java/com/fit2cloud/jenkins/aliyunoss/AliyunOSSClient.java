package com.fit2cloud.jenkins.aliyunoss;

import com.aliyun.oss.model.Bucket;
import hudson.FilePath;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.SlaveComputer;
import org.apache.commons.lang.time.DurationFormatUtils;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.ObjectMetadata;

public class AliyunOSSClient {
    private static final String fpSeparator = ";";

    public static boolean validateAliyunAccount(
            final String aliyunAccessKey, final String aliyunSecretKey) throws AliyunOSSException {
        try {
            OSSClient client = new OSSClient(aliyunAccessKey, aliyunSecretKey);
            client.listBuckets();
        } catch (Exception e) {
            throw new AliyunOSSException("阿里云账号验证失败：" + e.getMessage());
        }
        return true;
    }


    public static boolean validateOSSBucket(String aliyunAccessKey,
                                            String aliyunSecretKey, String bucketName) throws AliyunOSSException {
        try {
            OSSClient client = new OSSClient(aliyunAccessKey, aliyunSecretKey);
//			client.getBucketLocation(bucketName);   会报错
            String location = null;
            for (Bucket bucket : client.listBuckets()) {
                if (bucket.getName().equals(bucketName)) {
                    location = bucket.getLocation();
                    break;
                }
            }
            if (location == null) {
                throw new AliyunOSSException("Bucket:" + location + "不存在!");
            }
        } catch (Exception e) {
            throw new AliyunOSSException("验证Bucket名称失败：" + e.getMessage());
        }
        return true;
    }

    public static Map<String,Object> upload(Run build,FilePath filePath,TaskListener listener,
                                        final String aliyunAccessKey, final String aliyunSecretKey, final String aliyunEndPointSuffix, String bucketName, String expFP, String expVP) throws AliyunOSSException{
        OSSClient client = new OSSClient(aliyunAccessKey, aliyunSecretKey);
        String location = null;
        for (Bucket bucket : client.listBuckets()) {
            if (bucket.getName().equals(bucketName)) {
                location = bucket.getLocation();
                break;
            }
        }
        Map<String, Object> map = new HashMap<String, Object>();
        List<URI> uriList = new ArrayList<URI>();
        String endpoint = "http://" + location + aliyunEndPointSuffix;
        client = new OSSClient(endpoint, aliyunAccessKey, aliyunSecretKey);
        int filesUploaded = 0; // Counter to track no. of files that are uploaded
        map.put("filesUploaded", filesUploaded);
        try {
            FilePath workspacePath = filePath;
            //支节点
            if(Computer.currentComputer() instanceof SlaveComputer) {
                listener.getLogger().println("进入支节点,workspacePath:"+workspacePath);
            }
            //以/开头，则取绝对路径
            listener.getLogger().println("filePath:"+filePath);
            listener.getLogger().println("expFP:"+expFP);
            if( expFP.startsWith("/")){
                workspacePath = new FilePath(new File("/"));
                expFP = expFP.substring(1);
            }
            if (workspacePath == null) {
                listener.getLogger().println("工作空间中没有任何文件.");
                return map;
            }
            StringTokenizer strTokens = new StringTokenizer(expFP, fpSeparator);
            FilePath[] paths = null;

            listener.getLogger().println("开始上传到阿里云OSS...");
            listener.getLogger().println("上传的根workspacePath是：" + workspacePath);

            while (strTokens.hasMoreElements()) {
                String fileName = strTokens.nextToken();
                String embeddedVP = null;
                if (fileName != null) {
                    int embVPSepIndex = fileName.indexOf("::");
                    if (embVPSepIndex != -1) {
                        if (fileName.length() > embVPSepIndex + 1) {
                            embeddedVP = fileName.substring(embVPSepIndex + 2, fileName.length());
                            if (Utils.isNullOrEmpty(embeddedVP)) {
                                embeddedVP = null;
                            }
                            if (embeddedVP != null && !embeddedVP.endsWith(Utils.FWD_SLASH)) {
                                embeddedVP = embeddedVP + Utils.FWD_SLASH;
                            }
                        }
                        fileName = fileName.substring(0, embVPSepIndex);
                    }
                }

                if (Utils.isNullOrEmpty(fileName)) {
                    break;
                }

                FilePath fp = new FilePath(workspacePath, fileName);

                if (fp.exists() && !fp.isDirectory()) {
                    paths = new FilePath[1];
                    paths[0] = fp;
                } else {
                    paths = workspacePath.list(fileName);
                }
                listener.getLogger().println("paths.length:"+paths.length);
                if (paths.length != 0) {
                    for (FilePath src : paths) {
                        String key = "";
                        if (Utils.isNullOrEmpty(expVP)
                                && Utils.isNullOrEmpty(embeddedVP)) {
                            key = src.getName();
                        } else {
                            String prefix = expVP;
                            if (!Utils.isNullOrEmpty(embeddedVP)) {
                                if (Utils.isNullOrEmpty(expVP)) {
                                    prefix = embeddedVP;
                                } else {
                                    prefix = expVP + embeddedVP;
                                }
                            }
                            key = prefix + src.getName();
                        }
                        long startTime = System.currentTimeMillis();
                        InputStream inputStream = src.read();
                        try {
                            ObjectMetadata meta = new ObjectMetadata();
                            meta.setContentLength(src.length());
                            client.putObject(bucketName, key, inputStream, meta);
                        } finally {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                            }
                        }
                        long endTime = System.currentTimeMillis();
                        listener.getLogger().println("Uploaded object [" + key + "] in " + getTime(endTime - startTime));
                        filesUploaded++;
                        uriList.add(src.toURI());
                    }
                }
            }
            map.put("filesUploaded", filesUploaded);
            map.put("uriList", uriList);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AliyunOSSException(e.getMessage(), e.getCause());
        }
        return map;
    }

    public static String getTime(long timeInMills) {
        return DurationFormatUtils.formatDuration(timeInMills, "HH:mm:ss.S") + " (HH:mm:ss.S)";
    }

}
