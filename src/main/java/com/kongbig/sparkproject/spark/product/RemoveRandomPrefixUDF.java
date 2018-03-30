package com.kongbig.sparkproject.spark.product;

import org.apache.spark.sql.api.java.UDF1;

/**
 * Describe: 去除随机前缀
 * Author:   kongbig
 * Data:     2018/3/30 11:31.
 */
public class RemoveRandomPrefixUDF implements UDF1<String,String> {
    private static final long serialVersionUID = -8147221797745765846L;

    @Override
    public String call(String val) throws Exception {
        String[] valSplited = val.split("_");
        return valSplited[1];
    }
}
