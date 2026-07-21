package com.chat.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class HiveService {
    
    @Value("${hive.jdbc.url:jdbc:hive2://hadoop:10000/ods}")
    private String hiveJdbcUrl;
    
    @Value("${hive.jdbc.user:root}")
    private String hiveUser;
    
    @Value("${hive.jdbc.password:}")
    private String hivePassword;
    
    /**
     * 获取Hive连接
     */
    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            return DriverManager.getConnection(hiveJdbcUrl, hiveUser, hivePassword);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Hive驱动未找到", e);
        }
    }
    
    /**
     * 创建数据库（如果不存在）
     */
    public Map<String, Object> createDatabaseIfNotExists() {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        Statement stmt = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            
            // 创建数据库
            String createDbSql = "CREATE DATABASE IF NOT EXISTS ods";
            stmt.execute(createDbSql);
            
            // 使用数据库
            stmt.execute("USE ods");
            
            result.put("success", true);
            result.put("message", "数据库创建/检查成功");
            
        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "创建数据库失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(conn, stmt, null);
        }
        
        return result;
    }
    
    /**
     * 创建表（如果不存在）
     */
    public Map<String, Object> createTableIfNotExists() {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        Statement stmt = null;
        
        try {
            // 先创建数据库
            createDatabaseIfNotExists();
            
            conn = getConnection();
            stmt = conn.createStatement();
            
            // 使用数据库
            stmt.execute("USE ods");
            
            // 创建表的SQL
            String createTableSql = "CREATE TABLE IF NOT EXISTS ods_chat_analysis_info (" +
                    "user_id STRING, " +
                    "user_name STRING, " +
                    "message_content STRING, " +
                    "send_time STRING, " +
                    "message_type STRING, " +
                    "chat_room STRING, " +
                    "device_type STRING, " +
                    "message_length STRING" +
                    ") " +
                    "ROW FORMAT DELIMITED " +
                    "FIELDS TERMINATED BY '\\t' " +
                    "STORED AS TEXTFILE " +
                    "LOCATION '/user/hive/warehouse/ods.db/ods_chat_analysis_info'";
            
            stmt.execute(createTableSql);
            
            result.put("success", true);
            result.put("message", "表创建/检查成功");
            
        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "创建表失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(conn, stmt, null);
        }
        
        return result;
    }
    
    /**
     * 加载数据到Hive表
     */
    public Map<String, Object> loadDataToTable(String hdfsDataPath) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        Statement stmt = null;
        
        try {
            // 确保表和数据库存在
            createTableIfNotExists();
            
            conn = getConnection();
            stmt = conn.createStatement();
            
            stmt.execute("USE ods");
            
            String loadDataSql = "LOAD DATA INPATH '" + hdfsDataPath + "' " +
                    "OVERWRITE INTO TABLE ods_chat_analysis_info";
            
            stmt.execute(loadDataSql);
            
            // 查询数据条数
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ods_chat_analysis_info");
            long count = 0;
            if (rs.next()) {
                count = rs.getLong(1);
            }
            rs.close();
            
            result.put("success", true);
            result.put("message", "数据加载成功");
            result.put("recordCount", count);
            
        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "加载数据失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(conn, stmt, null);
        }
        
        return result;
    }
    
    /**
     * 创建外部表（基于CSV文件，路径为/data/chat）
     * 用于直接读取HDFS上的CSV文件
     */
    public Map<String, Object> createExternalTableIfNotExists() {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        Statement stmt = null;
        
        try {
            // 先创建数据库
            createDatabaseIfNotExists();
            
            conn = getConnection();
            stmt = conn.createStatement();
            
            // 使用数据库
            stmt.execute("USE ods");
            
            // 创建外部表的SQL（基于CSV格式，逗号分隔）
            String createExternalTableSql = "CREATE EXTERNAL TABLE IF NOT EXISTS ods.chat_data_external (" +
                    "user_id STRING COMMENT '用户ID', " +
                    "user_name STRING COMMENT '用户名', " +
                    "message_content STRING COMMENT '消息内容', " +
                    "send_time STRING COMMENT '发送时间', " +
                    "message_type STRING COMMENT '消息类型：text/image/voice', " +
                    "chat_room STRING COMMENT '聊天室/群组', " +
                    "device_type STRING COMMENT '设备类型：Android/iOS/Windows/Mac/Web', " +
                    "message_length INT COMMENT '消息长度'" +
                    ") " +
                    "COMMENT '聊天记录数据外部表（基于CSV文件）' " +
                    "ROW FORMAT DELIMITED " +
                    "FIELDS TERMINATED BY ',' " +
                    "LINES TERMINATED BY '\\n' " +
                    "STORED AS TEXTFILE " +
                    "LOCATION '/data/chat' " +
                    "TBLPROPERTIES ('skip.header.line.count'='1', 'serialization.null.format'='')";
            
            stmt.execute(createExternalTableSql);
            
            result.put("success", true);
            result.put("message", "外部表创建/检查成功");
            
        } catch (SQLException e) {
            result.put("success", false);
            result.put("message", "创建外部表失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeResources(conn, stmt, null);
        }
        
        return result;
    }
    
    /**
     * 查询外部表数据条数
     */
    public long getExternalTableRecordCount() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            stmt.execute("USE ods");
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM ods.chat_data_external");
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return 0;
    }
    
    /**
     * 查询表数据条数
     */
    public long getTableRecordCount() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            stmt.execute("USE ods");
            
            rs = stmt.executeQuery("SELECT COUNT(*) FROM ods_chat_analysis_info");
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return 0;
    }
    
    /**
     * 关闭资源
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

