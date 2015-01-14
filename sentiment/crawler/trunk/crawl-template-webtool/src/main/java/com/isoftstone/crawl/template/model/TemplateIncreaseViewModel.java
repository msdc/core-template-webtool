package com.isoftstone.crawl.template.model;

/**
 * 
 * 增量配置
 * */
public class TemplateIncreaseViewModel {
	private String period = "";
	private String pageCounts="";
	
	public String getPeriod() {
		return period;
	}
	public void setPeriod(String period) {
		this.period = period;
	}
	
	public String getPageCounts() {
		return pageCounts;
	}
	public void setPageCounts(String pageCounts) {
		this.pageCounts = pageCounts;
	}
}
