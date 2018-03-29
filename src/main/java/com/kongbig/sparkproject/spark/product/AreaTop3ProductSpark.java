package com.kongbig.sparkproject.spark.product;

import com.alibaba.fastjson.JSONObject;
import com.kongbig.sparkproject.conf.ConfigurationManager;
import com.kongbig.sparkproject.constant.Constants;
import com.kongbig.sparkproject.dao.ITaskDAO;
import com.kongbig.sparkproject.dao.impl.DAOFactory;
import com.kongbig.sparkproject.domain.Task;
import com.kongbig.sparkproject.util.ParamUtils;
import com.kongbig.sparkproject.util.SparkUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // 查询用户指定日期范围内的点击行为数据（city_id，在哪个城市发生的点击行为）
        // 技术点1：Hive数据源的使用
        JavaPairRDD<Long, Row> cityId2ClickActionRDD = getCityId2ClickActionRDDByDate(
                sqlContext, startDate, endDate);

        // 从MySQL中查询城市信息
        // 技术点2：异构数据源之MySQL的使用
        JavaPairRDD<Long, Row> cityId2CityInfoRDD = getCityId2CityInfoRDD(sqlContext);

        // 生成点击商品基础信息临时表
        // 技术点3：将RDD转换为DataFrame，并注册成临时表
        generateTempClickProductBasicTable(sqlContext, cityId2ClickActionRDD, cityId2CityInfoRDD);

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
    private static JavaPairRDD<Long, Row> getCityId2ClickActionRDDByDate(
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

        JavaRDD<Row> clickActionRDD = clickActionDF.javaRDD();

        JavaPairRDD<Long, Row> cityId2clickActionRDD = clickActionRDD.mapToPair(
                new PairFunction<Row, Long, Row>() {
                    private static final long serialVersionUID = 6774903296601608485L;

                    @Override
                    public Tuple2<Long, Row> call(Row row) throws Exception {
                        Long cityId = row.getLong(0);
                        return new Tuple2<Long, Row>(cityId, row);
                    }
                });
        return cityId2clickActionRDD;
    }

    /**
     * 使用Spark SQL从MySQL中查询城市信息
     *
     * @param sqlContext SQLContext
     * @return CityInfoRDD
     */
    private static JavaPairRDD<Long, Row> getCityId2CityInfoRDD(SQLContext sqlContext) {
        // 构建MySQL连接配置信息(直接从配置文件中获取)
        String url = null;
        Boolean local = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        if (local) {
            url = ConfigurationManager.getProperty(Constants.JDBC_URL);
        } else {
            url = ConfigurationManager.getProperty(Constants.JDBC_USER_PROD);
        }
        Map<String, String> options = new HashMap<String, String>();
        options.put("url", url);
        options.put("dbtable", "city_info");

        // 通过SQLContext去从MySQL中查询数据
        DataFrame cityInfoDF = sqlContext.read().format("jdbc").options(options).load();

        JavaRDD<Row> cityInfoRDD = cityInfoDF.javaRDD();
        JavaPairRDD<Long, Row> cityId2cityInfoRDD = cityInfoRDD.mapToPair(
                new PairFunction<Row, Long, Row>() {
                    private static final long serialVersionUID = 2552393766313917627L;

                    @Override
                    public Tuple2<Long, Row> call(Row row) throws Exception {
                        long cityId = row.getLong(0);
                        return new Tuple2<Long, Row>(cityId, row);
                    }
                });
        return cityId2cityInfoRDD;
    }

    /**
     * 生成点击商品基础信息临时表
     *
     * @param sqlContext            SQLContext
     * @param cityId2clickActionRDD 点击行为数据
     * @param cityId2cityInfoRDD    城市数据
     */
    private static void generateTempClickProductBasicTable(
            SQLContext sqlContext,
            JavaPairRDD<Long, Row> cityId2clickActionRDD,
            JavaPairRDD<Long, Row> cityId2cityInfoRDD) {
        // 执行join操作，进行点击行为数据和城市数据的关联
        JavaPairRDD<Long, Tuple2<Row, Row>> joinedRdd =
                cityId2clickActionRDD.join(cityId2cityInfoRDD);

        // 将上面的JavaPairRDD转换成JavaRDD<Row>（才能将RDD转换为DataFrame）
        JavaRDD<Row> mappedRDD = joinedRdd.map(new Function<Tuple2<Long, Tuple2<Row, Row>>, Row>() {
            private static final long serialVersionUID = -200524099028943510L;

            @Override
            public Row call(Tuple2<Long, Tuple2<Row, Row>> tuple) throws Exception {
                long cityId = tuple._1;
                Row clickAction = tuple._2._1;
                Row cityInfo = tuple._2._2;

                long productId = clickAction.getLong(1);
                String cityName = cityInfo.getString(1);
                String area = cityInfo.getString(2);

                return RowFactory.create(cityId, cityName, area, productId);
            }
        });

        // 基于JavaRDD<Row>的格式，就可以将其转换为DataFrame
        List<StructField> structFields = new ArrayList<StructField>();
        structFields.add(DataTypes.createStructField("city_id", DataTypes.LongType, true));
        structFields.add(DataTypes.createStructField("city_name", DataTypes.StringType, true));
        structFields.add(DataTypes.createStructField("area", DataTypes.StringType, true));
        structFields.add(DataTypes.createStructField("product_id", DataTypes.LongType, true));

        StructType schema = DataTypes.createStructType(structFields);
        DataFrame df = sqlContext.createDataFrame(mappedRDD, schema);

        // 将DataFrame中的数据，注册成临时表(tmp_clk_prod_base)
        df.registerTempTable("tmp_clk_prod_base");
    }

}
