# 基本数据类型（V6.2为例）

> redis的数据结构比较特殊，本质上都是`kv`，过期时间也是针对k。

## string（SDS，动态字符串）

~~~c
struct __attribute__ ((__packed__)) sdshdr8 {
    uint8_t len; // 当前字符串的长度
    uint8_t alloc; // 剩余可用空间的长度
    unsigned char flags; //头类型，用来控制头大小
    char buf[];  // 存储实际字符串的字符数组
};
~~~



![image-20220309165005792](images/image-20220309165005792.png)

> 1. free:还剩多少空间 len:字符串长度 buf:存放的字符数组。
> 2. 空间预分配：SDS 被修改后，程序不仅会为 SDS 分配所需要的必须空间，还会分配额外的未使用空间。
> 3. 惰性空间释放：当对 SDS 进行缩短操作时，程序并不会回收多余的内存空间，而是使用 free 字段将这些字节数量记录下来不释放，后面如果需要 append 操作，则直接使用 free 中未使用的空间，减少了内存的分配。

> 总结：
>
> Redis 的字符串是动态字符串，是可以修改的字符串，内部结构实现上类似于 Java 的ArrayList，采用预分配冗余空间的方式来减少内存的频繁分配，如图中所示，内部为当前字符串实际分配的空间 capacity 一般要高于实际字符串长度 len。当字符串长度小于 1M 时，扩容都是加倍现有的空间，如果超过 1M，扩容时一次只会多扩 1M 的空间。需要注意的是字符串最大长度为 512M。
>
> ![image-20231204165449009](images/image-20231204165449009.png)

与C的string比，SDS优点：

1. 计算len复杂度为O(1)。
2. 调用api安全，自动扩容，防止缓冲区溢出。
3. 扩容频率低。
4. 原样保存数据，不会受到特殊字符（如\0）干扰（因为有len），这就可以保存二进制数据。

### 实战

**键值对** 

~~~shell
\> set name codehole 

OK 

\> get name 

"codehole"

\> exists name 

(integer) 1 

\> del name 

(integer) 1 

\> get name 

(nil) 
~~~



**批量键值对**

可以批量对多个字符串进行读写，节省网络耗时开销。

~~~sh
\> set name1 codehole 

OK 

\> set name2 holycoder 

OK 

\> mget name1 name2 name3 # 返回一个列表

1) "codehole" 

2) "holycoder" 

3) (nil) 

\> mset name1 boy name2 girl name3 unknown 

\> mget name1 name2 name3 

1) "boy" 

2) "girl" 

3) "unknown"
~~~



**过期和 set 命令扩展** 

可以对 key 设置过期时间，到点自动删除，这个功能常用来控制缓存的失效时间。

~~~sh
\> set name codehole 

\> get name "codehole" 

\> expire name 5 # 5s 后过期

... # wait for 5s 

\> get name 

(nil) 

\> setex name 5 codehole # 5s 后过期，等价于 set+expire

\> get name 

"codehole" 

... # wait for 5s 

\> get name 

(nil) 

\> setnx name codehole # 如果 name 不存在就执行 set 创建

(integer) 1 

\> get name 

"codehole" 

\> setnx name holycoder 

(integer) 0 # 因为 name 已经存在，所以 set 创建不成功

\> get name 

"codehole"

\# 没有改变
~~~



**计数** 

如果 value 值是一个整数，还可以对它进行自增操作。自增是有范围的，它的范围是

signed long 的最大最小值，超过了这个值，Redis 会报错。

~~~sh
\> set age 30 

OK 

\> incr age 

(integer) 31 

\> incrby age 5 

(integer) 36 

\> incrby age -5 

(integer) 31 

\> set codehole 9223372036854775807 

\# Long.Max 

OK 

\> incr codehole

(error) ERR increment or decrement would overflow 
~~~

## 链表

每个链表节点使用一个`adlist.h/listNode`结构来表示：

~~~c
typedef struct listNode {
    struct listNode *prev;
    struct listNode *next;
    void *value;
} listNode;
~~~

![image-20231205153113467](images/image-20231205153113467.png)

用`adlist.h/list`封装后：

~~~c
typedef struct list {
    listNode *head;
    listNode *tail;
    void *(*dup)(void *ptr);  //复制
    void (*free)(void *ptr);  //释放
    int (*match)(void *ptr, void *key); //节点值对比
    unsigned long len;
} list;
~~~

![image-20231205155033377](images/image-20231205155033377.png)

## list

> Redis 的列表相当于 Java 语言里面的 LinkedList，注意它是链表而不是数组。这意味着list 的插入和删除操作非常快，时间复杂度为 O(1)，但是索引定位很慢，时间复杂度为O(n)，这点让人非常意外。
>
> 当列表弹出了最后一个元素之后，该数据结构自动被删除，内存被回收。
>
> Redis 的列表结构常用来做异步队列使用。将需要延后处理的任务结构体序列化成字符串塞进 Redis 的列表，另一个线程从这个列表中轮询数据进行处理。

### 实战

**右边进左边出：队列** 

~~~shell
\> rpush books python java golang 

(integer) 3 

\> llen books 

(integer) 3 

\> lpop books 

"python" 

\> lpop books 

"java" 

\> lpop books 

"golang" 

\> lpop books 

(nil) 
~~~



**右边进右边出：栈** 

~~~sh
\> rpush books python java golang 

(integer) 3 

\> rpop books 

"golang" 

\> rpop books 

"java" 

\> rpop books 

"python" 

\> rpop books

(nil) 
~~~



**慢操作** 

> lindex 相当于 Java 链表的 get(int index)方法，它需要对链表进行遍历，性能随着参数index 增大而变差。 ltrim 和字面上的含义不太一样，个人觉得它叫 lretain(保留) 更合适一些，因为 ltrim 跟的两个参数 start_index 和 end_index 定义了一个区间，在这个区间内的值，ltrim 要保留，区间之外统统砍掉。我们可以通过 ltrim 来实现一个定长的链表，这一点非常有用。index 可以为负数，index=-1 表示倒数第一个元素，同样 index=-2 表示倒数第二个元素。

~~~sh
\> rpush books python java golang 

(integer) 3 

\> lindex books 1 # O(n) 慎用

"java" 

\> lrange books 0 -1 # 获取所有元素，O(n) 慎用

1) "python" 

2) "java" 

3) "golang" 

\> ltrim books 1 -1 # O(n) 慎用

OK 

\> lrange books 0 -1 

1) "java" 

2) "golang" 

\> ltrim books 1 0 # 这其实是清空了整个列表，因为区间范围长度为负

OK 

\> llen books 

(integer) 0
~~~



## 压缩数据结构

### ziplist

> 压缩列表是 List 、hash、 sorted Set 三种数据类型底层实现之一。
>
> 当一个列表只有少量数据的时候，并且每个列表项要么就是小整数值，要么就是长度比较短的字符串，那么 Redis 就会使用压缩列表来做列表键的底层实现。

![image-20231120145608039](images/image-20231120145608039.png)



### intset

> Redis 的 intset 是一个紧凑的整数数组结构，它用于存放元素都是整数的并且元素个数较少的 set 集合。
>
> 【升级】如果整数可以用 uint16 表示，那么 intset 的元素就是 16 位的数组，如果新加入的整数超过了 uint16 的表示范围，那么就使用 uint32 表示，如果新加入的元素超过了 uint32 的表示范围，那么就使用 uint64 表示，Redis 支持 set 集合动态从 uint16 升级到 uint32，再升级到 uint64。
>
> 【不支持降级】

由`intset.h/intset`结构表示：

~~~c
typedef struct intset {
    uint32_t encoding;  //编码方式：int16、int32、int64
    uint32_t length;    //元素个数
    int8_t contents[];  //数据
} intset;
~~~

![image-20231206141418945](images/image-20231206141418945.png)

因为是int16，所以contents数组大小=sizeof(int16_t)=16*5=80位

### 存储界限

当集合对象的元素不断增加，或者某个 value 值过大，这种小对象存储也会被升级为标准结构。Redis 规定在小对象存储结构的限制条件如下：

hash-max-zipmap-entries 512 # hash 的元素个数超过 512 就必须用标准结构存储

hash-max-zipmap-value 64 # hash 的任意元素的 key/value 的长度超过 64 就必须用标准结构存储

list-max-ziplist-entries 512 # list 的元素个数超过 512 就必须用标准结构存储

list-max-ziplist-value 64 # list 的任意元素的长度超过 64 就必须用标准结构存储

zset-max-ziplist-entries 128 # zset 的元素个数超过 128 就必须用标准结构存储

zset-max-ziplist-value 64 # zset 的任意元素的长度超过 64 就必须用标准结构存储

set-max-intset-entries 512 # set 的整数元素个数超过 512 就必须用标准结构存储

## quicklist

> **quicklist 是 ziplist 和 linkedlist 的混合体，它将 linkedlist 按段切分，每一段使用 ziplist 来紧凑存储，多个 ziplist 之间使用双向指针串接起来。**

![image-20220304174431088](images/image-20220304174431088.png)

## skiplist /zset

跳跃表由`server.h/zskiplist `结构来表示：

~~~c
typedef struct zskiplist {
    struct zskiplistNode *header, *tail;
    unsigned long length;  //节点数(header不纳入计算)
    int level;    //节点的最高层(header不纳入计算，见图理解)
} zskiplist;
~~~

节点由`server.h/zskiplistNode `结构来表示：

~~~c
typedef struct zskiplistNode {
    sds ele;
    double score;
    struct zskiplistNode *backward; //后退指针
    struct zskiplistLevel {
        struct zskiplistNode *forward; //同一层的节点通过 forward 指针连起来形成一个单向链表
        unsigned long span; //同层的这两个节点之间距离几个节点
    } level[];  //数组长度是在节点生成时随机生成的(1-32)
} zskiplistNode;
~~~

![img](images/v2-3f8b456d3e102e19933e67bf12eb80f8_r.jpg)

> sorted set 类型的排序功能便是通过「跳跃列表」数据结构来实现。
>
> 跳跃表（skiplist）是一种有序数据结构，它通过在每个节点中维持多个指向其他节点的指针，从而达到快速访问节点的目的。
>
> 跳表在链表的基础上，增加了多层级索引，通过索引位置的几个跳转，实现数据的快速定位，如下图所示：

![image-20220304174422245](images/image-20220304174422245.png)

## hash/dict

哈希表由`dict.h/dictht`结构定义（V7.0已经不存在）：

~~~c
typedef struct dictht {
    dictEntry **table; //数据
    unsigned long size; //大小
    unsigned long sizemask; //哈希表大小掩码，用于计算index，总是等于size-1
    unsigned long used; //已有节点的数量
} dictht;
~~~

hash中的元素`dict.h/dictEntity`结构定义：

~~~c
typedef struct dictEntry {
    void *key;
    union {
        void *val;
        uint64_t u64;
        int64_t s64;
        double d;
    } v;
    struct dictEntry *next;
} dictEntry;
~~~

![image-20231205162919837](images/image-20231205162919837.png)

封装成的dict由`dict.h/dict`结构定义：

~~~c
typedef struct dict {
    dictType *type; //类型特定函数
    void *privdata; //私有数据
    dictht ht[2]; //哈希表
    long rehashidx; //rehash索引，当不在rehash时，为-1
    int16_t pauserehash; /* If >0 rehash暂停 (<0 indicates coding error) */
} dict;
~~~

type属性和privdata属性是针对不通类型的键值对，为创建多态字典而设置的：

- type属性是一个指向dictType的指针，每个dictType保存用于操作特定类型键值对的函数，Redis会为用途不同的字典设置不同的类型特定函数。
- privdata保存了需要传给那些类型特定函数的可选参数。

~~~c
typedef struct dictType {
    //计算哈希值的函数
    uint64_t (*hashFunction)(const void *key);
    //复制key的函数
    void *(*keyDup)(void *privdata, const void *key);
    //复制val的函数
    void *(*valDup)(void *privdata, const void *obj);
    //key比较的函数
    int (*keyCompare)(void *privdata, const void *key1, const void *key2);
    //销毁key的函数
    void (*keyDestructor)(void *privdata, void *key);
    //销毁值的函数
    void (*valDestructor)(void *privdata, void *obj);
    //扩容的函数
    int (*expandAllowed)(size_t moreMem, double usedRatio);
} dictType;
~~~

![image-20231205164313689](images/image-20231205164313689.png)

### Hash 冲突怎么办？(拉链法+渐进式rehash)

Redis 通过**链式哈希**解决冲突：**也就是同一个 桶里面的元素使用链表保存**。但是当链表过长就会导致查找性能变差可能，所以 Redis 为了追求快，使用了两个全局哈希表。用于 rehash 操作，增加现有的哈希桶数量，减少哈希冲突。

开始默认使用 「hash 表 0 」保存键值对数据，「hash 表 1」 此刻没有分配空间。当数据越来越多触发 rehash 操作，则执行以下操作：

1. 给 「hash 表 1 」分配更大的空间；空间大小取决于要执行的操作，以及ht[0].used。

   - 如果是扩展操作，ht[1].size = ht[0].used*2，赋值后size需是2的n次幂。例如：ht[0].used=4，根据公式ht[1].size=8，8正好是2的n次幂。
   - 如果是收缩操作，ht[1].size = ht[0].used，赋值后size需是2的n次幂。
2. 将 「hash 表 0 」的数据重新映射拷贝到 「hash 表 1」 中；
3. 释放 「hash 表 0」 的空间，将「hash 表 1」设置为「hash 表 0」，创建空的「hash 表 1」。

**值得注意的是，将 hash 表 1 的数据重新映射到 hash 表 2 的过程中并不是一次性的，这样会造成 Redis 阻塞，无法提供服务。**

而是采用了**渐进式 rehash**，每次处理客户端请求的时候，先从「 hash 表 1」 中第一个索引开始，将这个位置的 所有数据拷贝到 「hash 表 2」 中，就这样将 rehash 分散到多次请求过程中，避免耗时阻塞。

### 渐进式 rehash

![image-20231111212956056](images/image-20231111212956056.png)

**渐进式 rehash** 会在 rehash 的同时，保留新旧两个 hash 结构，查询时会先在ht[0]查询，查询不到则去ht[1]查询，然后在后续的定时任务中以及 hash 的子指令中，循序渐进地将旧 hash 的内容一点点迁移到新的 hash 结构中。

另外，在rehash执行期间，新添加的字典会放入ht[1]，这保证了ht[0]元素越来越少。



### 实战

~~~sh
\> hset books java "think in java" # 命令行的字符串如果包含空格，要用引号括起来

(integer) 1 

\> hset books golang "concurrency in go" 

(integer) 1 

\> hset books python "python cookbook" 

(integer) 1 

\> hgetall books # entries()，key 和 value 间隔出现

1) "java" 

2) "think in java" 

3) "golang" 

4) "concurrency in go" 

5) "python" 

6) "python cookbook" 

\> hlen books 

(integer) 3 

\> hget books java 

"think in java" 

\> hset books golang "learning go programming" # 因为是更新操作，所以返回 0

(integer) 0 

\> hget books golang "learning go programming" 

\> hmset books java "effective java" python "learning python" golang "modern golang 

programming" # 批量 set 

OK 

# 老钱又老了一岁
> hincrby user-laoqian age 1 
(integer) 30
~~~

### set

> Redis 的集合相当于 Java 语言里面的 HashSet，它内部的键值对是`无序`的`唯一`的。它的内部实现相当于一个特殊的字典，字典中所有的 value 都是一个值 NULL。

#### 实战

~~~sh
> sadd books python 
(integer) 1 
> sadd bookspython # 重复
(integer) 0 
> sadd books java golang 
(integer) 2 
> smembers books # 注意顺序，和插入的并不一致，因为 set 是无序的
1) "java" 
2) "python" 
3) "golang" 
> sismember books java # 查询某个 value 是否存在，相当于 contains(o)
(integer) 1 
> sismember books rust 
(integer) 0 
> scard books # 获取长度相当于 count()
(integer) 3 
> spop books # 弹出一个
"java"
~~~

### zset

> 它类似于 Java 的 SortedSet 和 HashMap 的结合体，一方面它是一个 set，保证了内部value 的唯一性，另一方面它可以给每个 value 赋予一个 `score`，代表这个 value 的排序权重。`内部实现为「跳跃列表」`。

#### 实战

~~~sh
> zadd books 9.0 "think in java" 
(integer) 1 
> zadd books 8.9 "java concurrency" 
(integer) 1 
> zadd books 8.6 "java cookbook" 
(integer) 1 
> zrange books 0 -1 # 按 score 排序列出，参数区间为排名范围
1) "java cookbook" 
2) "java concurrency" 
3) "think in java" 
> zrevrange books 0 -1 # 按 score 逆序列出，参数区间为排名范围
1) "think in java" 
2) "java concurrency" 
3) "java cookbook" 
> zcard books # 相当于 count()
(integer) 3 
> zscore books "java concurrency" # 获取指定 value 的 score
"8.9000000000000004" # 内部 score 使用 double 类型进行存储，所以存在小数点精度问题
> zrank books "java concurrency" # 排名
(integer) 1 
> zrangebyscore books 0 8.91 # 根据分值区间遍历 zset
1) "java cookbook" 
2) "java concurrency" 
> zrangebyscore books -inf 8.91 withscores # 根据分值区间 (-∞, 8.91] 遍历 zset，同时返
回分值。inf 代表 infinite，无穷大的意思。
1) "java cookbook" 
2) "8.5999999999999996" 
3) "java concurrency" 
4) "8.9000000000000004" 
> zrem books "java concurrency" # 删除 value
(integer) 1 
> zrange books 0 -1 
1) "java cookbook" 
2) "think in java"
~~~

# 其他数据类型

## 位图（bitmap）

> 位图的内容其实就是普通的字符串，本质就是byte[]，所以读取的时候是按8bit转成ASCII对应的字符串。自动扩展，如果设置了某个偏移位置超出了现有的内容范围，就会自动将位数组进行零扩充。
>
> 「零存」就是使用 setbit 对位值进行逐个设置，「整存」就是使用字符串一次性填充所有位数组，覆盖掉旧值。

### 实战

将hello存入，注意：bitmap存数据是按`低位->高位`存储的。

![image-20231113094143025](images/image-20231113094143025.png)

**零存零取**

~~~sh
127.0.0.1:6379> setbit w 1 1
(integer) 0
127.0.0.1:6379> setbit w 2 1
(integer) 0
127.0.0.1:6379> setbit w 4 1
(integer) 0
127.0.0.1:6379> getbit w 1 # 获取某个具体位置的值 0/1
(integer) 1
127.0.0.1:6379> getbit w 2
(integer) 1
127.0.0.1:6379> getbit w 4
(integer) 1
127.0.0.1:6379> getbit w 5
(integer) 0
~~~

**整存零取**

~~~sh
127.0.0.1:6379> set w h # 整存
(integer) 0
127.0.0.1:6379> getbit w 1
(integer) 1
127.0.0.1:6379> getbit w 2
(integer) 1
127.0.0.1:6379> getbit w 4
(integer) 1
127.0.0.1:6379> getbit w 5
(integer) 0
~~~

如果对应位的字节是不可打印字符，redis-cli 会显示该字符的 16 进制形式

~~~sh
127.0.0.1:6379> setbit x 0 1
(integer) 0
127.0.0.1:6379> setbit x 1 1
(integer) 0
127.0.0.1:6379> get x
"\xc0"
~~~

**统计和查找**

bitcount 用来统计指定位置范围内 1 的个数，bitpos 用来查找指定范围内出现的第一个 0 或 1。

遗憾的是， start 和 end 参数是字节索引，也就是说`指定的位范围必须是 8 的倍数`，而不能任意指定。

```sh
127.0.0.1:6379> set w hello
OK
127.0.0.1:6379> bitcount w
(integer) 21
127.0.0.1:6379> bitcount w 0 0 # 第一个字符中 1 的位数
(integer) 3
127.0.0.1:6379> bitcount w 0 1 # 前两个字符中 1 的位数
(integer) 7
127.0.0.1:6379> bitpos w 0 # 第一个 0 位
(integer) 0
127.0.0.1:6379> bitpos w 1 # 第一个 1 位
(integer) 1
127.0.0.1:6379> bitpos w 1 1 1 # 从第二个字符算起，第一个 1 位
(integer) 9
127.0.0.1:6379> bitpos w 1 2 2 # 从第三个字符算起，第一个 1 位
(integer) 17
```

**读取/设置多位**

~~~sh
127.0.0.1:6379> set w hello
OK
127.0.0.1:6379> bitfield w get u4 0 # 从第一个位开始取 4 个位，结果是无符号数 (u)
(integer) 6
127.0.0.1:6379> bitfield w get u3 2 # 从第三个位开始取 3 个位，结果是无符号数 (u)
(integer) 5
127.0.0.1:6379> bitfield w get i4 0 # 从第一个位开始取 4 个位，结果是有符号数 (i)
1) (integer) 6
127.0.0.1:6379> bitfield w get i3 2 # 从第三个位开始取 3 个位，结果是有符号数 (i)
1) (integer) -3

127.0.0.1:6379> bitfield w get u4 0 get u3 2 get i4 0 get i3 2
1) (integer) 6
2) (integer) 5
3) (integer) 6
4) (integer) -3

127.0.0.1:6379> bitfield w set u8 8 97 # 从第 8 个位开始，将接下来的 8 个位用无符号数 97 替换
1) (integer) 101
127.0.0.1:6379> get w
"hallo"
~~~

## HyperLogLog

> 用来统计数量，例如统计网站的UV。
>
> 实现原理比较复杂，涉及数学和概率论！！！

### 实战

~~~sh
127.0.0.1:6379> pfadd codehole user1
(integer) 1
127.0.0.1:6379> pfcount codehole
(integer) 1
127.0.0.1:6379> pfadd codehole user2
(integer) 1
127.0.0.1:6379> pfcount codehole
(integer) 2
127.0.0.1:6379> pfadd codehole user3
(integer) 1
127.0.0.1:6379> pfcount codehole
(integer) 3
127.0.0.1:6379> pfadd codehole user4
(integer) 1
127.0.0.1:6379> pfcount codehole
(integer) 4
127.0.0.1:6379> pfadd codehole user5
(integer) 1
127.0.0.1:6379> pfcount codehole
(integer) 5
127.0.0.1:6379> pfadd codehole user6
(integer) 1
127.0.0.1:6379> pfcount codehole
(integer) 6
127.0.0.1:6379> pfadd codehole user7 user8 user9 user10
(integer) 1
127.0.0.1:6379> pfcount codehole
(integer) 10
~~~

**pfmerge 适合什么场合用？**

用于将多个 pf 计数值累加在一起形成一个新的 pf 值。比如在网站中我们有两个内容差不多的页面，运营说需要这两个页面的数据进行合并。其中页面的 UV 访问量也需要合并，那这个时候 pfmerge 就可以派上用场了。

## GeoHash

> 存储经纬度，用于计算距离、附近等，底层zset。

### geoadd

新增数据

~~~sh
127.0.0.1:6379> geoadd company 116.48105 39.996794 juejin
(integer) 1
127.0.0.1:6379> geoadd company 116.514203 39.905409 ireader
(integer) 1
127.0.0.1:6379> geoadd company 116.489033 40.007669 meituan
(integer) 1
127.0.0.1:6379> geoadd company 116.562108 39.787602 jd 116.334255 40.027400 xiaomi
(integer) 2
~~~

### geodist

计算两个元素之间的距离，携带集合名称、2 个名称和距离单位

~~~sh
127.0.0.1:6379> geodist company juejin ireader km
"10.5501"
127.0.0.1:6379> geodist company juejin meituan km
"1.3878"
127.0.0.1:6379> geodist company juejin jd km
"24.2739"
127.0.0.1:6379> geodist company juejin xiaomi km
"12.9606"
127.0.0.1:6379> geodist company juejin juejin km
"0.0000"
~~~

### geopos

获取集合中任意元素的经纬度坐标，可以一次获取多个。获取的经纬度坐标和 geoadd 进去的坐标有`轻微的误差`，原因是 geohash 对二维坐标进行的一维映射是有损的，通过映射再还原回来的值会出现较小的差别。

~~~sh
127.0.0.1:6379> geopos company juejin
1) 1) "116.48104995489120483"
 2) "39.99679348858259686"
127.0.0.1:6379> geopos company ireader
1) 1) "116.5142020583152771"
 2) "39.90540918662494363"
127.0.0.1:6379> geopos company juejin ireader
1) 1) "116.48104995489120483"
 2) "39.99679348858259686"
2) 1) "116.5142020583152771"
 2) "39.90540918662494363"
~~~

###  geohash

获取元素的经纬度编码字符串，上面已经提到，它是 base32 编码。 可以使用这个编码值去 http://geohash.org/${hash}中进行直接定位，它是 geohash 的标准编码值。

~~~sh
127.0.0.1:6379> geohash company ireader
1) "wx4g52e1ce0"
127.0.0.1:6379> geohash company juejin
1) "wx4gd94yjn0"
~~~

![image-20231117092627430](images/image-20231117092627430.png)

### georadiusbymember

查询指定元素附近的其它元素，它的参数非常复杂。

~~~sh
# 范围 20 公里以内最多 3 个元素按距离正排，它不会排除自身
127.0.0.1:6379> georadiusbymember company ireader 20 km count 3 asc
1) "ireader"
2) "juejin"
3) "meituan"
# 范围 20 公里以内最多 3 个元素按距离倒排
127.0.0.1:6379> georadiusbymember company ireader 20 km count 3 desc
1) "jd"
2) "meituan"
3) "juejin"
# 三个可选参数 withcoord withdist withhash 用来携带附加参数
# withdist 很有用，它可以用来显示距离
127.0.0.1:6379> georadiusbymember company ireader 20 km withcoord withdist withhash count 3 asc
1) 
 1) "ireader"
 2) "0.0000"
 3) (integer) 4069886008361398
 4) 1) "116.5142020583152771"
 2) "39.90540918662494363"
2) 
 1) "juejin"
 2) "10.5501"
 3) (integer) 4069887154388167
 4) 1) "116.48104995489120483"
 2) "39.99679348858259686"
3) 
 1) "meituan"
 2) "11.5748"
 3) (integer) 4069887179083478
 4) 1) "116.48903220891952515"
 2) "40.00766997707732031"
~~~

Redis 还提供了根据坐标值来查询附近的元素，这个指令更加有用，它可以根据用户的定位来计算「附近的车」，「附近的餐馆」等。它的参数和 georadiusbymember 基本一致，除了将目标元素改成经纬度坐标值。

~~~sh
127.0.0.1:6379> georadius company 116.514202 39.905409 20 km withdist count 3 asc
1) 1) "ireader"
 2) "0.0000"
2) 1) "juejin"
 2) "10.5501"
3) 1) "meituan"
 2) "11.5748"
~~~

在一个地图应用中，车的数据、餐馆的数据、人的数据可能会有百万千万条，如果使用Redis 的 Geo 数据结构，它们将全部放在一个 zset 集合中。在 Redis 的集群环境中，集合可能会从一个节点迁移到另一个节点，如果单个 key 的数据过大，会对集群的迁移工作造成较大的影响，在集群环境中单个 key 对应的数据量不宜超过 1M，否则会导致集群迁移出现卡顿现象，影响线上服务的正常运行。

所以，这里建议 Geo 的数据使用单独的 Redis 实例部署，不使用集群环境。如果数据量过亿甚至更大，就需要对 Geo 数据进行拆分，按国家拆分、按省拆分，按市拆分，在人口特大城市甚至可以按区拆分。这样就可以显著降低单个 zset 集合的大小。

# redisObject（V6.2为例）

~~~c
typeof struct redisObject {
    unsigned type:4; // 类型
    unsigned encoding:4; //编码
    void *ptr;  // 指向底层实现数据结构的指针
    
    //...
};
~~~



![image-20231206154445637](images/image-20231206154445637.png)

~~~sh
> set msg zfc
OK
> type msg
"string"
> lpush nums 1 2 3
(integer) 3
> type nums
"list"
> hset stu name zhangsan age 12
2
> type stu
"hash"
> sadd stuSet zhangsan lisi
(integer) 2
> type stuSet
"set"
> zadd stuZset 1 zhangsan 2 lisi
(integer) 2
> type stuZset
"zset"
~~~

![image-20231206155812435](images/image-20231206155812435.png)

![image-20231206155827524](images/image-20231206155827524.png)

## string

string对象的编码可以是int、raw、embstr。

### int

字符串对象保存的是整数值，并且整数值可以用long表示。

![image-20231207150718527](images/image-20231207150718527.png)

### embstr

字符串对象保存的是字符串值、`浮点数`，且长度<=32字节。

![image-20231207151017954](images/image-20231207151017954.png)

### raw

字符串对象保存的是字符串值，且长度>32字节。

![image-20231207150827164](images/image-20231207150827164.png)

`embstr VS raw`

都使用redisObject和sdshdr结构来表示字符串对象，但raw编码会调用两次内存分配函数分别创建redisObject和sdshdr。而embstr仅调用一次来分配一块连续的空间。

![image-20231207151452775](images/image-20231207151452775.png)

因此embstr的优势：

1. 内存分配次数少一次。
2. 释放内存次数当然也少一次。
3. 数据都保存在一块连续的内存中，能更好利用缓存带来的优势。

### 转换

不满足int、embstr、raw定义时会发生转换。

Redis没有为embstr编码的字符串对象编写任何的修改程序（只读），因此只存在 int->raw、embstr->raw。

## list

列表对象的编码可以是ziplist和linkedlist。

以下条件`任一不满足`时，ziplist->linkedlist：

- 列表对象保存的所有字符串元素的长度都 <8字节（配置list-max-ziplist-size）。
- 列表对象保存的元素数量 <512个（`TODO V3.0以后没有这项了貌似`）；

### ziplist

![image-20231207152547469](images/image-20231207152547469.png)

### linkedlist

![image-20231207152555318](images/image-20231207152555318.png)

其中StringObject简化了，实际上是：

![image-20231207152720839](images/image-20231207152720839.png)

## hash

哈希对象的编码可以是ziplist和hashtable。

默认配置：

以下条件`任一不满足`时，ziplist->hashtable：

- 哈希对象保存的所有kv对的k和v的字符串元素的长度都 <64字节（配置hash-max-ziplist-value）。
- 哈希对象保存的kv对数量 <512个（配置hash-max-ziplist-entries）；

### ziplist

头插法

![image-20231207153506684](images/image-20231207153506684.png)

![image-20231207153516716](images/image-20231207153516716.png)

### hashtable

![image-20231207153623052](images/image-20231207153623052.png)

## set

集合对象的编码可以是intset和hashtable。

以下条件`任一不满足`时，intset->hashtable：

- 集合对象保存的都是整数值。
- 集合对象保存的元素数量 <512个（配置set-max-intset-entries）；

### intset

![image-20231207154145298](images/image-20231207154145298.png)

### hashtable

![image-20231207154157381](images/image-20231207154157381.png)

## zset

有序集合对象的编码可以是ziplist和skiplist。

以下条件`任一不满足`时，ziplist->skiplist：

- 有序集合对象保存的所有元素长度都 <64字节（配置zset-max-ziplist-value）。
- 有序集合对象保存的元素数量 <128个（配置zset-max-ziplist-entries）；

### ziplist

![image-20231207154820892](images/image-20231207154820892.png)

![image-20231207154832108](images/image-20231207154832108.png)

### skiplist

![image-20231207155001778](images/image-20231207155001778.png)

# 数据库

## 服务器中的数据库

~~~c
struct redisServer {
	//...
    redisDb *db;  //一个数组，保存服务器中所有数据库
    //...
}
~~~

![image-20231208091054250](images/image-20231208091054250.png)

# 布隆过滤器

> 创建布隆时两个参数：初始大小、误判率。
>
> 存在误判：存在有可能不存在，不存在一定不存在。

原理：多hash计算得到多个index，将arr[index]=1。

![image-20231113134541354](images/image-20231113134541354.png)

## 空间占用估计

布隆过滤器有两个参数，第一个是预计元素的数量 n，第二个是错误率 f。公式根据这两个输入得到两个输出，第一个输出是位数组的长度 l，也就是需要的存储空间大小 (bit)，第二个输出是 hash 函数的最佳数量 k。hash 函数的数量也会直接影响到错误率，最佳的数量会有最低的错误率。

~~~sh
k=0.7*(l/n) # 约等于

f=0.6185^(l/n) # ^ 表示次方计算，也就是 math.pow
~~~

从公式中可以看出

 1、位数组相对越长 (l/n)，错误率 f 越低，这个和直观上理解是一致的

 2、位数组相对越长 (l/n)，hash 函数需要的最佳数量也越多，影响计算效率

 3、当一个元素平均需要 1 个字节 (8bit) 的指纹空间时 (l/n=8)，错误率大约为 2%

 4、错误率为 10%，一个元素需要的平均指纹空间为 4.792 个 bit，大约为 5bit

 5、错误率为 1%，一个元素需要的平均指纹空间为 9.585 个 bit，大约为 10bit

 6、错误率为 0.1%，一个元素需要的平均指纹空间为 14.377 个 bit，大约为 15bit

## 实际元素超出时，误判率会怎样变化

当实际元素超出预计元素时，错误率会有多大变化，它会急剧上升么，还是平缓地上升，这就需要另外一个公式，引入参数 t 表示实际元素和预计元素的倍数 t

~~~sh
f=(1-0.5^t)^k # 极限近似，k 是 hash 函数的最佳数量
~~~

当 t 增大时，错误率，f 也会跟着增大，分别选择错误率为 10%,1%,0.1% 的 k 值，画出它的曲线进行直观观察。

![image-20231113153146345](images/image-20231113153146345.png)

# 限流

> 需求：[a,b]时间窗口内，最多有n个请求。

## 简单限流

![image-20231113153358090](images/image-20231113153358090.png)

~~~java
public class SimpleRateLimiter {
 private Jedis jedis;
 public SimpleRateLimiter(Jedis jedis) {
 	this.jedis = jedis;
 }
 public boolean isActionAllowed(String userId, String actionKey, int period, int maxCount) {
     String key = String.format("hist:%s:%s", userId, actionKey);
     long nowTs = System.currentTimeMillis();
     Pipeline pipe = jedis.pipelined();
     pipe.multi();
     pipe.zadd(key, nowTs, "" + nowTs);
     pipe.zremrangeByScore(key, 0, nowTs - period * 1000);
     Response<Long> count = pipe.zcard(key);
     pipe.expire(key, period + 1);
     pipe.exec();
     pipe.close();
     return count.get() <= maxCount;
 }
 public static void main(String[] args) {
     Jedis jedis = new Jedis();
     SimpleRateLimiter limiter = new SimpleRateLimiter(jedis);
     for(int i=0;i<20;i++) {
        System.out.println(limiter.isActionAllowed("laoqian", "reply", 60, 5));
     }
 }
}
~~~

> 整体思路：每一个行为到来时，都维护一次时间窗口。将时间窗口外的记录全部清理掉，只保留窗口内的记录。zset 集合中只有 score 值非常重要，value 值没有特别的意义，只需要保证它是唯一的就可以了。
>
> 因为这几个连续的 Redis 操作都是针对同一个 key 的，使用 pipeline 可以显著提升Redis 存取效率。但这种方案也有缺点，因为它要记录时间窗口内所有的行为记录，如果这个量很大，比如限定 60s 内操作不得超过 100w 次这样的参数，它是不适合做这样的限流的，因为会消耗大量的存储空间。

## 漏斗限流

~~~java
public class FunnelRateLimiter {
    static class Funnel {
        int capacity;
        float leakingRate;
        int leftQuota;
        long leakingTs;
        public Funnel(int capacity, float leakingRate) {
            this.capacity = capacity;
            this.leakingRate = leakingRate;
            this.leftQuota = capacity;
            this.leakingTs = System.currentTimeMillis();
        }
        void makeSpace() {
            long nowTs = System.currentTimeMillis();
            long deltaTs = nowTs - leakingTs;
            int deltaQuota = (int) (deltaTs * leakingRate);
            if (deltaQuota < 0) { // 间隔时间太长，整数数字过大溢出
                this.leftQuota = capacity;
                this.leakingTs = nowTs;
                return;
            }
            if (deltaQuota < 1) { // 腾出空间太小，最小单位是 1
                return;
            }
            this.leftQuota += deltaQuota;
            this.leakingTs = nowTs;
            if (this.leftQuota > this.capacity) {
                this.leftQuota = this.capacity;
            }
        }
        boolean watering(int quota) {
            makeSpace();
            if (this.leftQuota >= quota) {
                this.leftQuota -= quota;
                return true;
            }
            return false;
        }
    }
    private Map<String, Funnel> funnels = new HashMap<>();
    public boolean isActionAllowed(String userId, String actionKey, int capacity, float leakingRate) {
        String key = String.format("%s:%s", userId, actionKey);
        Funnel funnel = funnels.get(key);
        if (funnel == null) {
            funnel = new Funnel(capacity, leakingRate);
            Redis 深度历险：核心原理与应用实践 | 钱文品 著
            第 74 页 共 226 页
            funnels.put(key, funnel);
        }
        return funnel.watering(1); // 需要 1 个 quota
    }
}
~~~

> Funnel 对象的 make_space 方法是漏斗算法的核心，其在每次灌水前都会被调用以触发漏水，给漏斗腾出空间来。能腾出多少空间取决于过去了多久以及流水的速率。Funnel 对象占据的空间大小不再和行为的频率成正比，它的空间占用是一个常量。
>
> 问题来了，分布式的漏斗算法该如何实现？能不能使用 Redis 的基础数据结构来搞定？
>
> 我们观察 Funnel 对象的几个字段，我们发现可以将 Funnel 对象的内容按字段存储到一个 hash 结构中，灌水的时候将 hash 结构的字段取出来进行逻辑运算后，再将新值回填到hash 结构中就完成了一次行为频度的检测。
>
> 但是有个问题，我们无法保证整个过程的原子性。从 hash 结构中取值，然后在内存里运算，再回填到 hash 结构，这三个过程无法原子化，意味着需要进行适当的加锁控制。而一旦加锁，就意味着会有加锁失败，加锁失败就需要选择重试或者放弃。如果重试的话，就会导致性能下降。如果放弃的话，就会影响用户体验。同时，代码的复杂度也跟着升高很多。这真是个艰难的选择，我们该如何解决这个问题呢？Redis-Cell 救星来了。

### Redis-Cell 

> Redis 4.0 提供了一个限流 Redis 模块，它叫 redis-cell。该模块也使用了漏斗算法，并提供了原子的限流指令。

![image-20231117091218687](images/image-20231117091218687.png)

上面这个指令的意思是允许「用户老钱回复行为」的频率为每 60s 最多 30 次(漏水速率)，漏斗的初始容量为 15，也就是说一开始可以连续回复 15 个帖子，然后才开始受漏水速率的影响。我们看到这个指令中漏水速率变成了 2 个参数，替代了之前的单个浮点数。用两个参数相除的结果来表达漏水速率相对单个浮点数要更加直观一些。

~~~sh
\> cl.throttle laoqian:reply 15 30 60

1) (integer) 0 # 0 表示允许，1 表示拒绝

2) (integer) 15 # 漏斗容量 capacity

3) (integer) 14 # 漏斗剩余空间 left_quota

4) (integer) -1 # 如果拒绝了，需要多长时间后再试(漏斗有空间了，单位秒)

5) (integer) 2 # 多长时间后，漏斗完全空出来(left_quota==capacity，单位秒)
~~~

在执行限流指令时，如果被拒绝了，就需要丢弃或重试。cl.throttle 指令考虑的非常周到，连重试时间都帮你算好了，直接取返回结果数组的第四个值进行 sleep 即可，如果不想阻塞线程，也可以异步定时任务来重试。

# scan

> 查询key，相比 key* 性能优，功能强。
>
> 1、复杂度虽然也是 O(n)，但是它是通过游标分步进行的，不会阻塞线程;
>
> 2、提供 limit 参数，可以控制每次返回结果的最大条数，limit 只是一个 hint，返回的
>
> 结果可多可少;
>
> 3、同 keys 一样，它也提供模式匹配功能;
>
> 4、服务器不需要为游标保存状态，游标的唯一状态就是 scan 返回给客户端的游标整数;
>
> 5、返回的结果可能会有重复，需要客户端去重复，这点非常重要;
>
> 6、遍历的过程中如果有数据修改，改动后的数据能不能遍历到是不确定的;
>
> 7、单次返回的结果是空的并不意味着遍历结束，而要看返回的游标值是否为零

## 实战

scan 参数提供了三个参数，第一个是 cursor 整数值，第二个是 key 的正则模式，第三个是遍历的 limit hint。第一次遍历时，cursor 值为 0，然后将返回结果中第一个整数值作为下一次遍历的 cursor。一直遍历到返回的 cursor 值为 0 时结束。

~~~sh
127.0.0.1:6379> scan 0 match key99* count 1000
1) "13976"
2) 
 1) "key9911"
 2) "key9974"
 3) "key9994"
 4) "key9910"
 5) "key9907"
 6) "key9989"
 7) "key9971"
 8) "key99"
 9) "key9966"
 10) "key992"
 11) "key9903"
 12) "key9905"
127.0.0.1:6379> scan 13976 match key99* count 1000
1) "1996"
2)
 1) "key9982"
 2) "key9997"
 3) "key9963"
 4) "key996"
 5) "key9912"
 6) "key9999"
 7) "key9921"
 8) "key994"
 9) "key9956"
 10) "key9919"
......
127.0.0.1:6379> scan 11687 match key99* count 1000
1) "0"
2) 
 1) "key9969"
 2) "key998"
 3) "key9986"
 4) "key9968"
 5) "key9965"
 6) "key9990"
 7) "key9915"
 8) "key9928"
 9) "key9908"
 10) "key9929"
 11) "key9944"
~~~

从上面的过程可以看到虽然提供的 limit 是 1000，但是返回的结果只有 10 个左右。因为这个 limit 不是限定返回结果的数量，而是限定服务器单次遍历的字典槽位数量(约等于)。如果将 limit 设置为 10，你会发现返回结果是空的，但是游标值不为零，意味着遍历还没结束。

~~~sh
127.0.0.1:6379> scan 0 match key99* count 10
1) "3072"
2) (empty list or set)
~~~



# 大key

- 查询大key时，用scan不合适，应使用–-bigkeys

~~~sh
redis-cli -h 127.0.0.1 -p 7001 –-bigkeys
#每隔 100 条 scan 指令就会休眠 0.1s，ops 就不会剧烈抬升
redis-cli -h 127.0.0.1 -p 7001 –-bigkeys -i 0.1
~~~

- 删除大key时，del不合适，应使用unlink

~~~sh
unlink key1
~~~

# flush

flushdb 和flushall 用来删库跑路，但比较缓慢，可以在后面添async异步处理。

~~~sh
> flushall async
OK
~~~

# redis多线程配置

> Redis将所有数据放在内存中，内存响应时长约为100ns，对于小数据包Redis服务器可以处理8-10w的QPS,这也是Redis的极限了，对于大部分公司来说，单线程的Redis已经足够了。

![image-20220116203802482](images/image-20220116203802482.png)

# 缓存击穿

![image-20220116211235263](images/image-20220116211235263.png)

# list应用场景

1. 微信公众号订阅消息
2. 评论列表（评论按时间倒序）

# I/O 多路复用模型

## 非阻塞 IO

当我们调用套接字的读写方法，默认它们是阻塞的，比如 read 方法要传递进去一个参数n，表示读取这么多字节后再返回，如果没有读够线程就会卡在那里，直到新的数据到来或者连接关闭了，read 方法才可以返回，线程才能继续处理。而 write 方法一般来说不会阻塞，除非内核为套接字分配的写缓冲区已经满了，write 方法就会阻塞，直到缓存区中有空闲空间挪出来了。

![image-20231117113924385](images/image-20231117113924385.png)

非阻塞 IO 在套接字对象上提供了一个选项 Non_Blocking，当这个选项打开时，读写方法不会阻塞，而是能读多少读多少，能写多少写多少。能读多少取决于内核为套接字分配的读缓冲区内部的数据字节数，能写多少取决于内核为套接字分配的写缓冲区的空闲空间字节数。读方法和写方法都会通过返回值来告知程序实际读写了多少字节。有了非阻塞 IO 意味着线程在读写 IO 时可以不必再阻塞了，读写可以瞬间完成然后线程可以继续干别的事了。

## 事件轮询 (多路复用)

非阻塞 IO 有个问题，那就是线程要读数据，结果读了一部分就返回了，线程如何知道何时才应该继续读。也就是当数据到来时，线程如何得到通知。写也是一样，如果缓冲区满了，写不完，剩下的数据何时才应该继续写，线程也应该得到通知。

![image-20231117114246404](images/image-20231117114246404.png)

事件轮询 API 就是用来解决这个问题的，最简单的事件轮询 API 是 select 函数，它是操作系统提供给用户程序的 API。输入是读写描述符列表 read_fds & write_fds，输出是与之对应的可读可写事件。同时还提供了一个 timeout 参数，如果没有任何事件到来，那么就最多等待 timeout 时间，线程处于阻塞状态。一旦期间有任何事件到来，就可以立即返回。时间过了之后还是没有任何事件到来，也会立即返回。拿到事件后，线程就可以继续挨个处理相应的事件。处理完了继续过来轮询。于是线程就进入了一个死循环，我们把这个死循环称为事件循环，一个循环为一个周期。每个客户端套接字 socket 都有对应的读写文件描述符。

~~~c
read_events, write_events = select(read_fds, write_fds, timeout)

for event in read_events:

 handle_read(event.fd)

for event in write_events:

 handle_write(event.fd)

handle_others() # 处理其它事情，如定时任务等
~~~

因为我们通过 select 系统调用同时处理多个通道描述符的读写事件，因此我们将这类系统调用称为多路复用 API。现代操作系统的多路复用 API 已经不再使用 select 系统调用，而改用 epoll(linux)和 kqueue(freebsd & macosx)，因为 select 系统调用的性能在描述符特别多时，性能会非常差。它们使用起来可能在形式上略有差异，但是本质上都是差不多的，都可以使用上面的伪代码逻辑进行理解。服务器套接字 serversocket 对象的读操作是指调用 accept 接受客户端新连接。何时有新连接到来，也是通过 select 系统调用的读事件来得到通知的。事件轮询 API 就是 Java 语言里面的 NIO 技术Java 的 NIO 并不是 Java 特有的技术，其它计算机语言都有这个技术，只不过换了一个词汇，不叫 NIO 而已。



Redis 采用 I/O 多路复用技术，并发处理连接。采用了 epoll + 自己实现的简单的事件框架。epoll 中的读、写、关闭、连接都转化成了事件，然后利用 epoll 的多路复用特性，绝不在 IO 上浪费一点时间。

Redis 线程不会阻塞在某一个特定的监听或已连接套接字上，也就是说，不会阻塞在某一个特定的客户端请求处理上。正因为此，Redis 可以同时和多个客户端连接并处理请求，从而提升并发性。

![image-20220304174406427](images/image-20220304174406427.png)

## 指令队列

Redis 会将每个客户端套接字都关联一个指令队列。客户端的指令通过队列来排队进行顺序处理，先到先服务。

## 响应队列

Redis 同样也会为每个客户端套接字关联一个响应队列。Redis 服务器通过响应队列来将指令的返回结果回复给客户端。 如果队列为空，那么意味着连接暂时处于空闲状态，不需要去获取写事件，也就是可以将当前的客户端描述符从 write_fds 里面移出来。等到队列有数据了，再将描述符放进去。避免 select 系统调用立即返回写事件，结果发现没什么数据可以写。出这种情况的线程会飙高 CPU。

## 定时任务

服务器处理要响应 IO 事件外，还要处理其它事情。比如定时任务就是非常重要的一件事。如果线程阻塞在 select 系统调用上，定时任务将无法得到准时调度。那 Redis 是如何解决这个问题的呢？Redis 的定时任务会记录在一个称为最小堆的数据结构中。这个堆中，最快要执行的任务排在堆的最上方。在每个循环周期，Redis 都会将最小堆里面已经到点的任务立即进行处理。处理完毕后，将最快要执行的任务还需要的时间记录下来，这个时间就是 select 系统调用的 timeout 参数。因为 Redis 知道未来 timeout 时间内，没有其它定时任务需要处理，所以可以安心睡眠 timeout 的时间。Nginx 和 Node 的事件处理原理和 Redis 也是类似的。

# 通信协议

> RESP(Redis Serialization Protocol)
>
> Redis 协议将传输的结构数据分为 5 种最小单元类型，单元结束时统一加上回车换行符号\r\n。
>
> 1、单行字符串 以 + 符号开头。
>
> 2、多行字符串 以 $ 符号开头，后跟字符串长度。
>
> 3、整数值 以 : 符号开头，后跟整数的字符串形式。
>
> 4、错误消息 以 - 符号开头。
>
> 5、数组 以 * 号开头，后跟数组的长度。

**单行字符串** hello world

+hello world\r\n

**多行字符串** hello world

$11\r\nhello world\r\n

多行字符串当然也可以表示单行字符串。

**整数** 1024

:1024\r\n

**错误** 参数类型错误

-WRONGTYPE Operation against a key holding the wrong kind of value

**数组** [1,2,3]

*3\r\n:1\r\n:2\r\n:3\r\n

**NULL** 用多行字符串表示，不过长度要写成-1。

$-1\r\n

**空串** 用多行字符串表示，长度填 0。`注意这里有两个\r\n。为什么是两个? 因为两个\r\n 之间,隔的是空串。`

$0\r\n\r\n

**客户端 -> 服务器**

客户端向服务器发送的指令只有一种格式，多行字符串数组。比如一个简单的 set 指令set author codehole 会被序列化成下面的字符串。

*3\r\n$3\r\nset\r\n$6\r\nauthor\r\n$8\r\ncodehole\r\n

在控制台输出这个字符串如下，可以看出这是很好阅读的一种格式。

*3

$3

set

$6

author

$8

codehole

# RDB、AOF

## RDB

在 Redis 执行「写」指令过程中，内存数据会一直变化。所谓的内存快照，指的就是 Redis 内存中的数据在某一刻的状态数据。

Redis 跟这个类似，就是把某一刻的数据以文件的形式拍下来，写到磁盘上。这个快照文件叫做 **RDB 文件，RDB 就是 Redis DataBase 的缩写。**在做数据恢复时，直接将 RDB 文件读入内存完成恢复。

**有两个严重性能开销**：

1. 频繁生成 RDB 文件写入磁盘，磁盘压力过大。会出现上一个 RDB 还未执行完，下一个又开始生成，陷入死循环。
2. fork 出 bgsave 子进程会阻塞主线程，主线程的内存越大，阻塞时间越长。

### 在生成 RDB 期间，Redis 可以同时处理写请求么？

可以的，Redis 使用操作系统的多进程**写时复制技术 COW(Copy On Write)** 来实现快照持久化，保证数据一致性。

Redis 在持久化时会调用 glibc 的函数`fork`产生一个子进程，快照持久化完全交给子进程来处理，父进程继续处理客户端请求。

当主线程执行写指令修改数据的时候，这个数据就会复制一份副本， `bgsave` 子进程读取这个副本数据写到 RDB 文件。

这既保证了快照的完整性，也允许主线程同时对数据进行修改，避免了对正常业务的影响。

![image-20220304175427330](images/image-20220304175427330.png)

## AOF

AOF 日志记录了自 Redis 实例创建以来所有的**修改**性指令序列。

### fsync

AOF 日志是以文件的形式存在的，当程序对 AOF 日志文件进行写操作时，实际上是将内容写到了内核为文件描述符分配的一个内存缓存中，然后内核会异步将脏数据刷回到磁盘的。这就意味着如果机器突然宕机，AOF 日志内容可能还没有来得及完全刷到磁盘中，这个时候就会出现日志丢失。那该怎么办？

Linux 的 glibc 提供了 fsync(int fd)函数可以将指定文件的内容强制从内核缓存刷到磁盘。只要 Redis 进程实时调用 fsync 函数就可以保证 aof 日志不丢失。但是 fsync 是一个磁盘 IO 操作，它很慢！

**在生产环境的服务器中**，Redis 通常是每隔 1s 左右执行一次 fsync 操作，周期 1s 是可以配置的。这是在数据安全性和性能之间做了一个折中，在保持高性能的同时，尽可能使得数据少丢失。

Redis 提供的 AOF 配置项`appendfsync`写回策略直接决定 AOF 持久化功能的效率和安全性。

- **always**：同步写回，写指令执行完毕立马将 `aof_buf`缓冲区中的内容刷写到 AOF 文件。
- **everysec**：每秒写回，写指令执行完，日志只会写到 AOF 文件缓冲区，每隔一秒就把缓冲区内容同步到磁盘。
- **no：** 操作系统控制，写执行执行完毕，把日志写到 AOF 文件内存缓冲区，由操作系统决定何时刷写到磁盘。

### 重写机制

AOF 记录的是每个「写」指令操作。不会像 RDB 全量快照导致性能损耗，但是执行速度没有 RDB 快，同时日志文件过大也会造成性能问题。

所以，Redis 设计了一个杀手锏「AOF 重写机制」，Redis 提供了 `bgrewriteaof`指令用于对 AOF 日志进行瘦身。

其原理就是开辟一个子进程对内存进行遍历转换成一系列 Redis 的操作指令，序列化到一个新的 AOF 日志文件中。序列化完毕后再将操作期间发生的增量 AOF 日志追加到这个新的 AOF 日志文件中，追加完毕后就立即替代旧的 AOF 日志文件，瘦身工作就完成了。

![image-20220304180504923](images/image-20220304180504923.png)

## 如何实现数据尽可能少丢失又能兼顾性能呢？

**混合持久化**。将 rdb 文件的内容和增量的 AOF 日志文件存在一起。这里的 AOF 日志不再是全量的日志，而是**自持久化开始到持久化结束的这段时间发生的增量 AOF 日志**，通常这部分 AOF 日志很小。

**在 Redis 重启的时候，可以先加载 rdb 的内容，然后再重放增量 AOF 日志就可以完全替代之前的 AOF 全量文件重放，重启效率因此大幅得到提升**。



# 管道（Pipeline）

> Redis 管道(Pipeline) 是一种用于`优化多个连续命令执行的机制`。通过使用管道，可以将多个命令一次性发送到 Redis 服务器，减少了网络往返延迟的开销，从而提高了操作的效率。管道本身并不是 Redis 服务器直接提供的技术，这个技术本质上是由`客户端提供`的。

![image-20231117164934312](images/image-20231117164934312.png)

上图就是一个完整的请求交互流程图。用文字来仔细描述一遍：

 1、客户端进程调用 write 将消息写到操作系统内核为套接字分配的发送缓冲 send 

buffer。

 2、客户端操作系统内核将发送缓冲的内容发送到网卡，网卡硬件将数据通过「网际路

由」送到服务器的网卡。

 3、服务器操作系统内核将网卡的数据放到内核为套接字分配的接收缓冲 recv buffer。

 4、服务器进程调用 read 从接收缓冲中取出消息进行处理。

 5、服务器进程调用 write 将响应消息写到内核为套接字分配的发送缓冲 send buffer。

 6、服务器操作系统内核将发送缓冲的内容发送到网卡，网卡硬件将数据通过「网际路

由」送到客户端的网卡。

 7、客户端操作系统内核将网卡的数据放到内核为套接字分配的接收缓冲 recv buffer。

 8、客户端进程调用 read 从接收缓冲中取出消息返回给上层业务逻辑进行处理。

 9、结束。

其中步骤 5~8 和 1~4 是一样的，只不过方向是反过来的，一个是请求，一个是响应。

我们开始以为 write 操作是要等到对方收到消息才会返回，但实际上不是这样的。write操作只负责将数据写到本地操作系统内核的发送缓冲然后就返回了。剩下的事交给操作系统内核异步将数据送到目标机器。但是如果发送缓冲满了，那么就需要等待缓冲空出空闲空间来，这个就是写操作 IO 操作的真正耗时。

我们开始以为 read 操作是从目标机器拉取数据，但实际上不是这样的。read 操作只负责将数据从本地操作系统内核的接收缓冲中取出来就了事了。但是如果缓冲是空的，那么就需要等待数据到来，这个就是读操作 IO 操作的真正耗时。

所以对于 value = redis.get(key)这样一个简单的请求来说，write 操作几乎没有耗时，直接写到发送缓冲就返回，而 read 就会比较耗时了，因为它要等待消息经过网络路由到目标机器处理后的响应消息,再回送到当前的内核读缓冲才可以返回。这才是一个网络来回的真正开销。

而对于管道来说，连续的 write 操作根本就没有耗时，之后第一个 read 操作会等待一个网络的来回开销，然后所有的响应消息就都已经回送到内核的读缓冲了，后续的 read 操作直接就可以从缓冲拿到结果，瞬间就返回了。

# 事务

> redis事务与传统数据库事务有区别：`仅部分回滚（不满足原子性）`

## 不满足原子性

~~~sh
> multi
OK
> set books iamastring
QUEUED
> incr books
QUEUED
> set poorman iamdesperate
QUEUED
> exec
1) OK
2) (error) ERR value is not an integer or out of range
3) OK
> get books
"iamastring"
> get poorman
"iamdesperate
~~~

## discard

用于丢弃事务缓存队列中的所有指令，在 exec 执行之前。

~~~sh
> get books
(nil)
> multi
OK
> incr books
QUEUED
> incr books
QUEUED
> discard
OK
> get books
(nil)
~~~

## Watch(乐观锁)

底层伪代码

~~~c
while True:
 do_watch()
 commands()
 multi()
 send_commands()
 try:
 exec()
 break
 except WatchError:
 continue
~~~

具体使用

~~~sh
> watch books
OK
> incr books # 被修改了
(integer) 1
> multi
OK
> incr books
QUEUED
> exec # 事务执行失败
(nil)
~~~



watch 会在事务开始之前盯住 1 个或多个关键变量，当事务执行时，也就是服务器收到了 exec 指令要顺序执行缓存的事务队列时，Redis 会检查关键变量自 watch 之后，是否被修改了 (包括当前事务所在的客户端)。如果关键变量被人动过了，exec 指令就会返回 null 回复告知客户端事务执行失败，这个时候客户端一般会选择重试。不过也有些语言 (jedis) 不会抛出异常，而是通过在 exec 方法里返回一个 null，这样客户端需要检查一下返回结果是否为 null 来确定事务是否执行失败。

# 数据过期清理策略

## 过期键清理策略

- 定时删除，为每个过期键建立一个timer，缺点占用CPU

- 惰性删除，键获取的时候判断过期再清除，对内存不友好。

- `Redis使用惰性删除和定期删除结合的方式配合使用。`

- 定期删除，即根据设定执行时长和操作频率清理，缺点难以确定。

  > Redis 底层会通过限制删除操作执行的时长和频率来减少删除操作对CPU时间的影响，默认100ms就随机抽一些设置了过期时间的key，不会扫描全部的过期键，因为开销过大。
  >
  > 从过期字典中随机取20个key，删除已过期的key，如果过期key超过四分之一，重复上述步骤。

redis在内存空间不足的时候，为了保证命中率，就会选择一定的数据淘汰策略——**内存淘汰机制（过期键的补充措施）**

## 内存淘汰机制

内存淘汰机制：八种大体上可以分为4种，lru（最近最少使用）、lfu（最少使用频率）、random（随机）、ttl（根据生存时间，快过期）。

1. volatile-lru：从已设置过期时间的数据集中挑选最近最少使用的数据淘汰。
2. volatile-ttl：从已设置过期时间的数据集中挑选将要过期的数据淘汰。
3. volatile-random：从已设置过期时间的数据集中任意选择数据淘汰。
4. volatile-lfu：从已设置过期时间的数据集挑选使用频率最低的数据淘汰。
5. allkeys-lru：从数据集中挑选最近最少使用的数据淘汰
6. allkeys-lfu：从数据集中挑选使用频率最低的数据淘汰。
7. allkeys-random：从数据集（server.db[i].dict）中任意选择数据淘汰
8. no-enviction（驱逐）：禁止驱逐数据，这也是默认策略。意思是当内存不足以容纳新入数据时，新写入操作就会报错，请求可以继续进行，线上任务也不能持续进行，采用no-enviction策略可以保证数据不被丢失。

## 从库的过期策略

从库不会进行过期扫描，从库对过期的处理是被动的。主库在 key 到期时，会在 AOF 文件里增加一条 del 指令，同步到所有的从库，从库通过执行这条 del 指令来删除过期的key。

因为指令同步是异步进行的，所以主库过期的 key 的 del 指令没有及时同步到从库的话，会出现主从数据的不一致，主库没有的数据在从库里还存在，比如上一节的集群环境分布式锁的算法漏洞就是因为这个同步延迟产生的。

## 更多异步删除点

Redis 回收内存除了 del 指令和 flush 之外，还会存在于在 key 的过期、LRU 淘汰、rename 指令以及从库全量同步时接受完 rdb 文件后会立即进行的 flush 操作。

Redis4.0 为这些删除点也带来了异步删除机制，打开这些点需要额外的配置选项。

 1、slave-lazy-flush 从库接受完 rdb 文件后的 flush 操作

 2、lazyfree-lazy-eviction 内存达到 maxmemory 时进行淘汰

 3、lazyfree-lazy-expire key 过期删除

 4、lazyfree-lazy-server-del rename 指令删除 destKey

# 主从同步

## CAP 原理

CAP 原理就好比分布式领域的牛顿定律，它是分布式存储的理论基石。自打 CAP 的论

文发表之后，分布式存储中间件犹如雨后春笋般一个一个涌现出来。理解这个原理其实很简

单，本节我们首先对这个原理进行一些简单的讲解。

-  **C** - **C**onsistent ，一致性

-  **A** - **A**vailability ，可用性

-  **P** - **P**artition tolerance ，分区容忍性

分布式系统的节点往往都是分布在不同的机器上进行网络隔离开的，这意味着必然会有网络断开的风险，这个网络断开的场景的专业词汇叫着「**网络分区**」。

在网络分区发生时，两个分布式节点之间无法进行通信，我们对一个节点进行的修改操作将无法同步到另外一个节点，所以数据的「一致性」将无法满足，因为两个分布式节点的数据不再保持一致。除非我们牺牲「可用性」，也就是暂停分布式节点服务，在网络分区发生时，不再提供修改数据的功能，直到网络状况完全恢复正常再继续对外提供服务。

一句话概括 CAP 原理就是——**网络分区发生时，一致性和可用性两难全**。

## 最终一致

Redis 的主从数据是异步同步的，所以分布式的 Redis 系统并不满足「**一致性**」要求。当客户端在 Redis 的主节点修改了数据后，立即返回，即使在主从网络断开的情况下，主节点依旧可以正常对外提供修改服务，所以 Redis 满足「**可用性**」。Redis 保证「**最终一致性**」，从节点会努力追赶主节点，最终从节点的状态会和主节点的状态将保持一致。如果网络断开了，主从节点的数据将会出现大量不一致，一旦网络恢复，从节点会采用多种策略努力追赶上落后的数据，继续尽力保持和主节点一致。

## 主从同步

Redis 同步支持主从同步和从从同步，从从同步功能是 Redis 后续版本增加的功能，为了减轻主库的同步负担。后面为了描述上的方便，统一理解为主从同步。

## 增量同步

Redis 同步的是指令流，主节点会将那些对自己的状态产生修改性影响的指令记录在本地的内存 buffer 中，然后异步将 buffer 中的指令同步到从节点，从节点一边执行同步的指令流来达到和主节点一样的状态，一遍向主节点反馈自己同步到哪里了 (偏移量)。因为内存的 buffer 是有限的，所以 Redis 主库不能将所有的指令都记录在内存 buffer 中。Redis 的复制内存 buffer 是一个定长的环形数组，如果数组内容满了，就会从头开始覆盖前面的内容。

![image-20231121103850901](images/image-20231121103850901.png)

如果因为网络状况不好，从节点在短时间内无法和主节点进行同步，那么当网络状况恢复时，Redis 的主节点中那些没有同步的指令在 buffer 中有可能已经被后续的指令覆盖掉了，从节点将无法直接通过指令流来进行同步，这个时候就需要用到更加复杂的同步机制 —— 快照同步。

## 快照同步

快照同步是一个非常耗费资源的操作，它首先需要在主库上进行一次 bgsave 将当前内存的数据全部快照到磁盘文件中，然后再将快照文件的内容全部传送到从节点。从节点将快照文件接受完毕后，立即执行一次全量加载，加载之前先要将当前内存的数据清空。加载完毕后通知主节点继续进行增量同步。

在整个快照同步进行的过程中，主节点的复制 buffer 还在不停的往前移动，如果快照同步的时间过长或者复制 buffer 太小，都会导致同步期间的增量指令在复制 buffer 中被覆盖，这样就会导致快照同步完成后无法进行增量复制，然后会再次发起快照同步，如此极有可能会陷入快照同步的死循环。

![image-20231121104026380](images/image-20231121104026380.png)

所以务必配置一个合适的复制 buffer 大小参数，避免快照复制的死循环。

## 增加从节点

当从节点刚刚加入到集群时，它必须先要进行一次快照同步，同步完成后再继续进行增量同步。

## 无盘复制

主节点在进行快照同步时，会进行很重的文件 IO 操作，特别是对于非 SSD 磁盘存储时，快照会对系统的负载产生较大影响。特别是当系统正在进行 AOF 的 fsync 操作时如果发生快照，fsync 将会被推迟执行，这就会严重影响主节点的服务效率。

所以从 Redis 2.8.18 版开始支持无盘复制。所谓无盘复制是指主服务器直接通过套接字将快照内容发送到从节点，生成快照是一个遍历的过程，主节点会一边遍历内存，一遍将序列化的内容发送到从节点，从节点还是跟之前一样，先将接收到的内容存储到磁盘文件中，再进行一次性加载。

## Wait 指令

Redis 的复制是异步进行的，wait 指令可以让异步复制变身同步复制，确保系统的强一致性 (不严格)。wait 指令是 Redis3.0 版本以后才出现的。

~~~sh
> set key value
OK
> wait 1 0
(integer) 1
~~~

wait 提供两个参数，第一个参数是从库的数量 N，第二个参数是时间 t，以毫秒为单位。它表示等待 wait 指令之前的所有写操作同步到 N 个从库 (也就是确保 N 个从库的同步没有滞后)，最多等待时间 t。如果时间 t=0，表示无限等待直到 N 个从库同步完成达成一致。

假设此时出现了网络分区，wait 指令第二个参数时间 t=0，主从同步无法继续进行，wait 指令会永远阻塞，Redis 服务器将丧失可用性。

# Sentinel

> Redis Sentinel 集群看成是一个 ZooKeeper 集群，它是集群高可用的心脏，它一般是由 3～5 个节点组成，这样挂了个别节点集群还可以正常运转。它负责持续监控主从节点的健康，当主节点挂掉时，自动选择一个最优的从节点切换为主节点。客户端来连接集群时，会首先连接 sentinel，通过 sentinel 来查询主节点的地址，然后再去连接主节点进行数据交互。当主节点发生故障时，客户端会重新向 sentinel 要地址，sentinel 会将最新的主节点地址告诉客户端。如此应用程序将无需重启即可自动完成节点切换。比如上图的主节点挂掉后，集群将可能自动调整为下图所示结构。

![image-20231122194241038](images/image-20231122194241038.png)

从这张图中我们能看到主节点挂掉了，原先的主从复制也断开了，客户端和损坏的主节点也断开了。从节点被提升为新的主节点，其它从节点开始和新的主节点建立复制关系。客户端通过新的主节点继续进行交互。Sentinel 会持续监控已经挂掉了主节点，待它恢复后，集群会调整为下面这张图。

![image-20231122194335241](images/image-20231122194335241.png)

## 消息丢失

Redis 主从采用异步复制，意味着当主节点挂掉时，从节点可能没有收到全部的同步消息，这部分未同步的消息就丢失了。如果主从延迟特别大，那么丢失的数据就可能会特别多。Sentinel 无法保证消息完全不丢失，但是也尽可能保证消息少丢失。它有两个选项可以限制主从延迟过大。

min-slaves-to-write 1

min-slaves-max-lag 10

## 如何使用

TODO

# Codis

> Redis 集群方案之一，开源、使用 Go 语言开发，它是一个代理中间件，它和 Redis 一样也使用 Redis 协议对外提供服务，当客户端向 Codis 发送指令时，Codis 负责将指令转发到后面的 Redis 实例来执行，并将返回结果再转回给客户端。
>
> Codis 上挂接的所有 Redis 实例构成一个 Redis 集群，当集群空间不足时，可以通过动态增加 Redis 实例来实现扩容需求。
>
> 客户端操纵 Codis 同操纵 Redis 几乎没有区别，还是可以使用相同的客户端 SDK，不需要任何变化。
>
> 因为 Codis 是无状态的，它只是一个转发代理中间件，这意味着我们可以启动多个Codis 实例，供客户端使用，每个 Codis 节点都是对等的。因为单个 Codis 代理能支撑的QPS 比较有限，通过启动多个 Codis 代理可以显著增加整体的 QPS 需求，还能起到容灾功能，挂掉一个 Codis 代理没关系，还有很多 Codis 代理可以继续服务。

![image-20231122195420073](images/image-20231122195420073.png)

## Codis 分片原理

Codis 要负责将特定的 key 转发到特定的 Redis 实例，那么这种对应关系 Codis 是如何管理的呢？

Codis 将所有的 key 默认划分为 1024 个槽位(slot)，它首先对客户端传过来的 key 进行 crc32 运算计算哈希值，再将 hash 后的整数值对 1024 这个整数进行取模得到一个余数，这个余数就是对应 key 的槽位。

![image-20231122195548008](images/image-20231122195548008.png)

每个槽位都会唯一映射到后面的多个 Redis 实例之一，Codis 会在内存维护槽位和Redis 实例的映射关系。这样有了上面 key 对应的槽位，那么它应该转发到哪个 Redis 实例就很明确了。

槽位数量默认是 1024，它是可以配置的，如果集群节点比较多，建议将这个数值配置大一些，比如 2048、4096。

~~~sh
hash = crc32(command.key)
slot_index = hash % 1024
redis = slots[slot_index].redis
redis.do(command)
~~~

## 不同的 Codis 实例之间槽位关系如何同步？

如果 Codis 的槽位映射关系只存储在内存里，那么不同的 Codis 实例之间的槽位关系就无法得到同步。所以 Codis 还需要一个分布式配置存储数据库专门用来持久化槽位关系。Codis 开始使用 ZooKeeper，后来连 etcd 也一块支持了。

![image-20231122195730075](images/image-20231122195730075.png)

Codis 将槽位关系存储在 zk 中，并且提供了一个 Dashboard 可以用来观察和修改槽位关系，当槽位关系变化时，Codis Proxy 会监听到变化并重新同步槽位关系，从而实现多个Codis Proxy 之间共享相同的槽位关系配置。

## 扩容

刚开始 Codis 后端只有一个 Redis 实例，1024 个槽位全部指向同一个 Redis。然后一个 Redis 实例内存不够了，所以又加了一个 Redis 实例。这时候需要对槽位关系进行调整，将一半的槽位划分到新的节点。这意味着需要对这一半的槽位对应的所有 key 进行迁移，迁移到新的 Redis 实例。

**那** **Codis** **如果找到槽位对应的所有** **key** **呢？**

Codis 对 Redis 进行了改造，增加了 SLOTSSCAN 指令，可以遍历指定 slot 下所有的key。Codis 通过 SLOTSSCAN 扫描出待迁移槽位的所有的 key，然后挨个迁移每个 key 到新的 Redis 节点。

在迁移过程中，Codis 还是会接收到新的请求打在当前正在迁移的槽位上，因为当前槽位的数据同时存在于新旧两个槽位中，Codis 如何判断该将请求转发到后面的哪个具体实例呢？

Codis 无法判定迁移过程中的 key 究竟在哪个实例中，所以它采用了另一种完全不同的思路。当 Codis 接收到位于正在迁移槽位中的 key 后，会立即强制对当前的单个 key 进行迁移，迁移完成后，再将请求转发到新的 Redis 实例。

~~~sh
slot_index = crc32(command.key) % 1024
if slot_index in migrating_slots:
do_migrate_key(command.key) # 强制执行迁移
redis = slots[slot_index].new_redis
else:
redis = slots[slot_index].redis
redis.do(command)
~~~

我们知道 Redis 支持的所有 Scan 指令都是无法避免重复的，同样 Codis 自定义的SLOTSSCAN 也是一样，但是这并不会影响迁移。因为单个 key 被迁移一次后，在旧实例中它就彻底被删除了，也就不可能会再次被扫描出来了。

## 自动均衡

Redis 新增实例，手工均衡 slots 太繁琐，所以 Codis 提供了自动均衡功能。自动均衡会在系统比较空闲的时候观察每个 Redis 实例对应的 Slots 数量，如果不平衡，就会自动进行迁移。

## 缺点

**不支持事务**

一个事务可能对多个key做了操作，但事务只能在单个实例中完成，但是由于key分散在不同的实例中，因此Codis无法支持事务操作。

**不支持rename**

rename将一个key命名成另一个key，但是这两个key可能hash出来的槽位并不是同一个，而是在不同实例的槽位上，因此rename也不被支持。

官方提供的不支持的指令列表：https://github.com/CodisLabs/codis/blob/master/doc/unsupported_cmds.md

**扩容卡顿**

Codis在扩容过程中，对数据的迁移是将整个key直接迁移过去的，例如一个hash结构，Codis会直接 hgetall 拉取所有的内容，使用 hmset 放到 新节点中，

如果该hash的内容过大，将会引起卡顿，官方建议单个集合结构的总大小不超过1MB，在业务上可以通过分桶存储等，将大型数据拆成多个小的，做一个折中。

**网络开销**

由于 Codis 在 客户端与Redis实例之间充当网络Proxy，多了一层，网络开销自然多一些，比直接连接Redis的性能要稍低一些。

**中间件运维开销**

Codis集群配置需要使用Zk或Etcd，这意味着引入Codis集群又要引入其他中间件，增加运维机器资源成本。

## 优点

**简单**

Codis将分布式一致性的问题交给了第三方(ZK或Etcd)负责，省去了这方面的维护工作，降低实现代码的复杂性，

Redis官方的Cluster为了实现去中心化，引入了Raft与Gossip协议，以及大量需要调优的配置参数，复杂度骤增。

## MGET 指令

![image-20231122200622117](images/image-20231122200622117.png)

mget 指令用于批量获取多个 key 的值，这些 key 可能会分布在多个 Redis 实例中。Codis 的策略是将 key 按照所分配的实例打散分组，然后依次对每个实例调用 mget 方法，最后将结果汇总为一个，再返回给客户端。

## 后台管理

Codis 提供 Dashboard 界面化，以及 Codis-fe 对集群进行管理，还可以进行增加分组、节点、执行自动均衡等操作，查看 slot 状态以及 slot 对应的 redis 实例，这些功能使的运维更加方便轻松。

![img](images/27004470f512699dff0fb535cc3d8f3d.png)

![img](images/6ce84661a8c4c79b3b060091d4a237ae.png)

![img](images/1e94e7bc106c8d100cdc5c5de6127afe.png)

![img](images/9da83fdc948f4cd2a942600999083635.png)

# RedisCluster

> 是Redis官方的集群方案。与Codis 不同，它是去中心化的，如图所示，该集群有三个 Redis 节点组成，每个节点负责整个集群的一部分数据，每个节点负责的数据多少可能不一样。这三个节点相互连接组成一个对等的集群，它们之间通过一种特殊的二进制协议相互交互集群信息。
>
> ![image-20231122201449250](images/image-20231122201449250.png)
>
> Redis Cluster 将所有数据划分为 16384 的 slots，它比 Codis 的 1024 个槽划分的更为精细，每个节点负责其中一部分槽位。槽位的信息存储于每个节点中，它不像 Codis，它不需要另外的分布式存储来存储节点槽位信息。
>
> 当 Redis Cluster 的客户端来连接集群时，它也会得到一份集群的槽位配置信息。这样当客户端要查找某个 key 时，可以直接定位到目标节点。
>
> 这点不同于 Codis，Codis 需要通过 Proxy 来定位目标节点，RedisCluster 是直接定位。客户端为了可以直接定位某个具体的 key 所在的节点，它就需要缓存槽位相关信息，这样才可以准确快速地定位到相应的节点。同时因为槽位的信息可能会存在客户端与服务器不一致的情况，还需要纠正机制来实现槽位信息的校验调整。
>
> 另外，RedisCluster 的每个节点会将集群的配置信息持久化到配置文件中，所以必须确保配置文件是可写的，而且尽量不要依靠人工修改配置文件。

## 槽位定位算法

Cluster 默认会对 key 值使用 crc32 算法进行 hash 得到一个整数值，然后用这个整数值对 16384 进行取模来得到具体槽位。

Cluster 还允许用户强制某个 key 挂在特定槽位上，通过在 key 字符串里面嵌入 tag 标记，这就可以强制 key 所挂在的槽位等于 tag 所在的槽位。

~~~python
def HASH_SLOT(key)
 s = key.index "{"
 if s
 e = key.index "}",s+1
 if e && e != s+1
 key = key[s+1..e-1]
 end
 end
 crc16(key) % 16384
end
~~~

## 跳转

当客户端向一个错误的节点发出了指令，该节点会发现指令的 key 所在的槽位并不归自己管理，这时它会向客户端发送一个特殊的跳转指令携带目标操作的节点地址，告诉客户端去连这个节点去获取数据。

~~~sh
GET x
-MOVED 3999 127.0.0.1:6381
~~~

MOVED 指令的第一个参数 3999 是 key 对应的槽位编号，后面是目标节点地址。MOVED 指令前面有一个减号，表示该指令是一个错误消息。

客户端收到 MOVED 指令后，要立即纠正本地的槽位映射表。后续所有 key 将使用新的槽位映射表。

## 迁移

Redis Cluster 提供了工具 redis-trib 可以让运维人员手动调整槽位的分配情况，它使用Ruby 语言进行开发，通过组合各种原生的 Redis Cluster 指令来实现。这点 Codis 做的更加人性化，它不但提供了 UI 界面可以让我们方便的迁移，还提供了自动化平衡槽位工具，无需人工干预就可以均衡集群负载。不过 Redis 官方向来的策略就是提供最小可用的工具，其它都交由社区完成。

**迁移过程**

![image-20231122201951470](images/image-20231122201951470.png)

Redis 迁移的单位是槽，Redis 一个槽一个槽进行迁移，当一个槽正在迁移时，这个槽就处于中间过渡状态。这个槽在原节点的状态为 migrating，在目标节点的状态为 importing，表示数据正在从源流向目标。

迁移工具 redis-trib 首先会在源和目标节点设置好中间过渡状态，然后一次性获取源节点槽位的所有 key 列表(keysinslot 指令，可以部分获取)，再挨个 key 进行迁移。每个 key 的迁移过程是以原节点作为目标节点的「客户端」，原节点对当前的 key 执行 dump 指令得到序列化内容，然后通过「客户端」向目标节点发送指令 restore 携带序列化的内容作为参数，目标节点再进行反序列化就可以将内容恢复到目标节点的内存中，然后返回「客户端」OK，原节点「客户端」收到后再把当前节点的 key 删除掉就完成了单个 key 迁移的整个过程。

**从源节点获取内容** **=>** **存到目标节点** **=>** **从源节点删除内容。**

注意这里的迁移过程是同步的，在目标节点执行 restore 指令到原节点删除 key 之间，原节点的主线程会处于阻塞状态，直到 key 被成功删除。如果迁移过程中突然出现网络故障，整个 slot 的迁移只进行了一半。这时两个节点依旧处于中间过渡状态。待下次迁移工具重新连上时，会提示用户继续进行迁移。

在迁移过程中，如果每个 key 的内容都很小，migrate 指令执行会很快，它就并不会影响客户端的正常访问。如果 key 的内容很大，因为 migrate 指令是阻塞指令会同时导致原节点和目标节点卡顿，影响集群的稳定型。所以在集群环境下业务逻辑要尽可能避免大 key 的产生。

在迁移过程中，客户端访问的流程会有很大的变化。

首先新旧两个节点对应的槽位都存在部分 key 数据。客户端先尝试访问旧节点，如果对应的数据还在旧节点里面，那么旧节点正常处理。如果对应的数据不在旧节点里面，那么有两种可能，要么该数据在新节点里，要么根本就不存在。旧节点不知道是哪种情况，所以它会向客户端返回一个-ASK targetNodeAddr 的重定向指令。客户端收到这个重定向指令后，先去目标节点执行一个不带任何参数的 asking 指令，然后在目标节点再重新执行原先的操作指令。

为什么需要执行一个不带参数的 asking 指令呢？

因为在迁移没有完成之前，按理说这个槽位还是不归新节点管理的，如果这个时候向目标节点发送该槽位的指令，节点是不认的，它会向客户端返回一个-MOVED 重定向指令告诉它去源节点去执行。如此就会形成 **重定向循环**。asking 指令的目标就是打开目标节点的选项，告诉它下一条指令不能不理，而要当成自己的槽位来处理。从以上过程可以看出，迁移是会影响服务效率的，同样的指令在正常情况下一个 ttl 就能完成，而在迁移中得 3 个 ttl 才能搞定。

## 容错

Redis Cluster 可以为每个主节点设置若干个从节点，单主节点故障时，集群会自动将其中某个从节点提升为主节点。如果某个主节点没有从节点，那么当它发生故障时，集群将完全处于不可用状态。不过 Redis 也提供了一个参数 cluster-require-full-coverage 可以允许部分节点故障，其它节点还可以继续提供对外访问。

## 网络抖动

网络抖动是非常常见的一种现象，突然之间部分连接变得不可访问，然后很快又恢复正常。

为解决这种问题，Redis Cluster 提供了一种选项 cluster-node-timeout，表示当某个节点持续 timeout 的时间失联时，才可以认定该节点出现故障，需要进行主从切换。如果没有这个选项，网络抖动会导致主从频繁切换 (数据的重新复制)。

还有另外一个选项 cluster-slave-validity-factor 作为倍乘系数来放大这个超时时间来宽松容错的紧急程度。如果这个系数为零，那么主从切换是不会抗拒网络抖动的。如果这个系数大于 1，它就成了主从切换的松弛系数。

## 可能下线 (PFAIL-Possibly Fail) 与确定下线 (Fail)

因为 Redis Cluster 是去中心化的，一个节点认为某个节点失联了并不代表所有的节点都认为它失联了。所以集群还得经过一次协商的过程，只有当大多数节点都认定了某个节点失联了，集群才认为该节点需要进行主从切换来容错。

Redis 集群节点采用 Gossip 协议来广播自己的状态以及自己对整个集群认知的改变。比如一个节点发现某个节点失联了 (PFail)，它会将这条信息向整个集群广播，其它节点也就可以收到这点失联信息。如果一个节点收到了某个节点失联的数量 (PFail Count) 已经达到了集群的大多数，就可以标记该节点为确定下线状态 (Fail)，然后向整个集群广播，强迫其它节点也接收该节点已经下线的事实，并立即对该失联节点进行主从切换。

## Cluster 基本使用

TODO

## 槽位迁移感知

如果 Cluster 中某个槽位正在迁移或者已经迁移完了，client 如何能感知到槽位的变化呢？客户端保存了槽位和节点的映射关系表，它需要即时得到更新，才可以正常地将某条指令发到正确的节点中。

我们前面提到 Cluster 有两个特殊的 error 指令，一个是 moved，一个是 asking。

第一个 moved 是用来纠正槽位的。如果我们将指令发送到了错误的节点，该节点发现对应的指令槽位不归自己管理，就会将目标节点的地址随同 moved 指令回复给客户端通知客户端去目标节点去访问。这个时候客户端就会刷新自己的槽位关系表，然后重试指令，后续所有打在该槽位的指令都会转到目标节点。

第二个 asking 指令和 moved 不一样，它是用来临时纠正槽位的。如果当前槽位正处于迁移中，指令会先被发送到槽位所在的旧节点，如果旧节点存在数据，那就直接返回结果了，如果不存在，那么它可能真的不存在也可能在迁移目标节点上。所以旧节点会通知客户端去新节点尝试一下拿数据，看看新节点有没有。这时候就会给客户端返回一个 asking error携带上目标节点的地址。客户端收到这个 asking error 后，就会去目标节点去尝试。客户端不会刷新槽位映射关系表，因为它只是临时纠正该指令的槽位信息，不影响后续指令。

**重试 2 次**

moved 和 asking 指令都是重试指令，客户端会因为这两个指令多重试一次。客户端有可能重试 2 次，比如一条指令被发送到错误的节点，这个节点会先给你一个 moved 错误告知你去另外一个节点重试。所以客户端就去另外一个节点重试了，结果刚好这个时候运维人员要对这个槽位进行迁移操作，于是给客户端回复了一个 asking 指令告知客户端去目标节点去重试指令。所以这里客户端重试了 2 次。

**重试多次**

在某些特殊情况下，客户端甚至会重试多次，正是因为存在多次重试的情况，所以客户端的源码里在执行指令时都会有一个循环，然后会设置一个最大重试次数，Java 和 Python 都有这个参数，只是设置的值不一样。当重试次数超过这个值时，客户端会直接向业务层抛出异常。

## 集群变更感知

当服务器节点变更时，客户端应该即时得到通知以实时刷新自己的节点关系表。那客户端是如何得到通知的呢？这里要分 2 种情况：

1.  目标节点挂掉了，客户端会抛出一个 ConnectionError，紧接着会随机挑一个节点来重试，这时被重试的节点会通过 moved error 告知目标槽位被分配到的新的节点地址。
2.  运维手动修改了集群信息，将 master 切换到其它节点，并将旧的 master 移除集群。这时打在旧节点上的指令会收到一个 ClusterDown 的错误，告知当前节点所在集群不可用 (当前节点已经被孤立了，它不再属于之前的集群)。这时客户端就会关闭所有的连接，清空槽位映射关系表，然后向上层抛错。待下一条指令过来时，就会重新尝试初始化节点信息。

# Stream

TODO

# Info 指令

> info xx 命令可以显示Redis 内部一系列运行参数：
>
> 1、Server 服务器运行的环境参数
>
> 2、Clients 客户端相关信息
>
> 3、Memory 服务器运行内存统计数据
>
> 4、Persistence 持久化信息
>
> 5、Stats 通用统计数据
>
> 6、Replication 主从复制相关信息
>
> 7、CPU CPU 使用情况
>
> 8、Cluster 集群信息
>
> 9、KeySpace 键值对统计数量信息

~~~sh
# 获取所有信息
> info
# 获取内存相关信息
> info memory
# 获取复制相关信息
> info replication
~~~

详见：https://redis.io/commands/info/

# RedLock

TODO

# Redis通信安全

## 指令安全

Redis 在配置文件中提供了 rename-command 指令用于将某些危险的指令修改成特别的名称，用来避免人为误操作。比如在配置文件的 security 块增加下面的内容:

~~~sh
rename-command keys abckeysabc
~~~



如果还想执行 keys 方法，那就不能直接敲 keys 命令了，而需要键入 abckeysabc。 如果想完全封杀某条指令，可以将指令 rename 成空串，就无法通过任何字符串指令来执行这条指令了。

~~~sh
rename-command flushall ""
~~~

## Lua 脚本安全

开发者必须禁止 Lua 脚本由用户输入的内容 (UGC) 生成，这可能会被黑客利用以植入恶意的攻击代码来得到 Redis 的主机权限。

同时，我们应该让 Redis 以普通用户的身份启动，这样即使存在恶意代码黑客也无法拿到 root 权限。

## TLS/SSL代理

Redis 6.0 及以上版本支持 TLS/SSL 加密，可以通过配置 Redis 的 SSL/TLS 参数来启用加密通信。下面是配置 Redis TLS 加密的步骤：

1. 生成 SSL 证书和私钥

使用 OpenSSL 工具生成 SSL 证书和私钥：

```
openssl req -x509 -nodes -newkey rsa:2048 -keyout redis.key -out redis.crt -days 365
```

2. 将证书和私钥文件复制到 Redis 服务器上

将生成的 `redis.key` 和 `redis.crt` 文件复制到 Redis 服务器的某个目录中，例如 `/etc/ssl/redis/`。

3. 修改 Redis 配置文件

打开 Redis 的配置文件 `redis.conf`，找到以下配置项，取消注释并修改为对应的路径：

```
tls-cert-file /etc/ssl/redis/redis.crt
tls-key-file /etc/ssl/redis/redis.key
```

此外，还可以根据需要设置其他的 SSL 参数，例如：

```
tls-port 6379
tls-auth-clients yes
tls-ca-cert-file /etc/ssl/redis/ca.crt
tls-ciphers AES128-SHA256
```

4. 重启 Redis 服务器

完成配置后，重启 Redis 服务器，让 Redis 加载新的配置文件。

5. 使用 TLS 连接 Redis

现在，可以使用支持 SSL/TLS 的 Redis 客户端连接 Redis 服务器了。例如，使用 OpenSSL 的 s_client 工具测试连接：

```
openssl s_client -connect your_redis_host:6379 -tls1_2
```

其中 `your_redis_host` 是 Redis 服务器的主机名或 IP 地址。