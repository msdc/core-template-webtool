package com.isoftstone.crawl.template.webtool;

/**
 * 
 * 基本信息视图view-model
 * */
public class BasicInfoViewModel {
	private String url="";
	private String name = "";
	private String tag="";
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
}
