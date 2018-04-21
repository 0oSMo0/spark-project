package com.kongbig.sparkproject.dao;

import com.kongbig.sparkproject.domain.AdClickTrend;

import java.util.List;

/**
 * Describe: 广告点击趋势DAO接口
 * Author:   kongbig
 * Data:     2018/4/21 14:39.
 */
public interface IAdClickTrendDAO {

    void updateBatch(List<AdClickTrend> adClickTrends);

}
