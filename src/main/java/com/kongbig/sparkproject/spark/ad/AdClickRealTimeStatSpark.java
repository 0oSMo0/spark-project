package com.kongbig.sparkproject.spark.ad;

import com.kongbig.sparkproject.conf.ConfigurationManager;
import com.kongbig.sparkproject.constant.Constants;
import com.kongbig.sparkproject.util.DateUtils;
import kafka.serializer.StringDecoder;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import scala.Tuple2;

import java.util.*;

/**
 * Describe: 广告点击流量实时统计spark作业
 * Author:   kongbig
 * Data:     2018/4/3 15:21.
 */
public class AdClickRealTimeStatSpark {

    public static void main(String[] args) {
        // 构建Spark Streaming上下文
        SparkConf conf = new SparkConf()
                .setMaster("local[2]")
                .setAppName("AdClickRealTimeStatSpark");

        /*
         * Spark Streaming的上下文是构建JavaStreamingContext对象
         * 而不是像之前的JavaSparkContext、SQLContext/HiveContext
         * 传入的第一个参数，和之前的spark上下文一样，也是SparkConf对象；第二个参数则不太一样
         * 
         * 第二个参数是Spark Streaming类型作业比较有特色的一个参数，
         * 它是实时处理batch的interval
         * Spark Streaming，每隔一段时间，回去收集一次数据源(kafka)中的数据，做成一个batch
         * 
         * 通常来说，batch interval，就是指 每隔多少时间收集一次数据源中的数据，然后进行处理
         * 一般Spark Streaming的应用，都是设置数秒到数十秒(很少会超过1分钟)
         * 
         * 本项目中设置5秒钟的batch interval
         * 每隔5秒钟，Spark Streaming作业就会收集最近5秒钟内的数据源接收过来的数据
         */
        JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(5));

        // 正式开始进行代码的编写，实现需求中的实时计算的业务逻辑和功能

        /*
         * 创建针对Kafka数据来源的输入DStream(离散流，代表了一个源源不断的数据来源，抽象)
         * 选用Kafka direct api(很多好处，包括自己内部自适应调整每次接收数据量的特性，等等)
         */
        // 构建kafka参数map
        // 主要放置的是，要链接的kafka集群的地址(broker集群的地址列表)
        Map<String, String> kafkaParams = new HashMap<String, String>();
        kafkaParams.put(Constants.KAFKA_METADATA_BROKER_LIST,
                ConfigurationManager.getProperty(Constants.KAFKA_METADATA_BROKER_LIST));

        // 构建topic参数set
        String kafkaTopics = ConfigurationManager.getProperty(Constants.KAFKA_TOPICS);
        String[] kafkaTopicsSplited = kafkaTopics.split(",");
        Set<String> topics = new HashSet<String>();
        for (String kafkaTopic : kafkaTopicsSplited) {
            topics.add(kafkaTopic);
        }

        /*
         * 基于kafka direct api模式，构建出了针对kafka集群中指定topic的输入DStream
         * 两个值，val1，val2，val1没有什么特殊的意义；val2中包含了kafka topic中的一条一条的实时日志数据
         */
        JavaPairInputDStream<String, String> adRealTimeLogDStream = KafkaUtils.createDirectStream(
                jssc,// JavaStreamingContext
                String.class,// keyClass
                String.class,// valueClass
                StringDecoder.class,// keyDecoderClass
                StringDecoder.class,// valueDecoderClass
                kafkaParams,// kafkaParams
                topics);// topics

        // 一条一条的实时日志
        // timestamp province city userId adId
        // 某个时间点 某个省份 某个城市 某个用户 某个广告

        /*
         * 计算出每个5秒内的数据中，每天每个用户每个广告的点击量
         * 通过对原始实时日志的处理，将日志的格式处理成<yyyyMMdd_userId_adId, 1L>格式
         */
        JavaPairDStream<String, Long> dailyUserAdClickDStream = adRealTimeLogDStream.mapToPair(
                new PairFunction<Tuple2<String, String>, String, Long>() {
                    private static final long serialVersionUID = -8108532247433558834L;

                    @Override
                    public Tuple2<String, Long> call(Tuple2<String, String> tuple) throws Exception {
                        // 从tuple中获取到每一条原始的实时日志
                        String log = tuple._2;
                        String[] logSplited = log.split(" ");

                        // 提取出日期(yyyyMMdd)、userId、adId
                        String timeStamp = logSplited[0];
                        Date date = new Date(Long.valueOf(timeStamp));
                        String dateKey = DateUtils.formatDateKey(date);

                        long userId = Long.valueOf(logSplited[3]);
                        long adId = Long.valueOf(logSplited[4]);

                        // 拼接key
                        String key = dateKey + "_" + userId + "_" + adId;
                        return new Tuple2<String, Long>(key, 1L);
                    }
                });

        /*
         * 针对处理后的日志格式，执行reduceByKey算子即可
         * (只是每个batch中)每天每个用户对每个广告的点击量
         */
        JavaPairDStream<String, Long> dailyUserAdClickCountDStream = dailyUserAdClickDStream.reduceByKey(
                new Function2<Long, Long, Long>() {
                    private static final long serialVersionUID = -2985743887223910630L;

                    @Override
                    public Long call(Long v1, Long v2) throws Exception {
                        return v1 + v2;
                    }
                });

        /*
         * 到这里为止获取到的数据？
         * dailyUserAdClickCountDStream DStream
         * 源源不断的，每个5s的batch中，当天每个用户对每支广告的点击次数
         * <yyyyMMdd_userId_adId, clickCount>
         */

        // 构建完Spark Streaming上下文之后，记得要进行上下文的启动，等待执行结束、关闭
        jssc.start();
        jssc.awaitTermination();
        jssc.close();
    }

}
