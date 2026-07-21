package com.chat.service;

import com.chat.dto.DataSummary;
import com.chat.entity.*;
import com.chat.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ChatMessageService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private StatsDailyRepository statsDailyRepository;
    
    @Autowired
    private StatsTypeRepository statsTypeRepository;
    
    @Autowired
    private StatsGroupRepository statsGroupRepository;
    
    @Autowired
    private StatsPlatformRepository statsPlatformRepository;
    
    @Autowired
    private StatsLengthRepository statsLengthRepository;
    
    @Autowired
    private StatsHourRepository statsHourRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 测试连接
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            chatMessageRepository.count();
            result.put("success", true);
            result.put("message", "数据库连接正常");
        } catch (Exception e) {
            result.put("success", false);
            // 提供更详细的错误信息
            String errorMsg = e.getMessage();
            if (errorMsg != null) {
                if (errorMsg.contains("Access denied")) {
                    errorMsg = "数据库用户名或密码错误，请检查application.yml配置";
                } else if (errorMsg.contains("Unknown database")) {
                    errorMsg = "数据库不存在，请先创建chat_analysis数据库";
                } else if (errorMsg.contains("Communications link failure")) {
                    errorMsg = "无法连接到MySQL服务，请确保MySQL服务已启动";
                }
            }
            result.put("message", "数据库连接失败: " + errorMsg);
            // 打印详细错误到控制台
            System.err.println("数据库连接测试失败:");
            e.printStackTrace();
        }
        return result;
    }
    
    // 创建表（JPA会自动创建，这里只是检查）
    public Map<String, Object> createTable() {
        Map<String, Object> result = new HashMap<>();
        try {
            chatMessageRepository.count();
            result.put("success", true);
            result.put("message", "表已存在或创建成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "表创建失败: " + e.getMessage());
        }
        return result;
    }
    
    // 获取数据概览
    public DataSummary getDataSummary() {
        DataSummary summary = new DataSummary();
        try {
            Long totalCount = chatMessageRepository.countTotalMessages();
            Long userCount = chatMessageRepository.countDistinctUsers();
            LocalDateTime minTime = chatMessageRepository.getMinTime();
            LocalDateTime maxTime = chatMessageRepository.getMaxTime();
            
            summary.setSuccess(true);
            summary.setTotalCount(totalCount != null ? totalCount : 0L);
            summary.setUserCount(userCount != null ? userCount : 0L);
            summary.setMinTime(minTime != null ? minTime.format(DATE_FORMATTER) : "无");
            summary.setMaxTime(maxTime != null ? maxTime.format(DATE_FORMATTER) : "无");
        } catch (Exception e) {
            summary.setSuccess(false);
            summary.setMessage("获取数据概览失败: " + e.getMessage());
        }
        return summary;
    }
    
    // 获取每日统计
    public List<Map<String, Object>> getDailyStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 从统计结果表查询
            List<StatsDaily> stats = statsDailyRepository.findAll();
            for (StatsDaily stat : stats) {
                Map<String, Object> item = new HashMap<>();
                item.put("stat_date", stat.getStatDate().toString());
                item.put("message_count", stat.getMessageCount());
                result.add(item);
            }
            
            // 按日期排序
            result.sort((a, b) -> a.get("stat_date").toString().compareTo(b.get("stat_date").toString()));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // 获取消息类型统计
    public List<Map<String, Object>> getTypeStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 从统计结果表查询
            List<StatsType> stats = statsTypeRepository.findAll();
            for (StatsType stat : stats) {
                Map<String, Object> item = new HashMap<>();
                item.put("message_type", stat.getMessageType());
                item.put("message_count", stat.getMessageCount());
                result.add(item);
            }
            
            // 按消息类型排序
            result.sort((a, b) -> Integer.compare((Integer) a.get("message_type"), (Integer) b.get("message_type")));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // 获取聊天室/群组统计
    public List<Map<String, Object>> getGroupStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 从统计结果表查询
            List<StatsGroup> stats = statsGroupRepository.findAll();
            for (StatsGroup stat : stats) {
                Map<String, Object> item = new HashMap<>();
                item.put("group_id", stat.getGroupId());
                item.put("message_count", stat.getMessageCount());
                result.add(item);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // 获取平台统计
    public List<Map<String, Object>> getPlatformStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 从统计结果表查询
            List<StatsPlatform> stats = statsPlatformRepository.findAll();
            for (StatsPlatform stat : stats) {
                Map<String, Object> item = new HashMap<>();
                item.put("platform", stat.getPlatform());
                item.put("message_count", stat.getMessageCount());
                result.add(item);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // 获取消息长度统计
    public List<Map<String, Object>> getLengthStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 从统计结果表查询
            List<StatsLength> stats = statsLengthRepository.findAll();
            for (StatsLength stat : stats) {
                Map<String, Object> item = new HashMap<>();
                item.put("length_group", stat.getLengthGroup());
                item.put("message_count", stat.getMessageCount());
                result.add(item);
            }
            
            // 按长度区间排序
            result.sort((a, b) -> {
                String aGroup = a.get("length_group").toString();
                String bGroup = b.get("length_group").toString();
                // 特殊处理排序：0-9, 10-19, 20-49, 50-99, 100+
                String[] order = {"0-9", "10-19", "20-49", "50-99", "100+"};
                int aIndex = java.util.Arrays.asList(order).indexOf(aGroup);
                int bIndex = java.util.Arrays.asList(order).indexOf(bGroup);
                return Integer.compare(aIndex >= 0 ? aIndex : 999, bIndex >= 0 ? bIndex : 999);
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // 获取小时统计
    public List<Map<String, Object>> getHourStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            // 从统计结果表查询
            List<StatsHour> stats = statsHourRepository.findAll();
            for (StatsHour stat : stats) {
                Map<String, Object> item = new HashMap<>();
                item.put("hour", stat.getHour());
                item.put("message_count", stat.getMessageCount());
                result.add(item);
            }
            
            // 按小时排序
            result.sort((a, b) -> Integer.compare((Integer) a.get("hour"), (Integer) b.get("hour")));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    // 获取原始数据（分页）
    public Page<ChatMessage> getRawData(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        return chatMessageRepository.findAll(pageable);
    }

    public List<ChatMessage> getAllData() {
        return chatMessageRepository.findAll();
    }
    
    // 获取表结构
    public List<Map<String, String>> getTableSchema() {
        List<Map<String, String>> schema = new ArrayList<>();
        String[] fields = {
            "id", "msg_time", "user_id", "group_id", "message", 
            "ip_address", "message_length", "message_type", 
            "user_age", "user_gender", "platform", "user_name"
        };
        String[] types = {
            "BIGINT", "DATETIME", "VARCHAR(100)", "VARCHAR(100)", "TEXT",
            "VARCHAR(50)", "INT", "TINYINT",
            "INT", "VARCHAR(10)", "VARCHAR(50)", "VARCHAR(100)"
        };
        
        for (int i = 0; i < fields.length; i++) {
            Map<String, String> field = new HashMap<>();
            field.put("field", fields[i]);
            field.put("type", types[i]);
            schema.add(field);
        }
        return schema;
    }
    
    // 获取预览数据
    public List<ChatMessage> getPreview(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return chatMessageRepository.findAll(pageable).getContent();
    }
    
    // 删除单条数据
    @Transactional
    public Map<String, Object> deleteById(Long id) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (id == null) {
                result.put("success", false);
                result.put("message", "ID不能为空");
                return result;
            }
            
            if (chatMessageRepository.existsById(id)) {
                chatMessageRepository.deleteById(id);
                // 验证删除是否成功
                if (!chatMessageRepository.existsById(id)) {
                    result.put("success", true);
                    result.put("message", "删除成功");
                } else {
                    result.put("success", false);
                    result.put("message", "删除失败，记录仍存在");
                }
            } else {
                result.put("success", false);
                result.put("message", "记录不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    // 清空数据（使用TRUNCATE TABLE，性能更好）
    @Transactional
    public Map<String, Object> clearData() {
        Map<String, Object> result = new HashMap<>();
        try {
            long countBefore = chatMessageRepository.count();
            
            // 使用TRUNCATE TABLE清空数据（比DELETE快得多，适合大数据量）
            chatMessageRepository.truncateTable();
            
            // 验证清空是否成功
            long countAfter = chatMessageRepository.count();
            if (countAfter == 0) {
                result.put("success", true);
                result.put("message", "数据已清空，共删除 " + countBefore + " 条记录");
                result.put("deletedCount", countBefore);
            } else {
                result.put("success", false);
                result.put("message", "清空数据失败，仍有 " + countAfter + " 条记录");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "清空数据失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    // 插入测试数据
    @Transactional
    public Map<String, Object> insertTestData() {
        Map<String, Object> result = new HashMap<>();
        try {
            Random random = new Random();
            String[] genders = {"男", "女"};
            String[] platforms = {"Android", "iOS", "Windows", "Mac", "Web"};
            
            for (int i = 0; i < 100; i++) {
                ChatMessage message = new ChatMessage();
                message.setMsgTime(LocalDateTime.now().minusDays(random.nextInt(30)));
                message.setUserId("test_user_" + i);
                message.setGroupId("test_group_" + (i % 5));
                message.setMessage("测试消息 " + i);
                message.setIpAddress("192.168.1." + (random.nextInt(255) + 1));
                message.setMessageLength(random.nextInt(100) + 10);
                message.setMessageType(random.nextInt(3) + 1);
                message.setUserAge(random.nextInt(50) + 18);
                message.setUserGender(genders[random.nextInt(2)]);
                message.setPlatform(platforms[random.nextInt(platforms.length)]);
                message.setUserName("测试用户" + i);
                chatMessageRepository.save(message);
            }
            
            result.put("success", true);
            result.put("message", "测试数据插入成功，共100条");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "插入测试数据失败: " + e.getMessage());
        }
        return result;
    }
}

