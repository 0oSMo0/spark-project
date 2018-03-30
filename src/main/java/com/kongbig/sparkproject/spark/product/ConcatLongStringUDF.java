package com.kongbig.sparkproject.spark.product;

import org.apache.spark.sql.api.java.UDF3;

/**
 * Describe: 将两个字段拼接起来（使用指定的分隔符）
 * Author:   kongbig
 * Data:     2018/3/29 14:24.
 */
public class ConcatLongStringUDF implements UDF3<Long, String, String, String> {

    private static final long serialVersionUID = -2106182118591521379L;

    @Override
    public String call(Long v1, String v2, String split) throws Exception {
        return String.valueOf(v1) + split + v2;
    }
}
