package com.isoftstone.crawl.template.utils;

import java.util.Comparator;

import com.isoftstone.crawl.template.model.SeedsEffectiveStatusModel;

public class SeedsEffectiveModelComparator implements Comparator<SeedsEffectiveStatusModel> {
	public int compare(SeedsEffectiveStatusModel t1, SeedsEffectiveStatusModel t2) {
		return t1.getName().compareTo(t2.getName());
	}
}
