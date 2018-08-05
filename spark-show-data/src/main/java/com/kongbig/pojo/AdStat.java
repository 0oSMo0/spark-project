package com.kongbig.pojo;

/**
 * Describe: 广告实时统计
 * Author:   kongbig
 * Data:     2018/4/20 14:20.
 */
public class AdStat {

    private String date;
    private String province;
    private String city;
    private long adId;
    private long clickCount;

    public AdStat() {
    }

    public AdStat(String date, String province, String city, long adId, long clickCount) {
        this.date = date;
        this.province = province;
        this.city = city;
        this.adId = adId;
        this.clickCount = clickCount;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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

}
