package com.kongbig.sparkproject.dao;

import com.kongbig.sparkproject.domain.SessionDetail;

import java.util.List;

/**
 * Describe: Session明细DAO接口
 * Author:   kongbig
 * Data:     2018/2/6.
 */
public interface ISessionDetailDAO {

    /**
     * 插入一条session明细数据
     *
     * @param sessionDetail
     */
    void insert(SessionDetail sessionDetail);

    /**
     * 批量插入session明细数据
     *
     * @param sessionDetails
     */
    void insertBatch(List<SessionDetail> sessionDetails);

}
