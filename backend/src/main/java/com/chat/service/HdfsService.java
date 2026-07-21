package com.chat.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class HdfsService {
    
    private static final String HDFS_URI = "hdfs://hadoop:9000";
    private static final String HDFS_USER = System.getProperty("hdfs.user", "root");
    
    private Configuration getHadoopConfig() {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", HDFS_URI);
        conf.set("dfs.replication", "1");
        conf.setBoolean("dfs.support.append", true);
        // 设置HDFS用户
        System.setProperty("HADOOP_USER_NAME", HDFS_USER);
        return conf;
    }
    
    /**
     * 确保目录存在，如果不存在则创建
     */
    public boolean ensureDirectoryExists(String hdfsPath) {
        try {
            FileSystem fs = FileSystem.get(getHadoopConfig());
            Path path = new Path(hdfsPath);
            
            if (!fs.exists(path)) {
                boolean created = fs.mkdirs(path);
                fs.close();
                return created;
            }
            fs.close();
            return true;
        } catch (IOException e) {
            System.err.println("创建HDFS目录失败: " + hdfsPath + ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 上传文件到HDFS
     */
    public Map<String, Object> uploadToHdfs(InputStream inputStream, String hdfsPath, String fileName) {
        Map<String, Object> result = new HashMap<>();
        FileSystem fs = null;
        FSDataOutputStream out = null;
        
        try {
            Configuration conf = getHadoopConfig();
            fs = FileSystem.get(conf);
            
            // 确保父目录存在
            Path filePath = new Path(hdfsPath, fileName);
            Path parentPath = filePath.getParent();
            if (!fs.exists(parentPath)) {
                fs.mkdirs(parentPath);
            }
            
            // 如果文件已存在，删除旧文件
            if (fs.exists(filePath)) {
                fs.delete(filePath, false);
            }
            
            // 上传文件
            out = fs.create(filePath, true);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            out.flush();
            out.close();
            
            result.put("success", true);
            result.put("message", "文件上传成功");
            result.put("hdfsPath", filePath.toString());
            result.put("fileSize", totalBytes);
            
        } catch (IOException e) {
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (out != null) out.close();
                if (fs != null) fs.close();
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return result;
    }
    
    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String hdfsPath) {
        try {
            FileSystem fs = FileSystem.get(getHadoopConfig());
            Path path = new Path(hdfsPath);
            boolean exists = fs.exists(path);
            fs.close();
            return exists;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 删除HDFS文件或目录
     */
    public boolean deletePath(String hdfsPath, boolean recursive) {
        try {
            FileSystem fs = FileSystem.get(getHadoopConfig());
            Path path = new Path(hdfsPath);
            boolean deleted = fs.delete(path, recursive);
            fs.close();
            return deleted;
        } catch (IOException e) {
            System.err.println("删除HDFS路径失败: " + hdfsPath + ", 错误: " + e.getMessage());
            return false;
        }
    }
}

