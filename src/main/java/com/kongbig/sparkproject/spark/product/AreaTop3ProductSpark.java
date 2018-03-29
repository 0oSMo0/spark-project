package com.kongbig.sparkproject.spark.product;

import com.alibaba.fastjson.JSONObject;
import com.kongbig.sparkproject.constant.Constants;
import com.kongbig.sparkproject.dao.ITaskDAO;
import com.kongbig.sparkproject.dao.impl.DAOFactory;
import com.kongbig.sparkproject.domain.Task;
import com.kongbig.sparkproject.util.ParamUtils;
import com.kongbig.sparkproject.util.SparkUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;

/**
 * Describe: 各区域top3热门商品统计Spark作业
 * Author:   kongbig
 * Data:     2018/3/29 10:15.
 */
public class AreaTop3ProductSpark {

    public static void main(String[] args) {
        // 创建SparkConf
        SparkConf conf = new SparkConf()
                .setAppName("AreaTop3ProductSpark");
        SparkUtils.setMaster(conf);

        // 构建Spark上下文
        JavaSparkContext sc = new JavaSparkContext();
        SQLContext sqlContext = SparkUtils.getSQLContext(sc.sc());

        // 准备模拟数据
        SparkUtils.mockData(sc, sqlContext);

        // 获取命令行传入的taskId，查询对应的任务参数
        ITaskDAO taskDAO = DAOFactory.getTaskDAO();
        long taskId = ParamUtils.getTaskIdFromArgs(args, Constants.SPARK_LOCAL_TASKID_PRODUCT);
        Task task = taskDAO.findById(taskId);

        JSONObject taskParam = JSONObject.parseObject(task.getTaskParam());
        String startDate = ParamUtils.getParam(taskParam, Constants.PARAM_START_DATE);
        String endDate = ParamUtils.getParam(taskParam, Constants.PARAM_END_DATE);

        // 查询用户指定日期范围内的点击行为数据
        JavaRDD<Row> clickActionRDD = getClickActionRDDByDate(sqlContext, startDate, endDate);
        

        sc.stop();
    }

    /**
     * 查询指定日期范围内的点击行为数据
     *
     * @param sqlContext SQLContext
     * @param startDate  起始日期
     * @param endDate    截至日期
     * @return 点击行为数据
     */
    private static JavaRDD<Row> getClickActionRDDByDate(
            SQLContext sqlContext, String startDate, String endDate) {
        /**
         * 从user_visit_action中查询用户访问行为数据。
         * 限定1：click_product_id，限定为不为空的访问行为，那么就代表这点击行为。
         * 限定2：在用户指定的日期范围内的数据
         */
        String sql = "SELECT city_id, click_product_id product_id "
                + "FROM user_visit_action "
                + "WHERE click_product_id IS NOT NULL "
                + "AND click_product_id != 'NULL' "
                + "AND click_product_id != 'null' "
                + "AND action_time >= '" + startDate + "' "
                + "AND action_time <= '" + endDate + "'";

        DataFrame clickActionDF = sqlContext.sql(sql);
        return clickActionDF.javaRDD();
    }

}
