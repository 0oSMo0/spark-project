package com.kongbig.sparkproject.dao;

import com.kongbig.sparkproject.domain.AdStat;

import java.util.List;

/**
 * Describe: 广告实时统计DAO接口
 * Author:   kongbig
 * Data:     2018/4/20 14:22.
 */
public interface IAdStatDAO {

    void updateBatch(List<AdStat> adStats);

}
