package com.kongbig.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import com.kongbig.pojo.AdBlackList;
import com.kongbig.pojo.AdClickTrend;
import com.kongbig.pojo.AdProvinceTop3;
import com.kongbig.pojo.AdStat;
import com.kongbig.pojo.AreaTop3Product;
import com.kongbig.pojo.PageSplitConvertRate;
import com.kongbig.pojo.SessionAggrStat;
import com.kongbig.pojo.SessionDetail;
import com.kongbig.pojo.Task;
import com.kongbig.pojo.Top10Category;
import com.kongbig.pojo.Top10Session;

@Repository
public interface DataTableMapper {

	@Select("select * from task")
	@Results({ @Result(column = "task_id", property = "taskId"), @Result(column = "task_name", property = "taskName"),
			@Result(column = "create_time", property = "createTime"),
			@Result(column = "start_time", property = "startTime"),
			@Result(column = "finish_time", property = "finishTime"),
			@Result(column = "task_type", property = "taskType"),
			@Result(column = "task_status", property = "taskStatus"),
			@Result(column = "task_param", property = "taskParam") })
	public abstract List<Task> queryTask();

	@Select("select * from session_aggr_stat")
	@Results({ @Result(column = "task_id", property = "taskId"),
			@Result(column = "session_count", property = "session_count"),
			@Result(column = "1s_3s", property = "visit_length_1s_3s_ratio"),
			@Result(column = "4s_6s", property = "visit_length_4s_6s_ratio"),
			@Result(column = "7s_9s", property = "visit_length_7s_9s_ratio"),
			@Result(column = "10s_30s", property = "visit_length_10s_30s_ratio"),
			@Result(column = "30s_60s", property = "visit_length_30s_60s_ratio"),
			@Result(column = "1m_3m", property = "visit_length_1m_3m_ratio"),
			@Result(column = "3m_10m", property = "visit_length_3m_10m_ratio"),
			@Result(column = "10m_30m", property = "visit_length_10m_30m_ratio"),
			@Result(column = "30m", property = "visit_length_30m_ratio"),
			@Result(column = "1_3", property = "step_length_1_3_ratio"),
			@Result(column = "4_6", property = "step_length_4_6_ratio"),
			@Result(column = "7_9", property = "step_length_7_9_ratio"),
			@Result(column = "10_30", property = "step_length_10_30_ratio"),
			@Result(column = "30_60", property = "step_length_30_60_ratio"),
			@Result(column = "60", property = "step_length_60_ratio") })
	public abstract List<SessionAggrStat> querySessionAggrStat();

	@Select("select * from session_detail")
	@Results({ @Result(column = "task_id", property = "taskId"), @Result(column = "user_id", property = "userId"),
			@Result(column = "session_id", property = "sessionId"),
			@Result(column = "action_time", property = "actionTime"),
			@Result(column = "search_keyword", property = "searchKeyword"),
			@Result(column = "click_category_id", property = "clickCategoryId"),
			@Result(column = "click_product_id", property = "clickProductId"),
			@Result(column = "order_category_ids", property = "orderCategoryIds"),
			@Result(column = "order_product_ids", property = "orderProductIds"),
			@Result(column = "pay_category_ids", property = "payCategoryIds"),
			@Result(column = "pay_product_ids", property = "payProductIds") })
	public abstract List<SessionDetail> querySessionDetail();

	@Select("select * from top10_category")
	@Results({ @Result(column = "task_id", property = "taskId"),
			@Result(column = "category_id", property = "categoryId"),
			@Result(column = "click_count", property = "clickCount"),
			@Result(column = "order_count", property = "orderCount"),
			@Result(column = "pay_count", property = "payCount") })
	public abstract List<Top10Category> queryTop10Category();

	@Select("select * from top10_session")
	@Results({ @Result(column = "task_id", property = "taskId"),
			@Result(column = "category_id", property = "categoryId"),
			@Result(column = "session_id", property = "sessionId"),
			@Result(column = "click_count", property = "clickCount") })
	public abstract List<Top10Session> queryTop10Session();

	@Select("select * from page_split_convert_rate")
	@Results({ @Result(column = "taskid", property = "taskId"),
			@Result(column = "convert_rate", property = "convertRate") })
	public abstract List<PageSplitConvertRate> queryPageSplitConvertRate();

	@Select("select * from area_top3_product")
	@Results({ @Result(column = "task_id", property = "taskId"), @Result(column = "area", property = "area"),
			@Result(column = "area_level", property = "areaLevel"),
			@Result(column = "product_id", property = "productId"),
			@Result(column = "city_infos", property = "cityInfos"),
			@Result(column = "click_count", property = "clickCount"),
			@Result(column = "product_name", property = "productName"),
			@Result(column = "product_status", property = "productStatus") })
	public abstract List<AreaTop3Product> queryAreaTop3Product();

	@Select("select * from ad_blacklist")
	@Results({ @Result(column = "user_id", property = "userId") })
	public abstract List<AdBlackList> queryAdBlackList();

	@Select("select * from ad_click_trend")
	@Results({ @Result(column = "date", property = "date"),
		@Result(column = "hour", property = "hour"),
		@Result(column = "minute", property = "minute"),
		@Result(column = "ad_id", property = "adId"),
		@Result(column = "click_count", property = "clickCount") })
	public abstract List<AdClickTrend> queryAdClickTrend();

	@Select("select * from ad_province_top3")
	@Results({ @Result(column = "date", property = "date"),
		@Result(column = "province", property = "province"),
		@Result(column = "ad_id", property = "adId"),
		@Result(column = "click_count", property = "clickCount") })
	public abstract List<AdProvinceTop3> queryAdProvinceTop3();

	@Select("select * from ad_stat")
	@Results({ @Result(column = "date", property = "date"),
		@Result(column = "province", property = "province"),
		@Result(column = "city", property = "city"),
		@Result(column = "ad_id", property = "adId"),
		@Result(column = "click_count", property = "clickCount") })
	public abstract List<AdStat> queryAdStat();

}
