package com.kongbig.sparkproject.dao.impl;

import com.kongbig.sparkproject.dao.IPageSplitConvertRateDAO;
import com.kongbig.sparkproject.domain.PageSplitConvertRate;
import com.kongbig.sparkproject.jdbc.JDBCHelper;

/**
 * Describe: 页面切片转换率DAO实现类
 * Author:   kongbig
 * Data:     2018/3/27 16:01.
 */
public class PageSplitConvertRateDAOImpl implements IPageSplitConvertRateDAO {

    @Override
    public void insert(PageSplitConvertRate pageSplitConvertRate) {
        String sql = "insert into page_split_convert_rate value(?, ?)";
        Object[] param = new Object[]{pageSplitConvertRate.getTaskId(),
                pageSplitConvertRate.getConvertRate()};
        JDBCHelper jdbcHelper = JDBCHelper.getInstance();
        jdbcHelper.executeUpdate(sql, param);
    }

}
