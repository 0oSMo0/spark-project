package com.kongbig.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kongbig.mapper.DataTableMapper;
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

@Service
public class DataTableService {
	
	@Autowired
	private DataTableMapper dataTableMapper;
	
	public List<Task> queryTask() {
		return dataTableMapper.queryTask();
	}

	public List<SessionAggrStat> querySessionAggrStat() {
		return dataTableMapper.querySessionAggrStat();
	}

	public List<SessionDetail> querySessionDetail() {
		return dataTableMapper.querySessionDetail();
	}

	public List<Top10Category> queryTop10Category() {
		return dataTableMapper.queryTop10Category();
	}

	public List<Top10Session> queryTop10Session() {
		return dataTableMapper.queryTop10Session();
	}

	public List<PageSplitConvertRate> queryPageSplitConvertRate() {
		return dataTableMapper.queryPageSplitConvertRate();
	}

	public List<AreaTop3Product> queryAreaTop3Product() {
		return dataTableMapper.queryAreaTop3Product();
	}

	public List<AdBlackList> queryAdBlackList() {
		return dataTableMapper.queryAdBlackList();
	}

	public List<AdClickTrend> queryAdClickTrend() {
		return dataTableMapper.queryAdClickTrend();
	}

	public List<AdProvinceTop3> queryAdProvinceTop3() {
		return dataTableMapper.queryAdProvinceTop3();
	}

	public List<AdStat> queryAdStat() {
		return dataTableMapper.queryAdStat();
	}

}
