package com.kongbig.sparkproject.spark.product;

import org.apache.spark.sql.api.java.UDF2;

import java.util.Random;

/**
 * Describe: random_prefix()
 * Author:   kongbig
 * Data:     2018/3/30 11:16.
 */
public class RandomPrefixUDF implements UDF2<String, Integer, String> {

    private static final long serialVersionUID = 2944707412674719000L;

    @Override
    public String call(String val, Integer num) throws Exception {
        Random random = new Random();
        int randNum = random.nextInt(10);
        return randNum + "_" + val;
    }

}
