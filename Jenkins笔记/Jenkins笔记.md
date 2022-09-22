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

~~~groovy

~~~



