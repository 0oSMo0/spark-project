package com.kongbig.sparkproject.dao;

import com.kongbig.sparkproject.domain.AreaTop3Product;

import java.util.List;

/**
 * Describe: 各区域top3热门商品DAO接口
 * Author:   kongbig
 * Data:     2018/3/30 10:39.
 */
public interface IAreaTop3ProductDAO {
    
    void insertBatch(List<AreaTop3Product> areaTop3Products);
    
}
