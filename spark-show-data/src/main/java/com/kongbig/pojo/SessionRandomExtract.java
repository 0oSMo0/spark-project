package com.kongbig.pojo;

import javax.persistence.Column;
import javax.persistence.Table;

/**
 * 随机抽取的session
 * 
 * @author kongbig
 *
 */
@Table(name="session_random_extract")
public class SessionRandomExtract {

	@Column(name="task_id")
	private long taskId;
	@Column(name="session_id")
	private String sessionId;
	@Column(name="start_time")
	private String startTime;
	@Column(name="search_keywords")
	private String searchKeywords;
	@Column(name="click_category_ids")
	private String clickCategoryIds;

	public long getTaskId() {
		return taskId;
	}

	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getSearchKeywords() {
		return searchKeywords;
	}

	public void setSearchKeywords(String searchKeywords) {
		this.searchKeywords = searchKeywords;
	}

	public String getClickCategoryIds() {
		return clickCategoryIds;
	}

	public void setClickCategoryIds(String clickCategoryIds) {
		this.clickCategoryIds = clickCategoryIds;
	}
}
