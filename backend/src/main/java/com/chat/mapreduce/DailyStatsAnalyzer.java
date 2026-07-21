package com.chat.mapreduce;

import com.chat.util.FileUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 每日统计MapReduce作业
 * 从HDFS读取CSV数据，按日期统计消息数量
 */
public class DailyStatsAnalyzer {
    
    public static class DailyMapper extends Mapper<LongWritable, Text, Text, LongWritable> {
        private final static LongWritable one = new LongWritable(1);
        private Text dateKey = new Text();
        
        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String line = value.toString().trim();
            
            // 跳过空行和表头
            if (line.isEmpty() || line.startsWith("user_id")) {
                return;
            }
            
            try {
                // 解析CSV行：user_id,user_name,message_content,send_time,message_type,chat_room,device_type,message_length
                String[] fields = parseCSVLine(line);
                
                if (fields.length < 4) {
                    return;
                }
                
                // 获取send_time字段（索引3）
                String sendTime = fields[3].trim();
                if (sendTime.isEmpty()) {
                    return;
                }
                
                // 解析日期，提取日期部分（yyyy-MM-dd）
                String dateStr = extractDate(sendTime);
                if (dateStr != null) {
                    dateKey.set(dateStr);
                    context.write(dateKey, one);
                }
                
            } catch (Exception e) {
                // 跳过格式错误的数据
            }
        }
        
        private String[] parseCSVLine(String line) {
            java.util.List<String> fields = new java.util.ArrayList<>();
            StringBuilder currentField = new StringBuilder();
            boolean inQuotes = false;
            
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (c == '"') {
                    inQuotes = !inQuotes;
                } else if (c == ',' && !inQuotes) {
                    fields.add(currentField.toString());
                    currentField = new StringBuilder();
                } else {
                    currentField.append(c);
                }
            }
            fields.add(currentField.toString());
            return fields.toArray(new String[0]);
        }
        
        private String extractDate(String dateTime) {
            try {
                // 尝试多种日期格式
                String[] formats = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy-MM-dd",
                    "yyyy/MM/dd"
                };
                
                for (String format : formats) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                        if (format.contains("HH:mm")) {
                            LocalDate date = LocalDate.parse(dateTime.substring(0, 10).replace("/", "-"), 
                                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            return date.toString();
                        } else {
                            LocalDate date = LocalDate.parse(dateTime.replace("/", "-"), 
                                DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            return date.toString();
                        }
                    } catch (DateTimeParseException e) {
                        continue;
                    }
                }
                
                // 如果都失败，尝试简单提取前10个字符（yyyy-MM-dd格式）
                if (dateTime.length() >= 10) {
                    String datePart = dateTime.substring(0, 10).replace("/", "-");
                    try {
                        LocalDate.parse(datePart);
                        return datePart;
                    } catch (Exception e) {
                        return null;
                    }
                }
            } catch (Exception e) {
                return null;
            }
            return null;
        }
    }
    
    public static class DailyReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
        private LongWritable result = new LongWritable();
        
        @Override
        protected void reduce(Text key, Iterable<LongWritable> values, Context context) 
                throws IOException, InterruptedException {
            
            long sum = 0;
            for (LongWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }
    
    /**
     * 运行MapReduce作业
     * @param inputPath 输入路径
     * @param outputPath 输出路径
     */
    public static boolean runJob(String inputPath, String outputPath) throws Exception {
        System.setProperty("HADOOP_USER_NAME", "root");
        
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://hadoop:9000");
        
        // 检查并删除输出路径（如果存在）
        FileUtil.checkFileIsExists(conf, outputPath);
        
        Job job = Job.getInstance(conf, "DailyStatsAnalyzer");
        job.setJarByClass(DailyStatsAnalyzer.class);
        job.setMapperClass(DailyMapper.class);
        job.setCombinerClass(DailyReducer.class);
        job.setReducerClass(DailyReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        
        return job.waitForCompletion(true);
    }
    
    /**
     * 主方法，支持命令行运行
     * 用法: java -cp ... DailyStatsAnalyzer [inputPath] [outputPath]
     */
    public static void main(String[] args) throws Exception {
        String inputPath = args.length >= 1 ? args[0] : "/data/chat";
        String outputPath = args.length >= 2 ? args[1] : "/data/chat_analysis/stats/daily";
        
        System.out.println("输入路径: " + inputPath);
        System.out.println("输出路径: " + outputPath);
        
        boolean success = runJob(inputPath, outputPath);
        System.exit(success ? 0 : 1);
    }
}

