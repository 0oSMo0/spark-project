package com.kongbig.sparkproject.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kongbig.sparkproject.conf.ConfigurationManager;
import com.kongbig.sparkproject.constant.Constants;
import org.apache.log4j.Logger;

/**
 * Describe: 参数工具类
 * Author:   kongbig
 * Data:     2018/1/30.
 */
public class ParamUtils {

    private static final Logger LOGGER = Logger.getLogger(ParamUtils.class);

    /**
     * 从命令行参数中提取任务id
     * 本地模式：直接取出my.properties设置的任务id
     * 集群模式：取出args参数数组中传入的任务id
     *
     * @param args 命令行参数
     * @return 任务id
     */
    public static Long getTaskIdFromArgs(String[] args, String taskType) {
        boolean local = ConfigurationManager.getBoolean(Constants.SPARK_LOCAL);
        if (local) {
            return ConfigurationManager.getLong(taskType);
        } else {
            try {
                if (args != null && args.length > 0) {
                    return Long.valueOf(args[0]);// 取出了第一个参数
                }
            } catch (NumberFormatException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * 从JSON对象中提取参数
     *
     * @param jsonObject JSON对象
     * @param field      参数
     * @return
     */
    public static String getParam(JSONObject jsonObject, String field) {
        JSONArray jsonArray = jsonObject.getJSONArray(field);
        if (jsonArray != null && jsonArray.size() > 0) {
            return jsonArray.getString(0);
        }
        return null;
    }

}
