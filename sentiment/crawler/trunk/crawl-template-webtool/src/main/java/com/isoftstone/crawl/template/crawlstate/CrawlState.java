/*
 * @(#)CrawlStateUtil.java 2015-3-10 下午2:39:09 crawl-template-webtool Copyright
 * 2015 Isoftstone, Inc. All rights reserved. ISOFTSTONE
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.isoftstone.crawl.template.crawlstate;

import java.io.File;
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
import com.isoftstone.crawl.template.utils.Config;
import com.isoftstone.crawl.template.utils.RedisOperator;
import com.isoftstone.crawl.template.utils.RedisUtils;
import com.isoftstone.crawl.template.utils.ShellUtils;
import com.isoftstone.crawl.template.vo.DispatchVo;
import com.isoftstone.crawl.template.vo.Runmanager;

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

    /**
     * 爬虫增量.
     * @param dispatchName
     */
    public void crawlIncrement(String dispatchName) {
        String rootFolder = "/nutch_seeds";
        String shDir = "/nutch/local_incremental/bin/crawl";
        String proxyShDir = "/nutch/local_incremental_proxy/bin/crawl";
        String crawlDir = "/nutch_data/";
        String solrURL = "http://192.168.100.31:8080/solr/collection1/";
        String depth = "2";
        DispatchVo dispatchVo = RedisOperator.getDispatchResult(dispatchName,
            WebtoolConstants.DISPATCH_REDIS_DBINDEX);
        boolean userProxy = dispatchVo.isUserProxy();
        if (userProxy) {
            shDir = proxyShDir;
        }
        String folderNameSeed = dispatchName.substring(0,
            dispatchName.lastIndexOf("_"));
        String folderNameData = folderNameSeed.substring(0,
            folderNameSeed.lastIndexOf("_"));
        String[] folderNameStrs = folderNameSeed.split("_");
        folderNameSeed = folderNameStrs[0] + "_" + folderNameStrs[1] + "_"
                + WebtoolConstants.INCREMENT_FILENAME_SIGN + "_"
                + folderNameStrs[2];
        folderNameData = folderNameData.substring(0,
            folderNameData.lastIndexOf("_"))
                + "_" + WebtoolConstants.INCREMENT_FILENAME_SIGN;
        String seedFolder = rootFolder + File.separator + folderNameSeed;
        String command = shDir + " " + seedFolder + " " + crawlDir
                + folderNameData + "_data" + " " + solrURL + " " + depth;
        Runmanager runmanager = getRunmanager(command);
        ShellUtils.execCmd(runmanager);
    }

    /**
     * 爬虫全量.
     * @param dispatchName
     */
    public void crawlFull(String dispatchName) {
        String rootFolder = "/nutch_seeds";
        String shDir = "/nutch/deploy_normal/bin/crawl";
        String proxyShDir = "/nutch/deploy_normal_proxy/bin/crawl";
        String crawlDir = "/nutch_data/";
        String solrURL = "http://192.168.100.31:8080/solr/collection1/";
        String depth = "3";
        DispatchVo dispatchVo = RedisOperator.getDispatchResult(dispatchName,
            WebtoolConstants.DISPATCH_REDIS_DBINDEX);
        boolean userProxy = dispatchVo.isUserProxy();
        if (userProxy) {
            shDir = proxyShDir;
        }
        String folderNameSeed = dispatchName.substring(0,
            dispatchName.lastIndexOf("_"));
        String folderNameData = folderNameSeed.substring(0,
            folderNameSeed.lastIndexOf("_"));
        String seedFolder = rootFolder + File.separator + folderNameSeed;
        String command = shDir + " " + seedFolder + " " + crawlDir
                + folderNameData + "_data" + " " + solrURL + " " + depth;
        Runmanager runmanager = getRunmanager(command);
        ShellUtils.execCmd(runmanager);
    }

    /**
     * 重新索引.
     * @param dispatchName
     */
    public void reParse(String dispatchName, String model) {
        String nutch_root = "";
        String solr_index = "http://192.168.100.31:8080/solr/collection1/";
        String crawlDir = "/nutch_data/";
        String folderNameSeed = dispatchName.substring(0,
            dispatchName.lastIndexOf("_"));
        String folderNameData = folderNameSeed.substring(0,
            folderNameSeed.lastIndexOf("_"));
        String data_folder = crawlDir + folderNameData + "_data";
        if ("local".equals(model)) {
            nutch_root = "/nutch/local_incremental/bin/crawl";
        } else if ("deploy".equals(model)) {
            nutch_root = "/nutch/deploy_normal/bin/crawl";
        }
        ReParseAndIndex.reParseAndIndex(nutch_root, data_folder, solr_index,
            false);
    }

    /**
     * 停止爬虫.
     * @param dispatchName
     */
    public void stopCrawl(String dispatchName) {
        
    }

    private Runmanager getRunmanager(String command) {
        Runmanager runmanager = new Runmanager();
        runmanager.setHostIp(Config
                .getValue(WebtoolConstants.KEY_NUTCH_HOST_IP));
        runmanager.setUsername(Config
                .getValue(WebtoolConstants.KEY_NUTCH_HOST_USERNAME));
        runmanager.setPassword(Config
                .getValue(WebtoolConstants.KEY_NUTCH_HOST_PASSWORD));
        runmanager.setPort(22);
        runmanager.setCommand(command);
        return runmanager;
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
