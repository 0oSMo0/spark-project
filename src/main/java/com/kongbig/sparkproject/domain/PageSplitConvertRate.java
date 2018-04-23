package com.kongbig.sparkproject.domain;

/**
 * Describe: 页面切片转化率实体
 * Author:   kongbig
 * Data:     2018/3/27 15:57.
 */
public class PageSplitConvertRate {
    
    private long taskId;
    private String convertRate;

    public PageSplitConvertRate() {
    }

    public PageSplitConvertRate(long taskId, String convertRate) {
        this.taskId = taskId;
        this.convertRate = convertRate;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public String getConvertRate() {
        return convertRate;
    }

    public void setConvertRate(String convertRate) {
        this.convertRate = convertRate;
    }

    @Override
    public String toString() {
        return "PageSplitConvertRate{" +
                "taskId=" + taskId +
                ", convertRate='" + convertRate + '\'' +
                '}';
    }
    
}
