/*
 * @(#)WebtoolConstants.java 2015-1-7 下午1:59:48 crawl-template-webtool Copyright
 * 2015 Isoftstone, Inc. All rights reserved. ISOFTSTONE
 * PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.isoftstone.crawl.template.consts;

/**
 * WebtoolConstants
 * @author danhb
 * @date  2015-1-7
 * @version 1.0
 *
 */
public class WebtoolConstants {
    /**
     * config.propertis中文件根目录的key.
     */
    public static final String FOLDER_NAME_ROOT = "FOLDER_NAME_ROOT";

    public static final String URL_STATUS_FALSE = "false";
    
    public static final String DISPATCH_REIDIS_POSTFIX = "_dispatch";
    
    public static final String DISPATCH_STATIS_START = "start";
    
    public static final String SEED_FILE_NAME = "seed.txt";
    
    public static final Integer DISPATCH_REDIS_DBINDEX = 2;
    
    public static final String INCREASE_TEMPLATE_PARTERN =  "_increasetemplate";  
    
    public static final String KEY_HDFS_ROOT_FOLDER = "hdfsRootFolder";

    public static final String KEY_IS_COPYFOLDER = "isCopyFile";
    
    public static final String KEY_HOST_IP = "hostip";
    public static final String KEY_HOST_USERNAME = "hostUserName";
    public static final String KEY_HOST_PASSWORD = "hostPassword";
    public static final String KEY_DES_FOLDER = "desFolderName";
    
    public static final String BAIDU_SEARCH_NAME="百度新闻搜索";
    public static final String BING_SEARCH_NAME="Bing新闻搜索";
    public static final String SOUGOU_SEARCH_NAME="搜狗新闻搜索";
    
    public static final String BAIDU_NEWS_ENGINE="baidu_news";
    public static final String BING_NEWS_ENGINE="bing_news";
    public static final String SOUGOU_NEWS_ENGINE="sogou_news";
    
    public static final String SEARCH_KEYWORD_API_URL="SEARCH_KEYWORD_API_URL";
    
    //模板列表文件后缀
    public static final String TEMPLATE_LIST_KEY_PARTERN = "_templatelist";
    //模板导出文件后缀
    public static final String TEMPLATE_FILE_EXTENTIONS_NAME =  ".txt";
    
    //-- 增量文件夹命名标识.
  	public static final String INCREMENT_FILENAME_SIGN = "increment";
}
