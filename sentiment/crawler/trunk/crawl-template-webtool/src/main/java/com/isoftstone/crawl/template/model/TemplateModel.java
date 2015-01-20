package com.isoftstone.crawl.template.model;

/**
 * 
 * 模板列表实体
 * */
public class TemplateModel {	
   private String templateId="";
   private String name="";
   private String url="";
   private String status="";
   private String description="";
   private String schedulePeriod="";
   private String scheduleSequence="";
   private String increasePeriod="";
   private String increasePageCounts="";
   private String increasePageSort="";
   private String addedTime="";
   
public String getTemplateId() {
	return templateId;
}
public void setTemplateId(String templateId) {
	this.templateId = templateId;
}

public String getStatus() {
	return status;
}
public void setStatus(String status) {
	this.status = status;
}

public String getDescription() {
	return description;
}
public void setDescription(String description) {
	this.description = description;
}

public String getAddedTime() {
	return addedTime;
}
public void setAddedTime(String addedTime) {
	this.addedTime = addedTime;
}
public String getName() {
	return name;
}
public void setName(String name) {
	this.name = name;
}
public String getUrl() {
	return url;
}
public void setUrl(String url) {
	this.url = url;
}
public String getSchedulePeriod() {
	return schedulePeriod;
}
public void setSchedulePeriod(String schedulePeriod) {
	this.schedulePeriod = schedulePeriod;
}
public String getScheduleSequence() {
	return scheduleSequence;
}
public void setScheduleSequence(String scheduleSequence) {
	this.scheduleSequence = scheduleSequence;
}
public String getIncreasePeriod() {
	return increasePeriod;
}
public void setIncreasePeriod(String increasePeriod) {
	this.increasePeriod = increasePeriod;
}
public String getIncreasePageCounts() {
	return increasePageCounts;
}
public void setIncreasePageCounts(String increasePageCounts) {
	this.increasePageCounts = increasePageCounts;
}
public String getIncreasePageSort() {
	return increasePageSort;
}
public void setIncreasePageSort(String increasePageSort) {
	this.increasePageSort = increasePageSort;
}
}
