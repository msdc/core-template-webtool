package com.isoftstone.crawl.template.utils;

import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.alibaba.fastjson.JSON;
import com.isoftstone.crawl.template.impl.ParseResult;
import com.isoftstone.crawl.template.impl.TemplateFactory;
import com.isoftstone.crawl.template.impl.TemplateResult;
import com.isoftstone.crawl.template.vo.DispatchVo;

public class RedisOperator {

    private static final Log LOG = LogFactory.getLog(RedisOperator.class);

    // 增量模板库
    public static final int INCREASE_DBINDEX = 1;

    // 常规模板库
    public static final int DEFAULT_DBINDEX = 0;

    /**
     * 
     * 添加到标准模板
     * */
    public static void setToDefaultDB(String key, String value) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(DEFAULT_DBINDEX);
            jedis.set(key, value);
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            e.printStackTrace();
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
    }

    /**
     * 
     * 添加到模板
     * */
    public static void setToIncreaseDB(String key, String value) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(INCREASE_DBINDEX);
            jedis.set(key, value);
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            e.printStackTrace();
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
    }

    /**
     * 
     * 从标准模板库中取值
     * */
    public static String getFromDefaultDB(String key) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(DEFAULT_DBINDEX);
            String value = jedis.get(key);
            return value;
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            e.printStackTrace();
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
        return "";
    }

    /**
     * 
     * 从增量模板库中取值
     * */
    public static String getFromIncreaseDB(String key) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(INCREASE_DBINDEX);
            String value = jedis.get(key);
            return value;
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            e.printStackTrace();
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
        return "";
    }

    /**
     * 
     * 从标准模板库中删除
     * */
    public static long delFromDefaultDB(final String... keys) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(DEFAULT_DBINDEX);
            return jedis.del(keys);
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            e.printStackTrace();
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
        return -1;
    }

    /**
     * 
     * 从增量板库中删除
     * */
    public static long delFromIncreaseDB(final String... keys) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(INCREASE_DBINDEX);
            return jedis.del(keys);
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            e.printStackTrace();
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
        return -1;
    }

    /**
     * 
     * 从标准库中查询key
     * */
    public static Set<String> searchKeysFromDefaultDB(final String pattern) {
        JedisPool pool = null;
        Jedis jedis = null;
        Set<String> listKeys = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(DEFAULT_DBINDEX);
            listKeys = jedis.keys(pattern);
            return listKeys;
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            e.printStackTrace();
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
        return listKeys;

    }

    /**
     * 
     * 保存模板到标准库
     * */
    public static void saveTemplateToDefaultDB(TemplateResult templateResult,
            String templateGuid) {
        RedisUtils.setTemplateResult(templateResult, templateGuid,
            DEFAULT_DBINDEX);
    }

    /**
     * 
     * 保存模板到增量库
     * */
    public static void saveTemplateToIncreaseDB(TemplateResult templateResult,
            String templateGuid) {
        RedisUtils.setTemplateResult(templateResult, templateGuid,
            INCREASE_DBINDEX);
    }

    /**
     * 
     * 保存ParseResult到标准库
     * */
    public static ParseResult getParseResultFromDefaultDB(byte[] input,
            String encoding, String url) {
        ParseResult parseResult = TemplateFactory.process(input, encoding, url,
            DEFAULT_DBINDEX);
        return parseResult;
    }

    /**
     * 
     * 保存ParseResult到标准库
     * */
    public static ParseResult getParseResultFromIncreaseDB(byte[] input,
            String encoding, String url) {
        ParseResult parseResult = TemplateFactory.process(input, encoding, url,
            INCREASE_DBINDEX);
        return parseResult;
    }

    /**
     * 
     * 从标准库中获取模板
     * */
    public static TemplateResult getTemplateResultFromDefaultDB(
            String templateGuid) {
        TemplateResult templateResult = RedisUtils.getTemplateResult(
            templateGuid, DEFAULT_DBINDEX);
        return templateResult;
    }

    public static DispatchVo getDispatchResult(String guid, int dbindex) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(dbindex);
            String json = jedis.get(guid);
            if (json != null)
                return JSON.parseObject(json, DispatchVo.class);
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            LOG.error("get dispatch result from redis failed", e);
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
        return null;
    }

    public static void setDispatchResult(DispatchVo dispatchVo, String guid,
            int dbindex) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            StringBuilder str = new StringBuilder();
            str.append(JSON.toJSONString(dispatchVo));
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(dbindex);
            jedis.set(guid, str.toString());
        } catch (Exception e) {
            pool.returnBrokenResource(jedis);
            LOG.error("save dispatch result to redis failed", e);
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
    }
}
