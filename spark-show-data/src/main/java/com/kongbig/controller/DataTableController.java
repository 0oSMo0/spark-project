package com.kongbig.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
import com.kongbig.service.DataTableService;

@RestController
@EnableAutoConfiguration
public class DataTableController {

	@Autowired
	private DataTableService dataTableService;

	@RequestMapping("queryTask")
	@ResponseBody
	public List<Task> queryTask() {
		return dataTableService.queryTask();
	}

	@RequestMapping("querySessionAggrStat")
	@ResponseBody
	public List<SessionAggrStat> querySessionAggrStat() {
		return dataTableService.querySessionAggrStat();
	}

	@RequestMapping("querySessionDetail")
	@ResponseBody
	public List<SessionDetail> querySessionDetail() {
		return dataTableService.querySessionDetail();
	}
	
	@RequestMapping("queryTop10Category")
	@ResponseBody
	public List<Top10Category> queryTop10Category() {
		return dataTableService.queryTop10Category();
	}
	
	@RequestMapping("queryTop10Session")
	@ResponseBody
	public List<Top10Session> queryTop10Session() {
		return dataTableService.queryTop10Session();
	}
	
	@RequestMapping("queryPageSplitConvertRate")
	@ResponseBody
	public List<PageSplitConvertRate> queryPageSplitConvertRate() {
		return dataTableService.queryPageSplitConvertRate();
	}
	
	@RequestMapping("queryAreaTop3Product")
	@ResponseBody
	public List<AreaTop3Product> queryAreaTop3Product() {
		return dataTableService.queryAreaTop3Product();
	}
	
	@RequestMapping("queryAdBlackList")
	@ResponseBody
	public List<AdBlackList> queryAdBlackList() {
		return dataTableService.queryAdBlackList();
	}
	
	@RequestMapping("queryAdClickTrend")
	@ResponseBody
	public List<AdClickTrend> queryAdClickTrend() {
		return dataTableService.queryAdClickTrend();
	}
	
	@RequestMapping("queryAdProvinceTop3")
	@ResponseBody
	public List<AdProvinceTop3> queryAdProvinceTop3() {
		return dataTableService.queryAdProvinceTop3();
	}
	
	@RequestMapping("queryAdStat")
	@ResponseBody
	public List<AdStat> queryAdStat() {
		return dataTableService.queryAdStat();
	}

}
