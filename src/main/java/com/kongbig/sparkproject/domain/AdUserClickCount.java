package com.kongbig.sparkproject.domain;

/**
 * Describe: 用户广告点击量
 * Author:   kongbig
 * Data:     2018/4/19 11:28.
 */
public class AdUserClickCount {
    
    private String date;
    private long userId;
    private long adId;
    private long clickCount;

    public AdUserClickCount() {
    }

    public AdUserClickCount(String date, long userId, long adId, long clickCount) {
        this.date = date;
        this.userId = userId;
        this.adId = adId;
        this.clickCount = clickCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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
        return "AdUserClickCount{" +
                "date='" + date + '\'' +
                ", userId=" + userId +
                ", adId=" + adId +
                ", clickCount=" + clickCount +
                '}';
    }
}
