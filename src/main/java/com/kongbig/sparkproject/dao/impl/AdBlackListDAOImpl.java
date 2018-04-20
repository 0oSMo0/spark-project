package com.kongbig.sparkproject.dao.impl;

import com.kongbig.sparkproject.dao.IAdBlackListDAO;
import com.kongbig.sparkproject.domain.AdBlackList;
import com.kongbig.sparkproject.jdbc.JDBCHelper;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Describe: 广告黑名单DAO实现类
 * Author:   kongbig
 * Data:     2018/4/19 16:22.
 */
public class AdBlackListDAOImpl implements IAdBlackListDAO {

    @Override
    public void insertBatch(List<AdBlackList> adBlackLists) {
        String sql = "INSERT INTO ad_blacklist VALUES(?)";

        List<Object[]> paramsList = new ArrayList<Object[]>();

        for (AdBlackList adBlackList : adBlackLists) {
            Object[] params = new Object[]{adBlackList.getUserId()};
            paramsList.add(params);
        }

        JDBCHelper jdbcHelper = JDBCHelper.getInstance();
        jdbcHelper.executeBatch(sql, paramsList);
    }

    @Override
    public List<AdBlackList> findAll() {
        String sql = "SELECT * FROM  ad_blacklist";

        final List<AdBlackList> adBlackLists = new ArrayList<AdBlackList>();

        JDBCHelper jdbcHelper = JDBCHelper.getInstance();

        jdbcHelper.executeQuery(sql, null, new JDBCHelper.QueryCallback() {
            @Override
            public void process(ResultSet rs) throws Exception {
                while (rs.next()) {
                    long userId = Long.valueOf(String.valueOf(rs.getInt(1)));
                    AdBlackList adBlackList = new AdBlackList();
                    adBlackList.setUserId(userId);

                    adBlackLists.add(adBlackList);
                }
            }
        });

        return adBlackLists;
    }

}
