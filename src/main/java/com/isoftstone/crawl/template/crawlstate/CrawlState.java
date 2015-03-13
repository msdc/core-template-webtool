/*
 * @(#)CrawlStateUtil.java 2015-3-10 下午2:39:09 crawl-template-webtool Copyright
 * 2015 Isoftstone, Inc. All rights reserved. ISOFTSTONE
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.isoftstone.crawl.template.crawlstate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.isoftstone.crawl.template.consts.WebtoolConstants;
import com.isoftstone.crawl.template.model.CrawlStateBean;
import com.isoftstone.crawl.template.utils.RedisOperator;
import com.isoftstone.crawl.template.utils.RedisUtils;
import com.isoftstone.crawl.template.vo.DispatchVo;

/**
 * CrawlStateUtil
 * @author danhb
 * @date  2015-3-10
 * @version 1.0
 *
 */
public class CrawlState {

    private static final Log LOG = LogFactory.getLog(CrawlState.class);

    /**
     * 获取爬虫状态. 
     * @return 爬虫状态设置.
     */
    public List<CrawlStateBean> getCrawlState() {
        List<String> folderNameList = getResultList("*_dispatch",
            WebtoolConstants.DISPATCH_REDIS_DBINDEX);
        List<CrawlStateBean> crawlStateList = new ArrayList<CrawlStateBean>();
        for (Iterator<String> it = folderNameList.iterator(); it.hasNext();) {
            String redisKey = it.next();
            DispatchVo dispatchVo = RedisOperator.getDispatchResult(redisKey,
                WebtoolConstants.DISPATCH_REDIS_DBINDEX);
            CrawlStateBean bean = new CrawlStateBean();
            bean.setDispatchName(redisKey.substring(0,
                redisKey.lastIndexOf("_")));
            String crawlState = "";
            if ("running".equals(dispatchVo.getStatus())) {
                crawlState = "爬取中";
            } else if ("start".equals(dispatchVo.getStatus())) {
                crawlState = "未开始";
            } else if ("complete".equals(dispatchVo.getStatus())) {
                crawlState = "完成";
            }
            bean.setCrawlState(crawlState);
            crawlStateList.add(bean);
        }
        return crawlStateList;
    }

    public void crawlIncrement(String dispatchName) {

    }

    public void crawlFull(String dispatchName) {

    }

    public void stopCrawl(String dispatchName) {
        
    }

    /*
     * 获取所有符合条件的结果List.
     */
    private List<String> getResultList(String guid, int dbindex) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(dbindex);
            Set<String> set = jedis.keys(guid);
            List<String> resultList = new ArrayList<String>();
            resultList.addAll(set);
            return resultList;
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            LOG.error("", e);
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
        return null;
    }
}
