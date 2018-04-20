package com.kongbig.sparkproject.dao;

import com.kongbig.sparkproject.domain.AdBlackList;

import java.util.List;

/**
 * Describe: 广告黑名单DAO接口
 * Author:   kongbig
 * Data:     2018/4/19 16:20.
 */
public interface IAdBlackListDAO {

    /**
     * 批量插入广告黑名单用户
     *
     * @param adBlackLists List<AdBlackList>
     */
    void insertBatch(List<AdBlackList> adBlackLists);

    /**
     * 查询所有广告黑名单用户
     *
     * @return List<AdBlackList>
     */
    List<AdBlackList> findAll();

}
