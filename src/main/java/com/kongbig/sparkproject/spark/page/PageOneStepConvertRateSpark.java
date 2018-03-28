package com.kongbig.sparkproject.spark.page;

import com.alibaba.fastjson.JSONObject;
import com.kongbig.sparkproject.constant.Constants;
import com.kongbig.sparkproject.dao.IPageSplitConvertRateDAO;
import com.kongbig.sparkproject.dao.ITaskDAO;
import com.kongbig.sparkproject.dao.impl.DAOFactory;
import com.kongbig.sparkproject.dao.impl.PageSplitConvertRateDAOImpl;
import com.kongbig.sparkproject.domain.PageSplitConvertRate;
import com.kongbig.sparkproject.domain.Task;
import com.kongbig.sparkproject.util.DateUtils;
import com.kongbig.sparkproject.util.NumberUtils;
import com.kongbig.sparkproject.util.ParamUtils;
import com.kongbig.sparkproject.util.SparkUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import scala.Tuple2;

import java.util.*;

/**
 * Describe: 页面单跳转化率模块spark作业
 * Author:   kongbig
 * Data:     2018/3/26 11:20.
 */
public class PageOneStepConvertRateSpark {

    public static void main(String[] args) {
        // 1、构造Spark上下文
        SparkConf conf = new SparkConf()
                .setAppName(Constants.SPARK_APP_NAME_PAGE);
        SparkUtils.setMaster(conf);

        JavaSparkContext sc = new JavaSparkContext(conf);
        SQLContext sqlContext = SparkUtils.getSQLContext(sc.sc());

        // 2、生成模拟数据
        SparkUtils.mockData(sc, sqlContext);

        // 3、查询任务，获取任务的参数
        long taskId = ParamUtils.getTaskIdFromArgs(args, Constants.SPARK_LOCAL_TASKID_PAGE);
        ITaskDAO taskDAO = DAOFactory.getTaskDAO();
        Task task = taskDAO.findById(taskId);
        JSONObject taskParam = JSONObject.parseObject(task.getTaskParam());

        // 4、查询指定日期范围内的用户访问行为数据
        JavaRDD<Row> actionRDD = SparkUtils.getActionRDDByDateRange(sqlContext, taskParam);

        /**
         * 对用户访问行为数据做一个映射，将其映射为<sessionId, 访问行为>的格式
         * 用户访问页面切片的生成，是要基于每个session的访问数据，来进行生成的
         * 脱离了session，生成的页面访问切片是没有意义的。（必须基于用户session粒度的）
         */
        JavaPairRDD<String, Row> sessionId2actionRDD = getSessionId2actionRDD(actionRDD);

        /**
         * 对<sessionId, 访问行为>RDD，做一次groupByKey操作
         * 因为要拿到每个session对应的访问行为数据，才能去生成切片
         */
        JavaPairRDD<String, Iterable<Row>> sessionId2actionsRDD = sessionId2actionRDD.groupByKey();
        sessionId2actionsRDD = sessionId2actionsRDD.cache();// persist(StorageLevel.MEMORY_ONLY)

        /**
         * 最核心的一步，每个session的单跳页面切片生成，以及页面流的匹配，算法
         * pageSplitRDD <页面切片, 1>
         */
        JavaPairRDD<String, Integer> pageSplitRDD = generateAndMatchPageSplit(
                sc, sessionId2actionsRDD, taskParam);
        // PV：page view页面访问量
        Map<String, Object> pageSplitPvMap = pageSplitRDD.countByKey();
        /**
         * 使用者指定的页面流是3,2,5,8,6
         * 所以拿到的pageSplitPvMap是：<3_2, num>，<2_5, num>，<5_8, num>，<8_6, num>
         */
        long startPagePv = getStartPagePv(taskParam, sessionId2actionsRDD);

        // 计算目标页面流的各个页面切片的转化率
        Map<String, Double> convertRateMap = computePageSplitConvertRate(
                taskParam, pageSplitPvMap, startPagePv);

        // 持久化页面切片转化率
        persistConvertRate(taskId, convertRateMap);
    }

    /**
     * 获取<sessionId, 用户访问行为>格式的数据
     *
     * @param actionRDD 用户访问行为RDD
     * @return <sessionId, 用户访问行为>
     */
    private static JavaPairRDD<String, Row> getSessionId2actionRDD(JavaRDD<Row> actionRDD) {
        return actionRDD.mapToPair(new PairFunction<Row, String, Row>() {
            private static final long serialVersionUID = 1710078086967708164L;

            @Override
            public Tuple2<String, Row> call(Row row) throws Exception {
                String sessionId = row.getString(2);
                return new Tuple2<String, Row>(sessionId, row);
            }
        });
    }

    /**
     * 页面切片生成与匹配算法(生成和匹配页面切片)
     *
     * @param sc                   JavaSparkContext
     * @param sessionId2actionsRDD JavaPairRDD<String,Iterable<Row>>
     * @param taskParam            JSONObject
     * @return
     */
    private static JavaPairRDD<String, Integer> generateAndMatchPageSplit(
            JavaSparkContext sc,
            JavaPairRDD<String, Iterable<Row>> sessionId2actionsRDD,
            JSONObject taskParam) {
        final String targetPageFlow = ParamUtils.getParam(taskParam, Constants.PARAM_TARGET_PAGE_FLOW);
        final Broadcast<String> targetPageFlowBroadcast = sc.broadcast(targetPageFlow);

        return sessionId2actionsRDD.flatMapToPair(
                new PairFlatMapFunction<Tuple2<String, Iterable<Row>>, String, Integer>() {
                    private static final long serialVersionUID = 1700419038424810818L;

                    @Override
                    public Iterable<Tuple2<String, Integer>> call(
                            Tuple2<String, Iterable<Row>> tuple) throws Exception {
                        // 定义返回list
                        List<Tuple2<String, Integer>> list = new ArrayList<Tuple2<String, Integer>>();
                        // 获取到当前session的访问行为的迭代器
                        Iterator<Row> iterator = tuple._2.iterator();
                        /**
                         * 获取使用者指定的页面流
                         * 使用者指定的页面流可能是：1,2,3,4,5,6,7
                         * 1->2的转化率是多少？2->3的转化率是多少？
                         */
                        String[] targetPages = targetPageFlowBroadcast.value().split(",");
                        /**
                         * 这里拿到的session访问行为，默认情况是乱序的。
                         * 比如说，正常情况下，我们希望拿到的数据，是按照时间顺序排序的。
                         * 因此，应对session的访问行为数据按照时间进行排序。
                         */
                        List<Row> rows = new ArrayList<Row>();
                        while (iterator.hasNext()) {
                            rows.add(iterator.next());
                        }
                        Collections.sort(rows, new Comparator<Row>() {
                            @Override
                            public int compare(Row o1, Row o2) {
                                String actionTime1 = o1.getString(4);
                                String actionTime2 = o2.getString(4);
                                Date date1 = DateUtils.parseTime(actionTime1);
                                Date date2 = DateUtils.parseTime(actionTime2);
                                // 升序
                                return (int) (date1.getTime() - date2.getTime());
                            }
                        });

                        /**
                         * 页面切片的生成，以及页面流的匹配（rows已根据时间排过序了）
                         */
                        Long lastPageId = null;
                        for (Row row : rows) {
                            long pageId = row.getLong(3);

                            if (lastPageId == null) {
                                lastPageId = pageId;
                                continue;
                            }

                            // 生成一个页面切片
                            // 3,5,2,1,8,9
                            // lastPageId=3
                            // pageId=5，切片，3_5
                            String pageSplit = lastPageId + "_" + pageId;

                            // 对这个切片判断一下，看是否在用户指定的页面流中
                            for (int i = 1; i < targetPages.length; i++) {
                                // 比如说，用户指定的页面流是3,2,5,8,1
                                // 遍历的时候，从索引1开始，就是从第二个页面开始
                                // 3_2；2_5；5_8；8_1
                                String targetPageSplit = targetPages[i - 1] + "_" + targetPages[i];
                                if (pageSplit.equals(targetPageSplit)) {
                                    list.add(new Tuple2<String, Integer>(pageSplit, 1));
                                    break;// 匹配成功(使用者所指定的存在于用户访问的行为当中)
                                }
                            }
                            // 下次进来lastPageId就不是原来的值了，后移一位
                            lastPageId = pageId;
                        }
                        return list;
                    }
                });
    }

    /**
     * 获取页面流中初始页面的pv
     *
     * @param taskParam
     * @param sessionId2actionsRDD
     * @return
     */
    private static long getStartPagePv(
            JSONObject taskParam,
            JavaPairRDD<String, Iterable<Row>> sessionId2actionsRDD) {
        String targetPageFlow = ParamUtils.getParam(taskParam, Constants.PARAM_TARGET_PAGE_FLOW);
        final long startPageId = Long.valueOf(targetPageFlow.split(",")[0]);

        JavaRDD<Long> startPageRDD = sessionId2actionsRDD.flatMap(
                new FlatMapFunction<Tuple2<String, Iterable<Row>>, Long>() {
                    private static final long serialVersionUID = -2897323360958552067L;

                    @Override
                    public Iterable<Long> call(Tuple2<String, Iterable<Row>> tuple) throws Exception {
                        List<Long> list = new ArrayList<Long>();

                        Iterator<Row> iterator = tuple._2.iterator();
                        while (iterator.hasNext()) {
                            Row row = iterator.next();
                            long pageId = row.getLong(3);
                            if (pageId == startPageId) {
                                list.add(pageId);// 每个session的
                            }
                        }
                        return list;
                    }
                });
        return startPageRDD.count();
    }

    /**
     * 计算页面切片转换率
     *
     * @param pageSplitPvMap 页面切片pv
     * @param startPagePv    起始页面pv
     * @return
     */
    private static Map<String, Double> computePageSplitConvertRate(
            JSONObject taskParam,
            Map<String, Object> pageSplitPvMap,
            long startPagePv) {
        Map<String, Double> convertRateMap = new HashMap<String, Double>();
        String[] targetPages = ParamUtils.getParam(taskParam, Constants.PARAM_TARGET_PAGE_FLOW).split(",");
        long lastPageSplitPv = 0L;

        // 3,5,2,4,6
        // 3_5
        // 3_5 pv / 3 pv    即从3到5的数量除以访问3的数量
        // 5_2 rate = 5_2 pv / 3_5 pv
        // 通过for循环，获取目标页面流中的各个页面切片（pv）
        for (int i = 1; i < targetPages.length; i++) {
            String targetPageSplit = targetPages[i - 1] + "_" + targetPages[i];
            long targetPageSplitPv = Long.valueOf(pageSplitPvMap.get(targetPageSplit) + "");

            double convertRate = 0.0;

            if (i == 1) {
                convertRate = NumberUtils.formatDouble(
                        (double) targetPageSplitPv / (double) startPagePv, 2);
            } else {
                // 目标pv / 上一个pv
                convertRate = NumberUtils.formatDouble(
                        (double) targetPageSplitPv / (double) lastPageSplitPv, 2);
            }
            convertRateMap.put(targetPageSplit, convertRate);

            lastPageSplitPv = targetPageSplitPv;
        }
        return convertRateMap;
    }

    /**
     * 持久化转化率
     *
     * @param convertRateMap
     */
    private static void persistConvertRate(long taskId, Map<String, Double> convertRateMap) {
        StringBuffer buffer = new StringBuffer("");

        for (Map.Entry<String, Double> convertRateEntry : convertRateMap.entrySet()) {
            String pageSplit = convertRateEntry.getKey();
            double convertRate = convertRateEntry.getValue();

            buffer.append(pageSplit + "=" + convertRate + "|");
        }

        String convertRate = buffer.toString();
        convertRate = convertRate.substring(0, convertRate.length() - 1);

        PageSplitConvertRate pageSplitConvertRate = new PageSplitConvertRate(taskId, convertRate);

        IPageSplitConvertRateDAO pageSplitConvertRateDAO = DAOFactory.getPageSplitConvertRateDAO();
        pageSplitConvertRateDAO.insert(pageSplitConvertRate);
    }

}
