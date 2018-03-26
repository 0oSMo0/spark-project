package com.kongbig.sparkproject.util;

import com.alibaba.fastjson.JSONObject;
import com.kongbig.sparkproject.conf.ConfigurationManager;
import com.kongbig.sparkproject.constant.Constants;
import com.kongbig.sparkproject.spark.MockData;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.hive.HiveContext;

/**
 * Describe: Spark工具类
 * Author:   kongbig
 * Data:     2018/3/26 11:26.
 */
public class SparkUtils {
    
    /**
     * 根据当前是否为本地测试的配置
     * 决定，如何设置SparkConf的master
     *
     * @param conf SparkConf
     */
    public static void setMaster(SparkConf conf) {
        Boolean local = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        if (local) {
            // 本地模式
            conf.setMaster("local");
        }
    }

    /**
     * 生成模拟数据
     * 当spark.local配置设置为true，则生成模拟数据，否则不生成
     *
     * @param sc         JavaSparkContext
     * @param sqlContext SQLContext
     */
    public static void mockData(JavaSparkContext sc, SQLContext sqlContext) {
        Boolean local = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        if (local) {
            MockData.mock(sc, sqlContext);
        }
    }

    /**
     * 获取SQLContext
     * 如果是在本地测试环境(spark.local=true)的话，那么就生成SQLContext对象
     * 如果是在生产环境运行的话，那么就生成HiveContext对象
     *
     * @param sc SparkContext
     * @return SQLContext
     */
    public static SQLContext getSQLContext(SparkContext sc) {
        Boolean local = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        if (local) {
            return new SQLContext(sc);
        } else {
            // HiveContext是SQLContext的子类
            return new HiveContext(sc);
        }
    }

    /**
     * 获取指定日期范围内的用户访问行为数据
     * @param sqlContext SQLContext
     * @param taskParam 务参数
     * @return 行为数据RDD
     */
    public static JavaRDD<Row> getActionRDDByDateRange(SQLContext sqlContext, JSONObject taskParam) {
        String startDate = ParamUtils.getParam(taskParam, Constants.PARAM_START_DATE);
        String endDate = ParamUtils.getParam(taskParam, Constants.PARAM_END_DATE);
        String sql = "select * from user_visit_action " +
                "where date >= '" + startDate + "' " +
                "and date <= '" + endDate + "'";
        DataFrame actionDF = sqlContext.sql(sql);
        /**
         * 解决SparkSQL无法设置并行度和task数量的方法：
         * 使用repartition算子进行重分区。
         * 比如说：SparkSQL默认就给第一个stage设置了20个task，但是根据你的数据量以及算法的复杂度
         * 实际上，你需要1000个task去并行执行
         * 所以说，在这里，可以对SparkSQL查询出来的RDD执行repartition重分区操作
         */
//        actionDF.javaRDD().repartition(1000);
        return actionDF.javaRDD();
    }
    
}
