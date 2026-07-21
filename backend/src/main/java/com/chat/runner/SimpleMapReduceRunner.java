package com.chat.runner;

import com.chat.mapreduce.DailyStatsAnalyzer;
import com.chat.mapreduce.TypeStatsAnalyzer;
import com.chat.mapreduce.PlatformStatsAnalyzer;
import com.chat.mapreduce.HourStatsAnalyzer;
import com.chat.mapreduce.GroupStatsAnalyzer;
import com.chat.mapreduce.LengthStatsAnalyzer;

/**
 * 简单的MapReduce统计分析运行器
 * 直接调用6个MapReduce分析的runJob方法
 * 
 * 使用方法：
 * 1. 编译项目：mvn clean package
 * 2. 运行：java -cp target/classes com.chat.runner.SimpleMapReduceRunner
 */
public class SimpleMapReduceRunner {
    
    private static final String HDFS_INPUT_PATH = "/data/chat";
    private static final String HDFS_OUTPUT_BASE = "/data/chat_analysis/stats";
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("开始运行所有MapReduce统计分析...");
        System.out.println("========================================");
        
        int successCount = 0;
        int failCount = 0;
        
        // 1. 每日统计
        System.out.println("\n[1/6] 正在运行每日统计...");
        try {
            boolean success = DailyStatsAnalyzer.runJob(HDFS_INPUT_PATH, HDFS_OUTPUT_BASE + "/daily");
            if (success) {
                System.out.println("✓ 每日统计完成");
                successCount++;
            } else {
                System.out.println("✗ 每日统计失败");
                failCount++;
            }
        } catch (Exception e) {
            System.err.println("✗ 每日统计异常: " + e.getMessage());
            e.printStackTrace();
            failCount++;
        }
        
        // 2. 消息类型统计
        System.out.println("\n[2/6] 正在运行消息类型统计...");
        try {
            boolean success = TypeStatsAnalyzer.runJob(HDFS_INPUT_PATH, HDFS_OUTPUT_BASE + "/type");
            if (success) {
                System.out.println("✓ 消息类型统计完成");
                successCount++;
            } else {
                System.out.println("✗ 消息类型统计失败");
                failCount++;
            }
        } catch (Exception e) {
            System.err.println("✗ 消息类型统计异常: " + e.getMessage());
            e.printStackTrace();
            failCount++;
        }
        
        // 3. 平台统计
        System.out.println("\n[3/6] 正在运行平台统计...");
        try {
            boolean success = PlatformStatsAnalyzer.runJob(HDFS_INPUT_PATH, HDFS_OUTPUT_BASE + "/platform");
            if (success) {
                System.out.println("✓ 平台统计完成");
                successCount++;
            } else {
                System.out.println("✗ 平台统计失败");
                failCount++;
            }
        } catch (Exception e) {
            System.err.println("✗ 平台统计异常: " + e.getMessage());
            e.printStackTrace();
            failCount++;
        }
        
        // 4. 小时统计
        System.out.println("\n[4/6] 正在运行小时统计...");
        try {
            boolean success = HourStatsAnalyzer.runJob(HDFS_INPUT_PATH, HDFS_OUTPUT_BASE + "/hour");
            if (success) {
                System.out.println("✓ 小时统计完成");
                successCount++;
            } else {
                System.out.println("✗ 小时统计失败");
                failCount++;
            }
        } catch (Exception e) {
            System.err.println("✗ 小时统计异常: " + e.getMessage());
            e.printStackTrace();
            failCount++;
        }
        
        // 5. 聊天室/群组统计
        System.out.println("\n[5/6] 正在运行聊天室/群组统计...");
        try {
            boolean success = GroupStatsAnalyzer.runJob(HDFS_INPUT_PATH, HDFS_OUTPUT_BASE + "/group");
            if (success) {
                System.out.println("✓ 聊天室/群组统计完成");
                successCount++;
            } else {
                System.out.println("✗ 聊天室/群组统计失败");
                failCount++;
            }
        } catch (Exception e) {
            System.err.println("✗ 聊天室/群组统计异常: " + e.getMessage());
            e.printStackTrace();
            failCount++;
        }
        
        // 6. 消息长度统计
        System.out.println("\n[6/6] 正在运行消息长度统计...");
        try {
            boolean success = LengthStatsAnalyzer.runJob(HDFS_INPUT_PATH, HDFS_OUTPUT_BASE + "/length");
            if (success) {
                System.out.println("✓ 消息长度统计完成");
                successCount++;
            } else {
                System.out.println("✗ 消息长度统计失败");
                failCount++;
            }
        } catch (Exception e) {
            System.err.println("✗ 消息长度统计异常: " + e.getMessage());
            e.printStackTrace();
            failCount++;
        }
        
        System.out.println("\n========================================");
        System.out.println("MapReduce统计分析完成！");
        System.out.println("成功: " + successCount + " 个");
        System.out.println("失败: " + failCount + " 个");
        System.out.println("========================================");
    }
}

