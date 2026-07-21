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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 小时统计MapReduce作业
 * 统计每个小时（0-23）的消息数量
 */
public class HourStatsAnalyzer {
    
    public static class HourMapper extends Mapper<LongWritable, Text, Text, LongWritable> {
        private final static LongWritable one = new LongWritable(1);
        private Text hourKey = new Text();
        
        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String line = value.toString().trim();
            if (line.isEmpty() || line.startsWith("user_id")) {
                return;
            }
            
            try {
                String[] fields = parseCSVLine(line);
                if (fields.length < 4) {
                    return;
                }
                
                // 获取send_time字段（索引3）
                String sendTime = fields[3].trim();
                if (sendTime.isEmpty()) {
                    return;
                }
                
                // 提取小时
                Integer hour = extractHour(sendTime);
                if (hour != null && hour >= 0 && hour <= 23) {
                    hourKey.set(String.valueOf(hour));
                    context.write(hourKey, one);
                }
                
            } catch (Exception e) {
                // 跳过格式错误的数据
            }
        }
        
        private Integer extractHour(String dateTime) {
            try {
                // 尝试多种日期格式
                String[] formats = {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy/MM/dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm",
                    "yyyy/MM/dd HH:mm"
                };
                
                for (String format : formats) {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                        String normalized = dateTime.replace("/", "-");
                        LocalDateTime dt = LocalDateTime.parse(normalized, formatter);
                        return dt.getHour();
                    } catch (DateTimeParseException e) {
                        continue;
                    }
                }
                
                // 如果都失败，尝试简单提取
                // 格式通常是 yyyy-MM-dd HH:mm:ss 或 yyyy/MM/dd HH:mm:ss
                String normalized = dateTime.replace("/", "-");
                if (normalized.length() >= 13) {
                    try {
                        String hourStr = normalized.substring(11, 13);
                        return Integer.parseInt(hourStr);
                    } catch (Exception e) {
                        return null;
                    }
                }
            } catch (Exception e) {
                return null;
            }
            return null;
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
    }
    
    public static class HourReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
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
    
    public static boolean runJob(String inputPath, String outputPath) throws Exception {
        System.setProperty("HADOOP_USER_NAME", "root");
        
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://hadoop:9000");
        
        FileUtil.checkFileIsExists(conf, outputPath);
        
        Job job = Job.getInstance(conf, "HourStatsAnalyzer");
        job.setJarByClass(HourStatsAnalyzer.class);
        job.setMapperClass(HourMapper.class);
        job.setCombinerClass(HourReducer.class);
        job.setReducerClass(HourReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        
        return job.waitForCompletion(true);
    }
    
    public static void main(String[] args) throws Exception {
        String inputPath = args.length >= 1 ? args[0] : "/data/chat";
        String outputPath = args.length >= 2 ? args[1] : "/data/chat_analysis/stats/hour";
        boolean success = runJob(inputPath, outputPath);
        System.exit(success ? 0 : 1);
    }
}

