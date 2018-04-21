package com.kongbig.sparkproject.domain;

/**
 * Describe: 广告点击趋势
 * Author:   kongbig
 * Data:     2018/4/21 14:30.
 */
public class AdClickTrend {

    private String date;
    private String hour;
    private String minute;
    private long adId;
    private long clickCount;

    public AdClickTrend() {
    }

    public AdClickTrend(String date, String hour, String minute, long adId, long clickCount) {
        this.date = date;
        this.hour = hour;
        this.minute = minute;
        this.adId = adId;
        this.clickCount = clickCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public String getMinute() {
        return minute;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public long getAdId() {
        return adId;
    }

    public void setAdId(long adId) {
        this.adId = adId;
    }

    public long getClickCount() {
        return clickCount;
    }

    public void setClickCount(long clickCount) {
        this.clickCount = clickCount;
    }

    @Override
    public String toString() {
        return "AdClickTrend{" +
                "date='" + date + '\'' +
                ", hour='" + hour + '\'' +
                ", minute='" + minute + '\'' +
                ", adId=" + adId +
                ", clickCount=" + clickCount +
                '}';
    }
    
}
