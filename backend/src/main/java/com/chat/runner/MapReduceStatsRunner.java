package com.chat.runner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * MapReduce统计分析运行器
 * 运行所有6个统计分析：每日、类型、群组、平台、长度、小时
 * 
 * 使用方法：
 * 作为独立的Spring Boot应用运行：直接运行此类的main方法
 * 
 * 注意：此类不会在ChatAnalysisApplication启动时自动执行
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.chat")
public class MapReduceStatsRunner {
    
    public static void main(String[] args) {
        SpringApplication.run(MapReduceStatsRunner.class, args);
    }
}

