package com.isoftstone.crawl.template.vo;

import java.util.List;

public class DispatchVo {

	private List<Seed> seed;

	private String status;

	public List<Seed> getSeed() {
		return seed;
	}

	public void setSeed(List<Seed> seed) {
		this.seed = seed;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
