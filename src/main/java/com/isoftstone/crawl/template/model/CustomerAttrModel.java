package com.isoftstone.crawl.template.model;

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
	private String formatter="";
	private String formatCategory = "";
	private String replaceBefore = "";
	private String filterReplaceTo = "";
	
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
	
	public String getFilterReplaceTo() {
		return filterReplaceTo;
	}
	public void setFilterReplaceTo(String filterReplaceTo) {
		this.filterReplaceTo = filterReplaceTo;
	}
	
	public String getFormatCategory() {
		return formatCategory;
	}
	public void setFormatCategory(String formatCategory) {
		this.formatCategory = formatCategory;
	}
	
	public String getFormatter() {
		return formatter;
	}
	public void setFormatter(String formatter) {
		this.formatter = formatter;
	}
	
	
}
