# 基本概念

## Docker架构

![image-20220922171906666](images/image-20220922171906666.png)

K8S：CRI（Container Runtime Interface） 

Client： 客户端；操作docker服务器的客户端（命令行或者界面） 

Docker_Host：Docker主机；安装Docker服务的主机 

Docker_Daemon：后台进程；运行在Docker服务器的后台进程 

Containers：容器；在Docker服务器中的容器（一个容器一般是一个应用实例，容器间互相隔离） 

Images：镜像、映像、程序包；Image是只读模板，其中包含创建Docker容器的说明。容器是由Image运 行而来，Image固定不变。 

Registries：仓库；存储Docker Image的地方。官方远程仓库地址： https://hub.docker.com/search

>Docker用Go编程语言编写，并利用Linux内核的多种功能来交付其功能。 Docker使用一种称为名称 空间的技术来提供容器的隔离工作区。 运行容器时，Docker会为该容器创建一组名称空间。 这些 名称空间提供了一层隔离。 容器的每个方面都在单独的名称空间中运行，并且对其的访问仅限于 该名称空间。 

## Docker隔离原理

- **namespace 6项隔离** （资源隔离）

| namespace | **系统调用参数** | **隔离内容 **              |
| --------- | ---------------- | -------------------------- |
| UTS       | CLONE_NEWUTS     | 主机和域名                 |
| IPC       | CLONE_NEWIPC     | 信号量、消息队列和共享内存 |
| PID       | CLONE_NEWPID     | 进程编号                   |
| Network   | CLONE_NEWNET     | 网络设备、网络栈、端口等   |
| Mount     | CLONE_NEWNS      | 挂载点(文件系统)           |
| User      | CLONE_NEWUSER    | 用户和用户组               |

- **cgroups资源限制** （资源限制） 

  cgroup提供的主要功能如下：

  **资源限制**：限制任务使用的资源总额，并在超过这个 配额 时发出提示

  **优先级分配**：分配CPU时间片数量及磁盘IO带宽大小、控制任务运行的优先级

  **资源统计**：统计系统资源使用量，如CPU使用时长、内存用量等

  **任务控制**：对任务执行挂起、恢复等操作

cgroup资源控制系统，每种子系统独立地控制一种资源。功能如下

