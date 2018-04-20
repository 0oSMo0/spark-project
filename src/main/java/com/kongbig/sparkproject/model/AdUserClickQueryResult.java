package com.kongbig.sparkproject.model;

/**
 * Describe: 用户广告点击量查询结果
 * Author:   kongbig
 * Data:     2018/4/19 11:36.
 */
public class AdUserClickQueryResult {

    // SELECT count(*) FROM ad_user_click_count WHERE date = ? AND user_id = ? AND ad_id = ? 
    private int count;
    // SELECT click_count FROM ad_user_click_count WHERE date = ? AND user_id = ? AND ad_id = ?
    private int clickCount;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getClickCount() {
        return clickCount;
    }

    public void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }
}
