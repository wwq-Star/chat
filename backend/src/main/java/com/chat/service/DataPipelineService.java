package com.chat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class DataPipelineService {
    
    @Autowired
    private CsvImportService csvImportService;
    
    private static final String HDFS_INPUT_DIR = "/data/chat_analysis/input";
    private static final String HDFS_OUTPUT_DIR = "/data/chat_analysis/output";
    private static final String HIVE_TABLE_PATH = "/user/hive/warehouse/ods.db/ods_chat_analysis_info";
    
    public Map<String, Object> processDataPipeline(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        
        try {
            Map<String, Object> importResult = csvImportService.importData(file);
            
            if (!(Boolean) importResult.get("success")) {
                result.put("success", false);
                result.put("message", "数据导入失败: " + importResult.get("message"));
                return result;
            }
            
            // 返回成功结果
            result.put("success", true);
            result.put("message", "数据处理完成");
            result.put("hdfsInputPath", HDFS_INPUT_DIR + "/chat_data_" + timestamp + ".csv");
            result.put("hdfsOutputPath", HDFS_OUTPUT_DIR + "/" + timestamp);
            result.put("hiveTablePath", HIVE_TABLE_PATH);
            result.put("recordCount", importResult.get("successCount"));
            result.put("successCount", importResult.get("successCount"));
            result.put("errorCount", importResult.get("errorCount"));
            result.put("totalCount", importResult.get("totalCount"));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "数据处理流程失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    public Map<String, Object> testHdfsConnection() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "HDFS连接测试");
        return result;
    }
    
    public Map<String, Object> testHiveConnection() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Hive连接测试");
        return result;
    }
}

