package com.kongbig.pojo;

/**
 * Describe: 各省top3热门广告
 * Author:   kongbig
 * Data:     2018/4/21 11:01.
 */
public class AdProvinceTop3 {

    private String date;
    private String province;
    private long adId;
    private long clickCount;

    public AdProvinceTop3() {
    }

    public AdProvinceTop3(String date, String province, long adId, long clickCount) {
        this.date = date;
        this.province = province;
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
        return "AdProvinceTop3{" +
                "date='" + date + '\'' +
                ", province='" + province + '\'' +
                ", adId=" + adId +
                ", clickCount=" + clickCount +
                '}';
    }
    
}
