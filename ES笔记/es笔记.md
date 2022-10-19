# 安装

~~~sh
#准备文件和文件夹，并chmod -R 777 xxx
#配置文件内容，参照
https://www.elastic.co/guide/en/elasticsearch/reference/7.5/node.name.html 搜索相关配置
# 考虑为什么挂载使用esconfig ...
docker run --name=elasticsearch -p 9200:9200 -p 9300:9300 \
-e "discovery.type=single-node" \
-e ES_JAVA_OPTS="-Xms300m -Xmx300m" \
-v /app/es/data:/usr/share/elasticsearch/data \
-v /app/es/plugins:/usr/shrae/elasticsearch/plugins \
-v esconfig:/usr/share/elasticsearch/config \
-d elasticsearch:7.12.0

#######################################

docker pull elasticsearch:7.4.2  存储和检索数据
docker pull kibana:7.4.2 可视化检索数据   

mkdir -p /opt/docker/elasticsearch/{config,data,plugins} # 用来存放配置文件、数据、插件
echo "http.host: 0.0.0.0" >/opt/docker/elasticsearch/config/elasticsearch.yml # 允许任何机器访问
chmod -R 777 /opt/docker/elasticsearch/ ## 设置elasticsearch文件可读写权限

# 启动es
docker run --name elasticsearch -p 9200:9200 -p 9300:9300 \
-e  "discovery.type=single-node" \
-e ES_JAVA_OPTS="-Xms64m -Xmx512m" \
-v /opt/docker/elasticsearch/config/elasticsearch.yml:/usr/share/elasticsearch/config/elasticsearch.yml \
-v /opt/docker/elasticsearch/data:/usr/share/elasticsearch/data \
-v  /opt/docker/elasticsearch/plugins:/usr/share/elasticsearch/plugins \
-d elasticsearch:7.4.2

#以后再外面装好插件重启就可

#特别注意：
-e ES_JAVA_OPTS="-Xms64m -Xmx128m" \ 测试环境下，设置 ES 的初始内存和最大内存，否则导致过大启动不了ES

# 开机启动
docker update elasticsearch --restart=always

# 启动kibana
docker run --name kibana -e ELASTICSEARCH_HOSTS=http://192.168.200.128:9200 -p 5601:5601 -d kibana:7.4.2

~~~



# ES实现原理

![image-20220809103314866](C:\Users\Administrator\Desktop\learning-notes\ES笔记\images\image-20220809103314866.png)

>Lucene将上面三列分别作为词典文件、频率文件、位置文件保存。其中词典文件不仅保存了每个关键词，还保留了指向频率文件和位置文件的指针，通过指针即可找到对应文件信息。
>
>Lucene使用了field概念，用于表达信息所在位置（如标题中、文章中、URL中），在建索引时，该field信息也记录在词典文件中，每个关键词都有一个field信息，因为每个关键字一定属于一个或多个field。

# ES一些术语和概念

1. 索引词（term）

   > 精确值，foo、Foo、FOO被认为是不同的term

2. 文本（text）

   >文本是一段普通的非结构化文字。通常，**文本会被分析成一个个的索引词**，存储在索引库中。为了让文本能够进行搜索，文本字段需要事先进行分析，当对文本中的关键词进行查询的时候，搜索引擎应该根据搜索条件搜索出原文本。

3. 分析

   > 分析是将文本转换成索引词的过程，分析过程依赖分词器。

4. 集群

5. 节点

6. 路由

   > 当存储doc时，它会根据hash(doc的ID)值存储在唯一的主分片中。如果文档有一个指定的父文档，则从父文档ID中生成，该值可以存储文档的时候进行修改。

7. 分片（shard）

   > 分片是单个Lucene实例，这是ES管理比较底层的功能。索引是指向主分片和副本分片的逻辑空间。对于使用，只需要指定分片的数量。在开发使用过程中，我们对应的对象都是index，ES会自动管理集群中的所有分片，发生故障时，ES会把分片移动到不同的节点或新节点。
   >
   > 一个index可以存很大的数据，这些空间可以超过一个节点的物理空间，若数据都存储在单节点对搜索性能也有影响。因此，ES将index分解成多个分片。当你创建index，你可以简单定义你想要的分片数量，每个分片本身是一个全功能的、独立的单元，可以托管在集群中的任何节点。

8. 主分片

   > 每个doc都存储在一个分片中，当你存储一个doc时，系统会首先存储在主分片中，然后复制到不同的副本分片中。默认情况下一个index有5个主分片，你可以事先定义分片数量，但之后无法修改。

9. 副本分片

10. 复制（replica）

    > 复制是一个非常有用的功能，不然会有单点问题。当网络中的某个节点出现问题，复制可以故障转移，保证高可用。因此ES允许你创建一个或多个拷贝，你的索引分片就形成了所谓的副本或副本分片。
    >
    > 复制是重要的，原因：
    >
    > - 保证了高可用，当节点失败不受影响，需要注意的是，一个复制的分片不会存储在同一个节点中。
    > - 它允许你扩展搜索量，提供并发量，因为搜索可以在所有副本上并行执行。
    >
    > 每个索引可以拆分成多个分片，索引可以复制0个或多个分片。一旦复制，每个索引就有了主分片和副本分片。分片数量和副本的数量可以在创建索引时定义。但之后只能改变副本的数量，不能改变分片的数量。
    >
    > **注意**：每个ES分片是一个Lucene的索引，最大是Integer.MAX_VALUE-128。可以使用 _cat/shards 监控分片大小

11. 索引（index）

    > 具有相同结构的doc集合，类似MySQL的**表**。

12. 类型（已废除）

    > 类型是index的逻辑分区，一种类型被定义具有一组公共字段的doc。例如，你想将博客系统所有信息存入一个index，则底下的user、blog等就是一种类型。

13. 文档（doc）

    > doc是以json格式存储在ES中的字符串，类似MySQL中的**行**。
    >
    > 每个doc都有一个类型和ID，原始的JSON文档被存储在一个叫_source的字段中，当搜索文档时默认返回就是这个字段。

14. 映射（mapping）

    > 类似MySQL的**表结构**，可以事先定义，也可以被ES智能识别。

15. 字段（field）

    > 类型MySQL的**字段**。

16. 来源字段（source field）

    > 默认情况，你的源文档存储在_source这个字段中，当你查询时也会返回这个字段。这允许你可以从搜索结构中访问原始对象，这个对象返回一个精确的json，这个对象不显示索引分析后的其他任何数据。

17. 主键（ID）

    > ID是一个文件的唯一标识，如果在存库时没有提供ID，系统会自动生存ID，文档的index/type/id必须是唯一的。

**返回值字段说明**

![img](images/es-usage-3.png)

- `took` – Elasticsearch运行查询所花费的时间（以毫秒为单位）
- `timed_out` –搜索请求是否超时
- `_shards` - 搜索了多少个碎片，以及成功，失败或跳过了多少个碎片的细目分类。
- `max_score` – 找到的最相关文档的分数
- `hits.total.value` - 找到了多少个匹配的文档
- `hits.sort` - 文档的排序位置（不按相关性得分排序时）
- `hits._score` - 文档的相关性得分（使用match_all时不适用）

# API

## CRUD

> **注意：新版ES已经废除了type**，所以以前的/index/type/id推荐变为 /index/_doc/id
>
> ![image-20220809135847027](C:\Users\Administrator\Desktop\learning-notes\ES笔记\images\image-20220809135847027.png)

|      method      |                   url地址                    |          描述          |
| :--------------: | :------------------------------------------: | :--------------------: |
| PUT（创建,修改） |     localhost:9200/索引名称/_doc/文档id      | 创建文档（指定文档id） |
|   POST（创建）   |         localhost:9200/索引名称/_doc         | 创建文档（随机文档id） |
|   POST（修改）   | localhost:9200/索引名称/__doc/文档id/_update |        修改文档        |
|  DELETE（删除）  |     localhost:9200/索引名称/_doc/文档id      |        删除文档        |
|   GET（查询）    |     localhost:9200/索引名称/_doc/文档id      |   查询文档通过文档ID   |
|   POST（查询）   | localhost:9200/索引名称/__doc/文档id/_search |      查询所有数据      |

#### 创建空索引

> 说明：**可以直接创建索引**，不需要创建空索引后再修改数据

```json
PUT /myindex
{
    "settings": {
        "index": {
            "number_of_shards": "2", #分片数
            "number_of_replicas": "0" #副本数
        }
    }
}
#不指定分片数和副本数，系统默认1个分片，1个副本
```

#### 修改索引

~~~json
PUT /wujie/_settings
{
  "number_of_replicas": 2
}
#注意：无法修改分片数量，否则报错
~~~

#### 打开/关闭索引

~~~json
POST /wujie/_close
POST /wujie/_open
~~~

查看所有索引

~~~sh
Get /_cat/indices?v
~~~



#### 创建映射

~~~json
#为index创建mappings
PUT /zfc
{
  "mappings": {
    "properties": {
      "id":{
        "type": "integer"
      },
      "name":{
        "type": "text"
      },
      "brithday":{
        "type": "date"
      }
    }
  }
}


#为doc创建mappings
PUT /zfc/_doc/1
{
  "mappings": {
    "properties": {
      "id":{
        "type": "integer"
      },
      "name":{
        "type": "text"
      },
      "brithday":{
        "type": "date"
      }
    }
  }
}
~~~

#### 修改映射

>**index的mappings无法修改**，可以通过**_reindex** 重建索引,方式修改

~~~json
#doc的mappings可以修改（感觉没啥用）
POST /zfc/_doc/1
{
  "mappings": {
    "properties": {
      "id":{
        "type": "integer"
      },
      "name":{
        "type": "text"
      },
      "brithday":{
        "type": "date"
      },
      "key":{
        "type": "keyword"
      }
    }
  }
}
~~~

通过reindex修改index的mappings

~~~json
#1. 创建一个新的index，设置新的mappings
PUT /zfc1
{
  "mappings": {
    "properties": {
      "id":{
        "type": "long"  #修改处
      },
      "name":{
        "type": "text"
      },
      "brithday":{
        "type": "date"
      },
      "key":{
        "type": "keyword"
      }
    }
  }
}
#2. _reindex进行迁移
POST _reindex
{
  "source": {
    "index": "zfc"
  },
  "dest": {
    "index": "zfc1"
  }
}
#3. 删除旧的index
DELETE zfc
#4. 设置别名
POST _aliases
{
  "actions": [
    {
      "add": {
        "index": "zfc1",
        "alias": "zfc"
      }
    }
  ]
}
~~~

![image-20220809173456326](C:\Users\Administrator\Desktop\learning-notes\ES笔记\images\image-20220809173456326.png)

#### 插入数据

~~~json
PUT /wujie/_doc/1
{
  "id":2,
  "name":"zhangsan"
}

或

POST /wujie/_doc/1
{
  "id":2,
  "name":"zhangsan"
}
~~~

#### 查看数据

~~~json
#根据doc的id搜索index下的某个doc
GET /wujie/_doc/1
#搜索index下所有的doc（默认最多10条）
GET /wujie/_search
#查询年龄等于20的用户
GET /wujie/_search?q=age:20
~~~

#### 删除数据

~~~json
DELETE /wujie
~~~

#### 修改数据

~~~json
POST /wujie/_update/1
{
  "doc":{
    "id":1,
    "age":21
  }
}

或

PUT /wujie/_doc/1
{
  "id":1,
  "name":"lisi"
}
~~~

#### 索引别名

~~~json
#创建别名
POST _aliases
{
  "actions": [
    {
      "add": {
        "index": "zfc1",
        "alias": "zfc"
      },
      "add": {                   #一个别名是可以关联n个index的，根据别名查的时候会列出所有index
        "index": "wujie",
        "alias": "zfc"
      },
      "add": {                   #也可以通过通配符
        "index": "z*",
        "alias": "zfc"
      }
    }
  ]
}
#修改别名（没有修改），可以先remove，后add
#删除别名
DELETE /zfc1/_alias/zfc

或

POST _aliases
{
  "actions": [
    {
      "remove": {
        "index": "zfc1",
        "alias": "zfc"
      }
    }
  ]
}

#查看index下所有的别名
GET zfc1/_alias


#带filter和routing的别名
#作用：在使用别名进行查找时，只有满足filter中的要求，才能查到数据
POST _aliases
{
  "actions": [
    {
      "add": {
        "index": "wujie",
        "alias": "zfc2",
        "filter": {
          "term": {
            "age": "21"
          }
        },
        "routing": "1",
        "search_routing": "1,2",
        "index_routing": "1"    #index_routing只能一个值
      }
    }
  ]
}
~~~

#### 索引模板TODO ES实战P66

## DSL



## 路由TODO

>