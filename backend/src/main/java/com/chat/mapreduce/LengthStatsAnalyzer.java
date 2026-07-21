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

/**
 * 消息长度统计MapReduce作业
 * 按消息长度区间统计消息数量
 */
public class LengthStatsAnalyzer {
    
    public static class LengthMapper extends Mapper<LongWritable, Text, Text, LongWritable> {
        private final static LongWritable one = new LongWritable(1);
        private Text lengthGroupKey = new Text();
        
        @Override
        protected void map(LongWritable key, Text value, Context context) 
                throws IOException, InterruptedException {
            
            String line = value.toString().trim();
            if (line.isEmpty() || line.startsWith("user_id")) {
                return;
            }
            
            try {
                String[] fields = parseCSVLine(line);
                if (fields.length < 8) {
                    return;
                }
                
                // 获取message_length字段（索引7）
                String lengthStr = fields[7].trim();
                if (lengthStr.isEmpty()) {
                    return;
                }
                
                try {
                    int length = Integer.parseInt(lengthStr);
                    String lengthGroup = getLengthGroup(length);
                    lengthGroupKey.set(lengthGroup);
                    context.write(lengthGroupKey, one);
                } catch (NumberFormatException e) {
                    // 跳过无效的长度值
                }
                
            } catch (Exception e) {
                // 跳过格式错误的数据
            }
        }
        
        private String getLengthGroup(int length) {
            if (length < 10) return "0-9";
            else if (length < 20) return "10-19";
            else if (length < 50) return "20-49";
            else if (length < 100) return "50-99";
            else return "100+";
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
    
    public static class LengthReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
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
        
        Job job = Job.getInstance(conf, "LengthStatsAnalyzer");
        job.setJarByClass(LengthStatsAnalyzer.class);
        job.setMapperClass(LengthMapper.class);
        job.setCombinerClass(LengthReducer.class);
        job.setReducerClass(LengthReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(inputPath));
        FileOutputFormat.setOutputPath(job, new Path(outputPath));
        
        return job.waitForCompletion(true);
    }
    
    public static void main(String[] args) throws Exception {
        String inputPath = args.length >= 1 ? args[0] : "/data/chat";
        String outputPath = args.length >= 2 ? args[1] : "/data/chat_analysis/stats/length";
        boolean success = runJob(inputPath, outputPath);
        System.exit(success ? 0 : 1);
    }
}

