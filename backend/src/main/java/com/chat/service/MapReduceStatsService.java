package com.chat.service;

import com.chat.entity.*;
import com.chat.mapreduce.*;
import com.chat.repository.*;
import org.apache.hadoop.fs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MapReduce统计分析服务
 * 负责运行MapReduce作业，读取结果并写入MySQL
 */
@Service
public class MapReduceStatsService {
    
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
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired(required = false)
    private HdfsService hdfsService;
    
    private static final String HDFS_INPUT_PATH = "/data/chat";
    private static final String HDFS_OUTPUT_BASE = "/data/chat_analysis/stats";
    
    /**
     * 运行每日统计MapReduce并写入MySQL
     */
    @Transactional
    public Map<String, Object> runDailyStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            String outputPath = HDFS_OUTPUT_BASE + "/daily";
            
            // 运行MapReduce
            boolean success = DailyStatsAnalyzer.runJob(HDFS_INPUT_PATH, outputPath);
            if (!success) {
                result.put("success", false);
                result.put("message", "MapReduce作业执行失败");
                return result;
            }
            
            // 读取结果并写入MySQL
            int count = loadDailyStatsFromHDFS(outputPath);
            
            result.put("success", true);
            result.put("message", "每日统计完成");
            result.put("recordCount", count);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "每日统计失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * 运行消息类型统计MapReduce并写入MySQL
     */
    @Transactional
    public Map<String, Object> runTypeStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            String outputPath = HDFS_OUTPUT_BASE + "/type";
            
            boolean success = TypeStatsAnalyzer.runJob(HDFS_INPUT_PATH, outputPath);
            if (!success) {
                result.put("success", false);
                result.put("message", "MapReduce作业执行失败");
                return result;
            }
            
            int count = loadTypeStatsFromHDFS(outputPath);
            
            result.put("success", true);
            result.put("message", "消息类型统计完成");
            result.put("recordCount", count);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "消息类型统计失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * 运行聊天室/群组统计MapReduce并写入MySQL
     */
    @Transactional
    public Map<String, Object> runGroupStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            String outputPath = HDFS_OUTPUT_BASE + "/group";
            
            boolean success = GroupStatsAnalyzer.runJob(HDFS_INPUT_PATH, outputPath);
            if (!success) {
                result.put("success", false);
                result.put("message", "MapReduce作业执行失败");
                return result;
            }
            
            int count = loadGroupStatsFromHDFS(outputPath);
            
            result.put("success", true);
            result.put("message", "聊天室/群组统计完成");
            result.put("recordCount", count);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "聊天室/群组统计失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * 运行平台统计MapReduce并写入MySQL
     */
    @Transactional
    public Map<String, Object> runPlatformStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            String outputPath = HDFS_OUTPUT_BASE + "/platform";
            
            boolean success = PlatformStatsAnalyzer.runJob(HDFS_INPUT_PATH, outputPath);
            if (!success) {
                result.put("success", false);
                result.put("message", "MapReduce作业执行失败");
                return result;
            }
            
            int count = loadPlatformStatsFromHDFS(outputPath);
            
            result.put("success", true);
            result.put("message", "平台统计完成");
            result.put("recordCount", count);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "平台统计失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * 运行消息长度统计MapReduce并写入MySQL
     */
    @Transactional
    public Map<String, Object> runLengthStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            String outputPath = HDFS_OUTPUT_BASE + "/length";
            
            boolean success = LengthStatsAnalyzer.runJob(HDFS_INPUT_PATH, outputPath);
            if (!success) {
                result.put("success", false);
                result.put("message", "MapReduce作业执行失败");
                return result;
            }
            
            int count = loadLengthStatsFromHDFS(outputPath);
            
            result.put("success", true);
            result.put("message", "消息长度统计完成");
            result.put("recordCount", count);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "消息长度统计失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * 运行小时统计MapReduce并写入MySQL
     */
    @Transactional
    public Map<String, Object> runHourStats() {
        Map<String, Object> result = new HashMap<>();
        try {
            String outputPath = HDFS_OUTPUT_BASE + "/hour";
            
            boolean success = HourStatsAnalyzer.runJob(HDFS_INPUT_PATH, outputPath);
            if (!success) {
                result.put("success", false);
                result.put("message", "MapReduce作业执行失败");
                return result;
            }
            
            int count = loadHourStatsFromHDFS(outputPath);
            
            result.put("success", true);
            result.put("message", "小时统计完成");
            result.put("recordCount", count);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "小时统计失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }
    
    // 从HDFS读取每日统计结果并写入MySQL
    private int loadDailyStatsFromHDFS(String hdfsPath) throws Exception {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.set("fs.defaultFS", "hdfs://hadoop:9000");
        FileSystem fs = FileSystem.get(conf);
        
        // 清空旧数据
        statsDailyRepository.deleteAll();
        
        Path outputDir = new Path(hdfsPath);
        if (!fs.exists(outputDir)) {
            return 0;
        }
        
        int count = 0;
        FileStatus[] files = fs.listStatus(outputDir);
        for (FileStatus file : files) {
            if (file.getPath().getName().startsWith("part-")) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(fs.open(file.getPath())))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 2) {
                            StatsDaily stats = new StatsDaily();
                            stats.setStatDate(LocalDate.parse(parts[0]));
                            stats.setMessageCount(Long.parseLong(parts[1]));
                            statsDailyRepository.save(stats);
                            count++;
                        }
                    }
                }
            }
        }
        fs.close();
        return count;
    }
    
    // 从HDFS读取消息类型统计结果并写入MySQL
    private int loadTypeStatsFromHDFS(String hdfsPath) throws Exception {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.set("fs.defaultFS", "hdfs://hadoop:9000");
        FileSystem fs = FileSystem.get(conf);
        
        statsTypeRepository.deleteAll();
        
        Path outputDir = new Path(hdfsPath);
        if (!fs.exists(outputDir)) {
            return 0;
        }
        
        int count = 0;
        FileStatus[] files = fs.listStatus(outputDir);
        for (FileStatus file : files) {
            if (file.getPath().getName().startsWith("part-")) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(fs.open(file.getPath())))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 2) {
                            StatsType stats = new StatsType();
                            stats.setMessageType(Integer.parseInt(parts[0]));
                            stats.setMessageCount(Long.parseLong(parts[1]));
                            statsTypeRepository.save(stats);
                            count++;
                        }
                    }
                }
            }
        }
        fs.close();
        return count;
    }
    
    // 从HDFS读取平台统计结果并写入MySQL
    private int loadPlatformStatsFromHDFS(String hdfsPath) throws Exception {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.set("fs.defaultFS", "hdfs://hadoop:9000");
        FileSystem fs = FileSystem.get(conf);
        
        statsPlatformRepository.deleteAll();
        
        Path outputDir = new Path(hdfsPath);
        if (!fs.exists(outputDir)) {
            return 0;
        }
        
        int count = 0;
        FileStatus[] files = fs.listStatus(outputDir);
        for (FileStatus file : files) {
            if (file.getPath().getName().startsWith("part-")) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(fs.open(file.getPath())))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 2) {
                            StatsPlatform stats = new StatsPlatform();
                            stats.setPlatform(parts[0]);
                            stats.setMessageCount(Long.parseLong(parts[1]));
                            statsPlatformRepository.save(stats);
                            count++;
                        }
                    }
                }
            }
        }
        fs.close();
        return count;
    }
    
    /**
     * 运行所有6个统计分析
     * 按顺序执行：每日、类型、性别、平台、年龄、小时
     */
    @Transactional
    public Map<String, Object> runAllStats() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> details = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        
        System.out.println("开始运行所有统计分析...");
        
        // 1. 每日统计
        try {
            System.out.println("正在运行每日统计...");
            Map<String, Object> dailyResult = runDailyStats();
            details.put("daily", dailyResult);
            if ((Boolean) dailyResult.get("success")) {
                successCount++;
            } else {
                failCount++;
            }
        } catch (Exception e) {
            failCount++;
            Map<String, Object> errorDetail = new HashMap<>();
            errorDetail.put("success", false);
            errorDetail.put("message", e.getMessage());
            details.put("daily", errorDetail);
            e.printStackTrace();
        }
        
        // 2. 消息类型统计
        try {
            System.out.println("正在运行消息类型统计...");
            Map<String, Object> typeResult = runTypeStats();
            details.put("type", typeResult);
            if ((Boolean) typeResult.get("success")) {
                successCount++;
            } else {
                failCount++;
            }
        } catch (Exception e) {
            failCount++;
            Map<String, Object> errorDetail = new HashMap<>();
            errorDetail.put("success", false);
            errorDetail.put("message", e.getMessage());
            details.put("type", errorDetail);
            e.printStackTrace();
        }
        
        // 3. 聊天室/群组统计
        try {
            System.out.println("正在运行聊天室/群组统计...");
            Map<String, Object> groupResult = runGroupStats();
            details.put("group", groupResult);
            if ((Boolean) groupResult.get("success")) {
                successCount++;
            } else {
                failCount++;
            }
        } catch (Exception e) {
            failCount++;
            Map<String, Object> errorDetail = new HashMap<>();
            errorDetail.put("success", false);
            errorDetail.put("message", e.getMessage());
            details.put("group", errorDetail);
            e.printStackTrace();
        }
        
        // 4. 平台统计
        try {
            System.out.println("正在运行平台统计...");
            Map<String, Object> platformResult = runPlatformStats();
            details.put("platform", platformResult);
            if ((Boolean) platformResult.get("success")) {
                successCount++;
            } else {
                failCount++;
            }
        } catch (Exception e) {
            failCount++;
            Map<String, Object> errorDetail = new HashMap<>();
            errorDetail.put("success", false);
            errorDetail.put("message", e.getMessage());
            details.put("platform", errorDetail);
            e.printStackTrace();
        }
        
        // 5. 消息长度统计
        try {
            System.out.println("正在运行消息长度统计...");
            Map<String, Object> lengthResult = runLengthStats();
            details.put("length", lengthResult);
            if ((Boolean) lengthResult.get("success")) {
                successCount++;
            } else {
                failCount++;
            }
        } catch (Exception e) {
            failCount++;
            Map<String, Object> errorDetail = new HashMap<>();
            errorDetail.put("success", false);
            errorDetail.put("message", e.getMessage());
            details.put("length", errorDetail);
            e.printStackTrace();
        }
        
        // 6. 小时统计
        try {
            System.out.println("正在运行小时统计...");
            Map<String, Object> hourResult = runHourStats();
            details.put("hour", hourResult);
            if ((Boolean) hourResult.get("success")) {
                successCount++;
            } else {
                failCount++;
            }
        } catch (Exception e) {
            failCount++;
            Map<String, Object> errorDetail = new HashMap<>();
            errorDetail.put("success", false);
            errorDetail.put("message", e.getMessage());
            details.put("hour", errorDetail);
            e.printStackTrace();
        }
        
        result.put("success", failCount == 0);
        result.put("message", String.format("统计分析完成：成功 %d 个，失败 %d 个", successCount, failCount));
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("details", details);
        
        System.out.println("所有统计分析完成！成功: " + successCount + ", 失败: " + failCount);
        
        return result;
    }
    
    // 从HDFS读取小时统计结果并写入MySQL
    private int loadHourStatsFromHDFS(String hdfsPath) throws Exception {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.set("fs.defaultFS", "hdfs://hadoop:9000");
        FileSystem fs = FileSystem.get(conf);
        
        statsHourRepository.deleteAll();
        
        Path outputDir = new Path(hdfsPath);
        if (!fs.exists(outputDir)) {
            return 0;
        }
        
        int count = 0;
        FileStatus[] files = fs.listStatus(outputDir);
        for (FileStatus file : files) {
            if (file.getPath().getName().startsWith("part-")) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(fs.open(file.getPath())))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 2) {
                            StatsHour stats = new StatsHour();
                            stats.setHour(Integer.parseInt(parts[0]));
                            stats.setMessageCount(Long.parseLong(parts[1]));
                            statsHourRepository.save(stats);
                            count++;
                        }
                    }
                }
            }
        }
        fs.close();
        return count;
    }
    
    // 从HDFS读取聊天室/群组统计结果并写入MySQL
    private int loadGroupStatsFromHDFS(String hdfsPath) throws Exception {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.set("fs.defaultFS", "hdfs://hadoop:9000");
        FileSystem fs = FileSystem.get(conf);
        
        statsGroupRepository.deleteAll();
        
        Path outputDir = new Path(hdfsPath);
        if (!fs.exists(outputDir)) {
            return 0;
        }
        
        int count = 0;
        FileStatus[] files = fs.listStatus(outputDir);
        for (FileStatus file : files) {
            if (file.getPath().getName().startsWith("part-")) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(fs.open(file.getPath())))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 2) {
                            StatsGroup stats = new StatsGroup();
                            stats.setGroupId(parts[0]);
                            stats.setMessageCount(Long.parseLong(parts[1]));
                            statsGroupRepository.save(stats);
                            count++;
                        }
                    }
                }
            }
        }
        fs.close();
        return count;
    }
    
    // 从HDFS读取消息长度统计结果并写入MySQL
    private int loadLengthStatsFromHDFS(String hdfsPath) throws Exception {
        org.apache.hadoop.conf.Configuration conf = new org.apache.hadoop.conf.Configuration();
        conf.set("fs.defaultFS", "hdfs://hadoop:9000");
        FileSystem fs = FileSystem.get(conf);
        
        statsLengthRepository.deleteAll();
        
        Path outputDir = new Path(hdfsPath);
        if (!fs.exists(outputDir)) {
            return 0;
        }
        
        int count = 0;
        FileStatus[] files = fs.listStatus(outputDir);
        for (FileStatus file : files) {
            if (file.getPath().getName().startsWith("part-")) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(fs.open(file.getPath())))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        if (parts.length >= 2) {
                            StatsLength stats = new StatsLength();
                            stats.setLengthGroup(parts[0]);
                            stats.setMessageCount(Long.parseLong(parts[1]));
                            statsLengthRepository.save(stats);
                            count++;
                        }
                    }
                }
            }
        }
        fs.close();
        return count;
    }
}

