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
//写流水线的脚本（声明式、脚本式）
pipeline{
    //全部的CICD流程都需要在这里定义

    //任何一个代理可用就可以执行
   // agent none  //以后所有stage都必须指定自己的
    agent any




    //定义一些环境信息
    environment {
      hello = "123456"
      world = "456789"
      WS = "${WORKSPACE}"
      IMAGE_VERSION = "v1.0"

//引用Jenkins配置的全局秘钥信息
//       ALIYUN_SECRTE=credentials("aliyun-docker-repo")
    }

    //定义流水线的加工流程
    stages {
        //流水线的所有阶段
        stage('环境检查'){
            steps {
               sh 'printenv'
               echo "正在检测基本信息"
               sh 'java -version'
//                sh 'git --version'
               sh 'docker version'
               sh 'pwd && ls -alh'
               sh "echo $hello"
               //未来，凡是需要取变量值的时候，都用双引号
               sh 'echo ${world}'
//                sh "ssh --help"
            }
        }
        //1、编译 "abc"
        stage('maven编译'){
            //jenkins不配置任何环境的情况下， 仅适用docker 兼容所有场景
            agent {
                docker {
                    image 'maven:3-alpine'
                    //将maven包挂载出去，下次再运行就不需要重复下载
                    args '-v /var/jenkins_home/appconfig/maven/.m2:/root/.m2'
//                     args  '-v /a/settings.xml:/app/settings.xml'
                    //docker run -v /a/settings.xml:/app/settings.xml
                 }
            }
            steps {
               //git下载来的代码目录下
               sh 'pwd && ls -alh'
               sh 'mvn -v'
               //打包，jar.。默认是从maven中央仓库下载。 jenkins目录+容器目录；-s指定容器内位置
               //只要jenkins迁移，不会对我们产生任何影响
               sh "echo 默认的工作目录：${WS}"
//                sh 'cd ${WS}'
               //workdir
               //每一行指令都是基于当前环境信息。和上下指令无关
               sh 'cd ${WS} && mvn clean package -s "/var/jenkins_home/appconfig/maven/settings.xml"  -Dmaven.test.skip=true '
               //jar包推送给maven repo ，nexus
               //如何让他适用阿里云镜像源

            }
        }

        //2、测试，每一个 stage的开始，都会重置到默认的WORKSPACE位置
        stage('测试'){
            steps {
                sh 'pwd && ls -alh'
                echo "测试..."
            }
        }

        //3、打包
        stage('生成镜像'){
            steps {
                echo "打包..."
                //检查Jenkins的docker命令是否能运行
                sh 'docker version'
                sh 'pwd && ls -alh'
                sh 'docker build -t jenkins-demo .'

                //镜像就可以进行保存


            }
        }



//          stage('推送镜像'){
//          //没有起容器代理，默认就是jenkins环境
//              //step里面卡点这么写
// //              input message: '需要推送远程镜像吗？', ok: '需要', parameters: [text(defaultValue: 'v1.0', description: '生产环境需要部署的版本', name: 'APP_VER')]
//
//              //step外面这么写
//              input {
//                  message "需要推送远程镜像吗?"
//                  ok "需要"
//                  parameters {
//                      string(name: 'APP_VER', defaultValue: 'v1.0', description: '生产环境需要部署的版本')
//                      choice choices: ['bj-01', 'sh-02', 'wuhan-01'], description: '部署的大区', name: 'DEPLOY_WHERE'
//                  }
//              }
//
//
//              steps {
//                 //false就直接结束
//
//                 echo "$APP_VER"
//
//
//                 //脚本方式进行判断，和具体逻辑
//                 // 远程触发，自动分析是哪个分支，如果是master就部署生产，dev就集成测试等
//                 // gulimall
//                 //    mall-order
//                 //    mall-user
//                 //    xx 100个微服务
//                 //  哪一个微服务代码提交了部署哪一个微服务，不用手工干预，只需要一个jenkinsfile
//                 //Generic Webhook Trigger 远程触发jenkins，jenkins获取当前提交的分支，和所有分支
//                 // gitee会告诉我们这次 add了哪些，modify了哪些，delete了哪些，
//                 // add了哪些，modify了哪些，delete了哪些， 自己判断这些修改的文件在哪个模块下
//                 //对这个模块进行持续集成
//
//                 //Generic Webhook Trigger + script脚本 + 其他已讲过的内容 + 其他自己看看官网 = 搞定
//                 //别人提交一个issue，jenkins触发 自动把issue做成 fix分支，让程序员自己去修改
//                 //所有东西都是模板
//                 script {
//                    //groovy
//                    def  where = "${DEPLOY_WHERE}"
//
//                    if (where == "bj-01"){
//                     sh "echo 我帮你部署到 bj-01 区了"
//                    }else if(where == "sh-02"){
//                    sh "echo 我帮你部署到 sh-02 区了"
//                    }else{
//                         sh "echo 没人要的，我帮你部署到 wuhan-01 区了"
// //                    sh "docker push registry.cn-hangzhou.aliyuncs.com/lfy/java-devops-demo:${APP_VER}"
//                         withCredentials([usernamePassword(credentialsId: 'aliyun-docker-repo', passwordVariable: 'ali_pwd', usernameVariable: 'ali_user')]) {
//                             // some block
//                              sh "docker login -u ${ali_user} -p ${ali_pwd}   registry.cn-hangzhou.aliyuncs.com"
// //                              sh "docker tag java-devops-demo registry.cn-hangzhou.aliyuncs.com/lfy/java-devops-demo:${APP_VER}"
//                         }
//
//                         //ssh 秘钥文件配置到 jenkins 全局秘钥中
//                         withCredentials(ssh){
//                           //ansible 没有
// //                           sh "ssh root@xxxx  "
//                           //不应该的操作。
// //                           sh "远程操作其他机器。。。。"
//
//                           //k8s集群
//                           //动态切换k8s集群
//
//                         }
//                    }
//                 }
//
// //                 sh "docker push registry.cn-hangzhou.aliyuncs.com/lfy/java-devops-demo:${APP_VER}"
//
//              }
//          }

        //4、部署
        stage('部署'){
            steps {
                echo "部署..."
                sh 'docker rm -f jenkins-demo'
                sh 'docker run -d -p 80:8080 --name jenkins-demo jenkins-demo'
            }

            //后置执行
//             post {
//               failure {
//                 // One or more steps need to be included within each condition's block.
//                 echo "炸了.. ."
//               }
//
//               success {
//                 echo "成了..."
//               }
//             }
        }

        //5、推送报告
//         stage("发送报告"){
//             steps {
//                 //短信通知，购买api接口即可
// //                 sh 'curl -i -k -X POST 'https://gyytz.market.alicloudapi.com/sms/smsSend?mobile=mobile&param=**code**%3A12345%2C**minute**%3A5&smsSignId=2e65b1bb3d054466b82f0c9d125465e2&templateId=908e94ccf08b4476ba6c876d13f084ad'  -H 'Authorization:APPCODE dddddddd''
//                 //REST API 所有都行
// //                 sh 'curl '
//                 echo '准备发送报告'
//                 emailext body: '''<!DOCTYPE html>
//                 <html>
//                 <head>
//                 <meta charset="UTF-8">
//                 <title>${ENV, var="JOB_NAME"}-第${BUILD_NUMBER}次构建日志</title>
//                 </head>
//
//                 <body leftmargin="8" marginwidth="0" topmargin="8" marginheight="4"
//                     offset="0">
//                     <table width="95%" cellpadding="0" cellspacing="0"  style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">
//                 <h3>本邮件由系统自动发出，请勿回复！</h3>
//                         <tr>
//                            <br/>
//                             各位同事，大家好，以下为${PROJECT_NAME }项目构建信息</br>
//                             <td><font color="#CC0000">构建结果 - ${BUILD_STATUS}</font></td>
//                         </tr>
//                         <tr>
//                             <td><br />
//                             <b><font color="#0B610B">构建信息</font></b>
//                             <hr size="2" width="100%" align="center" /></td>
//                         </tr>
//                         <tr>
//                             <td>
//                                 <ul>
//                                     <li>项目名称 ： ${PROJECT_NAME}</li>
//                                     <li>构建编号 ： 第${BUILD_NUMBER}次构建</li>
//                                     <li>触发原因： ${CAUSE}</li>
//                                     <li>构建状态： ${BUILD_STATUS}</li>
//                                     <li>构建日志： <a href="${BUILD_URL}console">${BUILD_URL}console</a></li>
//                                     <li>构建  Url ： <a href="${BUILD_URL}">${BUILD_URL}</a></li>
//                                     <li>工作目录 ： <a href="${PROJECT_URL}ws">${PROJECT_URL}ws</a></li>
//                                     <li>项目  Url ： <a href="${PROJECT_URL}">${PROJECT_URL}</a></li>
//                                 </ul>
//
//
//                 <h4><font color="#0B610B">最近提交</font></h4>
//                 <ul>
//                 <hr size="2" width="100%" />
//                 ${CHANGES_SINCE_LAST_SUCCESS, reverse=true, format="%c", changesFormat="<li>%d [%a] %m</li>"}
//                 </ul>
//                 详细提交: <a href="${PROJECT_URL}changes">${PROJECT_URL}changes</a><br/>
//
//                             </td>
//                         </tr>
//                     </table>
//                 </body>
//                 </html>''', subject: '${ENV, var="JOB_NAME"}-第${BUILD_NUMBER}次构建日志', to: '17512080612@163.com'
//             }
//         }

        stage('部署到生产环境吗？'){
            steps {
                // 手动输入版本【参数化构建】,推荐生成器

//                 input {
//                     message "需要部署到生产环境吗?"
//                     ok "是的，赶紧部署"
// //                     submitter "alice,bob"
//                     parameters {
//                         //手动传入的参数
//                         string(name: 'APP_VERSION', defaultValue: 'v1.0', description: '请指定生产版本号')
//                     }
//                 }
                sh "echo 发布版本咯......"
                // 版本的保存。代码的保存。镜像的保存。存到远程仓库

            }
        }

    }



//后置处理过程
        post {
          failure {
            echo "这个阶段 完蛋了.... $currentBuild.result"
          }
          success {
            echo "这个阶段 成了.... $currentBuild.result"
          }
        }


}

~~~




临时容器导致的问题

- 1、第一次检出代码，默认在 /var/jenkins_home/workspace/【jenkins-demo】
- 2、使用docker临时agent的时候，每一个临时容器运行又分配临时目录/var/jenkins_home/workspace/jenkins-demo@2；默认就是workspace/jenkins-demo的内容
- 3、在临时容器里面 运行的mvn package命令，会在 /var/jenkins_home/workspace/jenkins-demo@2 进行工作
- 4、package到了 /var/jenkins_home/workspace/jenkins-demo@2 位置
- 5、进入下一步进行打包镜像，又会回到 /var/jenkins_home/workspace/【jenkins-demo】这个默认位置
- 6、这个位置没有运行过 mvn clean package ，没有target。 默认的 工作目录 没有 target