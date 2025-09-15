## NIFI两个线程数核心配置

在 Apache NiFi 中，**Maximum Timer Driven Thread Count** 和 **Maximum Event Driven Thread Count** 是控制不同类型处理器并发执行的核心参数，它们的区别如下：

### **一、核心区别**

| **参数**           | **Maximum Timer Driven Thread Count**        | **Maximum Event Driven Thread Count**     |
| ------------------ | -------------------------------------------- | ----------------------------------------- |
| **适用处理器类型** | 基于时间调度的处理器（Timer Driven 策略）    | 基于事件触发的处理器（Event Driven 策略） |
| **触发机制**       | 按固定时间间隔或 Cron 表达式触发             | 当上游有数据流入或特定事件发生时触发      |
| **典型处理器示例** | GenerateFlowFile、InvokeHTTP、ExecuteProcess | ListenTCP、ConsumeKafka、GetFile          |
| **线程池隔离**     | 独立线程池，互不影响                         | 独立线程池，互不影响                      |
| **默认值**         | 通常为 10                                    | 通常为 5                                  |
| **配置位置**       | NiFi 控制器设置 → General 选项卡             | NiFi 控制器设置 → General 选项卡          |

### **二、深入理解**

#### 1. **Timer Driven（定时器驱动）**

- **特点**：

处理器按预设时间间隔执行，与上游是否有数据无关。例如：

- - GenerateFlowFile：定期生成数据。

- - InvokeHTTP：定时调用外部 API。

- **线程池作用**：

限制所有 **Timer Driven** 处理器的总并发线程数。若参数设为 10，则所有此类处理器的 Concurrent Tasks 总和不能超过 10。

#### 2. **Event Driven（事件驱动）**

- **特点**：

处理器仅在接收到上游数据或特定事件时触发。例如：

- - ListenTCP：监听网络端口，有数据到达时触发。

- - GetFile：检测到新文件时触发。

- **线程池作用**：

限制所有 **Event Driven** 处理器的总并发线程数。若参数设为 5，则所有此类处理器的 Concurrent Tasks 总和不能超过 5。

### **三、调优建议**

#### 1. **Timer Driven 参数调整**

- **增加场景**：

当有大量定时任务（如批量数据同步）时，需提高该参数。

例如：10 个定时执行的 InvokeHTTP 处理器，每个设 Concurrent Tasks = 2，则至少需 10×2 = 20。

- **计算公式参考**：

```
Timer Driven 线程数 = CPU核心数 × 2 + 定时任务数
```

#### 2. **Event Driven 参数调整**

- **增加场景**：

当数据流入速度快、需高并发处理时（如实时日志收集），需提高该参数。

例如：5 个 ListenTCP 处理器同时接收网络数据，每个设 Concurrent Tasks = 3，则至少需 5×3 = 15。

- **计算公式参考**：

```
Event Driven 线程数 = 磁盘吞吐量 × 安全系数（建议 1.5）
```

### **四、常见误区**

1. **混淆两种驱动类型**：

- - 若将 ListenTCP（事件驱动）误设为 Timer Driven，会导致无法实时响应数据。

- - 若将 GenerateFlowFile（定时器驱动）设为 Event Driven，则不会自动触发。

1. **线程数分配失衡**：

- - 若仅提高 Timer Driven 线程数，而 Event Driven 线程数不足，会导致实时数据处理积压。

1. **忽略其他线程池**：

- - Cron 表达式调度的处理器由 Maximum Cron Driven Thread Count 控制，与前两者独立。

### **五、配置示例**

假设 NiFi 集群配置如下：

- 4 核 CPU，1 块 SSD 磁盘

- 任务类型：

- - 3 个定时调用 API 的 InvokeHTTP（Timer Driven，各需 2 个并发）

- - 2 个实时接收日志的 ListenTCP（Event Driven，各需 3 个并发）

**合理配置**：

- Maximum Timer Driven Thread Count：3×2 = 6（或按公式 4×2+3=11）

- Maximum Event Driven Thread Count：2×3 = 6（或按公式 10000IOPS×0.001×1.5=15）

通过合理分配这两个参数，可确保 NiFi 中定时任务和实时任务都能高效执行，避免资源竞争导致的性能瓶颈。



## FlowFile存储库和内容库

### FlowFile存储库

| Property                                         | Description                                                  |
| ------------------------------------------------ | ------------------------------------------------------------ |
| nifi.flowfile.repository.implementation FlowFile | 存储库实现。 缺省值是org.apache.nifi.controller.repository.WriteAheadFlowFileRepository，谨慎更改。 要将流文件存储在内存中而不是磁盘上（在发生电源/机器故障时可能会丢失数据），请将此属性设置为org.apache.nifi.controller.repository.VolatileFlowFileRepository。 |
| nifi.flowfile.repository.directory*              | FlowFile存储库的位置。 缺省值是./flowfile_repository。       |
| nifi.flowfile.repository.partitions              | 分区的数量。 默认值是256。                                   |
| nifi.flowfile.repository.checkpoint.interval     | FlowFile存储库checkpoint间隔。 默认值是2分钟。               |
| nifi.flowfile.repository.always.sync             | 如果设置为true，则对存储库的任何更改都将同步到磁盘，这意味着NiFi将要求操作系统不要缓存信息。 这代价是非常大的的，可能显著降低NiFi性能。 但是，如果是false，如果突然断电或操作系统崩溃，则可能会有数据丢失的可能性。 默认值是false。 |

FlowFile存储库会跟踪系统中每个FlowFile的属性和当前状态。 默认情况下，该存储库与其他所有存储库安装在同一根安装目录中; 但是，如果可能的话，建议在单独的驱动器上进行配置。

### 内容库 Content Repository

Content Repository保存系统中所有FlowFile的内容。 默认情况下，它保存在与所有其他存储库相同的根安装目录中; 但是，管理员最好将其配置在单独的驱动器上。 如果没有其他内容，最好是内容存储库与FlowFile存储库不在同一个驱动器上。 在处理大量数据的数据流中，内容存储库可能会填满一个磁盘，如果该磁盘上还存在FlowFile存储库，则可能会损坏该存储库。 为避免这种情况，请将这些存储库配置在不同的驱动器上。

#### File System Content Repository Properties的内容库属性

当nifi.content.repository.implementation设置为org.apache.nifi.controller.repository.FileSystemRepository支持的属性：

| Property                                             | Description                                                  |
| ---------------------------------------------------- | ------------------------------------------------------------ |
| nifi.content.repository.implementation               | Content Repository实现。 缺省值是org.apache.nifi.controller.repository.FileSystemRepository，谨慎更改。 要将流文件内容存储在内存中而不是磁盘上（在发生电源/机器故障时可能会丢失数据），请将此属性设置为org.apache.nifi.controller.repository.VolatileContentRepository。 |
| nifi.content.claim.max.appendable.size               | 内容claim的最大大小。 默认值是10 MB。                        |
| nifi.content.claim.max.flow.files                    | 要分配给一个内容claim的最大FlowFiles数量。 默认值是100。     |
| nifi.content.repository.directory.default*           | 内容存储库的位置。 默认值是./content_repository。注：可以使用nifi.content.repository.directory指定多个内容存储库。 前缀具有唯一的后缀和单独的路径作为值。例如，要提供另外两个位置作为内容存储库的一部分，用户还可以使用以下键指定其他属性：nifi.content.repository.directory.content1=/repos/content1nifi.content.repository.directory.content2=/repos/content2提供三个总位置，包括nifi.content.repository.directory.default。 |
| nifi.content.repository.archive.max.retention.period | 如果存档已启用（请参阅下面的nifi.content.repository.archive.enabled），则此属性将指定保留存档数据的最长时间。 默认值是12小时。 |
| nifi.content.repository.archive.max.usage.percentage | 如果存档已启用（请参阅下面的nifi.content.repository.archive.enabled），则此属性必须具有一个值，该值表示内容存储库磁盘使用率达到百分之多少就开始删除存档。 如果存档为空且内容存储库磁盘使用率高于此百分比，则归档将暂时禁用。 当磁盘使用率低于此百分比时，归档将恢复。 默认值是50％。 |
| nifi.content.repository.archive.enabled              | 是否启用内容存档，请将其设置为true，并为上面的nifi.content.repository.archive.max.usage.percentage属性指定一个值。 内容存档使源代码用户界面能够查看或重放不再位于数据流队列中的内容。 默认情况下，存档已启用。 |
| nifi.content.repository.always.sync                  | 如果设置为true，则对存储库的任何更改都将同步到磁盘，这意味着NiFi将要求操作系统不要缓存信息。 这代价是非常昂贵的，NiFi性能会显著降低。 但是，如果为false，如果突然断电或操作系统崩溃，则可能会有数据丢失的可能性。 默认值是false。 |
| nifi.content.viewer.url                              | 基于Web的内容查看器的URL（如果有）。 默认空。                |

#### Volatile Content Repository Properties  易变的内容库属性

当nifi.content.repository.implementation的值为org.apache.nifi.controller.repository.VolatileContentRepository支持的属性：

| Property                                    | Description                               |
| ------------------------------------------- | ----------------------------------------- |
| nifi.volatile.content.repository.max.size   | 内存中的内容库最大大小。 默认值是100 MB。 |
| nifi.volatile.content.repository.block.size | 内容存储库块大小。 默认值是32 KB。        |