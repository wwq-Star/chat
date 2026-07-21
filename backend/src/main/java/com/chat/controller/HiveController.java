package com.chat.controller;

import com.chat.dto.DataSummary;
import com.chat.entity.ChatMessage;
import com.chat.service.ChatMessageService;
import com.chat.service.CsvImportService;
import com.chat.service.DataPipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hive")
@CrossOrigin(origins = "*")
public class HiveController {
    
    @Autowired
    private ChatMessageService chatMessageService;
    
    @Autowired
    private CsvImportService csvImportService;
    
    @Autowired
    private DataPipelineService dataPipelineService;
    
    @Autowired
    private com.chat.service.MapReduceStatsService mapReduceStatsService;
    
    // 测试连接
    @GetMapping("/test-connection")
    public Map<String, Object> testConnection() {
        return chatMessageService.testConnection();
    }
    
    // 创建表
    @PostMapping("/create-table")
    public Map<String, Object> createTable() {
        return chatMessageService.createTable();
    }
    
    // 获取数据概览
    @GetMapping("/data-summary")
    public DataSummary getDataSummary() {
        return chatMessageService.getDataSummary();
    }
    
    // 获取每日统计
    @GetMapping("/daily-stats")
    public List<Map<String, Object>> getDailyStats() {
        return chatMessageService.getDailyStats();
    }
    
    // 获取消息类型统计
    @GetMapping("/type-stats")
    public List<Map<String, Object>> getTypeStats() {
        return chatMessageService.getTypeStats();
    }
    
    // 获取聊天室/群组统计
    @GetMapping("/group-stats")
    public List<Map<String, Object>> getGroupStats() {
        return chatMessageService.getGroupStats();
    }
    
    // 获取平台统计
    @GetMapping("/platform-stats")
    public List<Map<String, Object>> getPlatformStats() {
        return chatMessageService.getPlatformStats();
    }
    
    // 获取消息长度统计
    @GetMapping("/length-stats")
    public List<Map<String, Object>> getLengthStats() {
        return chatMessageService.getLengthStats();
    }
    
    // 获取小时统计
    @GetMapping("/hour-stats")
    public List<Map<String, Object>> getHourStats() {
        return chatMessageService.getHourStats();
    }
    
    // 获取原始数据（分页）
    @GetMapping("/raw-data")
    public Map<String, Object> getRawData(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ChatMessage> pageData = chatMessageService.getRawData(page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageData.getContent());
        result.put("totalElements", pageData.getTotalElements());
        result.put("totalPages", pageData.getTotalPages());
        result.put("currentPage", page);
        result.put("pageSize", size);
        return result;
    }

    // 获取原始数据（分页）
    @GetMapping("/all-data")
    public List<ChatMessage> getAllData() {
        List<ChatMessage> pageData = chatMessageService.getAllData();
        return pageData;
    }
    
    // 获取表结构
    @GetMapping("/table-schema")
    public List<Map<String, String>> getTableSchema() {
        return chatMessageService.getTableSchema();
    }
    
    // 获取预览数据
    @GetMapping("/preview")
    public List<ChatMessage> getPreview(@RequestParam(defaultValue = "5") int limit) {
        return chatMessageService.getPreview(limit);
    }
    
    // 导入数据（LOAD DATA方式）
    @PostMapping("/import-data")
    public Map<String, Object> importData(@RequestParam("file") MultipartFile file) {
        return csvImportService.importData(file);
    }
    
    // 简单导入（逐行）
    @PostMapping("/simple-import")
    public Map<String, Object> simpleImport(@RequestParam("file") MultipartFile file) {
        return csvImportService.simpleImport(file);
    }
    
    // 删除单条数据
    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteById(@PathVariable Long id) {
        return chatMessageService.deleteById(id);
    }
    
    // 清空数据
    @DeleteMapping("/clear-data")
    public Map<String, Object> clearData() {
        return chatMessageService.clearData();
    }
    
    // 插入测试数据
    @PostMapping("/insert-test-data")
    public Map<String, Object> insertTestData() {
        return chatMessageService.insertTestData();
    }
    
    // ========== HDFS和Hive相关接口 ==========
    
    // 测试HDFS连接
    @GetMapping("/test-hdfs")
    public Map<String, Object> testHdfs() {
        return dataPipelineService.testHdfsConnection();
    }
    
    // 测试Hive连接
    @GetMapping("/test-hive")
    public Map<String, Object> testHive() {
        return dataPipelineService.testHiveConnection();
    }
    
    // 完整数据处理流程（上传到HDFS -> MapReduce清洗 -> 导入Hive）
    @PostMapping("/process-to-hive")
    public Map<String, Object> processToHive(@RequestParam("file") MultipartFile file) {
        return dataPipelineService.processDataPipeline(file);
    }
    
    // 执行MapReduce分析并写入MySQL
    @PostMapping("/run-analysis")
    public Map<String, Object> runAnalysis() {
        return mapReduceStatsService.runAllStats();
    }
}

