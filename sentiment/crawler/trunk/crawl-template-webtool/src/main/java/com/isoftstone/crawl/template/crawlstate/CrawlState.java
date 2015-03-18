/*
 * @(#)CrawlStateUtil.java 2015-3-10 下午2:39:09 crawl-template-webtool Copyright
 * 2015 Isoftstone, Inc. All rights reserved. ISOFTSTONE
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.isoftstone.crawl.template.crawlstate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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
import com.isoftstone.crawl.template.vo.Seed;
import com.isoftstone.crawl.template.webtool.CrawlToolResource;

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
    public void crawlIncrement(String folderName) {
        String rootFolder = "/nutch_seeds";
        String shDir = "/nutch/local_incremental/bin/crawl";
        String proxyShDir = "/nutch/local_incremental_proxy/bin/crawl";
        String crawlDir = "/nutch_data/";
        String solrURL = "http://192.168.100.31:8080/solr/collection1/";
        String depth = "2";
        String dispatchName = folderName + WebtoolConstants.DISPATCH_REIDIS_POSTFIX;
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
    public void crawlFull(String folderName) {
        String rootFolder = "/nutch_seeds";
        String shDir = "/nutch/deploy_normal/bin/crawl";
        String proxyShDir = "/nutch/deploy_normal_proxy/bin/crawl";
        String crawlDir = "/nutch_data/";
        String solrURL = "http://192.168.100.31:8080/solr/collection1/";
        String depth = "3";
        String dispatchName = folderName + WebtoolConstants.DISPATCH_REIDIS_POSTFIX;
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
    public void reParse(String folderNameSeed, String model) {
        String nutch_root = "";
        String solr_index = "http://192.168.100.31:8080/solr/collection1/";
        String crawlDir = "/nutch_data/";
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
    public void stopCrawl(String folderName) {
        // 1.修改redis中种子状态
        String redisKey = folderName + WebtoolConstants.DISPATCH_REIDIS_POSTFIX;
        DispatchVo dispatchVo = RedisOperator.getDispatchResult(redisKey,
            WebtoolConstants.DISPATCH_REDIS_DBINDEX);
        if (dispatchVo == null) {
            return;
        }
        List<Seed> seedList = dispatchVo.getSeed();
        if (seedList == null) {
            return;
        }
        for (Iterator<Seed> it = seedList.iterator(); it.hasNext();) {
            Seed seed = it.next();
            seed.setIsEnabled("false");
        }
        RedisOperator.setDispatchResult(dispatchVo, redisKey,
            WebtoolConstants.DISPATCH_REDIS_DBINDEX);
        // 2.修改文件中 种子状态.
        String[] folderNameArr = folderName.split("_");
        String domain = folderNameArr[0];
        String period = folderNameArr[1];
        String sequence = folderNameArr[2];
        // 2.1 修改模板种子状态.
        contextToFile(folderName);
        // 2.2 修改增量种子状态.
        String incrementFolderName = domain + "_" + "1" + period + "_"
                + WebtoolConstants.INCREMENT_FILENAME_SIGN + "_" + sequence;
        contextToFile(incrementFolderName);
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

    private void contextToFile(String folderName) {
        String folderRoot = Config.getValue(WebtoolConstants.FOLDER_NAME_ROOT);
        String filePath = folderRoot + File.separator + folderName
                + File.separator + WebtoolConstants.SEED_FILE_NAME;
        String str = null; // 原有txt内容
        StringBuffer strBuf = new StringBuffer();// 内容更新
        BufferedReader input = null;
        BufferedWriter output = null;
        try {
            File f = new File(filePath);
            File parentDir = f.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                return;
            }
            if (!f.exists()) {
                return;
            } else {
                input = new BufferedReader(new FileReader(f));
                List<String> fileSeedList = new ArrayList<String>();
                while ((str = input.readLine()) != null) {
                    fileSeedList.add(str);
                }
                input.close();

                // --写入未包含到本次种子中的历史数据.
                for (int i = 0; i < fileSeedList.size(); i++) {
                    String tempStr = fileSeedList.get(i);
                    strBuf.append("#" + tempStr
                            + System.getProperty("line.separator"));
                }

            }
            output = new BufferedWriter(new FileWriter(f));
            output.write(strBuf.toString());
            output.close();
            String isCopy = Config.getValue(WebtoolConstants.KEY_IS_COPYFOLDER);
            if ("true".equals(isCopy)) {
                CrawlToolResource.putSeedsFolder(folderName, "local");
            }
            //HdfsCommon.upFileToHdfs(filePath);
            CrawlToolResource.putSeedsFolder(folderName, "deploy");
        } catch (Exception e) {
            LOG.error("生成文件错误.", e);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
                if (output != null) {
                    output.close();
                }
            } catch (IOException e) {
                LOG.error("关闭流异常.", e);
            }
        }
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
