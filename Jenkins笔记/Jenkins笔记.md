# 安装

## Docker安装Jenkins

**1.安装**

~~~sh
docker run \ 
  -u root \ 
  -d \ 
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins-data:/var/jenkins_home \ 
  -v /etc/localtime:/etc/localtime:ro \ 
  -v /var/run/docker.sock:/var/run/docker.sock \ 
  --restart=always \ 
  jenkinsci/blueocean
   
   
  #自己构建镜像 RUN的时候就把时区设置好 
  #如果是别人的镜像，docker hub，UTC； 容器运行时 ， -v  /etc/localtime:/etc/localtime:ro 
   
   
  jenkinsci/jenkins 是没有 blueocean插件的，得自己装  
  jenkinsci/blueocean：带了的 
   
  #/var/run/docker.sock 表示Docker守护程序通过其监听的基于Unix的套接字。 该映射允许  jenkinsci/blueocean 容器与Docker守护进程通信， 如果 jenkinsci/blueocean 容器需要实例化 其他Docker容器，则该守护进程是必需的。 如果运行声明式管道，其语法包含agent部分用 docker；例 如， agent { docker { ... } } 此选项是必需的。  
   
  #如果你的jenkins 安装插件装不上。使用这个镜像【 registry.cn- qingdao.aliyuncs.com/lfy/jenkins:plugins-blueocean 】默认访问账号/密码是 
【admin/admin】 
~~~

**2.获取初始token登录**

~~~sh
docker logs xx
token在logs里，或者在刚刚挂载的目录下的secret里慢慢找
~~~



# 实战 

> 1. 创建一个java项目，上传gitee
>
> 2. jenkins中创建任务（流水线），绑定git仓库，实现代码提交jenkins自动构建（钩子）
>
>    远程构建即使配置了github 的webhook，默认会403.我们应该使用用户进行授权 
>
>    1)、创建一个用户 
>
>    2)、一定随便登陆激活一次 
>
>    3)、生成一个apitoken 
>
>    4)、将`http://leifengyang:113620edce6200b9c78ecadb26e9cf122e@139.198.186.134:8080/job/devops-java-demo/build?token=leifengyang`填入webhook
>
> 3. 编写Jenkinsfile，实现代码提交git后，自动打包、部署

- jenkins装docker pipeline插件

![image-20220923102424647](images/image-20220923102424647.png)

- 配置maven加速

~~~sh
# 新建目录
mkdir -p /var/lib/docker/volumes/jenkins-data/_data/appconfig/maven
/var/lib/docker/volumes/jenkins-data/_data # 是jenkins的挂载目录

vi settings.xml

<?xml version="1.0" encoding="utf-8"?>

<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">  
  <!-- localRepository
 | The path to the local repository maven will use to store artifacts.
 |
 | Default: ${user.home}/.m2/repository
 <localRepository>${user.home}/.m2</localRepository>
用户目录下的.m2是所有jar包的地方; maven容器内jar包的位置
 -->  
  <localRepository>/var/jenkins_home/appconfig/maven/.m2</localRepository>  
  <pluginGroups></pluginGroups>  
  <proxies></proxies>  
  <servers></servers>  
  <mirrors> 
    <mirror> 
      <id>nexus-aliyun</id>  
      <mirrorOf>central</mirrorOf>  
      <name>Nexus aliyun</name>  
      <url>http://maven.aliyun.com/nexus/content/groups/public</url> 
    </mirror> 
  </mirrors>  
  <profiles> 
    <profile> 
      <id>jdk-1.8</id>  
      <activation> 
        <activeByDefault>true</activeByDefault>  
        <jdk>1.8</jdk> 
      </activation>  
      <properties> 
        <maven.compiler.source>1.8</maven.compiler.source>  
        <maven.compiler.target>1.8</maven.compiler.target>  
        <maven.compiler.compilerVersion>1.8</maven.compiler.compilerVersion> 
      </properties> 
    </profile> 
  </profiles> 
</settings>
~~~



~~~sh
pipeline {
    agent any

    //定义一些环境信息
    environment {
        WS = "${WORKSPACE}"
        IMAGE_VERSION = "v1.0"
    }

    //定义流水线的加工流程
    stages {
        //流水线的所有阶段
        stage('环境检查') {
            steps {
                echo "环境检查..."
                sh 'printenv'
                echo "正在检测基本信息"
                sh 'java -version'
                sh 'git --version'
                sh 'docker version'
                sh 'pwd && ls -alh'
            }
        }
        //1、编译 "abc"
        stage('maven编译') {
            agent {
                docker {
                    image 'maven:3-alpine'
                    //将maven包挂载出去，下次再运行就不需要重复下载
                    args '-v /var/jenkins_home/appconfig/maven/.m2:/root/.m2'
                }
            }
            steps {
                echo "maven编译..."
                //git下载来的代码目录下
                sh 'pwd && ls -alh'
                sh 'mvn -v'
                //打包，jar.。默认是从maven中央仓库下载。 jenkins目录+容器目录；-s指定容器内位置
                //只要jenkins迁移，不会对我们产生任何影响
                sh "echo 默认的工作目录：${WS}"

                //workdir
                //每一行指令都是基于当前环境信息。和上下指令无关
                sh 'cd ${WS} && mvn clean package -s "/var/jenkins_home/appconfig/maven/settings.xml"  -Dmaven.test.skip=true '
                //这里可以配置把镜像jar包推送给maven repo ，nexus
            }
        }

        //2、测试，每一个 stage的开始，都会重置到默认的WORKSPACE位置
        stage('测试') {
            steps {
                echo "测试..."
            }
        }

        //3、打包
        stage('生成镜像') {
            steps {
                echo "打包..."
            }
        }

        //4、运行
        stage('运行') {
            steps {
                echo "运行..."
            }
        }
    }
}

~~~




临时容器导致的问题

- 1、第一次检出代码，默认在 /var/jenkins_home/workspace/【java-devops-demo】
- 2、使用docker临时agent的时候，每一个临时容器运行又分配临时目录/var/jenkins_home/workspace/java-devops-demo@2；默认就是workspace/java-devops-demo的内容
- 3、在临时容器里面 运行的mvn package命令，会在 /var/jenkins_home/workspace/java-devops-demo@2 进行工作
- 4、package到了 /var/jenkins_home/workspace/java-devops-demo@2 位置
- 5、进入下一步进行打包镜像，又会回到 /var/jenkins_home/workspace/【java-devops-demo】这个默认位置
- 6、这个位置没有运行过 mvn clean package ，没有target。 默认的 工作目录 没有 target