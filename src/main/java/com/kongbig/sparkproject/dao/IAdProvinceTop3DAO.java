package com.kongbig.sparkproject.dao;

import com.kongbig.sparkproject.domain.AdProvinceTop3;

import java.util.List;

/**
 * Describe: 各省份top3热门广告DAO接口
 * Author:   kongbig
 * Data:     2018/4/21 11:04.
 */
public interface IAdProvinceTop3DAO {

    /**
     * 批量更新
     *
     * @param adProvinceTop3s List<AdProvinceTop3>
     */
    void updateBatch(List<AdProvinceTop3> adProvinceTop3s);

}
