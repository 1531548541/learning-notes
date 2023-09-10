# Golang安装



# Golang基本语法

## helloworld

~~~go
package main

import "fmt"

func main() {
	fmt.Println("go helloworld!")
}
~~~

> 注意：main函数想执行，必须**package main**

## 变量

![image-20230910183141311](images/image-20230910183141311.png)

> **注意:go语言中局部变量和包必须申明即使用，否则会报错**

### 变量申明

~~~go
package main

import "fmt"

var nn = 1

//使用这种方式一次性申明多个
var (
	age  = 12
	name = "111"
)

func main() {
	//指定类型的变量
	var num int = 3
	fmt.Println(num)
	//自动推断类型的变量
	var num1 = 3.1
	fmt.Printf("num1的类型:%T,值:%f \n", num1, num1) //num1的类型:float64,值:3.100000
	//快捷写法s
	num2 := 3.2
	fmt.Printf("num2的类型:%T,值:%f \n", num2, num2)      //num2的类型:float64,值:3.200000
	fmt.Printf("nn:%d,age:%d,name:%s", nn, age, name) //nn:1,age:12,name:111
}

~~~

### 默认值

> go中局部变量和全局变量都有默认值

~~~go
package main

import "fmt"

var nn int

func main() {
	//指定类型的变量
	var num int
	fmt.Printf("num:%d,nn:%d", num, nn) //num:0,nn:0
}
~~~

### 字符类型

> 在go中没有单独类型表示char
>
> 如果存储单个字符，使用byte
>
> 字符使用UTF-8编码
>
> 字符本质就是ASCII表中对应的数值

### 类型转换

~~~go
package main

import (
	"fmt"
	"strconv"
)

func main() {
	num := 2
	price := 2.1122
	//int => float64
	numFloat := float64(num)
	fmt.Printf("numFloat的类型:%T,值:%f \n", numFloat, numFloat)
	//基本数据类型 => string
	//方法1 借助fmt
	str1 := fmt.Sprint(num)
	var str2 string = fmt.Sprintf("%d", num)
	str4 := fmt.Sprintf("%.3f", price)
	fmt.Printf("str1的类型:%T,值:%s \n", str1, str1)
	fmt.Printf("str2的类型:%T,值:%s \n", str2, str2)
	fmt.Printf("str4的类型:%T,值:%s \n", str4, str4)

	//方法2 借助strconv,参数2代表进制
	str3 := strconv.FormatInt(int64(num), 10)
	str5 := strconv.FormatFloat(price, 'f', 2, 64)

	fmt.Printf("str3的类型:%T,值:%s \n", str3, str3)
	fmt.Printf("str5的类型:%T,值:%s \n", str5, str5)

	//string => 基本类型
	strNum, _ := strconv.ParseInt(str1, 10, 64)
	fmt.Printf("strNum的类型:%T,值:%d \n", strNum, strNum)
}

~~~

## 指针

