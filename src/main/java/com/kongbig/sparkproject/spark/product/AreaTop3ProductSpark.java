package com.kongbig.sparkproject.spark.product;

import com.alibaba.fastjson.JSONObject;
import com.kongbig.sparkproject.conf.ConfigurationManager;
import com.kongbig.sparkproject.constant.Constants;
import com.kongbig.sparkproject.dao.IAreaTop3ProductDAO;
import com.kongbig.sparkproject.dao.ITaskDAO;
import com.kongbig.sparkproject.dao.impl.DAOFactory;
import com.kongbig.sparkproject.domain.AreaTop3Product;
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
        JavaSparkContext sc = new JavaSparkContext(conf);
        SQLContext sqlContext = SparkUtils.getSQLContext(sc.sc());

        // 注册自定义函数
        sqlContext.udf().register("concat_long_string",
                new ConcatLongStringUDF(), DataTypes.StringType);
        sqlContext.udf().register("group_concat_distinct", new GroupConcatDistinctUDAF());
        sqlContext.udf().register("get_json_object",
                new GetJsonObjectUDF(), DataTypes.StringType);
        sqlContext.udf().register("random_prefix",
                new RandomPrefixUDF(), DataTypes.StringType);
        sqlContext.udf().register("remove_random_prefix",
                new RemoveRandomPrefixUDF(), DataTypes.StringType);

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

        // 生成各区域各商品点击次数的临时表
        generateTempAreaProductClickCountTable(sqlContext);

        // 生成包含完整商品信息的各区域各商品点击次数的临时表
        generateTempAreaFullProductClickCountTable(sqlContext);

        // 使用开窗函数获取各个区域内点击次数排名前3的热门商品
        JavaRDD<Row> areaTop3ProductRDD = getAreaTop3ProductRDD(sqlContext);

        /*
         * 写入MySQL
         * 总共不到10个区域，每个区域还是top3热门商品，总共最后数据量也就是几十个
         * 所以可以直接将数据collect()到本地，用批量插入的方式，一次性插入mysql即可。
         */
        List<Row> rows = areaTop3ProductRDD.collect();
        persistAreaTop3Product(taskId, rows);

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
        /*
         * 从user_visit_action中查询用户访问行为数据。
         * 限定1：click_product_id，限定为不为空的访问行为，那么就代表这点击行为。
         * 限定2：在用户指定的日期范围内的数据
         */
        String sql = "SELECT city_id, click_product_id product_id "
                + "FROM user_visit_action "
                + "WHERE click_product_id IS NOT NULL "
                + "AND date >= '" + startDate + "' "
                + "AND date <= '" + endDate + "'";

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
        String user = null;
        String password = null;
        Boolean local = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        if (local) {
            url = ConfigurationManager.getProperty(Constants.JDBC_URL);
            user = ConfigurationManager.getProperty(Constants.JDBC_USER);
            password = ConfigurationManager.getProperty(Constants.JDBC_PASSWORD);
        } else {
            url = ConfigurationManager.getProperty(Constants.JDBC_URL_PROD);
            user = ConfigurationManager.getProperty(Constants.JDBC_USER_PROD);
            password = ConfigurationManager.getProperty(Constants.JDBC_PASSWORD_PROD);
        }
        Map<String, String> options = new HashMap<String, String>();
        options.put("url", url);
        options.put("dbtable", "city_info");
        options.put("user", user);
        options.put("password", password);

        // 通过SQLContext去从MySQL中查询数据
        DataFrame cityInfoDF = sqlContext.read().format("jdbc").options(options).load();

        JavaRDD<Row> cityInfoRDD = cityInfoDF.javaRDD();
        JavaPairRDD<Long, Row> cityId2cityInfoRDD = cityInfoRDD.mapToPair(
                new PairFunction<Row, Long, Row>() {
                    private static final long serialVersionUID = 2552393766313917627L;

                    @Override
                    public Tuple2<Long, Row> call(Row row) throws Exception {
                        long cityId = Long.valueOf(String.valueOf(row.get(0)));
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

        // 将DataFrame中的数据，注册成临时表(tmp_click_product_basic)
        df.registerTempTable("tmp_click_product_basic");
    }

    /**
     * 生成各区域各商品点击次数临时表
     *
     * @param sqlContext SQLContext
     */
    private static void generateTempAreaProductClickCountTable(SQLContext sqlContext) {
        /*
         * 按照area和product_id两个字段进行分组
         * 计算出各区域各商品的点击次数
         * 可以获取到每个area下的每个product_id的城市信息拼接起来的串
         */
        String sql = "SELECT area, product_id, count(*) click_count, "
                // concat_long_string作用：0:北京、1:上海
                // group_concat_distinct作用：0:北京,1:上海
                + "group_concat_distinct(concat_long_string(city_id, city_name, ':')) city_infos "
                + "FROM tmp_click_product_basic "
                + "GROUP BY area, product_id ";
        /*
         * 双重group by
         */
//		String _sql = 
//				"SELECT "
//					+ "product_id_area,"
//					+ "count(click_count) click_count,"
//					+ "group_concat_distinct(city_infos) city_infos "
//				+ "FROM ( "
//					+ "SELECT "
//						+ "remove_random_prefix(product_id_area) product_id_area,"
//						+ "click_count,"
//						+ "city_infos "
//					+ "FROM ( "
//						+ "SELECT "
//							+ "product_id_area,"
//							+ "count(*) click_count,"
//							+ "group_concat_distinct(concat_long_string(city_id,city_name,':')) city_infos " 
//						+ "FROM ( "
//							+ "SELECT "  
//								+ "random_prefix(concat_long_string(product_id,area,':'), 10) product_id_area,"
//								+ "city_id,"
//								+ "city_name "
//							+ "FROM tmp_click_product_basic "
//						+ ") t1 "
//						+ "GROUP BY product_id_area "
//					+ ") t2 "  
//				+ ") t3 "
//				+ "GROUP BY product_id_area ";

        // 使用Spark SQL执行这条SQL语句
        DataFrame df = sqlContext.sql(sql);

        // 再次将查询出来的数据注册为一个临时表
        // 各区域各商品的点击次数（以及额外的城市列表）
        df.registerTempTable("tmp_area_product_click_count");
    }

    /**
     * 生成区域商品点击次数临时表（包含了商品的完整信息）
     *
     * @param sqlContext SQLContext
     */
    private static void generateTempAreaFullProductClickCountTable(SQLContext sqlContext) {
        /**
         * 将之前得到的各区域各商品点击次数表，product_id
         * 去关联商品信息表，product_id，product_name和product_status
         * product_status要特殊处理，0，1，分别代表了自营和第三方的商品，放在了一个json串里面
         * get_json_object()函数，可以从json串中获取制定的字段的值
         * if()函数，判断，product_status=0就是自营商品，product_status=1就是第三方商品
         * area, product_id, click_count, city_infos, product_name, product_status
         *
         * 技术点：内置if函数的使用
         */
        String sql = "SELECT tapcc.area, tapcc.product_id, tapcc.click_count, tapcc.city_infos, "
                + "pi.product_name,"
                + "if(get_json_object(pi.extend_info, 'product_status') = '0', '自营商品', '第三方商品') product_status "
                + "FROM tmp_area_product_click_count tapcc "
                + "JOIN product_info pi ON tapcc.product_id = pi.product_id ";

        DataFrame df = sqlContext.sql(sql);
        df.registerTempTable("tmp_area_fullprod_click_count");
    }

    /**
     * 获取各区域top3热门商品
     *
     * @param sqlContext SQLContext
     * @return
     */
    private static JavaRDD<Row> getAreaTop3ProductRDD(SQLContext sqlContext) {
        /*
         * 使用开窗函数先进行一个子查询
         * 按照area进行分组，给每个分组内的数据，按照点击次数降序排序，打上一个组内的行号
         * 接着在外层查询中，过滤出各个组内的行号排名前3的数据
         * 结果就是各个区域下top3热门商品
         *
         * A级：华北、华东
         * B级：华南、华中
         * C级：西北、西南
         * D级：东北
         *
         * case when then ... when then ...else ... end
         * 根据多个条件，不同的条件对应不同的值
         */
        String sql = "SELECT area, "
                + "CASE "
                + "WHEN area = '华北' OR area = '华东' THEN 'A级' "
                + "WHEN area = '华南' OR area = '华中' THEN 'B级' "
                + "WHEN area = '西北' OR area = '西南' THEN 'C级' "
                + "ELSE 'D级' "
                + "END area_level, "
                + "product_id, click_count, city_infos, product_name, product_status "
                + "FROM ("
                + "SELECT area, product_id, click_count, city_infos, product_name, product_status,"
                // 按area分区，按点击次数排序。
                + "row_number() OVER (PARTITION BY area ORDER BY click_count DESC) rank "
                + "FROM tmp_area_fullprod_click_count "
                + ") t "
                // 取每个分组的top3
                + "WHERE rank <= 3";
        DataFrame df = sqlContext.sql(sql);
        return df.javaRDD();
    }

    /**
     * 将计算出来的各区域top3热门商品写入MySQL中
     *
     * @param rows
     */
    private static void persistAreaTop3Product(long taskId, List<Row> rows) {
        List<AreaTop3Product> areaTop3Products = new ArrayList<AreaTop3Product>();

        for (Row row : rows) {
            AreaTop3Product areaTop3Product = new AreaTop3Product();
            areaTop3Product.setTaskId(taskId);
            areaTop3Product.setArea(row.getString(0));
            areaTop3Product.setAreaLevel(row.getString(1));
            areaTop3Product.setProductId(row.getLong(2));
            areaTop3Product.setClickCount(Long.valueOf(String.valueOf(row.get(3))));
            areaTop3Product.setCityInfos(row.getString(4));
            areaTop3Product.setProductName(row.getString(5));
            areaTop3Product.setProductStatus(row.getString(6));
            areaTop3Products.add(areaTop3Product);
        }

        IAreaTop3ProductDAO areaTop3ProductDAO = DAOFactory.getAreaTop3ProductDAO();
        areaTop3ProductDAO.insertBatch(areaTop3Products);
    }

}
