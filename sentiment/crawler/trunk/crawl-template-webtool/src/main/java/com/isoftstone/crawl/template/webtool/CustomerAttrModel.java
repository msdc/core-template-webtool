package com.isoftstone.crawl.template.webtool;

/**
 * 
 * 自定义属性Model实体
 * */
public class CustomerAttrModel {
	private String target="";
	private String selector="";
	private String attr = "";
	private String filter="";
	private String filterCategory = "";
	private String replaceBefore = "";
	private String replaceTo = "";
	
	public String getSelector() {
		return selector;
	}
	public void setSelector(String selector) {
		this.selector = selector;
	}
	
	public String getAttr() {
		return attr;
	}
	public void setAttr(String attr) {
		this.attr = attr;
	}
	
	public String getFilter() {
		return filter;
	}
	public void setFilter(String filter) {
		this.filter = filter;
	}
	
	public String getFilterCategory() {
		return filterCategory;
	}
	public void setFilterCategory(String filterCategory) {
		this.filterCategory = filterCategory;
	}
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	
	public String getReplaceBefore() {
		return replaceBefore;
	}
	public void setReplaceBefore(String replaceBefore) {
		this.replaceBefore = replaceBefore;
	}
	
	public String getReplaceTo() {
		return replaceTo;
	}
	public void setReplaceTo(String replaceTo) {
		this.replaceTo = replaceTo;
	}
	
	
}
