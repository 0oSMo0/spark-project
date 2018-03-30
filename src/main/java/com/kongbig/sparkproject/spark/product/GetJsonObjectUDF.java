package com.kongbig.sparkproject.spark.product;

import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.apache.spark.sql.api.java.UDF2;

/**
 * Describe: get_json_object()
 * Author:   kongbig
 * Data:     2018/3/29 16:27.
 */
public class GetJsonObjectUDF implements UDF2<String, String, String> {
    private static Logger LOGGER = Logger.getLogger(GetJsonObjectUDF.class);

    private static final long serialVersionUID = 2938948660563412545L;

    @Override
    public String call(String json, String field) throws Exception {
        try {
            JSONObject jsonObject = JSONObject.parseObject(json);
            return jsonObject.getString(field);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
