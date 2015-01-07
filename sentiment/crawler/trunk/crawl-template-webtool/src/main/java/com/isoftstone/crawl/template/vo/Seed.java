package com.isoftstone.crawl.template.vo;

public class Seed {
	private String url;

	private String isEnabled;

	public Seed() {
		super();
	}

	public Seed(String url, String isEnabled) {
		super();
		this.url = url;
		this.isEnabled = isEnabled;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getIsEnabled() {
		return isEnabled;
	}

	public void setIsEnabled(String isEnabled) {
		this.isEnabled = isEnabled;
	}

}
