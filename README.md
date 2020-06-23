Aliyun-OSS-Plugin for Jenkins
====================
描述：使用阿里云对象存储服务来建立交付仓库。

好处：1.可靠性、高可用性  2.上传、下载速度

Jenkins是当前最常用的CI服务器，Aliyun-OSS-Plugin for Jenkins的功能是：将构建后的artifact上传到OSS的指定位置上去。
 	
一、安装说明
-------------------------

在Jenkins中安装插件, 请到 Manage Jenkins | Advanced | Upload，上传插件(.hpi文件)
安装完毕后请重新启动Jenkins
插件位置：mvn打包后存在于target下

二、配置说明
-------------------------

在使用插件之前，必须先在[Manage Jenkins | Configure System | 阿里云OSS账户设置]中配置阿里云帐号的Access Key、Secret Key和阿里云EndPoint后缀.

三、Post-build actions: 上传Artifact到阿里云OSS
-------------------------

在Jenkins Job的Post-build actions，用户可以设上传Artifact到阿里云OSS。需要填写的信息是：

1. Bucket名称: artifact要存放的bucket
2. 要上传的artifacts: 文件之间用;隔开。支持通配符描述，比如 text/*.zip
3. Object前缀设置：可以设置object key的前缀，支持Jenkins环境变量比如: "${JOB_NAME}/${BUILD_ID}/${BUILD_NUMBER}/"
4. 完成后删除文件：勾选表示上传成功后会把本地文件删除。

例如一个job的名称是test，用户的设置如下

1. bucketName: f2c
2. 要上传的artifacts: hello.txt;hello1.txt
3. Object前缀: ${JOB_NAME}/${BUILD_ID}/${BUILD_NUMBER}

那么上传后的文件url为: http://f2c.oss-cn-hangzhou.aliyuncs.com/test/2015-01-20_14-22-46/5/hello.txt

四、Pipeline: 上传Artifact到阿里云OSS
-------------------------

集成jenkins pipeline，用户可以上传Artifact到阿里云OSS

例如一个job的名称是test，用户的设置如下
1. Bucket名称: bucket-local-2
2. 要上传的artifacts:usr/local/dingdang/src/github.com/dingdang/*/target/*.jar
3. Object前缀设置：{JOB_NAME}


则在jenkins的pipeline中配置

    node {
        stage('upload') {
	        echo '开始执行upload'
	        bucketName = 'bucket-local-2'//bucket名称 
	        filesPath = 'usr/local/dingdang/src/github.com/dingdang/*/target/*.jar'//文件位置 (可以以变量的方式传入tag版本，指定文件)
	        objectPrefix = 'test/${JOB_NAME}'//前缀
	        isDel = 'true'//写true，则会删除文件

	        step([$class: 'AliyunOSSPublisher', bucketName: bucketName,filesPath: filesPath,objectPrefix: objectPrefix,isDel: isDel])
	        echo 'deploy执行完毕'
            }
        }
pipeline项目构建的时候，就会触发上传参数

五、说明
-------------------------
原作者:zhimin@fit2cloud.com
原code地址：https://github.com/fit2cloud/aliyun-oss-plugin

本篇code在的基础上增加了两个功能：1.支持通过pipeline完成文件上传 2.支持文件上传后删除本地文件

优点：
1.原先的构建方式每个jenkins job都需要配置一次Post-build Actions，本篇code可以让多个job复用一条pipeline完成上传操作。

非常感谢原作者提供的框架，若有侵权请联系我删除：2775468731@qq.com
