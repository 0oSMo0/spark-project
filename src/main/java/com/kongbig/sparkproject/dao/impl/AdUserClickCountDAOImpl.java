package com.kongbig.sparkproject.dao.impl;

import com.kongbig.sparkproject.dao.IAdUserClickCountDAO;
import com.kongbig.sparkproject.domain.AdUserClickCount;
import com.kongbig.sparkproject.jdbc.JDBCHelper;
import com.kongbig.sparkproject.model.AdUserClickQueryResult;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe: 用户广告点击量DAO实现类
 * Author:   kongbig
 * Data:     2018/4/19 11:31.
 */
public class AdUserClickCountDAOImpl implements IAdUserClickCountDAO {

    @Override
    public void updateBatch(List<AdUserClickCount> adUserClickCounts) {
        JDBCHelper jdbcHelper = JDBCHelper.getInstance();

        // 首先对用户广告点击量进行分类，分成待插入的和待更新的
        List<AdUserClickCount> insertAdUserClickCounts = new ArrayList<AdUserClickCount>();
        List<AdUserClickCount> updateAdUserClickCounts = new ArrayList<AdUserClickCount>();

        String selectSQL = "SELECT count(*) FROM ad_user_click_count "
                + "WHERE date = ? AND user_id = ? AND ad_id = ? ";
        Object[] selectParams = null;

        for (AdUserClickCount adUserClickCount : adUserClickCounts) {
            final AdUserClickQueryResult queryResult = new AdUserClickQueryResult();

            selectParams = new Object[]{adUserClickCount.getDate(),
                    adUserClickCount.getUserId(), adUserClickCount.getAdId()};

            jdbcHelper.executeQuery(selectSQL, selectParams, new JDBCHelper.QueryCallback() {
                @Override
                public void process(ResultSet rs) throws Exception {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        queryResult.setCount(count);
                    }
                }
            });

            int count = queryResult.getCount();

            if (count > 0) {// 已存在于数据库中就更新；否则，插入；
                updateAdUserClickCounts.add(adUserClickCount);
            } else {
                insertAdUserClickCounts.add(adUserClickCount);
            }
        }

        // 执行批量插入
        String insertSQL = "INSERT INTO ad_user_click_count VALUES(?,?,?,?)";
        List<Object[]> insertParamsList = new ArrayList<Object[]>();
        for (AdUserClickCount adUserClickCount : insertAdUserClickCounts) {
            Object[] insertParams = new Object[]{adUserClickCount.getDate(),
                    adUserClickCount.getUserId(),
                    adUserClickCount.getAdId(),
                    adUserClickCount.getClickCount()};
            insertParamsList.add(insertParams);
        }
        jdbcHelper.executeBatch(insertSQL, insertParamsList);

        // 执行批量更新（*应该是累加）
        String updateSQL = "UPDATE ad_user_click_count SET click_count = click_count + ? "
                + "WHERE date = ? AND user_id = ? AND ad_id = ? ";
        List<Object[]> updateParamsList = new ArrayList<Object[]>();
        for (AdUserClickCount adUserClickCount : updateAdUserClickCounts) {
            Object[] updateParams = new Object[]{adUserClickCount.getClickCount(),
                    adUserClickCount.getDate(),
                    adUserClickCount.getUserId(),
                    adUserClickCount.getAdId()};
            updateParamsList.add(updateParams);
        }
        jdbcHelper.executeBatch(updateSQL, updateParamsList);
    }

    @Override
    public int findClickCountByMultiKey(String date, long userId, long adId) {
        String sql = "SELECT click_count FROM ad_user_click_count "
                + "WHERE date = ? AND user_id = ? AND ad_id = ?";
        Object[] params = new Object[]{date, userId, adId};

        final AdUserClickQueryResult queryResult = new AdUserClickQueryResult();

        JDBCHelper jdbcHelper = JDBCHelper.getInstance();
        jdbcHelper.executeQuery(sql, params, new JDBCHelper.QueryCallback() {
            @Override
            public void process(ResultSet rs) throws Exception {
                if (rs.next()) {
                    int clickCount = rs.getInt(1);
                    queryResult.setClickCount(clickCount);
                }
            }
        });

        int clickCount = queryResult.getClickCount();

        return clickCount;
    }

}
