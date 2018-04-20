package com.kongbig.sparkproject.dao;

import com.kongbig.sparkproject.domain.AdUserClickCount;

import java.util.List;

/**
 * Describe: 用户广告点击量DAO接口
 * Author:   kongbig
 * Data:     2018/4/19 11:27.
 */
public interface IAdUserClickCountDAO {

    /**
     * 批量更新用户广告点击量（已存在于数据库中就更新；否则，插入；）
     *
     * @param adUserClickCounts List<AdUserClickCount>
     */
    void updateBatch(List<AdUserClickCount> adUserClickCounts);

    /**
     * 根据多个key查询用户广告点击量
     *
     * @param date   日期
     * @param userId 用户id
     * @param adId   广告id
     * @return 点击次数
     */
    int findClickCountByMultiKey(String date, long userId, long adId);

}
