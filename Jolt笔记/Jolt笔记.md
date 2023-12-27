# Jolt类型

jolt插件主要转换方法
（1）shift:主要作用是拷贝，将输入json的某个结构拷贝到目标json
（2）default:主要作用是设置默认值，如果输入json某个value不存在，则使用默认值补齐
（3）remove:主要作用是删除，可以删除输入json结构的某个结构
（4）sort：对key值进行排序
（5）cardinality：jsonobject和jsonarray之间的切换
（6）modify：修改json数值

Jolt spec示例：

~~~json
[ 
  { 
    "operation": "shift", 
    "spec": {} 
  }, 
  { 
    "operation": "modify-default-beta", 
    "spec": {} 
  } 
  , 
  { 
    "operation": "sort" 
  } 
] 
~~~

