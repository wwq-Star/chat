package com.chat.util;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;

/**
 * HDFS文件操作工具类
 */
public class FileUtil {
    
    /**
     * 检查输出路径是否存在，如果存在则删除
     */
    public static void checkFileIsExists(Configuration conf, String outPath) throws IOException {
        try {
            FileSystem fs = FileSystem.get(conf);
            Path path = new Path(outPath);
            
            if (fs.exists(path)) {
                System.out.println("输出路径已存在，正在删除: " + outPath);
                boolean deleted = fs.delete(path, true);
                if (deleted) {
                    System.out.println("输出路径删除成功: " + outPath);
                } else {
                    System.out.println("警告：输出路径删除失败: " + outPath);
                }
            }
            
            fs.close();
        } catch (Exception e) {
            System.err.println("检查/删除输出路径时出错: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("无法检查/删除输出路径: " + outPath, e);
        }
    }
}

