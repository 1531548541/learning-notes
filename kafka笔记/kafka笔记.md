# 安装

1. **检查是否安装jdk**，解压kafka包

2. 修改解压文件中的 service.properties(broker、主机、log目录)

   ````shell
   broker.id=0
   
   log.dirs=/opt/module/kafka/datas
   
   zookeeper.connect=kafka01:2181,kafka02:2181,kafka03:2181/kafka
   ````

3. 配置环境变量

   

4. 启动

# 介绍

1. **消息系统**： afka 和传统的消息系统（也称作消息中间件〉都具备系统解稿、冗余存储、流量 峰、缓冲、异步通信、扩展性、 可恢复性等功能。与此同时， Kafka供了大多数消息系统难以实现的消息 序性保障及回溯消费的功能。
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

**1****）**Kafka** **本身是分布式集群，可以采用分区技术，并行度高**

**2****）读数据采用稀疏索引，可以快速定位要消费的数据**

**3****）顺序写磁盘**

Kafka 的 producer 生产数据，要写入到 log 文件中，写的过程是一直追加到文件末端，

为顺序写。**官网有数据表明**，同样的磁盘，顺序写能到 600M/s，而随机写只有 100K/s。这

与磁盘的机械机构有关，顺序写之所以快，是因为其省去了大量磁头寻址的时间。

![image-20220615143644474](images\image-20220615143644474.png)

**4****）页缓存** **+** **零拷贝技术**

![image-20220615143758698](images\image-20220615143758698.png)

| 参数                        | 描述                                                         |
| :-------------------------- | ------------------------------------------------------------ |
| log.flush.interval.messages | 强制页缓存刷写到磁盘的条数，默认是 long 的最大值，一般不建议修改。 |
| log.flush.interval.ms       | 每隔多久，刷数据到磁盘，默认是 null。一般不建议修改。        |



## 生产者

### 核心参数

> batch size:批次大小，默认16k
>
> linger.ms:等待时间，修改为5-100ms
>
> compression.type:压缩
>
> RecordAccumulator:缓冲区大小，修改为64m

### 发送过程

![image-20220610142448052](images\image-20220610142448052.png)



### 分区策略

1. 指定分区
2. key不为空：hash(key)%分区数
3. 其他：粘性策略，随机一个分区，分区满（16k）或完成则选择其他分区（与前一个不重复）

### ACK应答级别

1. acks=0：生产者发送的数据不需要等数据落盘应答（类似异步）
2. acks=1：leader收到数据后应答
3. acks=-1(all)：leader和ISR队列中所有节点收齐数据后应答

**合理情况：ACK级别为-1 + 分区副本>=2 + ISR里应答的最小副本数>=2**

> 生产环境中，acks=0很少使用；acks=1一般用于传输普通日志，允许丢个别数据；acks=-1一般用于传输和钱相关的信息，保证高可靠性。
>
> **注意！！！ acks=-1会导致重复数据，leader挂了选举新的leader后，由于还未ack，收到重复消息**

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

​	收到所有分区的数据进行排序



### zookeeper中的kafka信息

![image-20220614105659275](images\image-20220614105659275.png)



## 消费者

### 消费者组

> 拥有同一个groupid的消费者即是一组
>
> - 消费者组内每个消费者负责消费不同分区的数据，一个分区只能由组内的一个消费者消费
> - 消费者组之间互不影响

### 消费过程

![image-20220615144405848](images\image-20220615144405848.png)

![image-20220615144712847](images\image-20220615144712847.png)

# 分区优化

## 优先副本的选举

在创建主题的时候，该主题的分区及副本会**尽可能均匀地**分布到 Kafka 集群的各个 broker节点上，对 leader 本的分配也比较均匀。 比如我们使用 kafka-topics sh 建一个分区数为 3、副本因子为3 的主题 topi partitions 创建之后的分布信息如下：

~~~sh
[root@nodel kafka 2 . 11 - 2 . 0 . 0 ]# bin/kafka- topics. sh --zookeeper localhost : 2181/ 
kafka --describe --topic topic - partitions 

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

为了能够有效治理负载失衡，Kafka 引入了**优先副本**（ preferred replica )的概念。优先副本是指在 AR 集合列表中的第一个副本，如上面主题 topic-partitions 分区AR 集合表（ Replicas ）为[1,2,0]，那么分区0的优先副本即为1。理想情况下，优先本就是该分区的 leader 副本，所 以也可以称之为 preferred leader。kafka 要确保所有主题的优先本在 Kafka 集群中均匀分布，这样就保证了所有分区 leader 均衡分布， 如果 leader分布过于集中，就会造成集群负载不均衡。

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

## 复制限流

> 

# 常用命令

~~~shell
#创建分区、副本
#创建生产者
bin/kafka-console-producer.sh --bootstrap-server node01:9092 --topic first
#创建消费者
bin/kafka-console-consumer.sh --bootstrap-server node01:9092 --topic first
~~~

