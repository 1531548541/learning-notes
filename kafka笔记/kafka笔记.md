# 安装（集群）

**环境介绍**

| 名称                             | 版本       |
| -------------------------------- | ---------- |
| linux                            | centos7    |
| kafka                            | 2.12-3.0.0 |
| jdk                              | 8          |
| zookeeper（新版kafka，可不装zk） | 3.8        |
| rsync                            | 3.1.2      |

**0.环境准备(可选)**

~~~sh
#配置主机名
$ vim /etc/hosts

192.168.200.128 node01
192.168.200.129 node02
192.168.200.130 node03

$ vim /etc/hostname
node01


#配置 ssh
$ vim /etc/ssh/sshd_config

# 允许登录 root 用户
PermitRootLogin yes
# 允许密码登录
PasswordAuthentication yes


#重启 ssh 服务
$ service ssh restart

~~~

~~~sh
#安装rsync(依赖gcc，make需要下perl，若没有则需要安装)
cd rsync-2.6.9
./configure --prefix=/usr/local/rsync
make && make install 
~~~

- 克隆虚拟机（移除网络适配器重新添加，避免mac冲突）

- 免密登录(所有机子间都必须配置免密登录)

生成公私钥

```shell
# 默认位置在 ~/.ssh
$ ssh-keygen

#id_rsa 是私钥，id_rsa.pub 是公钥
#把生成的公钥复制给要登录的机子上 ~/.ssh/authorized_keys
#配置多个只要另起一行就行

#！！！！！！！！！！！一定要通过这种方式添加公钥到authorized_keys！！！！！！！！！！！！！！！！！
$ ssh-copy-id -i /root/.ssh/id_rsa.pub root@IP
```

- 分发脚本

~~~sh
#分发脚本
$ vim /bin/xsync

#!/bin/bash
#1 获取输入参数个数，如果没有参数，直接退出
pcount=$#
if((pcount==0)); then
echo no args;
exit;
fi

#2 获取文件名称
p1=$1
fname=`basename $p1`
echo fname=$fname

#3 获取上级目录到绝对路径
pdir=`cd -P $(dirname $p1); pwd`
echo pdir=$pdir

#4 获取当前用户名称
user=`whoami`

#5 循环，分发到 node01 ~ node03
for((i=1; i<=3; i++)); do
echo ------------------- node0$i --------------
        rsync -rvl $pdir/$fname $user@node0$i:$pdir
done
~~~

~~~sh
$ chmod 777 /bin/xsync
~~~

将分发脚本分发下去

~~~sh
$ xsync /bin/xsync
~~~



**1.安装jdk**

~~~sh
$ vim /etc/profile
# 拷贝以下内容
export JAVA_HOME=/opt/jdk1.8.0_202
export JRE_HOME=$JAVA_HOME/jre
export PATH=$PATH:$JAVA_HOME/bin
export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar

# 刷新配置
$ source /etc/profile

$ java -version # 出现以下结果，表示配置成功
java version "1.8.0_202"
Java(TM) SE Runtime Environment (build 1.8.0_202-b08)
Java HotSpot(TM) 64-Bit Server VM (build 25.202-b08, mixed mode)

# 配置成功后分发（为了同步其他node）
$ xsync /opt/jdk1.8.0_202
$ xsync /etc/profile
~~~

**2.安装zookeeper集群**

~~~sh
$ vim /etc/profile
# 拷贝以下内容
export ZK_HOME=/opt/zookeeper-3.8.0
export PATH=$PATH:$JAVA_HOME/bin:$ZK_HOME/bin

$ xsync /opt/zookeeper-3.8.0
$ xsync /etc/profile
~~~

修改 zoo.cfg

~~~sh
$ cd /opt/zookeeper-3.8.0
$ mkdir zkData
$ cd conf
$ mv zoo_sample.cfg zoo.cfg
$ vim zoo.cfg

dataDir=/opt/zookeeper-3.8.0/zkData
server.1=node01:2888:3888
server.2=node02:2888:3888
server.3=node03:2888:3888

$ xsync /opt/zookeeper-3.8.0/conf/zoo.cfg
~~~

配置 myid：填写上面 `server.x` 中对应的数字 `x`，如：1、2、3。每个机子都不一样

~~~sh
$ vim /opt/zookeeper-3.8.0/zkData/myid
~~~

**3.安装kafka集群**

~~~sh
$ vim /etc/profile
# 尾部添加以下内容
export KAFKA_HOME=/opt/kafka-3.2.0
export PATH=$PATH:$KAFKA_HOME/bin

$ xsync $KAFKA_HOME
$ xsync /etc/profile
~~~

修改解压文件中的 service.properties(broker、主机、log目录)

````shell
$ vim $KAFKA_HOME/config/server.properties

# broker 全局唯一编号，每个node不能重复
broker.id=0

log.dirs=/opt/module/kafka/datas

zookeeper.connect=kafka01:2181,kafka02:2181,kafka03:2181/kafka
````

**4.启动**

~~~sh
#集群启动脚本
$ vim /bin/xcall


#!/bin/bash
pcount=$#
if((pcount==0));
then
        echo "command can not be null !"
        exit
fi

user=`whoami`

for ((i = 1; i <= 3; i++))
do
        echo ---------------- node0$i ----------------
        ssh $user@node0$i 'source /etc/profile;'$@
done

echo --------------- complete ---------------
~~~

~~~sh
$ chmod 777 /bin/xcall
$ xsync /bin/xcall
~~~



- ZK 集群启动

~~~sh
$ xcall zkServer.sh start


$ xcall zkServer.sh status
---------------- node01 ----------------
ZooKeeper JMX enabled by default
Using config: /opt/module/zookeeper-3.8.0/bin/../conf/zoo.cfg
Client port found: 2181. Client address: localhost. Client SSL: false.
Mode: follower
---------------- node02 ----------------
ZooKeeper JMX enabled by default
Using config: /opt/module/zookeeper-3.8.0/bin/../conf/zoo.cfg
Client port found: 2181. Client address: localhost. Client SSL: false.
Mode: leader
---------------- node03 ----------------
ZooKeeper JMX enabled by default
Using config: /opt/module/zookeeper-3.8.0/bin/../conf/zoo.cfg
Client port found: 2181. Client address: localhost. Client SSL: false.
Mode: follower
--------------- complete ---------------
~~~

- kafka集群启动

~~~sh
# 启动
$ xcall kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties
# 关闭
$ xcall kafka-server-stop.sh


#或者写个脚本
$ vi /bin/kafka

#!/bin/bash
case $1 in
"start"){
	for i in node01 node02 node03
	do 
		echo "------启动 $i kafka-----"
		ssh $i "source /etc/profile;$KAFKA_HOME/bin/kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties"
	done
}
;;
"stop"){
	for i in node01 node02 node03
	do 
		echo "------停止 $i kafka-----"
		ssh $i "$KAFKA_HOME/bin/kafka-server-stop.sh"
	done
}
;;
esac
~~~

**注意：停止 Kafka 集群时，一定要等 Kafka 所有节点进程全部停止后再停止 Zookeeper 集群。因为 Zookeeper 集群当中记录着 Kafka 集群相关信息，Zookeeper 集群一旦先停止，Kafka 集群就没有办法再获取停止进程的信息，只能手动杀死 Kafka 进程了。**



检验是否ok

~~~sh
# 创建 topic
$ kafka-topics.sh --bootstrap-server node01:9092,node02:9092,node03:9092 --create --partitions 3 --replication-factor 3 --topic hello
# 创建生产者
$ kafka-console-producer.sh --bootstrap-server node01:9092,node02:9092,node03:9092 --topic hello
# 创建消费者
$ kafka-console-consumer.sh --bootstrap-server node01:9092,node02:9092,node03:9092 --topic hello
~~~

可能出现的问题

- 内网环境，缺各种tar包，如gcc、perl等

- 配置完静态ip，ifconfig看不到ip，重启网络失败:Job for network.service failed

~~~sh
#解决：
#关闭 NetworkManger
service NetworkManager stop
#并且禁止开机启动 
chkconfig NetworkManager off
#之后重启就好了
~~~

- 配置了ssh免密登录，但还要输入密码

~~~sh
文件权限、目录是否 ~/.ssh/authorized_keys 、是否是~/.ssh/id_rsa.pub中的公钥、删除known_hosts

#踩大坑：
如果还不行，删除.ssh文件，重新创建，并把权限设置700
#一定要通过这种方式添加公钥到authorized_keys
ssh-copy-id -i /root/.ssh/id_rsa.pub root@IP
~~~

- 启动zookeeper和kafka时，看日志。Connection refused

~~~sh
#防火墙关了，或者开启对应的端口
~~~



# 介绍

1. **消息系统**： kafka 和传统的消息系统（也称作消息中间件〉都具备系统解稿、冗余存储、流量削峰、缓冲、异步通信、扩展性、 可恢复性等功能。与此同时， Kafka供了大多数消息系统难以实现的消息 序性保障及回溯消费的功能。
2. **存储系统**： Kafka 把消息持久化到磁盘，相比于其他基于内存存储的系统而言，有效地降低了数据丢失的风险 也正是得益于 Kafka 的消息持久化功能和多副本机制，我们可以把 Kafka 作为长期的数据存储系统来使用，只需要把对应的数据保留策略设置为“永久”或启用主题的日志压缩功能即可。
3. **流式处理平台**： Kafka 不仅为每个流行的流式处理框架提供了可靠的数据来源，还供了一个完整的流式处理类库，比如窗口、连接、变换和聚合等各类操作。

# kafka架构

### 一些概念

**分区**：topic下有多个分区，可以跨节点

**副本**：一个分区有多个副本，一主多从，leader负责读写，follower们负责从leader拉取数据保持同步。

![image-20220610135801493](images\image-20220610135801493.png)



### Leader选举流程

![image-20220615135850269](images\image-20220615135850269.png)



### Leader故障处理细节

![image-20220615140438511](images\image-20220615140438511.png)



### Follower故障处理细节

![image-20220615140242257](images\image-20220615140242257.png)



### 文件清理策略

> Kafka 中默认的日志保存时间为 7 天，可以通过调整如下参数修改保存时间。
>
> - log.retention.hours，最低优先级小时，默认 7 天。 
>
> - log.retention.minutes，分钟。 
>
> - log.retention.ms，最高优先级毫秒。 
>
> - log.retention.check.interval.ms，负责设置检查周期，默认 5 分钟。
>
> 
>
> **那么日志一旦超过了设置的时间，怎么处理呢？**
>
> Kafka 中提供的日志清理策略有 delete 和 compact 两种。 
>
> 1）delete 日志删除：将过期数据删除
>
> - log.cleanup.policy = delete 所有数据启用删除策略
>
> （1）基于时间：默认打开。以 segment 中所有记录中的最大时间戳作为该文件时间戳。
>
> （2）基于大小：默认关闭。超过设置的所有日志总大小，删除最早的 segment。
>
> log.retention.bytes，默认等于-1，表示无穷大。
>
> 2）compact 日志压缩：对于相同key的不同value值，只保留最后一个版本。(**用得少**)
>
> - log.cleanup.policy = compact 所有数据启用压缩策略
>
> ![image-20220615142825667](images\image-20220615142825667.png)
>
> ​	压缩后的offset可能是不连续的，比如上图中没有6，当从这些offset消费消息时，将会拿到比这个offset大 
>
> 的offset对应的消息，实际上会拿到offset为7的消息，并从这个位置开始消费。
>
> ​	这种策略只适合特殊场景，比如消息的key是用户ID，value是用户的资料，通过这种压缩策略，整个消息
>
> 集里就保存了所有用户最新的资料。 

### **高效读写数据**

**1**）**Kafka** **本身是分布式集群，可以采用分区技术，并行度高**

**2**）**读数据采用稀疏索引，可以快速定位要消费的数据**

**3**）**顺序写磁盘**

Kafka 的 producer 生产数据，要写入到 log 文件中，写的过程是一直追加到文件末端，

为顺序写。**官网有数据表明**，同样的磁盘，顺序写能到 600M/s，而随机写只有 100K/s。这

与磁盘的机械机构有关，顺序写之所以快，是因为其省去了大量磁头寻址的时间。

![image-20220615143644474](images\image-20220615143644474.png)

**4）页缓存** **+** **零拷贝技术**

![image-20220615143758698](images\image-20220615143758698.png)

| 参数                        | 描述                                                         |
| :-------------------------- | ------------------------------------------------------------ |
| log.flush.interval.messages | 强制页缓存刷写到磁盘的条数，默认是 long 的最大值，一般不建议修改。 |
| log.flush.interval.ms       | 每隔多久，刷数据到磁盘，默认是 null。一般不建议修改。        |



## 生产者（是线程安全的）

### 核心参数

> - **acks**:ACK应答级别
>
>   1. acks=0：生产者发送的数据不需要等数据落盘应答（类似异步）
>   2. acks=1：leader收到数据后应答
>   3. acks=-1(all)：leader和ISR队列中所有节点收齐数据后应答。并不意味着消息就一定可靠，因JSR 中可能只有 leader 副本，就退化成了acks=1，要获得更高的消息可靠性需要配合 min.insync.replicas 等参数的联动。
>
>   **合理情况：ACK级别为-1 + 分区副本>=2 + ISR里应答的最小副本数>=2**
>
>   > 生产环境中，acks=0很少使用；acks=1一般用于传输普通日志，允许丢个别数据；acks=-1一般用于传输和钱相关的信息，保证高可靠性。
>   >
>   > **注意！！！ acks=-1会导致重复数据，leader挂了选举新的leader后，由于还未ack，收到重复消息**
>
> - **max.request.size**：生产者能发送的消息的最大数,默认1MB。并不建议盲目地增大这个参数的配置值，尤其是在对 Kafka 整体脉络没有足够把控的时候。因为这个参数还涉及一些其参数的联动，比如 broker 端的 message.max.bytes 参数，如果配置错误可能会引起一些不必要的异常 比如将 broker 端的 message.max.bytes 参数配置为 10 ，而 max.request.size参数配置为20，那么当发送一条大小为 15B 的消息时，生产者客户端报异常。
>
> - **max.in.flight.requests.per.connection**：限制客户端与Node间连接最多缓存的请求数，默认5。
>
> - **retries和retry.backoff.ms**：retries 参数用来配置生产者重试的次数，默认值为0，即在发生异常的时候不重试。重试还和另一个参数 retry.backoff.ms 有关，这个参数的默认值为 100。它用来设定两次重试之间的时间间隔，避免无效的频繁重试。在配置 retries和retry.backoff.ms之前，最好先估算一下可能的异常恢复时间，这样可以设定总的重试时间大于这个异常恢复时间，以此来避免生产者过早地放弃重试。
>
>   ​	如果将 acks 参数配置为非零值，并且 max.in.flight.requests.per.connection 参数配置为大于1的值，那么就会出现错序的现象。如果第一批消息写入失败，而第二批次消息写入成功，那么生产者会重试发送第一批次的消息， 此时如果第一次的消息写入成功，那么这两个批次的消息就出现了错序，一般而言，在需要保证消息顺序的场合建议把参数max.in.flight.requests.per.connection 配置为 1，而不是把 acks 配置为0，不过这样也会影响整体的吞吐。
>
> -  **linger.ms**：等待时间。这个参数用来指定生产者发送 ProducerBatch 之前等待更多消息（ ProducerRecord ）加入Producer Batch 时间，默认值为0。生产者客户端会在 ProducerBatch 填满或等待时间超过linger.ms 值时发送出去。增大这个参数的值会增加消息的延迟，但是同时能提升一定的吞吐量。 这个linger.ms 参数与 TCP 协议中的 Nagle 算法有异曲同工之妙。建议设置为5-100ms。
>
> -  **transactional. id**：事务id，全局唯一，默认null。
>
> - **max.block.ms** ：send和parititonsFor方法的阻塞时间，默认60s。当生产者的发送缓冲区满了，或者没有可用的元数据时，这些方法就会阻塞。
>
> - **receive.buffer.bytes**：Socket接收消息缓冲区大小，默认32KB。如果设置为-1，则使用操作系统的默认值。如果 Producer和Kafka处于不同的机房，则可以适当调大这个参数值。
>
> - **send.buffer.bytes：Socket** 发送消息缓冲区大小，默认128KB。如果设置为-1，则使用操作系统的默认值。
>
> - **request.timeout.ms**：默认30s。注意这个参数需要比broker端的replica.lag.time.max.ms值要大，这样可以减少因客户端重试而引起的消息重复的概率。
>
> - **batch.size**:批次大小，用于指定 ProducerBatch 可以复用内存区域的大小，默认16k
>
> - **compression.type**:消息压缩类型，默认none
>
> - **connections.max.idle.ms**：多久后关闭连接，默认9分钟
>
> - **RecordAccumulator**:缓冲区大小，修改为64m
>
> - **metadata.max.age.ms**：如果在这个时间内元数据没有更新的话会被强制更新，默认5分钟。

### 架构

![image-20220811190637461](images\image-20220811190637461.png)

> - 整个生产者客户端由两个线程协调运行，这两个线程分别为**主线程**和 **Sender** 线程。
>
> - **RecrdAccumulator 主要用来缓存消息 Sender 线程可以批量发送，大小由buffer.memory参数决定，默认32MB。**如果生产者发送消息的速度超过发送到服务器的速度 ，则会导致生产者空间不足，这个时候 KafkaProducer send（） 方法调用要么被阻塞，要么抛出异常，这个取决于参数 max.block.ms 的配置，此参数的默认值为60000（60秒）。
>
> - 主线程中发送过来的消息都会被迫加到 RecordAccumulator 的某个双端队列（ Deque<ProducerBatch＞）中。消息在网络上都是以Byte的形式传输的，在发送之前需要创建一块内存区域来保存对应的消息 。在 Kafka 产者客户端中，通过 java.io.ByteBuffer 实现消息内存的创建和释放。不过频繁的 建和释放是比较耗费资源的，在 RecordAccumulator 的内部还有一个 BufferPool,它主要用来实现 ByteBuffer 的复用，以实现缓存的高效利用 。不过BufferPool 只针对特定大小的ByteBuffer 进行管理，而其他大小的 ByteBuffer 不会缓存进 BufferPool 中，这个特定的大小batch.size 参数来指定，默认值为 16384B ，即 16KB。我们可以适当地调大 batch.size参数以便多缓存一些消息。
>
>   ​	ProducerBatch 大小和 batch.size 参数也有着密切的关系。当一条消息（ProducerRecord ) 流入 RecordAccumulator 时，会先找与消息分区所对应的双端队列（如果没有则新建），再从这个双端队列的尾部获取一个 ProducerBatch （如果没有则新建），查看 ProducerBatch 中是否还可以写入这个 ProducerRecord ，如果可以则写入，如果不可以则需要 建一个新ProducerBatch 。在新建 ProducerBatch时评估这条消息的大小是否超过 batch.size 参数大小，如果不超过，那么就以 batch.size 参数的大小来创建 ProducerBatch ，这样在使用完这段内存区域之后，可以通过 BufferPool 的管理来进行复用；如果超过，那就以评估的大小来创建ProducerBatch ，这段内存区域不会被复用。
>
>   ​	Sender从RecordAccumulator 获取缓存的消息之后，会进一步将原本<分区, Deque<ProducerBatch>>的保存形式转变成＜Node,List< ProducerBatch>>的形式，其中 Node 表示 Kafka集群 broker 节点 。对于网络连接来说，生产者客户端是与具体 broker 节点建立的连接，也就是向具体的 broker 节点发送消息，而并不关心消息属于哪一个分区；而对于 KafkaProducer的应用逻辑而言，我们只关注向哪个分区中发送哪些消息，所以在这里需要做一个应用逻辑层面到网络IO层面的转换。
>
>   ​	在转换成＜Node, List ProducerBatch>>的形式之后， Sender会进一步封装成＜Node,Request> 的形式，这样就可以将Request请求发往各个Node了， 这里Request是指Kafka的各种协议请求，对于消息发送而言就是指具体的 ProduceRequest。
>
>   ​	请求在从Sender 线程发往Kafka之前还会保存到 InFlightRequests 中， InFlightRequests保存对象的具体形式为 Map<Nodeld, Deque<Request>＞，它的**主要作用是缓存了已经发出去但还没有收到响应的请求**。与此同时，InFlightRequests 还提供了许多管理类的方法，并且通过配置参数还可以限制每个连接（也就是客户端与 Node 之间的连接）最多缓存的请求数。**这个配置参数为 max.in.flight.requests.per.connection ，默认值为5，即每个连接最多只能缓存5个未响应的请求**，超过该数值之后就不能再向这个连接发送更多的请求了，除非有缓存的请求收到了响应（ Response ）。通过比Deque<Request> 的size 与这个参数的大小来判断对应的 Node 中是否己经堆积了很多未响应的消息，如果真是如此，那么说明这个 Node 节点负载较大或网络连接有问题，再继续向其发送请求会增大请求超时的可能。
>

### 元数据更新

![image-20220812103411850](images/image-20220812103411850.png)

​	InFlightRequests 还可以获得 leastLoadedNode ，即所有 Node 中负载最小的那一个。这里的负载最小是通过每个 Node在InFlightRequests 中还未确认的请求决定的，未确认的请求越多则认为负载越大。对于图 2-2 中的 InFlightRequests 来说，图中展示了三个节点Node0 Node1 Node2，很明显 Node1 负载最小。也就是说， Node1为当前的 leastLoadedNode。选择leastLoadedNode 发送请求可以使它能够尽快发出，避免因网络拥塞等异常而影响整体的进度。 leastLoadedNode 的概念可以用于多个应用场合，比如元数据请求、消费者组播协议的交互。

​	当客户端中没有需要使用的元数据信息时，比如没有指定的主题信息，或者超metadata.max.age.ms 时间没有更新元数据都会引起元数据的更新操作 。客户端参数metadata.max.age.ms 的默认值为 300000 ，即5分钟。元数据的更新操作是在客户端内部进行的，对客户端的外部使用者不可见。当需要更新元数据时，会先挑选出 leastLoadedNode, 然后向这个Node 发送 MetadataRequest 请求来获取具体的元数据信息。这个更新操作是由 Sender线程发起的， 建完MetadataRequest 之后同样会存入InF!ightRequests ，之后的步骤就和发送消息时的类似。元数据虽然由 Sender 线程负责更新，但是主线程也需要读取这些信息，这里的数据同步通过 synchronized final关键字来保障。

### 发送过程

![image-20220610142448052](images\image-20220610142448052.png)

### 生产者分区策略

kafka具体会按以下几种情况选择分区分配策略：

1. 如果发送消息时指定分区，就按指定的分区投递消息
2. 没有指定分区，但有key，则hash(key)%分区数
3. 既没有指定分区，也没有key，采用轮询方式选择一个分区

### 数据重复

> 问题出现：acks=-1会导致重复数据，leader挂了选举新的leader后，由于还未ack，收到重复消息

重复数据的判断依据：具有<PID,Partition,SeqNumber>相同主键的消息提交时，Broker只会持久化一次。其中PID在Kafka每次重启都会更新，SeqNumber单调递增。

解决：

1. 幂等性：参数enable.idempotence=true（默认开启）,由于重启后PID变了，所以幂等性无法真正过滤重复数据。
2. 事务

![image-20220610155147302](images\image-20220610155147302.png)

### 数据有序性

单分区下的：

![image-20220610160418578](images\image-20220610160418578.png)

多分区下的：

​	收到所有分区的数据进行排序（人为干预）



### zookeeper中的kafka信息

![image-20220614105659275](images\image-20220614105659275.png)



## 消费者

### 消费者组

> 拥有同一个groupid的消费者即是一组
>
> - 消费者组内每个消费者负责消费不同分区的数据，**一个分区只能由组内的一个消费者消费**
> - 消费者组之间互不影响

### 一些概念

> - 点对点、发布订阅模式都支持。如果所有的消费者都隶属于同1个消费组，那么所有的消息都会被均衡地投递给每个消费者，即每条消息只会被一个消费者处理，这就相当于点对点模式的应用如果所有的消费者都隶属于不同的消费组，那么所有的消息都会被广播给所有的消费者，即每条消息会被所有的消费者处理，这就相当于发布订阅模式的应用。
> - subscribe和assign都能订阅topic，但subscribe方法具有消费者自动再均衡功能（更强大）。

### 核心参数

> - auto.offset.reset ：默认latest。参数值为字符串类型，有效值为“earliest ”、＂latest“、”none ”，配置为其余值会报异常
>
> - enable.auto.commit：是否开启自动提交消费位移，默认true。当然不是每消费1条消息就提交，而是定期提交，频率由auto.commit.interval.ms控制
>
> - auto.commit.interval.ms：表示开启自动提交消费位移时的时间间隔，默认5000ms
>
> - partition.assignment.strategy ：消费者的分区分配策略，默认org.apache.kafka.clients.consumer.RangeAssignor 
>
> - reconnect.backoff.ms：试重新连接指定主机之前的等待时间（也称为退避时间，避免频繁地连接主机，默认值为 50 ms ）。这种机制适用于消费者向 broker 发送的所有请求。
>
> - retry.backoff.ms：这个参数用来配置尝试重新发送失败的请求到指定的主题分区之前的等待（退避〉时间，避免在某些故障情况下频繁地重复发送，默认值为 100 (ms ）。
>
> - isolation.level：这个参数用来配置消费者的事务隔离级别。字符串类型，有效值为“read uncommitted ，和“ read committed ＂，表示消费者所消费到的位置，如果设置为“read committed ”，那么消费者就会忽略事务未提交的消息，即只能消费到 LSO ( LastStableOffset ）的位置，默认情况下为“ read_ uncommitted ”，即可以消费到 HW (High Watermark ）处的位置。
>
> - max.poll.interval.ms：默认5分钟，当通过消费组管理消费者时，该配置指定拉取消息线程最长空闲时间，若超过这个时间间隔还没有发起 poll 操作，则消费组认为该消费者己离开了消费组，将进行再均衡操作
>
> - fetch.min.bytes：消费者在一次拉取请求中能拉取的最小数据量，默认1B
>
> - fetch.max.bytes：消费者在一次拉取请求中能拉取的最大数据量，默认50MB。该参数设定的不是绝对的最大值，如果在第一个非空分区中拉取的第一条消息大于该值，那么该消息将仍然返回，以确保消费者继续工作 也就是说，上面问题的答案是可以正常消费。与此相关的，kafka 中所能接收的最大消息 大小通过服务端参数 message max bytes （对应于主题端参数 max.message bytes ）来设置。
>
> - fetch.max.wait.ms：这个参数也和 fetch.min bytes 参数有关，如果 Kafka 仅仅参考 fetch.min.byte参数的要求，那么有可能会 直阻塞等待而无法发送响应 Consumer 显然这是不合理的fetch.max.wait.ms 参数用于指定 Kafka 的等待时间，默认值为 500 ）。如果 Kafka没有足够多的消息而满足不了 fetch min bytes 参数的要求，那么最终会等待 500m 这个参数 设定和 Consumer Kafka 间的延迟也有关系，如果业务应用对延迟敏感，那么可以适当调小这个参数
>
> - max.partition.fetch.bytes：这个参数用来配置从每个分区里返回给 Co sum町的最大数据 ，默认值为 1MB 。这个参数与 fetch.max.bytes 参数相似，只不过前者用来限制一次拉取中每个分区的消息大小，而后者用来限制一次拉取中整体消息的大小。同样，如果这个参数设定的值比消息的大小要小，那么也不会造成无法消费， Kafka 为了保持消费逻辑的正常运转不会对此做强硬的限制。
>
> - max.poll.records：消费者在一次拉取请求中拉取的最大消息数，默认500条。
>
> - connections.max.idle.ms：多久之后关闭限制的连接，默认值是 540000 (ms ），即9分钟。
>
> - exclude.internal.topics：内部主题是否向消费者公开，默认 true。Kafka 中有两个内部的主题： __ consumer_offsets和  __transaction_state
>
>   如果设置 true ，那么只能使用 subscribe( Collection）的方式而不能使用 subscribe(Pattern）的方式来订阅内部主题，设置为false 则没有这个限制。
>
> - receive.buffer.bytes：socket接收消息缓冲区的大小，默认64KB，如果设置为-1则使用操作系统默认值。如果 Consumer与 Kafka 处于不同的机房，则可以适当调大这个参数值。
>
> - send.buffer.bytes：socket发送消息缓冲区的大小，默认64KB，如果设置为-1则使用操作系统默认值。
>
> - request.timeout.ms：消费者等待请求响应时间，默认30s。
>
> - metadata.max.age.ms：这个参数用来配置元数据的过期时间，默认值为 300000 ms ），即 5分钟。如果元数据在此参数所限定的时间范围内没有进行更新，则会被强制更新，即使没有任何分区变化或有新的broker 加入。

### 消费过程

![image-20220615144405848](images\image-20220615144405848.png)

![image-20220615144712847](images\image-20220615144712847.png)



### 消费者分区策略

- RangeAssignor 分配策略（**默认**）：基于topic，提前分配好方案，尽可能均匀分配，不够分给前面的消费者多分一个**分区**。

  > 假设同一消费者组中2个消费者订阅2个主题，每个有3个分区。分配如下：
  >
  > 消费者1: t0p0、t0p1、t1p0、t1p1
  >
  > 消费者2: t0p2、t1p2 

- RoundRobinAssignor 分配策略：一个个轮询分

  >如果同一个消费组内所有的消费者的订阅信息都是相同的，那么分区分配会是均匀的。
  >
  >假设同一消费者组中2个消费者订阅2个主题，每个有3个分区。分配如下：
  >
  >消费者1: t0p0、t0p2、t1p1
  >
  >消费者2: t0p1、t1p0、t1p2
  >
  > 
  >
  >分配不均的情况：假设同一消费者组中3个消费者，3个主题，分区数分别是1、2、3。消费者1订阅t0，消费者2订阅t0、t1，消费者3订阅t0、t1、t2，分配如下：
  >
  >消费者1: t0p0
  >
  >消费者2: t1p0
  >
  >消费者3: t1p1、t2p0、t2p1、t2p2

- StickyAssignor 分配策略：优先保留原有分配，在此基础上轮询分配

  >假设同一消费者组中3个消费者，4个主题，每个有2个分区。分配如下：
  >
  >消费者1: t0p0、t1p1、t3p0
  >
  >消费者2: t0p1、t2p0、t3p1
  >
  >消费者3: t1p0、t2p1
  >
  > 
  >
  >此时消费者2挂了，分配如下：
  >
  >消费者1: t0p0、t1p1、t3p0、t2p0
  >
  >消费者3: t1p0、t2p1、t0p1、t3p1

### offset提交

> 同步提交和异步提交，对应于 KafkaConsumer 中的 commitSync（）和 commitAsync（）两种类型的方法

对于采用 commitSync（）的无参方法 ，它提交消费位移的频率和拉取批次消息、处理批次消息的频率是一样的，如果想寻求**更细粒度的、更精准的提交**，那么就需要使用 commitSync(final Map< TopicPartition，OffsetAndMetadata > offsets) 

~~~java
/*
  每消费一条消息就提交一次位移
*/
while (true){
    ConsumerRecords<String, Company> res = kafkaConsumer.poll(Duration.ofSeconds(1));
    for (ConsumerRecord<String, Company> record : res) {
        kafkaConsumer.commitSync(Collections.singletonMap(
            new TopicPartition(record.topic(),record.partition()),
            new OffsetAndMetadata(record.offset()+1)));
    }
}

/*
  按照分区的粒度划分提交位移的界限
*/
for (TopicPartition partition : res.partitions()) {
    List<ConsumerRecord<String, Company>> records = res.records(partition);
    for (ConsumerRecord<String, Company> record : records) {
        System.out.println("分区："+partition+",消息："+record);
    }
    
    long lastConsumedOffset=records.get(records.size()-1).offset();
    kafkaConsumer.commitSync(Collections.singletonMap(
        partition,
        new OffsetAndMetadata(lastConsumedOffset+1)));
}
~~~

对于异步提交也是同理。会产生重复消费的问题，我们可以设置一个递增的序号来维护异步提交的顺序，每次位移提交之后就增加序号相对应的值。在遇到位移提交失败需要重试的时候，可以检查所提交的位移和序号的值的大小，如果前者小于后者，则说明有更大的位移己经提交了，不需要再进行本次重试：如果两者相同，则说明可以进行重试提交。除非程序编码错误，否则不会出现前者大于后者的情况。如果位移提交失败的情况经常发生，那么说明系统肯定出现了故障，在－般情况下，位移提交失败的情况很少发生，不重试也没有关系，后面的提交也会有成功的 。重试会增加代码逻辑的复杂度，不重试会增加重复消费的概率。如果 费者异常退出，那么这个重复消费的问题就很难避免，因为这种情况下无法及时提交消费位移；如果消费者正常退出或发生再均衡况，那么可以在退出或再均衡执行之前使用同步提交的方式做最后的把关。

### 指定offset消费

>每当消费者**查找不到所记录的消费位移**或**位移越界**时，就会根据消费者客户端参数auto.offset.reset来决定从何处开始进行消费，参数默认值为latest。
>
>![image-20220822151640630](images/image-20220822151640630.png)
>
>如果配置为none，那么此时会报OffsetForPartitionException 异常
>
>![image-20220822151703451](images/image-20220822151703451.png)

#### poll()与seek()

> poll() 相当于黑盒，无法精确地掌控其消费的起始位置，提供auto.offset.reset参数也只能在找不到消费位移或位移越界的情况下粗粒度地从开头或末尾开始消费。
>
> seek(TopicPartition partition，long offset) 是一种更细粒度的掌控，可以让我们从**特定的位移处开始拉取消息**，**追前消费**或**回溯消费**。seek()方法只能重置消费者分配到的分区的消费位置，而分区的分配是在 poll ()方法的调用过程中实现的，也就是说，**在执行 seek()方法之前需要先执行一次 poll ()方法**，等到分配到分区之后才可以重置消费位置。
>
> 此外，offset是保存在 __ consumer_offset__ 中的，而使用seek()可以将offset保存在任意存储介质中（mysql、redis等）

~~~java
kafkaConsumer.subscribe(topics);
Set<TopicPartition> assignment =new HashSet<>();
while (assignment.size()==0){   //不等于0，说明已经成功分配到了分区
    kafkaConsumer.poll(Duration.ofSeconds(1));
    assignment = kafkaConsumer.assignment();
}
for (TopicPartition topicPartition : assignment) {
    kafkaConsumer.seek(topicPartition,10);
}
//后面同以往一样正常消费
while (true){
    ConsumerRecords<String, Company> res = kafkaConsumer.poll(Duration.ofSeconds(1));
    //直接拿数据
    for (ConsumerRecord<String, Company> record : res) {
        System.out.println(record);
        //每消费一条消息就提交一次位移
        kafkaConsumer.commitSync(Collections.singletonMap(
            new TopicPartition(record.topic(),record.partition()),
            new OffsetAndMetadata(record.offset()+1)));
    }
    //按分区拿数据
    for (String topic : topics) {
        for (ConsumerRecord<String, Company> record : res.records(topic)) {
            System.out.println("分区："+topic+",消息："+record);
        }
    }
    kafkaConsumer.commitAsync();
}
~~~



### 再均衡（TODO原理）

>再均衡是指分区的所属权从一个消费者转移到另一消费者的行为，**它为消费组具备高可用性和伸缩性提供保障**，使我们可以**既方便又安全地删除和添加消费组内的消费者**。**不过在再均衡发生期间，消费组内的消费者是无法读取消息的**。另外，当 个分区被重新分配给另一个消费时， **消费者当前的状态也会丢失**。比如消费者消费完某个分区中的一部分消息时还没有来得及提交消费位移就发生了再均衡操作 之后这个分区又被分配给了消费组 的另一个消费者，原来被消费完的那部分消息又被重新消费一遍，也就是发生了重复消费。一般情况下，**应尽量避免不必要的再均衡的发生**。
>
>subscribe(Collection< String>  topics, ConsumerRebalanceListener listener）。ConsumerRebalanceListener 一个接口 ，包含2个方法：
>
>( 1) void onPartitionsRevoked(Collection< TopicPartition> partitions) 
>
>这个方法会在**再均衡开始之前和消费者停止读取消息之后被调用**。可以通过这个回调方法来处理消费位移的提交，以此来**避免**一些不必要的**重复消费**现象的发生。参数partitions表示**再均衡前**所分配到的分区。
>
>( 2) void onPartitionsAssigned(Collection< TopicPartition> partitions) 
>
>这个方法会在**重新分配分区之后和消费者开始读取消费之前被调用** 。参数 partitions**再均衡后**所分配到的分区。

![image-20220822172009502](images/image-20220822172009502.png)

![image-20220822172102542](images/image-20220822172102542.png)

# 分区优化

## 优先副本的选举

在创建主题的时候，该主题的分区及副本会**尽可能均匀地**分布到 Kafka 集群的各个 broker节点上，对 leader 本的分配也比较均匀。 比如我们使用 kafka-topics.sh 建一个分区数为 3、副本因子为3 的主题 topic partitions 创建之后的分布信息如下：

~~~sh
[root@nodel kafka 2.11-2.0.0]# bin/kafka-topics.sh --zookeeper localhost:2181/kafka --describe --topic topic-partitions 

Topic :topic-partitions 
PartitionCount : 3 ReplicationFactor : 3 Configs : 
Topic: topic-partitions Partition: 0 Leader: 1 Replicas: 1 , 2 , 0 Isr: 1 , 2 , 0 
Topic: topic-partitions Partition: 1 Leader: 2 Replicas: 2 , 0 , 1 Isr: 2 , 0 , 1 
Topic: topic-partitions Partition: 2 Leader: 0 Replicas: 0 , 1 , 2 Isr: 0 , 1,  2 
~~~

如果brokerId =2的节点重启，那么主题 topic-partitions 分布信息如下：

~~~sh
[root@nodel kafka 2 . 11 - 2 . 0 . 0 ]# bin/kafka- topics. sh --zookeeper localhost : 2181/ 
kafka --describe --topic topic - partitions 

Topic :topic-partitions 
PartitionCount : 3 ReplicationFactor : 3 Configs : 
Topic: topic-partitions Partition: 0 Leader: 1 Replicas: 1 , 2 , 0 Isr: 1 , 0 , 2 
Topic: topic-partitions Partition: 1 Leader: 0 Replicas: 2 , 0 , 1 Isr: 0 , 1 , 2 
Topic: topic-partitions Partition: 2 Leader: 0 Replicas: 0 , 1 , 2 Isr: 0 , 1,  2 
~~~

可以看到，现在的分区已经不平衡了！

为了能够有效治理负载失衡，Kafka 引入了**优先副本**（ preferred replica )的概念。优先副本是指在 AR 集合列表中的第一个副本，如上面主题 topic-partitions 分区AR 集合表（ Replicas ）为[1,2,0]，那么分区0的优先副本即为1。**理想情况下，优先副本就是该分区的 leader 副本**，所 以也可以称之为 preferred leader。kafka 要确保所有主题的优先副本在 Kafka 集群中均匀分布，这样就保证了所有分区 leader 均衡分布， 如果 leader分布过于集中，就会造成集群负载不均衡。

> 此外，auto.leader.rebalance.enable=true，可以实现自平衡，但在线上环境中**不建议使用**！

kafka-perferred-replica-election.sh 脚本提供了对分区 leader 副本（全部分区）进行重新平衡的功能。优先副本的选举过程是一个安全的过程， Kafka 客户端可以自动感知分区 leader 副本的变更。下面的示例演示了脚本的具体用法：

![image-20220808202659721](images/image-20220808202659721.png)

![image-20220808200959609](images/image-20220808200959609.png)

## 分区重分配

> 新增broker节点时，旧的分区、副本无法知晓新的broker，这时需要将其重分配到新broker。

**kafka-reassign-partitions.sh** 脚本的使用分为3个步骤：

1. 首先创建需要 1个包含主题清单的JSON 文件

   ~~~json
   {
   	"topics":[
   			{
   					"topic":"topic-reassign"
   			}
   		],
   		"version":1
   }
   ~~~

   

2. 其次根据主题清单和 broker节点清单生成一份重分配方案

   ![image-20220808204325363](images/image-20220808204325363.png)

   上面示例中打印出了两个 JSON 格式的内容。第1个“ Current partition replica assignment" 所对应的 JSON 内容为当前的分区副本分配情况，在执行分区重分配的时候最好将这个内容保存起来，以备后续的回滚操作。第2个“Proposed partition reassignment configuration ，，所对应的JSON 容为重分配的候选方案，注意这里只是生成一份可行性的方案，并没有真正执行重分配的动作 。生成的可行性方案的具体算法和创建主题时的 样，这里也包含了机架信息。

3. 最后根据这份方案执行具体的重分配动作。

   ![image-20220808204512709](images/image-20220808204512709.png)

   对于分区重分配而言，这里还有可选的第四步操作，即验证查看分区分配的进度，只需将上面的 execute 替换为 verify 即可， 具体示例如下：

   ![image-20220808204733500](images/image-20220808204733500.png)

## 复制限流（TODO）

> 

# 常用命令

~~~shell
#创建主题、分区、副本
kafka-topics.sh --create --bootstrap-server node01:9092 --topic zfc --partitions 3 --replication-factor 2
#创建生产者
kafka-console-producer.sh --bootstrap-server node01:9092 --topic first
#创建消费者
kafka-console-consumer.sh --bootstrap-server node01:9092 --topic first
#查看分区、副本、isr信息
kafka-topics.sh --bootstrap-server node01:9092 --describe --topic wujie
#查看某个消费者组的消费情况
kafka-consumer-groups.sh --bootstrap-server node01:9092 --describe --group wujiea
#修改offset
#移动偏移至最新
kafka-consumer-groups.sh --bootstrap-server node01:9092 --group wujiea --reset-offsets --topic wujie -to-latest --execute
#移动偏移至最早
kafka-consumer-groups.sh --bootstrap-server node01:9092 --group wujiea --reset-offsets --topic wujie -to-earliest --execute
#移动到指定时间偏移
kafka-consumer-groups.sh --bootstrap-server node01:9092 --group wujiea --reset-offsets --topic wujie --to-datetime 2020-11-07T00:00:00.000 --execute
~~~

# 日志存储（TODO）



# Java API

## 生产者

### 乞丐版

~~~java
private final String KAFKA_SERVER="192.168.200.128:9092";

    @GetMapping("/produce")
    public String product(){
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,KAFKA_SERVER);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);
        List<PersonVO> list=new ArrayList<>();
        for(int i=1;i<=2;i++){
            PersonVO personVO = new PersonVO();
            personVO.setId(i);
            personVO.setName("zfc"+i);
            list.add(personVO);
        }
        // 通过kafka发送出去
        String res = JSON.toJSONString(list);
        ProducerRecord<String, String> record = new ProducerRecord<>("wujie", res);
        try {
            kafkaProducer.send(record);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            kafkaProducer.close();
        }
        return "success";
    }
~~~

### 自定义拦截器

~~~java
/**
 * 自定义拦截器（对topic、key、partition等元数据进行修改）
 *
 * 支持多个拦截器，根据调用顺序，后一个拦截器依赖前一个拦截器的数据
 * prefix2-prefix1-xx
 */
public class MyInterceptor implements ProducerInterceptor<String,Company> {
    private volatile long success=0,fail=0;
    @Override
        public ProducerRecord<String, Company> onSend(ProducerRecord<String, Company> producerRecord) {
        //msg加前缀
        Company company = producerRecord.value();
        String newVal="地球-"+company.getAddress();
        company.setAddress(newVal);
        return new ProducerRecord<>(producerRecord.topic(),producerRecord.partition(),
                producerRecord.timestamp(),producerRecord.key(),company,producerRecord.headers());
    }

    /**
     * ack前或消息发送失败时进入该方法，优先于用户的callback
     * @param recordMetadata
     * @param e
     */
    @Override
    public void onAcknowledgement(RecordMetadata recordMetadata, Exception e) {
        //统计发送成功率
        if(e!=null){
            fail++;
        }else {
            success++;
        }
    }

    @Override
    public void close() {
        //显示发送成功率
        double successRatio=success/(success+fail);
        System.out.println("消息发送成功率："+(successRatio*100)+"%");
    }

    @Override
    public void configure(Map<String, ?> map) {

    }
}
~~~

### 自定义序列化器

~~~java
/**
 * 自定义序列化器
 */
public class CompanySerializer implements Serializer<Company> {

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public byte[] serialize(String topic, Company company) {
        if(company==null) return null;
        byte[] name=new byte[0];
        byte[] address=new byte[0];
        if(company.getName()!=null){
            name = company.getName().getBytes(StandardCharsets.UTF_8);
        }
        if(company.getAddress()!=null){
            address = company.getAddress().getBytes(StandardCharsets.UTF_8);
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 + 4 + name.length + address.length);
        byteBuffer.putInt(name.length);
        byteBuffer.put(name);
        byteBuffer.putInt(address.length);
        byteBuffer.put(address);
        return byteBuffer.array();
    }

    @Override
    public void close() {

    }
}

~~~



### 自定义分区器

~~~java
/**
 * 自定义分区器(变更分区规则)
 */
public class MyPartitioner implements Partitioner {

    private final AtomicInteger count=new AtomicInteger(0);

    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        /**
         * kafka规定：key=null,只会存到可用分区
         * 下面我们通过自定义分区器打破这一规则
         */
        List<PartitionInfo> partitionInfos = cluster.partitionsForTopic(topic);
        if(keyBytes==null){
            return count.getAndIncrement()%partitionInfos.size();
        }else {
            return Utils.toPositive(Utils.murmur2(keyBytes)%partitionInfos.size());
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> map) {

    }
}

~~~



## 消费者

### 乞丐版

~~~java
@GetMapping("/consume")
    public void consume(){
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,KAFKA_SERVER);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,"wujiea");  //消费者组
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(properties);
        ArrayList<String> topics = new ArrayList<>();
        topics.add("wujie");
        kafkaConsumer.subscribe(topics);
        while (true){
            ConsumerRecords<String, String> res = kafkaConsumer.poll(Duration.ofSeconds(1));
            //直接拿数据
            for (ConsumerRecord<String, String> record : res) {
                System.out.println(record);
            }
            //按分区拿数据
            for (String topic : topics) {
                for (ConsumerRecord<String, Company> record : res.records(topic)) {
                    System.out.println("分区："+topic+",消息："+record);
                }
            }
            kafkaConsumer.commitAsync();
        }
    }
~~~

### 自定义反序列化器

~~~java
/**
 * 自定义反序列化器
 */
public class CompanyDeserializer implements Deserializer<Company> {

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public Company deserialize(String topic, byte[] bytes) {
        if(bytes==null) return null;
        if(bytes.length<8){
            throw new SerializationException("最小长度不应该小于8!");
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        int nameLen=byteBuffer.getInt();
        byte[] nameBytes= new byte[nameLen];
        byteBuffer.get(nameBytes);
        int addressLen=byteBuffer.getInt();
        byte[] addressBytes= new byte[addressLen];
        byteBuffer.get(addressBytes);
        String name=new String(nameBytes,StandardCharsets.UTF_8);
        String address=new String(addressBytes,StandardCharsets.UTF_8);
        return new Company(name,address);
    }

    @Override
    public void close() {

    }
}
~~~



## KafkaAdminClient

> 通过javaAPI对topic进行增删改查

~~~java

/**
 * KafkaAdminClient内部使用Kafka一套自定义二进制协议来实现诸如创建主题管理功能。
 * 它主要的实现步骤如下
 * (1)客户端根据方法的调用创建相应的协议请求，比如创建主题的createTopics方法，其内部就是发送CreateTopicRequest请求
 * (2)客户端将请求发送至服务端
 * (3)服务端处理相应的请求并返回响应，比如这个与createTopicRequest请求对应的就是CreateTopicResponse
 * 客户端接收相应的响应井进行解析处理。和协议相关请求和相应类基本都在org.apache.kafka.common.requests包下，
 * abstractRequest AbstractResponse 是这些请求和响应的两个基本父类。
 */
@RestController
@RequestMapping("/kafkaAdminClient")
public class KafkaAdminClientDemo {

    private final String KAFKA_SERVER="192.168.200.128:9092";
    private AdminClient adminClient;

    public KafkaAdminClientDemo(){
        //init
        Properties properties = new Properties();
        properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,KAFKA_SERVER);
        properties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,30000);
//        properties.put("create.topic.policy.class.name", MyPolicy.class.getName());  //无效！！！ why？不知道
        adminClient = KafkaAdminClient.create(properties);
    }

    @RequestMapping("/createTopic")
    public String  createTopic(){
        NewTopic topic;
        String newTopicName="zfc";
        //创建前先审查是否存在该topic
        try {
            Set<String> allTopicNames = adminClient.listTopics().names().get();
            if(allTopicNames.contains(newTopicName)){
                return "该topic已经存在!";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        //1.通过分区数+副本数创建topic
//        topic = new NewTopic("zfc", 2, (short) 2);
        //2.通过分区副本分配方案来创建topic
        Map<Integer, List<Integer>> replicasAssignments=new HashMap<>();
        replicasAssignments.put(0,Arrays.asList(0,1));
//        replicasAssignments.put(1,Arrays.asList(1,0));
        topic=new NewTopic(newTopicName,replicasAssignments);
        //指定配置
        Map<String, String> configs=new HashMap<>();
        configs.put("cleanup.policy","compact");
        //创建分区合法性校验
        topic.configs(configs);
        CreateTopicsResult res = adminClient.createTopics(Collections.singleton(topic));
        try {
            res.all().get();
            return "已成功创建topic";
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return "系统异常";
    }

    /**
     * 查看topic详细信息
     */
    @RequestMapping("/describeTopic")
    public Object describe(){
        DescribeTopicsResult res = adminClient.describeTopics(Collections.singleton("wujie"));
        try {
            return res.all().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return "null";
    }

    /**
     * 增加分区
     */
    @RequestMapping("/incrPartition")
    public String incrPartition(){
        Map<String, NewPartitions> newPartitions=new HashMap<>();
        newPartitions.put("zfc",NewPartitions.increaseTo(5));
        //先审查是否存在该topic
        try {
            Set<String> allTopicNames = adminClient.listTopics().names().get();
            if(!allTopicNames.contains("zfc")){
                return "该topic不存在!";
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        CreatePartitionsResult res = adminClient.createPartitions(newPartitions);
        try {
            res.all().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return "增加成功";
    }

    /**
     * 删除主题
     */
    @RequestMapping("/delTopic/{topic}")
    public String delTopic(@PathVariable String topic){
        try {
            adminClient.deleteTopics(Collections.singleton(topic)).all().get();
            return "删除成功";
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return "系统异常";
    }
}
~~~

### 创建topic合法性验证

> 创建自定义合法性验证类，搭配 create.topic.policy.class.name=

~~~java
/**
 * 创建topic合法性校验
 */
public class MyPolicy implements CreateTopicPolicy {

    public MyPolicy(){
        System.out.println("创建topic合法性校验");
    }
    @Override
    public void validate(RequestMetadata requestMetadata) throws PolicyViolationException {
        if (requestMetadata.numPartitions()==null||requestMetadata.numPartitions()<2) {
            throw new PolicyViolationException("分区数<2,不符合线上环境要求!");
        }
        if (requestMetadata.replicationFactor()==null||requestMetadata.replicationFactor()<2) {
            throw new PolicyViolationException("副本数<2,不符合线上环境要求!");
        }
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public void configure(Map<String, ?> map) {

    }
}
~~~



# 监控指标采集

> 目前的 Kafka 监控产品有很多，比如 KafkaManager、Kafka Eagle、Kafka Monitor、KafkaOffsetMonitor、Kafka Web Console、Burrow等

## **获取监控指标**

Kafka可以配置使用JMX进行运行状态的监控，既可以通过JDK自带Jconsole来观察结果，也可以通过Java API的方式来.

 

- 需要kafka服务开启JMX端口  在kafka安装目录下bin文件夹下 kafka-server-start.sh串配置JMX端口

~~~sh
if [ "x$KAFKA_HEAP_OPTS" = "x" ]; 
then

  export KAFKA_HEAP_OPTS="-Xmx1G -Xms1G"

  export JMX_PORT="9999"   #配置JMX端口

fi
~~~

- 通过jconsole远程测试连接jconsole工具在jdk安装目录下bin文件夹中，连接方式 ip地址:JMX端口号

- 通过JavaAPI来访问

例：

![img](images/FrQtI7sQ_ZnzNPTxOUHweIM4RxfO)



![img](images/FhTPMlfZzPH5B8eAONkKnPYqeqAS)

~~~java
/**
 * 通过JMX获取kafka监控指标
 */
@RestController
@RequestMapping("/jmx")
public class JMXDemo {

    private MBeanServerConnection conn;

    private final String JMX_URL="service:jmx:rmi:///jndi/rmi://";

    private final String ipAndPort="192.168.200.128:9999";

    public JMXDemo(){
        //init
        try {
            JMXServiceURL serviceURL = new JMXServiceURL(JMX_URL+ipAndPort+"/jmxrmi");
            JMXConnector connect = JMXConnectorFactory.connect(serviceURL);
            conn=connect.getMBeanServerConnection();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequestMapping("/info")
    public Object getInfoByJMX(){
        Map<String,Object> map=new HashMap<>();
        //获取MsgIn指标
        String oneMinuteRateObjName="kafka.server:type=BrokerTopicMetrics,name=MessagesInPerSec";
        String oneMinuteRateObjAttr="OneMinuteRate";
        map.put(oneMinuteRateObjAttr,getAttr(oneMinuteRateObjName, oneMinuteRateObjAttr));
        return map;
    }

    private Object getAttr(String objName,String objAttr){
        if(conn!=null){
            try {
                ObjectName objectName = new ObjectName(objName);
                return conn.getAttribute(objectName,objAttr);
            } catch (MalformedObjectNameException | MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
~~~



## **重点指标项：**

### **Kafka本身的指标：**

| **名称**                          | **MBean的名字**                                              | **描述**                                            | **指标类型**           |
| --------------------------------- | ------------------------------------------------------------ | --------------------------------------------------- | ---------------------- |
| UnderReplicatedPartitions         | kafka.server:type=ReplicaManager, name=UnderReplicatedPartitions | 未复制分区数                                        | Resource: Availability |
| IsrShrinksPerSec IsrExpandsPerSec | kafka.server:type=ReplicaManager, name=IsrShrinksPerSec kafka.server:type=ReplicaManager,name=IsrExpandsPerSec | 同步副本(isr)池收缩/扩展的速率                      | Resource: Availability |
| ActiveControllerCount             | kafka.controller:type=KafkaController, name=ActiveControllerCount | 集群中活动控制器的个数                              | Resource: Error        |
| OfflinePartitionsCount            | kafka.controller:type=KafkaController, name=OfflinePartitionsCount | 脱机分区数                                          | Resource: Availability |
| LeaderElectionRateAndTimeMs       | kafka.controller:type=ControllerStats, name=LeaderElectionRateAndTimeMs | 领袖选举率和延迟                                    | Other                  |
| UncleanLeaderElectionsPerSec      | kafka.controller:type=ControllerStats, name=UncleanLeaderElectionsPerSec | 每秒“不洁”选举的次数                                | Resource: Error        |
| TotalTimeMs                       | kafka.network:type=RequestMetrics,name=TotalTimeMs,request={Produce-FetchConsumer-FetchFollower} | 服务于指定请求的总时间(以毫秒为单位)(产生/获取)     | Work: Performance      |
| PurgatorySize                     | kafka.server:type=ProducerRequestPurgatory,name=PurgatorySize kafka.server:type=FetchRequestPurgatory,name=PurgatorySize | 在生产者炼狱中等待的请求数;在获取炼狱中等待的请求数 | Other                  |
| BytesInPerSec BytesOutPerSec      | kafka.server:type=BrokerTopicMetrics,name=BytesInPerSec kafka.server:type=BrokerTopicMetrics,name=BytesOutPerSec | 汇总输入/输出字节速率                               | Work: Throughput       |

**UnderReplicatedPartitions**: 在一个运行健康的集群中，处于同步状态的副本数（ISR）应该与总副本数（简称AR:Assigned Repllicas）完全相等，如果分区的副本远远落后于leader，那这个follower将被ISR池删除，随之而来的是IsrShrinksPerSec(可理解为isr的缩水情况，后面会讲)的增加。由于kafka的高可用性必须通过副本来满足，所有有必要重点关注这个指标，让它长期处于大于0的状态。

**IsrShrinksPerSec/IsrExpandsPerSec**: 任意一个分区的处于同步状态的副本数（ISR）应该保持稳定，只有一种例外，就是当你扩展broker节点或者删除某个partition的时候。为了保证高可用性，健康的kafka集群必须要保证最小ISR数，以防在某个partiton的leader挂掉时它的follower可以接管。一个副本从ISR池中移走有以下一些原因：follower的offset远远落后于leader（改变replica.lag.max.messages 配置项），或者某个follower已经与leader失去联系了某一段时间（改变replica.socket.timeout.ms 配置项），不管是什么原因，如果IsrShrinksPerSec（ISR缩水） 增加了，但并没有随之而来的IsrExpandsPerSec（ISR扩展）的增加，就将引起重视并人工介入，[kafka官方文档](http://10.10.88.21/wiki/#brokerconfigs)提供了大量关于broker的用户可配置参数。

**ActiveControllerCount**: kafka集群中第一个启动的节点自动成为了controller，有且只能有一个这样的节点。controller的职责是维护partitio leader的列表，和协调leader的变更（当遇到某个partiton leader不可用时）。如果有必要更换controller，一个新的controller将会被zookeeper从broker池中随机的选取出来，通常来说，这个值（ActiveControllerCount）不可能大于1，但是当遇到这个值等于0且持续了一小段时间（<1秒）的时候，必须发出明确的告警。

**OfflinePartitionsCount** (只有controller有): 这个指标报告了没有活跃leader的partition数，由于所有的读写操作都只在partition leader上进行，因此非0的这个值需要被告警出来，从而防止服务中断。任何没有活跃leader的partition都会彻底不可用，且该parition上的消费者和生产者都将被阻塞，直到leader变成可用。

**LeaderElectionRateAndTimeMs**: 当parition leader挂了以后，新leader的选举就被触发。当partition leader与zookeeper失去连接以后，它就被人为是“死了”，不像zookeeper zab，kafka没有专门对leader选举采用majority-consensus算法。是kafka的broker集群所有的机器列表，是由每一个parition的ISR所包含的机器这个子集，加起来的并集组成的，怎么说，假设一共有3个parition，第一个parition的ISR包含broker1、2、3，第二个parition包含broker2、3、4，第三个parition包含broker3、4、5，那么这三个parition的ISR所在broker节点加起来的并集就是整个kafka集群的所有broker全集1、2、3、4、5。当副本可以被leader捕获到的时候，我们就人为它处于同步状态（in-sync），这意味着任何在ISR池中的节点，都有可能被选举为leader。

**LeaderElectionRateAndTimeMs** 报告了两点：leader选举的频率（每秒钟多少次）和集群中无leader状态的时长（以毫秒为单位），尽管不像UncleanLeaderElectionsPerSec这个指标那么严重，但你也需要时长关注它，就像上文提到的，leader选举是在与当前leader通信失败时才会触发的，所以这种情况可以理解为存在一个挂掉的broker。

**UncleanLeaderElectionsPerSec**: 这个指标如果存在的话很糟糕，这说明kafka集群在寻找partition leader节点上出现了故障，通常，如果某个作为partition leader的broker挂了以后，一个新的leader会被从ISR集合中选举出来，不干净的leader选举（Unclean leader elections ）是一种特殊的情况，这种情况是副本池中没有存活的副本。基于每个topic必须拥有一个leader，而如果首领是从处于不同步状态的副本中选举出来的话，意味着那些与之前的leader没有被同步的消息，将会永久性丢失。事实上，不干净的leader选举将牺牲持久性（consistency）来保证可用性（availability）。所以，我们必须明确地得到这个指标的告警，从而告知数据的丢失。

**TotalTimeMs**: The TotalTimeMs metric family measures the total time taken to service a request (be it a produce, fetch-consumer, or fetch-follower request):

这个指标族（很多地方都涉及到它）衡量了各种服务请求的时间（包括produce，fetch-consumer，fetch-follower）

1. produce:从producer发起请求发送数据
2. fetch-consumer: 从consumer发起请求获取数据
3. fetch-follower:follower节点向leader节点发起请求，同步数据

TotalTimeMs 这个指标是由4个其他指标的总和构成的：

1. queue:处于请求队列中的等待时间
2. local:leader节点处理的时间
3. remote:等待follower节点响应的时间（只有当requests.required.acks=-1时）
4. response:发送响应的时间

通常情况下，这个指标值是比较稳定的，只有很小的波动。当你看到不规则的数据波动，你必须检查每一个queue,local,remote和response的值，从而定位处造成延迟的原因到底处于哪个segment。

**PurgatorySize**: 请求炼狱（request purgatory）作为一个临时存放的区域，使得生产(produce)和消费(fetch)的请求在那里等待直到被需要的时候。每个类型的请求都有各自的参数配置，从而决定是否（将消息）添加到炼狱中：

1. fetch：当fetch.wait.max.ms定义的时间已到，还没有足够的数据来填充（congsumer的fetch.min.bytes）请求的时候，获取消息的请求就会被扔到炼狱中。
2. produce：当request.required.acks=-1，所有的生产请求都会被暂时放到炼狱中，直到partition leader收到follower的确认消息。

关注炼狱的大小有助于判断导致延迟的原因是什么，比如说，导致fetch时间的增加，很显然可以认为是由于炼狱中fetch的请求增加了。

**BytesInPerSec/BytesOutPerSec**: 通常，磁盘的吞吐量往往是决定kafka性能的瓶颈，但也不是说网络就不会成为瓶颈。根据你实际的使用场景，硬件和配置，网络将很快会成为消息传输过程中最慢的一个环节，尤其是当你的消息跨数据中心传输的时候。跟踪节点之间的网络吞吐量，可以帮助你找到潜在的瓶颈在哪里，而且可以帮助决策是否需要把端到端的消息做压缩处理。

 

### 主机层面的broker性能指标

**Page cache read ratio**: kafka在设计最初的时候，通过内核中的页缓存，来达到沟通可靠性（基于磁盘）和高效性（基于内存）之间的桥梁。page cache read ratio（可理解为页缓存读取率），和数据库中的cache-hit ratio(缓存命中率)比较相似，如果这个值比较大，则等价于更快的读取速度，从而有更好的性能。如果发现页缓存读取率<80%，则说明需要增加broker了。

**Disk usage**: 由于kafka将所有数据持久化到磁盘上，很有必要监控一下kafka的剩余磁盘空间。当磁盘占满时，kafka会失败，所以，随着时间的推移，跟踪磁盘的增长率是很有必要的。一旦你了解了磁盘的增长速率，你就可以在磁盘将要占满之前选择一个合适的时间通知管理员。

**CPU usage**: 尽管kafka主要的瓶颈通常是内存，但并不妨碍观察一下cpu的使用率。虽然即便在使用gzip压缩的场景下，cpu都不太可能对性能产生影响，但是，如果发现cpu使用率突然增高，那肯定要引起重视了。

**Network bytes sent/received**: 如果你只是在监控kafka的网络in/out指标，那你只会了解到跟kafka相关的信息。如果要全面了解主机的网络使用情况，你必须监控主机层面的网络吞吐量，尤其是当你的kafka主机还承载了其他与网络有关的服务。高网络使用率是性能下降的一种表现，此时需要联系TCP重传和丢包错误，来决定性能的问题是否是网络相关的。

 

### JVM垃圾回收指标

| **名称**                  | **MBean的名字**                                          | **描述**                           | **指标类型** |
| ------------------------- | -------------------------------------------------------- | ---------------------------------- | ------------ |
| ParNew count              | java.lang:type=GarbageCollector,name=ParNew              | 年轻代集合的数量                   | Other        |
| ParNew time               | java.lang:type=GarbageCollector,name=ParNew              | 年轻代收集经过的时间，以毫秒为单位 | Other        |
| ConcurrentMarkSweep count | java.lang:type=GarbageCollector,name=ConcurrentMarkSweep | 老年代集合的数量                   | Other        |
| ConcurrentMarkSweep time  | java.lang:type=GarbageCollector,name=ConcurrentMarkSweep | 老年代收集的运行时间，以毫秒为单位 | Other        |

 

**ParNew**:可以理解成年轻代，这部分的垃圾回收会相当频繁，ParNew是一个stop-the-world的垃圾回收，意味着所有应用线程都将被暂停，知道垃圾回收完成，所以ParNew延迟的任何增加都会对kafka的性能造成严重影响。

**ConcurrentMarkSweep (CMS)** ：这种垃圾回收清理了堆上的老年代不用的内存，CMS是一个短暂暂停的垃圾回收算法，尽管会造成应用线程的短暂停顿，但这只是间歇性的，如果CMS需要几秒钟才能完成，或者发生的频次增加，那么集群就没有足够的内存来满足基本功能。

 

### kafka生产者指标

| **名称**            | **v0.9.0.x MBean 的名字**                               | **描述**                                    | **指标类型**     |
| ------------------- | ------------------------------------------------------- | ------------------------------------------- | ---------------- |
| Response rate       | kafka.producer:type=producer-metrics,client-id=([-.w]+) | 每秒接收到的平均响应数                      | Work: Throughput |
| Request rate        | kafka.producer:type=producer-metrics,client-id=([-.w]+) | 每秒发送的平均请求数                        | Work: Throughput |
| Request latency avg | kafka.producer:type=producer-metrics,client-id=([-.w]+) | 平均请求延迟(毫秒)                          | Work: Throughput |
| Outgoing byte rate  | kafka.producer:type=producer-metrics,client-id=([-.w]+) | 每秒传出/传入的平均字节数                   | Work: Throughput |
| IO wait time ns avg | kafka.producer:type=producer-metrics,client-id=([-.w]+) | I/O线程等待套接字的平均时间长度(以ns为单位) | Work: Throughput |

**Request rate**:请求的速率是指数据从producer发送到broker的速率，很显然，请求的速率变化是否健康，也是由使用的场景所决定的。关注速率走势的上和下，对于保证服务的可用性非常关键，如果不开启速率限制（rate-limiting）（0.9+版本才有），那么当流量高峰来临时，broker就将变得很慢，因为他要忙于处理大量涌入的数据。

**Request** **latency** **average**: 平均请求延迟，这是用来衡量从producer调用KafkaProducer.send()方法到接收到broker响应的时长。“接收到”包含很多层意思，可参考response rate那一块。

有多种途径可以减少延迟，主要的途径是producer的linger.ms 配置项，这个配置项告诉producer，在累积够一个消息批次之前，需要等待多久才能发送。默认地，producer只要接收到上一次发送的确认消息后，就立即发送新的消息，但并非所有场景都适用，为了累积消息而等待一点时间会提高吞吐量。

由于延迟和吞吐量有着必然的联系，就很有必要关注batch.size这个producer配置项，从而达到更完美的吞吐量。并不是只要配置一个合适的值就可以一劳永逸了，要视情况决定如何选择一个更优的批大小。要记住，你所配置的批大小是一个上限值，意思是说，如果数据满了，就立即发送，但如果没满的话，最多只等linger.ms 毫秒，小的批量将会导致更多次数的网络通信，然后降低吞吐量，反之亦然。

**Outgoing byte rate**: 在kafka的broker中，肯定需要监控producer的网络吞吐量，随着时间的变化观察网络上的数据传输量是很有必要的，从而决定是否有必要调整网络设备。另外，也很有必要知道producer是否以一个恒定的速率发送数据，从而让consumer获取到。监控producer的网络传输情况，除了可以决定是否需要调整网络设备，也可以了解producer的生产效率，以及定位传输延迟的原因。

**IO wait time**: Producer通常做了这么一些事：等待数据和发送数据。当producer产生了超越他发送能力的数据量，那结果就是只能等待网络资源。当如果producer没有发送速度限制，或者尽可能增加带宽，就很难说这（网络延迟）是个瓶颈了。因为磁盘的读写速率往往是最耗时的一个环节，所以对producer而言，最好检查一下I/O等待的时间。请记住，I/O等待表示当CPU停下来等待I/O的时间，如果你发现了过分的等待时间，这说明producer无法足够快地获取他需要的数据，如果你还在使用传统的机械磁盘作为存储，那请考虑采用SSD。

 

### Kafka消费者指标

**ConsumerLag/MaxLag**:这是所有人都很中意的kafka指标，ConsumerLag是指consumer当前的日志偏移量相对生产者的日志偏移量，MaxLag和ConsumerLag的关系很紧密，相当于是观察到的ConsumerLag的最大值，这两个度量指标的重要性在于，可以决定你的消费者在做什么。如果采用一个消费者组在存储设备上存储大量老的消息，你就需要重点关注消费者的延迟。当然，如果你的消费者处理的是实时消息，如果lag值一直居高不下，那就说明消费者有些过载（overloaded）了，遇到这种情况，就需要采用更多的消费者，和把topic切分成多个parition，从而可以提高吞吐量和降低延迟。

注意：ConsumerLag 是kafka之中过载的表现，正如上面的定义中所描述的额一样，但它也可被用来表示partition leader和follower之间的offset差异。

**BytesPerSec**:正如前文提到的生产者和broker的关系，也需要监控消费者的网络吞吐量。比如，MessagesPerSec的突然下降可能会导致消费失败，但如果BytesPerSec还保持不变，那如果消费少批次大体量的消息问题还不大。不断观察网络的流量，就像其他度量指标中提到的一样，诊断不正常的网络使用情况是很重要的。

**MessagesPerSec**: 消息的消费速度并不完全等同于比特的消费速度，因为消息本身可能有不同大小。依赖生产者和工作负载量，在典型的部署环境中，往往希望这个值是相当稳定的。通过随着时间的推移监控这个指标，可以观察出消费数据的趋势，然后定出一个基线，从而确定告警的阈值。这个曲线的走势取决于你的使用场景，但不管怎样，在很多情况下，定出一条基线然后对于异常情况做出告警是很有必要的。

**ZooKeeperCommitsPerSec**:只有0.8x版本有，如果把zookeeper作为offset的存储（在0.8x版本中是默认的，0.9+版本必须显式地在配置中定义offsets.storage=zookeeper），那你肯定需要监控这个值。注意到如果想要在0.9+版本中明确使用zookeeper作为offset存储，这个指标并没有被开放。当zookeeper处于高写负载的时候，将会遇到成为性能瓶颈，从而导致从kafka管道抓取数据变得缓慢。随着时间推移跟踪这个指标，可以帮助定位到zookeeper的性能问题，如果发现有大量发往zookeeper的commit请求，你需要考虑的是，要不对zookeeper集群进行扩展，要不直接把offset的存储变为kafka（offsets.storage=kafka）。记住，这个指标只对高阶消费者有用，简单消费者自行管理offset。

**MinFetchRate**: 消费者拉取的速率很好反映了消费者的整体健康状况，如果最小拉取速率接近0的话，就可能说明消费者出现问题了，对一个健康的消费者来说，最小拉取速率通常都是非0的，所以如果发现这个值在下降，往往就是消费者失败的标志。

 

Kafka主要性能指标的解释，可以参考：https://www.cnblogs.com/xinxiucan/p/12666967.html

 

 

**Kafka监控工具监控页面**

![img](images/Fszs34yKpKlNc0Ba8zAgrF7VpPD2)

Kafka Manager

管理几个不同的集群；

监控集群的状态(topics，brokers，副本分布，分区分布)；

产生分区分配(Generate partition assignments)基于集群的当前状态；

重新分配分区

![img](images/Fp0S-MGjzCLZemWhJW8rA7G3--se)

Kafka Web Manager

可以监控：

Brokers列表；

Kafka集群中Topic列表，及对应的Partition、LogSize等信息；

点击Topic、可以浏览对应的Consumer Groups、Offset、Lag等信息；

生产和消费流量图、消息预览......

![img](images/Fp0IlUXuK0pPAEVUirWYa1Gc7EuN)

Kafka Offset Moinitor

可以实时监控：

Kafka集群状态；

Topic、Consumer Group 列表；

图形化展示topic和consumer之间的关系；

图形化展示consumer的Offset、Lag等信息



# 几个问题

## ![image-20220901163855284](images/image-20220901163855284.png)

## 为什么partition2数据分的多？

- 主要原因是生产消息时，该分区消息就比其他的多（看log-end-offset）。

>1.  是否存在代码层面的自定义分区？（看代码）
>2. 生产者分区策略（指定分区？分区按hash(key)%n进行，看key指定的是否均匀？）
>3. 此外，Kafka规定，当key为null时，默认只会分配到可用分区中，其他分区是不是都已不可用了？

- 发现LAG过高，大量消息积压。

> 消费者应付不过来，对比其他的分区就可看出

## 如何确定partition2在那个服务器上？

~~~sh
kafka-topics.sh --bootstrap-server 192.168.200.130:9092 --describe --topic enis-file
~~~

![img](images/wps4.jpg)

自己画的图，箭头是leader指向follower

![img](images/wps5.png)

## 如何提高kafka读写效率？

1. producer

   - 设置acks=1，表示leader写入即认为写入成功，但如果要保证消息写入可靠性的话，这个配置要慎重

   - compression.type=lz4，设置消息压缩格式，降低网络传输

   - buffer.memory:33554432 (32m)，在Producer端用来存放尚未发送出去的Message的缓冲区大小。缓冲区满了之后可以选择阻塞发送或抛出异常，由block.on.buffer.full的配置来决定。

   - linger.ms:0，Producer默认会把两次发送时间间隔内收集到的所有Requests进行一次聚合然后再发送，以此提高吞吐量，而linger.ms则更进一步，这个参数为每次发送增加一些delay，以此来聚合更多的Message。

   - batch.size:16384，Producer会尝试去把发往同一个Topic、Partition的多个Requests进行合并，batch.size指明了一次Batch合并后Requests总大小的上限。如果这个值设置的太小，可能会导致所有的Request都不进行Batch

2. broker

   - log.dirs配置多个磁盘存储不同分区，不要把配置一个磁盘多个目录作为多个分区的存储路径

   - num.network.threads=3 ， broker处理消息的最大线程数 ，一般不改num.io.threads=3, broker处理磁盘IO的线程数 ，建议配置线程数量为cpu核数2倍，最大不超过3倍

   - log.retention.hours=72 ，设置消息保留时间，如保留三天，也可以更短

   - log.segment.bytes=1073741824 ，段文件配置1GB，有利于快速回收磁盘空间，重启kafka加载也会加快(如果文件过小，则文件数量比较多，kafka启动时是单线程扫描目录(log.dir)下所有数据文件

   - replica.lag.time.max.ms:10000，replica.lag.max.messages:4000，用来控制副本在什么条件下从ISR队列移除

   - num.replica.fetchers:1，在Replica上会启动若干Fetch线程把对应的数据同步到本地，而num.replica.fetchers这个参数是用来控制Fetch线程的数量。每个Partition启动的多个Fetcher，通过共享offset既保证了同一时间内Consumer和Partition之间的一对一关系，又允许我们通过增多Fetch线程来提高效率。

   - default.replication.factor:1，这个参数指新创建一个topic时，默认的Replica数量，Replica过少会影响数据的可用性，太多则会白白浪费存储资源，一般建议在3为宜

3. consumer

   - num.consumer.fetchers:1，启动Consumer拉取数据的线程个数，适当增加可以提高并发度，新版规定只能是1，即单线程。

   - fetch.min.bytes:1，每次Fetch Request至少要拿到多少字节的数据才可以返回， 在Fetch Request获取的数据至少达到fetch.min.bytes之前，允许等待的最大时长。对应上面说到的Purgatory中请求的超时时间： fetch.wait.max.ms:100

## kafka数据丢了怎么办，可以恢复吗？

![img](images/wps3.jpg)

**1.生产端数据丢失**

1）如果是同步模式

ack机制能够保证数据的不丢失，如果ack设置为0，风险很大，一般不建议设置为0

producer.type=sync 

request.required.acks=1

2）如果是异步模式

通过buffer来进行控制数据的发送，有两个值来进行控制，时间阈值与消息的数量阈值，如果buffer满了数据还没有发送出去，如果设置的是立即清理模式，风险很大，一定要设置为阻塞模式

producer.type=async 

request.required.acks=1 

queue.buffering.max.ms=5000 

queue.buffering.max.messages=10000 

queue.enqueue.timeout.ms = -1 

batch.num.messages=200

3)重试

retries:MAX_VALUE

reconnect.backoff.ms:20000

retry.backoff.ms:20000

**2.存储端消息丢失**

多副本机制

设置 min.insync.replicas > 1。这控制的是消息至少要被写入到多少个副本才算是“已提交”。设置成大于 1 可以提升消息持久性。在实际环境中千万不要使用默认值 1。

确保 replication.factor > min.insync.replicas。如果两者相等，那么只要有一个副本挂机，整个分区就无法正常工作了。推荐设置成 replication.factor = min.insync.replicas + 1。

**3.消费端数据丢失**

设置auto.commit.enable=false,消费端手动提交，确保消息真的被消费并处理完成。

**如何恢复?**：kafka依赖zk，在zk中配置文件的dataDir参数路径下记录着kafka的数据。**没落盘的就没了。**



## 如何重跑kafka数据？

1. 指定新的group(会重跑all data)

   ~~~sh
   bin/kafka-console-consumer.sh --bootstrap-server node01:9092 --topic wujie --group zfcdb --from-beginning
   ~~~

2. 修改offset（灵活）

- Api层面就用seek()

- 命令层面

  ~~~sh
  #修改offset
  #移动偏移至最新
  kafka-consumer-groups.sh --bootstrap-server node01:9092 --group wujiea --reset-offsets --topic wujie -to-latest --execute
  #移动偏移至最早
  kafka-consumer-groups.sh --bootstrap-server node01:9092 --group wujiea --reset-offsets --topic wujie -to-earliest --execute
  #移动到指定时间偏移
  kafka-consumer-groups.sh --bootstrap-server node01:9092 --group wujiea --reset-offsets --topic wujie --to-datetime 2020-11-07T00:00:00.000 --execute
  ~~~



## 为什么kafka吞吐那么高？（慢慢啃，慢慢研究）

主要依赖于以下5点:

1. **Zero Copy(零拷贝)技术**

   - 传统I/O

     > 在Linux系统中，传统I/O主要是通过read()和write()两个系统调用来实现的，通过read()函数读取文件到缓存区中，然后通过write()函数将缓存中的数据输出到网络端口。

     ![img](images/8f73f65d2ce44ef299b9aa6e15d74168.png)

     **整个过程涉及2次CPU拷贝、2次DMA拷贝总共4次拷贝，以及4次上下文切换。**

     > 上下文切换：当用户程序向内核发起系统调用时，CPU将用户进程从用户态切换到内核态；当系统调用返回时，CPU将用户进程从内核态切换回用户态。
     > CPU拷贝：由CPU直接处理数据的传送，数据拷贝时会一直占用CPU的资源。
     > DMA拷贝：由CPU向DMA磁盘控制器下达指令，让DMA控制器来处理数据的传送，数据传送完毕再把信息反馈给CPU，从而减轻了CPU资源的占有率。

   - **sendfile**

     > sendfile系统调用在Linux内核版本2.1中被引入，目的是简化通过网络在两个通道之间进行的数据传输过程。sendfile系统调用的引入，不仅减少了CPU拷贝的次数，还减少了上下文切换的次数。

     ![img](images/c8c3fd1948b94662b5a742a9e8312550.png)

     > 通过sendfile系统调用，数据可以直接在内核空间内部进行I/O传输，从而省去了数据在用户空间和内核空间之间的来回拷贝。基于sendfile系统调用的零拷贝方式，整个拷贝过程会发生2次上下文切换，1次CPU拷贝和2次DMA拷贝。
     >
     > **sendfile存在的问题是用户程序不能对数据进行修改，而只是单纯地完成了一次数据传输过程。**

   - **sendfile + DMA gather copy**

     > Linux 2.4版本的内核对sendfile系统调用进行修改，为DMA拷贝引入了gather 操作。它将内核空间（kernel space）的读缓冲区（read buffer）中对应的数据描述信息（内存地址、地址偏移量）记录到相应的网络缓冲区（ socket buffer）中，由DMA根据内存地址、地址偏移量将数据批量地从读缓冲区（read buffer）拷贝到网卡设备中，这样就省去了内核空间中仅剩的1次CPU拷贝操作。

     ![img](images/998a1ed40c254b88a8ab0a9bc2dfa290.png)

     > 在硬件的支持下，sendfile拷贝方式不再从内核缓冲区的数据拷贝到socket缓冲区，取而代之的仅仅是缓冲区文件描述符和数据长度的拷贝，这样DMA引擎直接利用gather操作将页缓存中数据打包发送到网络中即可。
     >
     > 基于sendfile + DMA gather copy 系统调用的零拷贝方式，整个拷贝过程会发生2次上下文切换、0次CPU拷贝以及2次DMA拷贝。
     >
     > sendfile + DMA gather copy 拷贝方式同样存在用户程序不能对数据进行修改的问题，而且本身需要硬件的支持，它只适用于将数据从文件拷贝到socket套接字上的传输过程。

   - **mmap + write**

     > 一种零拷贝方式是使用mmap + write 代替原来的 read + write 方式，减少了1次CPU拷贝操作。mmap是Linux提供的一种内存映射文件方法，即将一个进程的地址空间中的一段虚拟地址映射到磁盘文件地址。

     ![img](images/045d9d3132ea44808193e35c7785ee88.png)

     > 使用mmap的目的是将内核中读缓冲区（read buffer）的地址与用户空间的缓冲区（user buffer）进行映射，从而实现内核缓冲区与应用程序内存的共享，省去了将数据从内核读缓冲区（read buffer）拷贝到用户缓冲区（user buffer）的过程，然而内核读缓冲区（read buffer）仍需将数据到内核写缓冲区（socket buffer）。
     >
     > **基于mmap + write 系统调用的零拷贝方式，整个拷贝过程会发生4次上下文切换，1次CPU 拷贝和2次DMA拷贝。**
     >
     > mmap主要的用处是提高I/O性能，特别是针对大文件。对于小文件，内存映射文件反而会导致碎片空间的浪费，因为内存映射总是要对齐页边界，最小单位是4 KB，一个5 KB的文件将会映射占用8 KB内存，也就会浪费3 KB内存。
     >
     > mmap的拷贝虽然减少了1次拷贝，提升了效率，但也存在一些隐藏的问题。当mmap一个文件时，如果这个文件被另一个进程所截获，那么write系统调用会因为访问非法地址被SIGBUS信号终止，SIGBUS默认会杀死进程并产生一个coredump，服务器可能因此被终止。

   - **splice**

     > sendfile只适用于将数据从文件拷贝到socket套接字上，同时需要硬件的支持，这也限定了它的使用范围。Linux在2.6.17版本引入splice系统调用，不仅不需要硬件支持，还实现了两个文件描述符之间的数据零拷贝。

     ![img](images/a9e311bbb38e4ed9a1c4da98dbcf2388.png)

     > splice系统调用可以在内核空间的读缓冲区（read buffer）和网络缓冲区（socket buffer）之间建立管道（pipeline），从而避免了两者之间的CPU拷贝操作。
     >
     > **基于splice系统调用的零拷贝方式，整个拷贝过程会发生2次上下文切换，0次CPU拷贝以及2次DMA拷贝。**
     >
     > splice拷贝方式也同样存在用户程序不能对数据进行修改的问题。除此之外，它使用了Linux的管道缓冲机制，可以用于任意两个文件描述符中传输数据，但是它的两个文件描述符参数中有一个必须是管道设备。

     ![在这里插入图片描述](images/508fe7f721b94ac39e86f23b714b391c.png)

     > 在Kafka中消息存储模式中，数据存储在底层文件系统中。当有Consumer订阅了相应的Topic消息，数据需要从磁盘中读取然后将数据写回到套接字中（Socket）。此动作看似只需较少的CPU活动，但它的效率非常低：首先内核读出全盘数据，然后将数据跨越内核用户推到应用程序，然后应用程序再次跨越内核用户将数据推回，写出到套接字。
     >
     > Kafka Consumer执行上述过程中，使用了Java类库java.nio.channels.FileChannel中的transferTo()方法来实现零拷贝，transferTo()方法将数据从文件通道传输到了给定的可写字节通道。在内部，它依赖底层操作系统对零拷贝的支持；在UNIX 和各种Linux系统中，此调用被传递到sendfile()系统调用中。
     >
     > 所以，如果底层网络接口卡支持收集操作的话，Kafka Consumer实现零拷贝的方式为sendfile + DMA gather copy，否则为sendfile。

2. **Page Cache(页缓存)+磁盘顺序写**

   操作系统本身有一层缓存，叫做page cache，是在内存里的缓存，我们也可以称之为os cache，意思就是操作系统自己管理的缓存。

   你在写入磁盘文件的时候，可以直接写入这个os cache里，也就是仅仅写入内存中，接下来由操作系统自己决定什么时候把os cache里的数据真的刷入磁盘文件中。

   仅仅这一个步骤，就可以将磁盘文件写性能提升很多了，因为其实这里相当于是在写内存，不是在写磁盘。
   ![在这里插入图片描述](images/2d2f44793aea40b3a2027538a175ebb0.png)

   同时，Page Cache将数据flush到磁盘的时候，Kafka是以磁盘顺序写的方式来写的。也就是说，仅仅将数据追加到文件的末尾，不是在文件的随机位置来修改数据，避免了磁盘随机写性能差的问题。

   另外，Kafka读取数据的时候，也会先检查下Page Cache里是否存在待检索数据，若有则直接返回，否则再走零拷贝I/O流程。所以，如果Kafka的生产者和消费者的数据速率差不多时，会发现大量的数据都是直接写入os cache中，然后读数据的时候也是从os cache中读。相当于是Kafka完全基于内存提供数据的写和读，Page Cache实现了数据的“空中接力”。

   Kafka利用了操作系统本身的Page Cache，而没有使用JVM的空间内存。主要是：

   避免Object消耗：如果是使用Java堆，Java对象的内存消耗比较大，通常是所存储数据的两倍甚至更多。
   避免GC问题：随着JVM中数据不断增多，垃圾回收将会变得复杂与缓慢，使用系统缓存就不会存在GC问题。
   同时，相比于使用JVM或in-memory cache等数据结构，利用操作系统的Page Cache更加简单可靠。首先，操作系统层面的缓存利用率会更高，因为存储的都是紧凑的字节结构而不是独立的对象。其次，操作系统本身也对于Page Cache做了大量优化，提供了write-behind、read-ahead以及flush等多种机制。再者，即使服务进程重启，系统缓存依然不会消失，避免了in-process cache重建缓存的过程。

3. **分区分段+索引**

   Kafka的message是按topic分类存储的，topic中的数据又是按照一个一个的partition即分区存储到不同broker节点。每个partition对应了操作系统上的一个文件夹，partition实际上又是按照segment分段存储的。这也非常符合分布式系统分区分桶的设计思想。

   通过这种分区分段的设计，Kafka的message消息实际上是分布式存储在一个一个小的segment中的，每次文件操作也是直接操作的segment。为了进一步的查询优化，Kafka又默认为分段后的数据文件建立了稀疏索引文件，就是文件系统上的.index文件。这种分区分段+索引的设计，不仅提升了数据读取的效率，同时也提高了数据操作的并行度。

4. **批量读写**

   Kafka数据读写也是批量的而不是单条的。

   除了利用底层的技术外，Kafka还在应用程序层面提供了一些手段来提升性能。最明显的就是使用批次。在向Kafka写入数据时，可以启用批次写入，这样可以避免在网络上频繁传输单个消息带来的延迟和带宽开销。假设网络带宽为10MB/S，一次性传输10MB的消息比传输1KB的消息10000万次显然要快得多。

5. **批量压缩**

   Kafka使用了批量压缩，多个消息一起压缩。降低网络带宽。Kafka允许使用递归的消息集合，批量的消息可以通过压缩的形式传输并且在日志中也可以保持压缩格式，直到被消费者解压缩。

   当然，消息的压缩和解压缩需要消耗一定的CPU资源，用户需要均衡好CPU和带宽的关系。

