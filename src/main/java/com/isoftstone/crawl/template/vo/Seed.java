package com.isoftstone.crawl.template.vo;

public class Seed {
    private String url;

    private String isEnabled;
    
    private String status;

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
    
    /**
     * @param url
     * @param isEnabled
     * @param status
     */
    public Seed(String url, String isEnabled, String status) {
        super();
        this.url = url;
        this.isEnabled = isEnabled;
        this.status = status;
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

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
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
