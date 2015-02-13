package com.isoftstone.crawl.template.vo;

public class Seed {
    private String url;

    private String isEnabled;

    public Seed() {
        super();
    }
    
    /**
     * @param url
     */
    public Seed(String url) {
        super();
        this.url = url;
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Seed) {
           return ((Seed) obj).getUrl().equals(this.getUrl());
        }
        return false;
    }

}
