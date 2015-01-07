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
}
