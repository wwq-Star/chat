# 聊天记录分析系统 - 大数据处理流程文档

## 系统架构概述

本系统实现了完整的大数据处理流程，从CSV数据导入开始，经过HDFS存储、MapReduce数据清洗、Hive数据仓库存储，最终进行数据查询和分析。

```
┌─────────────┐
│  CSV数据文件 │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────┐
│  1. 数据导入与格式转换            │
│  - 解析CSV文件                   │
│  - 数据格式转换                   │
│  - 字段映射与验证                 │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  2. HDFS分布式存储                │
│  - 上传到 /data/chat_analysis/   │
│  - 数据分片存储                   │
│  - 副本机制保证可靠性              │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  3. MapReduce数据清洗            │
│  - 数据清洗与格式化               │
│  - 去除无效数据                   │
│  - 统一数据格式                   │
│  - 输出到 /data/chat_analysis/   │
│    output/                       │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  4. Hive数据仓库                 │
│  - 创建ods数据库                 │
│  - 创建ods_chat_analysis_info表  │
│  - 加载清洗后的数据               │
│  - 数据存储在HDFS表目录           │
└──────┬──────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│  5. 数据查询与分析                │
│  - SQL查询接口                   │
│  - 统计分析                       │
│  - 可视化展示                     │
└─────────────────────────────────┘
```


## 完整数据流程示例

### 示例：导入50000条聊天记录

**步骤1：用户上传CSV文件**
```
用户操作：点击"导入数据"按钮，选择 chat_data.csv (50000条记录)
前端：POST /api/hive/import-data
```

**步骤2：数据导入与格式转换**
```
后端处理：
- 解析CSV文件
- 转换日期格式：2024/1/4 19:24 → 2024-01-04 19:24:00
- 映射字段：send_time → msg_time
- 生成随机数据：IP地址、年龄、性别
- 验证数据完整性
处理时间：约10-30秒（取决于数据量）
```

**步骤3：上传到HDFS**
```
HDFS操作：
- 创建目录：/data/chat_analysis/input/
- 上传文件：/data/chat_analysis/input/chat_data_20241220164530.csv
- 文件大小：约5MB
- 副本数：3个
上传时间：约2-5秒
```

**步骤4：MapReduce数据清洗**
```
MapReduce作业：
- 输入：/data/chat_analysis/input/chat_data_20241220164530.csv
- 输出：/data/chat_analysis/output/20241220164530/part-m-00000
- Mapper处理：清洗50000条记录
- 输出格式：Tab分隔的文本文件
处理时间：约30-60秒（取决于集群性能）
```

**步骤5：加载到Hive**
```
Hive操作：
- 使用数据库：ods
- 目标表：ods_chat_analysis_info
- 加载数据：LOAD DATA INPATH '/data/chat_analysis/output/20241220164530/part-m-00000'
- 数据移动：从output目录移动到Hive表目录
- 验证：查询表记录数确认加载成功
加载时间：约5-10秒
```

**步骤6：数据查询与分析**
```
查询操作：
- 数据概览：总消息数50000，用户数1100
- 每日统计：生成日期分布图表
- 类型统计：文本30000，图片15000，语音5000
- 性别统计：男25000，女25000
- 平台统计：Android 20000，iOS 15000，Windows 10000，Mac 5000
查询时间：每个查询约1-3秒
```

---

## 详细流程说明

### 阶段1：数据导入与格式转换

**功能描述：**
- 接收用户上传的CSV文件
- 解析CSV文件内容
- 进行数据格式转换和字段映射
- 验证数据完整性

**技术实现：**
- 使用Apache Commons CSV解析CSV文件
- 支持多种日期格式自动识别
- 处理BOM字符和编码问题
- 字段映射：CSV字段 → 数据库字段

**字段映射关系：**
```
CSV字段              →  数据库字段
─────────────────────────────────
send_time           →  msg_time
user_id             →  user_id
chat_room           →  group_id
message_content     →  message
message_type        →  message_type (text→1, image→2, voice→3)
device_type         →  platform
message_length      →  message_length
user_name           →  user_name
```

**自动生成字段：**
- `ip_address`: 随机生成IP地址
- `user_age`: 随机生成年龄（18-67岁）
- `user_gender`: 随机生成性别（男/女）

**代码位置：**
- `CsvImportService.java` - CSV解析和数据转换服务
- `HiveController.java` - `/api/hive/import-data` 接口

---

### 阶段2：HDFS分布式存储

**功能描述：**
- 将导入的数据文件上传到HDFS
- 存储在 `/data/chat_analysis/input/` 目录
- 利用HDFS的分布式存储和副本机制
- 确保数据的高可用性和可靠性

**HDFS目录结构：**
```
/data/
  └── chat_analysis/
      ├── input/              # 原始数据输入目录
      │   └── chat_data_*.csv
      └── output/             # MapReduce输出目录
          └── {timestamp}/
              └── part-m-00000
```

**HDFS配置：**
- NameNode地址：`hdfs://hadoop:9000`
- 操作用户：`root`
- 副本数：默认3个副本

**操作流程：**
1. 检查HDFS目录是否存在，不存在则创建
2. 将CSV文件流式上传到HDFS
3. 验证文件上传成功
4. 返回HDFS文件路径

**代码位置：**
- `HdfsService.java` - HDFS操作服务
- `DataPipelineService.java` - 数据管道服务

**关键方法：**
```java
// 确保目录存在
hdfsService.ensureDirectoryExists("/data/chat_analysis/input");

// 上传文件到HDFS
hdfsService.uploadToHdfs(inputStream, "/data/chat_analysis/input", fileName);
```

---

### 阶段3：MapReduce数据清洗

**功能描述：**
- 从HDFS读取原始数据
- 使用MapReduce进行数据清洗
- 去除无效数据和格式不正确的记录
- 统一数据格式（日期格式、字段分隔符等）
- 输出清洗后的数据到HDFS

**MapReduce作业配置：**
- Job名称：`ChatDataCleaner`
- 输入路径：`/data/chat_analysis/input/chat_data_*.csv`
- 输出路径：`/data/chat_analysis/output/{timestamp}/`
- Mapper类：`CleanMapper`
- Reducer：无（仅使用Mapper）

**数据清洗规则：**
1. **日期时间格式化：**
   - 统一转换为 `yyyy-MM-dd HH:mm:ss` 格式
   - 处理多种日期格式（斜杠、连字符、单数字等）

2. **字段分隔符转换：**
   - CSV逗号分隔 → Tab分隔（Hive标准格式）

3. **数据验证：**
   - 检查必填字段是否存在
   - 验证数据类型是否正确
   - 过滤空值和无效数据

4. **数据格式化：**
   - 去除首尾空白
   - 处理特殊字符
   - 统一编码格式（UTF-8）

**MapReduce输出格式：**
```
msg_time    user_id    group_id    message    ip_address    message_length    message_type    user_age    user_gender    platform    user_name
```
（Tab分隔）

**代码位置：**
- `ChatDataCleaner.java` - MapReduce作业类
- `DataPipelineService.java` - 调用MapReduce作业

**关键代码：**
```java
// 运行MapReduce作业
boolean mrSuccess = ChatDataCleaner.runJob(
    hdfsInputPath,    // HDFS输入路径
    hdfsOutputPath    // HDFS输出路径
);
```

---

### 阶段4：Hive数据仓库

**功能描述：**
- 创建Hive数据库和表结构
- 将MapReduce清洗后的数据加载到Hive表
- 数据存储在HDFS的Hive表目录中
- 提供SQL查询接口

**Hive数据库和表：**
- 数据库：`ods` (Operational Data Store)
- 表名：`ods_chat_analysis_info`
- 存储格式：文本文件（Tab分隔）
- 存储位置：`/user/hive/warehouse/ods.db/ods_chat_analysis_info/`

**表结构：**
```sql
CREATE DATABASE IF NOT EXISTS ods;

USE ods;

CREATE TABLE IF NOT EXISTS ods_chat_analysis_info (
    msg_time STRING COMMENT '消息时间',
    user_id STRING COMMENT '用户ID',
    group_id STRING COMMENT '群组ID',
    message STRING COMMENT '消息内容',
    ip_address STRING COMMENT 'IP地址',
    message_length INT COMMENT '消息长度',
    message_type INT COMMENT '消息类型：1-文本，2-图片，3-语音',
    user_age INT COMMENT '用户年龄',
    user_gender STRING COMMENT '用户性别',
    platform STRING COMMENT '平台',
    user_name STRING COMMENT '用户名'
)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/ods.db/ods_chat_analysis_info';
```

**数据加载流程：**
1. 确保Hive数据库存在
2. 创建或验证表结构
3. 使用 `LOAD DATA INPATH` 将MapReduce输出数据加载到表
4. 验证数据加载成功（查询记录数）

**Hive配置：**
- HiveServer2地址：`jdbc:hive2://hadoop:10000/ods`
- 连接用户：`root`
- 连接密码：根据实际配置

**代码位置：**
- `HiveService.java` - Hive操作服务
- `DataPipelineService.java` - 数据管道服务

**关键方法：**
```java
// 创建数据库
hiveService.createDatabaseIfNotExists();

// 创建表
hiveService.createTableIfNotExists();

// 加载数据到表
hiveService.loadDataToTable(cleanedDataPath);
```

---

### 阶段5：数据查询与分析

**功能描述：**
- 通过SQL查询Hive表中的数据
- 进行多维度统计分析
- 提供RESTful API接口
- 支持前端可视化展示

**查询接口：**

#### 1. 数据概览
```
GET /api/hive/data-summary
```
返回：总消息数、用户数、最早时间、最晚时间

#### 2. 每日消息统计
```
GET /api/hive/daily-stats
```
返回：按日期分组的消息数量统计

#### 3. 消息类型统计
```
GET /api/hive/type-stats
```
返回：文本、图片、语音消息的数量统计

#### 4. 用户性别统计
```
GET /api/hive/gender-stats
```
返回：按性别分组的消息数量统计

#### 5. 平台统计
```
GET /api/hive/platform-stats
```
返回：按平台（Android、iOS、Windows等）分组的消息数量统计

#### 6. 年龄统计
```
GET /api/hive/age-stats
```
返回：按年龄段分组的消息数量统计

#### 7. 小时活跃度统计
```
GET /api/hive/hour-stats
```
返回：按小时分组的消息数量统计（0-23点）

#### 8. 原始数据查询（分页）
```
GET /api/hive/raw-data?page=1&size=10
```
返回：分页的原始数据列表

**SQL查询示例：**

**每日统计：**
```sql
SELECT DATE(msg_time) as stat_date, COUNT(*) as message_count
FROM ods.ods_chat_analysis_info
GROUP BY DATE(msg_time)
ORDER BY stat_date;
```

**消息类型统计：**
```sql
SELECT message_type, COUNT(*) as message_count
FROM ods.ods_chat_analysis_info
GROUP BY message_type;
```

**性别统计：**
```sql
SELECT user_gender, COUNT(*) as message_count
FROM ods.ods_chat_analysis_info
WHERE user_gender IS NOT NULL
GROUP BY user_gender;
```

**代码位置：**
- `ChatMessageService.java` - 数据查询服务
- `ChatMessageRepository.java` - 数据访问层（JPA Repository）
- `HiveController.java` - RESTful API控制器

---


## 技术栈说明

### 大数据组件

1. **Hadoop HDFS**
   - 版本：3.3.4
   - 用途：分布式文件存储
   - 特点：高可靠性、高吞吐量

2. **MapReduce**
   - 版本：3.3.4
   - 用途：分布式数据处理
   - 特点：并行处理、容错机制

3. **Apache Hive**
   - 版本：3.1.3
   - 用途：数据仓库和SQL查询
   - 特点：SQL接口、元数据管理

### 后端技术

1. **Spring Boot**
   - 版本：2.7.18
   - 用途：应用框架

2. **Spring Data JPA**
   - 用途：数据访问层

3. **Apache Commons CSV**
   - 用途：CSV文件解析

### 前端技术

1. **Vue.js 2.x**
   - 用途：前端框架

2. **Element UI**
   - 用途：UI组件库

3. **ECharts**
   - 用途：数据可视化

---

## API接口文档

### 数据导入接口

#### 1. 批量导入数据
```
POST /api/hive/import-data
Content-Type: multipart/form-data

参数：
- file: CSV文件

响应：
{
  "success": true,
  "message": "数据导入完成",
  "successCount": 50000,
  "errorCount": 0,
  "totalCount": 50000
}
```

#### 2. 简单导入（逐行）
```
POST /api/hive/simple-import
Content-Type: multipart/form-data

参数：
- file: CSV文件

响应：同上
```

### HDFS和Hive接口

#### 1. 测试HDFS连接
```
GET /api/hive/test-hdfs

响应：
{
  "success": true,
  "message": "HDFS连接正常"
}
```

#### 2. 测试Hive连接
```
GET /api/hive/test-hive

响应：
{
  "success": true,
  "message": "Hive连接正常"
}
```

#### 3. 完整数据处理流程
```
POST /api/hive/process-to-hive
Content-Type: multipart/form-data

参数：
- file: CSV文件

响应：
{
  "success": true,
  "message": "数据处理流程完成",
  "hdfsInputPath": "/data/chat_analysis/input/chat_data_20241220164530.csv",
  "hdfsOutputPath": "/data/chat_analysis/output/20241220164530",
  "hiveTablePath": "/user/hive/warehouse/ods.db/ods_chat_analysis_info",
  "recordCount": 50000
}
```

### 数据查询接口

#### 1. 数据概览
```
GET /api/hive/data-summary

响应：
{
  "success": true,
  "totalCount": 50000,
  "userCount": 1100,
  "minTime": "2024-01-01 01:17:00",
  "maxTime": "2024-12-20 15:39:47"
}
```

#### 2. 原始数据（分页）
```
GET /api/hive/raw-data?page=1&size=10

响应：
{
  "content": [...],
  "totalElements": 50000,
  "totalPages": 5000,
  "currentPage": 1,
  "pageSize": 10
}
```

---

## 环境配置

### Hadoop配置

**core-site.xml:**
```xml
<property>
  <name>fs.defaultFS</name>
  <value>hdfs://hadoop:9000</value>
</property>
```

**hdfs-site.xml:**
```xml
<property>
  <name>dfs.replication</name>
  <value>3</value>
</property>
```

### Hive配置

**hive-site.xml:**
```xml
<property>
  <name>hive.metastore.uris</name>
  <value>thrift://hadoop:9083</value>
</property>
<property>
  <name>javax.jdo.option.ConnectionURL</name>
  <value>jdbc:mysql://hadoop:3306/hive_metastore</value>
</property>
```

### 应用配置

**application.yml:**
```yaml
# HDFS配置
hdfs:
  uri: hdfs://hadoop:9000
  user: root

# Hive配置
hive:
  jdbc:
    url: jdbc:hive2://hadoop:10000/ods
    user: root
    password: ""
```

---

## 性能优化建议

### 1. HDFS优化
- 调整块大小（默认128MB）
- 优化副本策略
- 使用压缩存储

### 2. MapReduce优化
- 调整Mapper和Reducer数量
- 使用Combiner减少数据传输
- 优化数据分区策略

### 3. Hive优化
- 使用分区表（按日期分区）
- 使用ORC或Parquet存储格式
- 建立合适的索引
- 使用列式存储

### 4. 查询优化
- 使用分区裁剪
- 避免全表扫描
- 使用合适的JOIN策略

---

## 故障排查

### 常见问题

1. **HDFS连接失败**
   - 检查NameNode是否启动
   - 检查网络连接
   - 检查防火墙设置

2. **MapReduce作业失败**
   - 检查YARN是否启动
   - 检查资源是否充足
   - 查看作业日志

3. **Hive连接失败**
   - 检查HiveServer2是否启动
   - 检查元数据库连接
   - 检查JDBC驱动

4. **数据加载失败**
   - 检查HDFS文件路径
   - 检查表结构是否匹配
   - 检查数据格式是否正确

---

## 总结

本系统实现了完整的大数据处理流程：

1. **数据导入**：CSV文件解析和格式转换
2. **HDFS存储**：分布式文件存储，保证高可用性
3. **MapReduce清洗**：分布式数据处理，清洗和格式化数据
4. **Hive数据仓库**：SQL接口查询，支持复杂分析
5. **数据查询**：多维度统计分析，可视化展示

整个流程充分利用了Hadoop生态系统的优势，实现了大数据的高效处理和分析。

