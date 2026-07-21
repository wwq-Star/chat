package com.chat.service;

import com.chat.entity.ChatMessage;
import com.chat.repository.ChatMessageRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CSV导入服务
 * 功能：直接解析CSV文件，转换数据格式，批量写入MySQL数据库，同时同步到Hive
 * 
 * 数据转换逻辑：
 * - CSV字段 -> MySQL字段映射
 *   - send_time -> msg_time (支持多种日期格式)
 *   - user_id -> user_id
 *   - chat_room -> group_id
 *   - message_content -> message
 *   - message_type -> message_type (text->1, image->2, voice->3)
 *   - device_type -> platform
 *   - message_length -> message_length
 *   - user_name -> user_name
 * - 自动生成字段：
 *   - ip_address (随机生成)
 *   - user_age (随机生成 18-67)
 *   - user_gender (随机生成 男/女)
 * 
 * Hive同步功能：
 * - 将原始CSV文件上传到HDFS的/data/chat目录
 * - Hive外部表ods.chat_data_external需要提前创建（路径：/data/chat）
 * - 外部表会自动读取该目录下的所有CSV文件，无需额外数据加载步骤
 * - 注意：需要启动Hive Metastore服务，否则无法查询外部表
 */
@Service
public class CsvImportService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired(required = false)
    private HdfsService hdfsService;
    
    private static final String HDFS_CHAT_DIR = "/data/chat";
    
    // 支持多种日期格式
    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),  // 标准格式：2024-12-13 23:39:41
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),  // 斜杠格式：2024/12/13 23:39:41
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),      // 无秒：2024-12-13 23:39
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),      // 斜杠无秒：2024/12/13 23:39
        DateTimeFormatter.ofPattern("yyyy/M/d HH:mm"),        // 单数字：2024/1/4 19:24
        DateTimeFormatter.ofPattern("yyyy-M-d HH:mm"),       // 单数字连字符：2024-1-4 19:24
        DateTimeFormatter.ofPattern("yyyy/M/d H:mm"),         // 单数字小时：2024/1/4 8:22
        DateTimeFormatter.ofPattern("yyyy-M-d H:mm")           // 单数字小时连字符：2024-1-4 8:22
    };
    private static final Random random = new Random();
    
    // 消息类型映射：text->1, image->2, voice->3
    private int mapMessageType(String type) {
        if (type == null) return 1;
        switch (type.toLowerCase()) {
            case "text": return 1;
            case "image": return 2;
            case "voice": return 3;
            default: return 1;
        }
    }
    
    // 生成随机IP地址
    private String generateRandomIp() {
        return String.format("192.168.%d.%d", random.nextInt(255), random.nextInt(255) + 1);
    }
    
    // 生成随机年龄
    private Integer generateRandomAge() {
        return random.nextInt(50) + 18; // 18-67岁
    }
    
    // 生成随机性别
    private String generateRandomGender() {
        String[] genders = {"男", "女"};
        return genders[random.nextInt(2)];
    }
    
    /**
     * 批量导入数据到MySQL和Hive（推荐方式）
     * 使用批量插入，性能更好，适合大量数据
     * 同时将原始CSV文件上传到HDFS并创建Hive外部表
     * 
     * @param file CSV文件
     * @return 导入结果（成功数量、失败数量、总数量、Hive导入状态）
     */
    @Transactional
    public Map<String, Object> importData(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;
        List<ChatMessage> batch = new ArrayList<>();
        int batchSize = 1000;
        
        // 先读取文件内容到字节数组（因为InputStream只能读取一次）
        byte[] fileBytes = null;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "读取文件失败: " + e.getMessage());
            return result;
        }
        
        try {
            // 1. 导入数据到MySQL
            ByteArrayInputStream mysqlStream = new ByteArrayInputStream(fileBytes);
            InputStreamReader reader = new InputStreamReader(mysqlStream, StandardCharsets.UTF_8);
            // 配置CSV解析器，忽略大小写并去除空白，处理BOM
            CSVParser csvParser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .parse(reader);
            
            for (CSVRecord record : csvParser) {
                try {
                    ChatMessage message = parseRecord(record);
                    if (message != null) {
                        batch.add(message);
                        
                        if (batch.size() >= batchSize) {
                            chatMessageRepository.saveAll(batch);
                            successCount += batch.size();
                            batch.clear();
                        }
                    } else {
                        errorCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("解析记录失败: " + e.getMessage());
                }
            }
            
            // 保存剩余数据
            if (!batch.isEmpty()) {
                chatMessageRepository.saveAll(batch);
                successCount += batch.size();
            }
            
            Long totalCount = chatMessageRepository.countTotalMessages();
            
            result.put("success", true);
            result.put("message", "数据导入完成");
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            result.put("totalCount", totalCount);
            
            // 2. 同步上传到HDFS并创建Hive外部表
            Map<String, Object> hiveResult = syncToHive(fileBytes, file.getOriginalFilename());
            result.put("hiveSync", hiveResult);
            if (!(Boolean) hiveResult.get("success")) {
                result.put("message", result.get("message") + "，但Hive同步失败: " + hiveResult.get("message"));
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
        }
        
        return result;
    }
    
    /**
     * 同步数据到Hive（仅上传到HDFS）
     * 将CSV文件上传到HDFS的/data/chat目录，Hive外部表会自动读取该目录下的数据
     * 注意：Hive外部表需要提前创建，此方法只负责上传数据文件
     * 
     * @param fileBytes 文件字节数组
     * @param fileName 文件名
     * @return 同步结果
     */
    private Map<String, Object> syncToHive(byte[] fileBytes, String fileName) {
        Map<String, Object> result = new HashMap<>();
        
        // 如果HdfsService未注入，跳过Hive同步
        if (hdfsService == null) {
            result.put("success", false);
            result.put("message", "HDFS服务未配置，跳过同步");
            return result;
        }
        
        try {
            // 1. 确保HDFS目录存在
            hdfsService.ensureDirectoryExists(HDFS_CHAT_DIR);
            
            // 2. 生成唯一的文件名（带时间戳，避免覆盖）
            String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String hdfsFileName = "chat_data_" + timestamp + ".csv";
            if (fileName != null && !fileName.isEmpty()) {
                // 保留原文件名但添加时间戳
                String baseName = fileName.substring(0, fileName.lastIndexOf('.') > 0 ? 
                    fileName.lastIndexOf('.') : fileName.length());
                String extension = fileName.lastIndexOf('.') > 0 ? 
                    fileName.substring(fileName.lastIndexOf('.')) : ".csv";
                hdfsFileName = baseName + "_" + timestamp + extension;
            }
            
            // 3. 上传文件到HDFS
            ByteArrayInputStream hdfsStream = new ByteArrayInputStream(fileBytes);
            Map<String, Object> uploadResult = hdfsService.uploadToHdfs(
                hdfsStream, 
                HDFS_CHAT_DIR, 
                hdfsFileName
            );
            
            if (!(Boolean) uploadResult.get("success")) {
                result.put("success", false);
                result.put("message", "上传到HDFS失败: " + uploadResult.get("message"));
                return result;
            }
            
            result.put("success", true);
            result.put("message", "数据已上传到HDFS，Hive外部表将自动读取");
            result.put("hdfsPath", HDFS_CHAT_DIR + "/" + hdfsFileName);
            result.put("hiveTable", "ods.chat_data_external");
            result.put("note", "请确保Hive外部表已创建，且Metastore服务已启动");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Hive同步失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * 逐行导入数据到MySQL和Hive
     * 逐条插入，适合小数据量或需要逐条验证的场景
     * 同时将原始CSV文件上传到HDFS并创建Hive外部表
     * 
     * @param file CSV文件
     * @return 导入结果（成功数量、失败数量、总数量、Hive导入状态）
     */
    @Transactional
    public Map<String, Object> simpleImport(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int errorCount = 0;
        
        // 先读取文件内容到字节数组（因为InputStream只能读取一次）
        byte[] fileBytes = null;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "读取文件失败: " + e.getMessage());
            return result;
        }
        
        try {
            // 1. 导入数据到MySQL
            ByteArrayInputStream mysqlStream = new ByteArrayInputStream(fileBytes);
            InputStreamReader reader = new InputStreamReader(mysqlStream, StandardCharsets.UTF_8);
            // 配置CSV解析器，忽略大小写并去除空白，处理BOM
            CSVParser csvParser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .parse(reader);
            
            for (CSVRecord record : csvParser) {
                try {
                    ChatMessage message = parseRecord(record);
                    if (message != null) {
                        chatMessageRepository.save(message);
                        successCount++;
                    } else {
                        errorCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    System.err.println("导入记录失败: " + e.getMessage());
                }
            }
            
            Long totalCount = chatMessageRepository.countTotalMessages();
            
            result.put("success", true);
            result.put("message", "数据导入完成");
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
            result.put("totalCount", totalCount);
            
            // 2. 同步上传到HDFS并创建Hive外部表
            Map<String, Object> hiveResult = syncToHive(fileBytes, file.getOriginalFilename());
            result.put("hiveSync", hiveResult);
            if (!(Boolean) hiveResult.get("success")) {
                result.put("message", result.get("message") + "，但Hive同步失败: " + hiveResult.get("message"));
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
            result.put("successCount", successCount);
            result.put("errorCount", errorCount);
        }
        
        return result;
    }
    
    // 解析日期时间，支持多种格式
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        String trimmed = dateTimeStr.trim();
        
        // 尝试使用各种格式解析
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(trimmed, formatter);
            } catch (Exception e) {
                // 继续尝试下一个格式
            }
        }
        
        // 如果所有格式都失败，记录错误并返回当前时间
        System.err.println("无法解析日期时间: " + trimmed + "，使用当前时间");
        return LocalDateTime.now();
    }
    
    // 去除BOM字符
    private String removeBOM(String str) {
        if (str != null && str.length() > 0 && str.charAt(0) == '\uFEFF') {
            return str.substring(1);
        }
        return str;
    }
    
    // 安全获取CSV字段值（处理BOM）
    private String getField(CSVRecord record, String fieldName) {
        try {
            // 先尝试直接获取
            return record.get(fieldName);
        } catch (IllegalArgumentException e) {
            // 如果失败，尝试去除BOM后的字段名
            try {
                return record.get(removeBOM(fieldName));
            } catch (IllegalArgumentException e2) {
                // 如果还是失败，尝试所有可能的字段名（包括带BOM的）
                for (String header : record.getParser().getHeaderMap().keySet()) {
                    if (removeBOM(header).equals(fieldName) || header.equals(fieldName)) {
                        return record.get(header);
                    }
                }
                // 如果都找不到，返回null
                return null;
            }
        }
    }
    
    /**
     * 解析CSV记录并转换为ChatMessage实体
     * 完成所有字段的格式转换和映射
     * 
     * CSV字段映射：
     * - send_time -> msgTime (日期时间，支持多种格式)
     * - user_id -> userId (字符串，处理BOM)
     * - chat_room -> groupId (字符串)
     * - message_content -> message (文本内容)
     * - message_type -> messageType (类型转换：text->1, image->2, voice->3)
     * - device_type -> platform (平台类型)
     * - message_length -> messageLength (整数)
     * - user_name -> userName (用户名)
     * 
     * 自动生成字段：
     * - ipAddress (随机IP地址)
     * - userAge (随机年龄 18-67)
     * - userGender (随机性别 男/女)
     * 
     * @param record CSV记录
     * @return ChatMessage实体对象，解析失败返回null
     */
    private ChatMessage parseRecord(CSVRecord record) {
        try {
            ChatMessage message = new ChatMessage();
            
            // 1. 解析时间（支持多种格式：yyyy-MM-dd HH:mm:ss, yyyy/MM/dd HH:mm等）
            String sendTime = getField(record, "send_time");
            message.setMsgTime(parseDateTime(sendTime));
            
            // 2. 用户ID（处理BOM字符）
            String userId = getField(record, "user_id");
            if (userId != null) {
                userId = removeBOM(userId); // 去除可能的BOM
            }
            message.setUserId(userId);
            
            // 3. 群组ID（从chat_room字段映射）
            message.setGroupId(getField(record, "chat_room"));
            
            // 4. 消息内容（从message_content字段映射）
            message.setMessage(getField(record, "message_content"));
            
            // 5. 消息长度（整数转换，失败则默认为0）
            try {
                String lengthStr = getField(record, "message_length");
                if (lengthStr != null && !lengthStr.isEmpty()) {
                    message.setMessageLength(Integer.parseInt(lengthStr.trim()));
                } else {
                    message.setMessageLength(0);
                }
            } catch (Exception e) {
                message.setMessageLength(0);
            }
            
            // 6. 消息类型（字符串转整数：text->1, image->2, voice->3）
            message.setMessageType(mapMessageType(getField(record, "message_type")));
            
            // 7. 平台（从device_type字段映射）
            message.setPlatform(getField(record, "device_type"));
            
            // 8. 用户名
            message.setUserName(getField(record, "user_name"));
            
            // 9. 生成随机数据（CSV中没有的字段）
            message.setIpAddress(generateRandomIp());
            message.setUserAge(generateRandomAge());
            message.setUserGender(generateRandomGender());
            
            return message;
        } catch (Exception e) {
            System.err.println("解析记录异常: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

