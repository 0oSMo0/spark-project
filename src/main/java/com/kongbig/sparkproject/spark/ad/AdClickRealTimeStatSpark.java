package com.kongbig.sparkproject.spark.ad;

import com.google.common.base.Optional;
import com.kongbig.sparkproject.conf.ConfigurationManager;
import com.kongbig.sparkproject.constant.Constants;
import com.kongbig.sparkproject.dao.IAdBlackListDAO;
import com.kongbig.sparkproject.dao.IAdUserClickCountDAO;
import com.kongbig.sparkproject.dao.impl.DAOFactory;
import com.kongbig.sparkproject.domain.AdBlackList;
import com.kongbig.sparkproject.domain.AdUserClickCount;
import com.kongbig.sparkproject.util.DateUtils;
import kafka.serializer.StringDecoder;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
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

        // 根据动态黑名单进行数据过滤
        JavaPairDStream<String, String> filteredAdRealTimeLogDStream =
                filterByBlacklist(adRealTimeLogDStream);

        // 生成动态黑名单
        generateDynamicBlackList(filteredAdRealTimeLogDStream);
        
        // 构建完Spark Streaming上下文之后，记得要进行上下文的启动，等待执行结束、关闭
        jssc.start();
        jssc.awaitTermination();
        jssc.close();
    }

    /**
     * 根据动态黑名单进行数据过滤
     *
     * @param adRealTimeLogDStream adRealTimeLogDStream
     * @return filteredAdRealTimeLogDStream
     */
    private static JavaPairDStream<String, String> filterByBlacklist(
            JavaPairInputDStream<String, String> adRealTimeLogDStream) {
         /*
         * 刚接收到原始的用户点击行为日志之后
         * 根据MySQL中的动态黑名单，进行实时的黑名单过滤（黑名单用户的点击行为，直接过滤掉，不要了）
         * 使用transform算子（将dStream中的每个batch RDD进行处理，转换为任意的其他RDD，功能很强大）
         */
        JavaPairDStream<String, String> filteredAdRealTimeLogDStream = adRealTimeLogDStream.transformToPair(
                new Function<JavaPairRDD<String, String>, JavaPairRDD<String, String>>() {
                    private static final long serialVersionUID = -7394690665006298414L;

                    @Override
                    public JavaPairRDD<String, String> call(
                            JavaPairRDD<String, String> rdd) throws Exception {
                        // 首先，从MySQL中查询所有黑名单用户，将其转换成一个rdd
                        IAdBlackListDAO adBlackListDAO = DAOFactory.getAdBlackListDAO();
                        List<AdBlackList> adBlackLists = adBlackListDAO.findAll();

                        List<Tuple2<Long, Boolean>> tuples = new ArrayList<Tuple2<Long, Boolean>>();

                        for (AdBlackList adBlackList : adBlackLists) {
                            tuples.add(new Tuple2<Long, Boolean>(adBlackList.getUserId(), true));
                        }

                        JavaSparkContext sc = new JavaSparkContext(rdd.context());
                        JavaPairRDD<Long, Boolean> blacklistRDD = sc.parallelizePairs(tuples);

                        // 将原始数据rdd映射成<userId, tuple2<string, string>>
                        JavaPairRDD<Long, Tuple2<String, String>> mappedRDD = rdd.mapToPair(new PairFunction<Tuple2<String, String>, Long, Tuple2<String, String>>() {
                            private static final long serialVersionUID = -474088790752820741L;

                            @Override
                            public Tuple2<Long, Tuple2<String, String>> call(Tuple2<String, String> tuple) throws Exception {
                                String log = tuple._2;
                                String[] logSplited = log.split(" ");
                                // timestamp province city userId adId
                                long userId = Long.valueOf(logSplited[3]);
                                return new Tuple2<Long, Tuple2<String, String>>(userId, tuple);
                            }
                        });

                        /*
                         * 将原始日志数据rdd，与黑名单rdd，进行左外连接
                         * 如果说原始日志的userId，没有在对应的黑名单中，join不到，左外连接
                         * 用inner join，内连接，会导致数据丢失
                         */
                        JavaPairRDD<Long, Tuple2<Tuple2<String, String>, Optional<Boolean>>> joinedRDD =
                                mappedRDD.leftOuterJoin(blacklistRDD);

                        JavaPairRDD<Long, Tuple2<Tuple2<String, String>, Optional<Boolean>>> filteredRDD
                                = joinedRDD.filter(new Function<Tuple2<Long, Tuple2<Tuple2<String, String>,
                                Optional<Boolean>>>, Boolean>() {
                            private static final long serialVersionUID = -5795562402398165436L;

                            @Override
                            public Boolean call(
                                    Tuple2<Long, Tuple2<Tuple2<String, String>,
                                            Optional<Boolean>>> tuple) throws Exception {
                                Optional<Boolean> optional = tuple._2._2;

                                // 如果这个值存在，那么说明原始日志中的userid，join到了某个黑名单用户
                                if (optional.isPresent() && optional.get()) {
                                    return false;
                                }

                                return true;
                            }
                        });

                        JavaPairRDD<String, String> resultRDD = filteredRDD.mapToPair(
                                new PairFunction<Tuple2<Long, Tuple2<Tuple2<String, String>, Optional<Boolean>>>,
                                        String, String>() {
                                    private static final long serialVersionUID = 2065530707629254550L;

                                    @Override
                                    public Tuple2<String, String> call(
                                            Tuple2<Long, Tuple2<Tuple2<String, String>,
                                                    Optional<Boolean>>> tuple) throws Exception {
                                        return tuple._2._1;
                                    }
                                });

                        return resultRDD;
                    }
                });
        return filteredAdRealTimeLogDStream;
    }

    /**
     * 生成动态黑名单
     *
     * @param filteredAdRealTimeLogDStream 根据动态黑名单进行数据过滤后的DStream
     */
    private static void generateDynamicBlackList(JavaPairDStream<String, String> filteredAdRealTimeLogDStream) {
        // 一条一条的实时日志
        // timestamp province city userId adId
        // 某个时间点 某个省份 某个城市 某个用户 某个广告
        
        /*
         * 计算出每个5秒内的数据中，每天每个用户每个广告的点击量
         * 通过对原始实时日志的处理，将日志的格式处理成<yyyyMMdd_userId_adId, 1L>格式
         */
        JavaPairDStream<String, Long> dailyUserAdClickDStream = filteredAdRealTimeLogDStream.mapToPair(
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
         * <yyyyMMdd_userId_adId, clickCount>   每个batch的
         */
        dailyUserAdClickCountDStream.foreachRDD(new Function<JavaPairRDD<String, Long>, Void>() {
            private static final long serialVersionUID = 3693827418676478596L;

            @Override
            public Void call(JavaPairRDD<String, Long> rdd) throws Exception {
                rdd.foreachPartition(new VoidFunction<Iterator<Tuple2<String, Long>>>() {
                    private static final long serialVersionUID = -6985467523670573286L;

                    @Override
                    public void call(Iterator<Tuple2<String, Long>> iterator) throws Exception {
                        /*
                         * 对每个分区的数据就去获取一次连接对象
                         * 每次都是从连接池中获取，而不是每次都创建
                         * 写数据库操作，性能已经提到最高了
                         */

                        List<AdUserClickCount> adUserClickCounts = new ArrayList<AdUserClickCount>();

                        while (iterator.hasNext()) {
                            Tuple2<String, Long> tuple = iterator.next();

                            String[] keySplited = tuple._1.split("_");
                            // yyyyMMdd字符串转换成Date，Date再转换成yyyy-MM-dd字符串
                            String date = DateUtils.formatDate(DateUtils.parseDateKey(keySplited[0]));
                            long userId = Long.valueOf(keySplited[1]);
                            long adId = Long.valueOf(keySplited[2]);
                            long clickCount = Long.valueOf(keySplited[3]);

                            AdUserClickCount adUserClickCount = new AdUserClickCount(date, userId, adId, clickCount);

                            adUserClickCounts.add(adUserClickCount);
                        }

                        IAdUserClickCountDAO adUserClickCountDAO = DAOFactory.getAdUserClickCountDAO();
                        adUserClickCountDAO.updateBatch(adUserClickCounts);
                    }
                });

                return null;
            }
        });
        
        /*
         * 到这里，MySQL中已经有了累计的每天各用户对各广告的点击量
         * 遍历每个batch中所有记录，对每条记录都要去查询一下，这一天这个用户对这个广告的累计点击量是多少
         * 从MySQL中查询
         * 查询出来的结果，如果是100（某个用户某天对某个广告的点击量已经大于等于100了）
         * 那么就判定这个用户就是黑名单用户，就写入MySQL的表中，持久化！
         * 
         * 对batch中的数据，去查询MySQL中的点击次数，应该使用dailyUserAdClickDStream这个dStream
         * 因为这个batch是聚合过的数据，已经按照yyyyMMdd_userId_adId进行过聚合了
         * 比如原始数据可能是一个batch有一万条，聚合过后可能只有五千条
         * 所以选用这个聚合后的dStream，既满足需求，也可以尽量减少要处理的数据量
         */
        JavaPairDStream<String, Long> blackListDStream = dailyUserAdClickCountDStream.filter(
                new Function<Tuple2<String, Long>, Boolean>() {
                    private static final long serialVersionUID = 3267263868305512059L;

                    @Override
                    public Boolean call(Tuple2<String, Long> tuple) throws Exception {
                        String key = tuple._1;
                        String[] keySplited = key.split("_");
                        // yyyyMMdd -> yyyy-MM-dd
                        String date = DateUtils.formatDate(DateUtils.parseDateKey(keySplited[0]));
                        long userId = Long.valueOf(keySplited[1]);
                        long adId = Long.valueOf(keySplited[2]);

                        // 从MySQL中查询指定日期指定用户对指定广告的点击量
                        IAdUserClickCountDAO adUserClickCountDAO = DAOFactory.getAdUserClickCountDAO();
                        int clickCount = adUserClickCountDAO.findClickCountByMultiKey(date, userId, adId);

                        /*
                         * 判断如果点击量大于等于100，那么就是黑名单用户，拉入黑名单，返回true
                         */
                        if (clickCount >= 100) {
                            return true;
                        }
                        // 反之，点击量小于100的，就暂时不用管。
                        return false;
                    }
                });
        
        /*
         * blackListDStream
         * 里面的每个batch，其实就是都是过滤出来的已经在某天对某个广告点击量超过100的用户
         * 遍历这个dStream中的每个rdd，然后将黑名单用户增加到MySQL中
         * 这里一旦增加以后，在整个这段程序的前面，会加上根据黑名单动态过滤用户的逻辑
         * 这里认为，一旦用户被拉入黑名单之后，以后就不会再出现在这里了
         * 所以直接插入MySQL即可
         * 
         * 问题：
         * blackListDStream中可能有个userId是重复的，如果直接这样插入的话
         * 那么是不是会发生，插入重复的黑名单用户的情况？
         * 所以我们插入前要进行去重
         * yyyyMMdd_userId_adId
         * 20180419_10001_10002 100
         * 20180419_10001_10003 100
         * 10001这个用户id就插入重复了
         */
        // 实际上是要通过dStream执行操作，对其中的rdd中的userId进行全局的去重
        JavaDStream<Long> blackListUserIdDStream = blackListDStream.map(
                new Function<Tuple2<String, Long>, Long>() {
                    private static final long serialVersionUID = 8487952329739153467L;

                    @Override
                    public Long call(Tuple2<String, Long> tuple) throws Exception {
                        String key = tuple._1;
                        String[] keySplited = key.split("_");
                        Long userId = Long.valueOf(keySplited[1]);
                        return userId;
                    }
                });

        JavaDStream<Long> distinctBlackListUserIdDStream = blackListUserIdDStream.transform(
                new Function<JavaRDD<Long>, JavaRDD<Long>>() {
                    private static final long serialVersionUID = -6477389751705519716L;

                    @Override
                    public JavaRDD<Long> call(JavaRDD<Long> rdd) throws Exception {
                        return rdd.distinct();
                    }
                });
        /*
         * 到这一步为止，distinctBlackListUserIdDStream
         * 每一个rdd，之包含了userId，而且还进行了全局的去重，保证每一次过滤出来的黑名单用户都没有重复的
         */

        distinctBlackListUserIdDStream.foreachRDD(new Function<JavaRDD<Long>, Void>() {
            private static final long serialVersionUID = 914644016511817508L;

            @Override
            public Void call(JavaRDD<Long> rdd) throws Exception {
                rdd.foreachPartition(new VoidFunction<Iterator<Long>>() {
                    private static final long serialVersionUID = 5354905994017409833L;

                    @Override
                    public void call(Iterator<Long> iterator) throws Exception {
                        List<AdBlackList> adBlackLists = new ArrayList<AdBlackList>();

                        while (iterator.hasNext()) {
                            long userId = iterator.next();

                            AdBlackList adBlackList = new AdBlackList();
                            adBlackList.setUserId(userId);

                            adBlackLists.add(adBlackList);
                        }

                        // 把每个分区的黑名单插入到MySQL
                        IAdBlackListDAO adBlackListDAO = DAOFactory.getAdBlackListDAO();
                        adBlackListDAO.insertBatch(adBlackLists);

                        /*
                         * 到此为止，我们已经实现了动态黑名单了
                         * 步骤：
                         * 1.计算出每个batch中的每天每个用户对每个广告的点击量，并持久化到MySQL中
                         * 2.依据上述计算出来的数据，查询一下，对应的累计的点击次数，如果超过了100，那么就认定为
                         * 黑名单，然后对黑名单用户进行去重，去重后，将黑名单用户，持久化插入到MySQL中，所以说
                         * MySQL中的ad_blacklist表中的黑名单用户，就是动态地实时地增长的。所以说，MySQL中的
                         * ad_blacklist表，就可以认为是一张动态黑名单表。
                         * 
                         * 3.基于上述计算出来的动态黑名单，在最一开始，就对每个batch中的点击行为
                         * 根据动态黑名单进行过滤
                         * 把黑名单中的用户的点击行为，直接过滤掉
                         * 那动态黑名单机制就完成了
                         */
                    }
                });
                return null;
            }
        });
    }

}
