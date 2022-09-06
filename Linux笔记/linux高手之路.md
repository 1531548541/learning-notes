# VM配置静态ip

| 环境   | version                  |
| ------ | ------------------------ |
| centos | CentOS-7-x86_64-DVD-2009 |

~~~sh
#1.修改网络配置
vi /etc/sysconfig/network-scripts/ifcfg-ens33
~~~

![image-20220906163549077](images/image-20220906163549077.png)

~~~sh
#2.重启网络
systemctl restart network
~~~

3.修改VM配置

![image-20220902143805437](images/image-20220902143805437.png)

![image-20220902144025535](images/image-20220902144025535.png)

![image-20220902144110025](images/image-20220902144110025.png)

~~~sh
#4.验证
ip addr
~~~

# VM克隆

**注意：克隆之后注意要将mac重新生成，此时ip和源主机一样，所以先别登源主机，改完ip再登。**

![image-20220906153521772](images/image-20220906153521772.png)

~~~sh
#修改ip
vi /etc/sysconfig/network-scripts/ifcfg-ens33
~~~



# 防火墙、端口相关

~~~sh
#开启端口
firewall-cmd --zone=public --add-port=80/tcp --permanent
#已开启端口
firewall-cmd --list-port
#某个端口是否开启
firewall-cmd --query-port=80/tcp
#重启防火墙
firewall-cmd --reload
#关闭防火墙
systemctl stop firewalld.service
~~~

# rpm

~~~sh
#解压
rpm -Uvh xx.rpm
#查看某个rpm是否安装了
rpm -qa|grep xx
~~~

# unzip

~~~sh
#把/home目录下面的data目录压缩为data.zip
zip -r data.zip data #压缩data目录
#把/home目录下面的data.zip解压到databak目录里面
unzip data.zip -d databak
#/home目录下面的a文件夹和3.txt压缩成为a123.zip
zip -r a123.zip a 3.txt
#/home目录下面的t.zip直接解压到/home目录里面
unzip t.zip
#/home目录下面的a1.zip、a2.zip、a3.zip同时解压到/home目录里面
unzip a*.zip
#/home目录下面w.zip里面的所有文件解压到第一级目录
unzip -j wt.zip
~~~



# Kafka

~~~sh
#创建分区
bin/kafka-topics.sh --create --topic topicname --replication-factor 1 --partitions 1 --zookeeper localhost:2181
~~~

