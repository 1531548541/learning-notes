# Golang安装

## 采坑

当你开启了GO111MODULE，仍然使用GOPATH模式的方法，在引入自定义模块时会报错。go mod具体使用将在下一篇介绍

GO111MODULE 有三个值：off, on和auto（默认值）。

- GO111MODULE=off，go命令行将不会支持module功能，寻找依赖包的方式将会沿用旧版本那种通过vendor目录或者GOPATH模式来查找。

- GO111MODULE=on，go命令行会使用modules，而一点也不会去GOPATH目录下查找。

- GO111MODULE=auto，默认值，go命令行将会根据当前目录来决定是否启用module功能。这种情况下可以分为两种情形：

  - ```
    当前目录在GOPATH/src之外且该目录包含go.mod文件
    ```

  - ```
    当前文件在包含go.mod文件的目录下面。
    ```

*当modules 功能启用时，依赖包的存放位置变更为$GOPATH/pkg，允许同一个package多个版本并存，且多个项目可以共享缓存的 module。*

（1）使用了了相对路径：*import “./models”*

报错：build command-line-arguments: cannot find module for path */D*/dev这里后面一堆本地路径
这是因为在go module下 你源码中 impot …/ 这样的引入形式不支持了， 应该改成 impot 模块名/ 。 这样就ok了

（2）使用结合了GOPATH的形式：import “Go-Player/src/ademo/models”

于是我们把上面的import改成了结合GOPATH的如上形式

报错：package Go-Player/src/ademo/models is not in GOROOT D:/development/go/src/GPlayer/src/ademo/models
（3）彻底解决方法：用go env -u 恢复初始设置

不再使用go mod：

- go env -w GO111MODULE=off 或者 go env -w GO111MODULE=auto
- go env -u GO111MODULE

区别在于，如果GO111MODULE=on或者auto，在go get下载包时候，会下载到GOPATH/pkg/mod，引入时也是同样的从这个目录开始。如果这行了上述命令，那么在go get下载包时候，会下载到GOPATH/src 目录下

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
>
> **如果变量名、函数名、常量名⾸字⺟⼤写，则可以被其他的包访问**

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

## 指针（与c一致）

~~~go
package main

import "fmt"

func main() {
	num := 2
	var numPointer *int = &num
	//简写var numPointer = &num
	fmt.Printf("num的地址:%p,num值:%d", numPointer, *numPointer)
}
~~~

## 获取用户输入

~~~go
package main

import "fmt"

func main() {
	//实现功能：获取用户输入的name和age
	//方式一：scanln
	var name string
	var age int
	fmt.Println("请输入name:")
	fmt.Scanln(&name)
	fmt.Println("请输入age:")
	fmt.Scanln(&age)zfc
	fmt.Printf("name:%s,age:%d", name, age)
	//方式二: scanf
	fmt.Println("请输入name,age")
	fmt.Scanf("%s %d", &name, &age)
	fmt.Printf("name:%s,age:%d", name, age)
}
~~~

## 流程控制

### if/else

~~~go
package main

import "fmt"

func main() {
	if n := 2;n > 2 {
		fmt.Println("n>2")
	} else {
		fmt.Println("n<=2")
	}
}
~~~

### swich case

> 与java相比区别：
>
> 1.case可以接收多个值
>
> 2.不需要写break，默认带
>
> 3.switch后是⼀个表达式(即:常量值、变量、⼀个有返回值的函数等都可以)

~~~go
package main

import "fmt"

func main() {
	age := 18
	switch age {
	case 10:
		fmt.Println("age=10")
	case 17, 18:
		fmt.Println("age=17|18")
        fallthrough  //穿透一层，下面case也会执行
	case 19:
		fmt.Println("age=19")
	default:
		fmt.Println("啥也没匹配上")
	}
}
~~~

### for

~~~go
package main

import "fmt"

func main() {
	for i := 0; i < 18; i++ {
	 	fmt.Println(i)
	}
	var str = "go你"
	//遍历str
	//方式一：传统，不支持中文
	for i := 0; i < len(str); i++ {
		fmt.Printf("%c \n", str[i])
	}
	//方式二：
	for i, value := range str {
		fmt.Printf("index:%d,val:%c \n", i, value)
	}
}

g 
o 
ä
½
 
index:0,val:g
index:1,val:o
index:2,val:你
~~~

### label

~~~go
package main

import "fmt"

func main() {
label:
	for i := 0; i < 18; i++ {
		fmt.Println(i)
		if i == 15 {
			break label
		}
	}
	fmt.Println("结束1")
}

0
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
结束1
~~~

~~~go
package main

import "fmt"

func main() {
	for i := 0; i < 18; i++ {
		fmt.Println(i)
		if i == 15 {
			goto label2
		}
	}
	fmt.Println("结束1")
label2:
	fmt.Println("结束2")
}

0
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
结束2
~~~

## 函数

> init函数是go中特殊的函数

~~~go
package main

import "fmt"

//返回单个值
func add(num1 int, num2 int) int {
	return num1 + num2
}

//返回多个值
func plusOrMinus(num1 int, num2 int) (int, int) {
	return (num1 + num2), (num1 - num2)
}

func main() {
	fmt.Printf("sum:%d \n", add(1, 2))
	plus, minus := plusOrMinus(1, 2)
	//如果返回值不想接收，用_   即plus, _ := plusOrMinus(1, 2)
	fmt.Printf("sum:%d,minus:%d", plus, minus)
}
~~~

### 不定参数

~~~go
//不定参数
func printArgs(args ...int) {
	for i := 0; i < len(args); i++ {
		fmt.Printf("%d ", args[i])
	}
}
~~~

### 生命周期

> 在一个类中：全局变量>init>main
>
> 若引用了其他类的变量方法，会先加载其他类的 全局变量>init>main

~~~go
package main

import "fmt"

var n = testGrobalVar()

func init() {
	fmt.Println("init函数...")
}

func testGrobalVar() int {
	fmt.Println("全局变量初始化...")
	return 1
}

func main() 
	fmt.Println("main执行...")
}


自己的全局变量初始化...
自己的init函数...
自己的main执行...
~~~

一个类调用另一个类的全局变量时

~~~go
package main

import (
	"fmt"
	"helloworld/hello/cc"
)

var n = testGrobalVar()

func init() {
	fmt.Println("自己的init函数...")
}

func testGrobalVar() int {
	fmt.Println("自己的全局变量初始化...")
	return 1
}

func main() {
	fmt.Println("自己的main执行...")
	fmt.Println("Name:%s", cc.Name)
}

goUtil的全局变量初始化...
goUtil的init函数...
自己的全局变量初始化...
自己的init函数...
自己的main执行...
Name:%s zfc
~~~

~~~go
package cc

import "fmt"

var Name = testGrobalVar()

func init() {
	fmt.Println("goUtil的init函数...")
}

func testGrobalVar() string {
	fmt.Println("goUtil的全局变量初始化...")
	return "zfc"
}
~~~

### 匿名函数

~~~go
//定义的同时调用(10)
res := func(num int) int {
		return num
	}(10)
fmt.Println("res:%d", res)
~~~

