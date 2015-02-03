package com.isoftstone.crawl.template.vo;

import java.util.List;

public class DispatchVo {

    private List<Seed> seed;

    private String status;

    private boolean userProxy;

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

    /**
     * @return the userProxy
     */
    public boolean isUserProxy() {
        return userProxy;
    }

    /**
     * @param userProxy the userProxy to set
     */
    public void setUserProxy(boolean userProxy) {
        this.userProxy = userProxy;
    }

}
