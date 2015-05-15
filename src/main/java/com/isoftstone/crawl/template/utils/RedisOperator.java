package com.isoftstone.crawl.template.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.alibaba.fastjson.JSON;
import com.isoftstone.crawl.template.global.Constants;
import com.isoftstone.crawl.template.impl.ParseResult;
import com.isoftstone.crawl.template.impl.TemplateFactory;
import com.isoftstone.crawl.template.impl.TemplateResult;
import com.isoftstone.crawl.template.model.TemplateModel;
import com.isoftstone.crawl.template.vo.DispatchVo;

public class RedisOperator {

	private static final Log LOG = LogFactory.getLog(RedisOperator.class);

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
			jedis.select(Constants.DEFAULT_REDIS_DBINDEX);
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
	 * 添加到缓存库
	 * */
	public static void setToCacheDB(String key, String value) {
		JedisPool pool = null;
		Jedis jedis = null;
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.select(Constants.CACHE_REDIS_DBINDEX);
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
			jedis.select(Constants.INCREASE_REDIS_DBINDEX);
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
			jedis.select(Constants.DEFAULT_REDIS_DBINDEX);
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
	public static List<TemplateModel> getFromDefaultDB(List<String> keys){
		JedisPool pool = null;
		Jedis jedis = null;
		List<TemplateModel> result=new ArrayList<TemplateModel>();
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.select(Constants.DEFAULT_REDIS_DBINDEX);
			List<String> values = jedis.mget(keys.toArray(new String[0]));
			for(String va:values)
				result.add(JSON.parseObject(va,TemplateModel.class));
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		return result;
	}

	/**
	 * 
	 * 从缓存库库中取值
	 * */
	public static String getFromCacheDB(String key) {
		JedisPool pool = null;
		Jedis jedis = null;
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.select(Constants.CACHE_REDIS_DBINDEX);
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
			jedis.select(Constants.INCREASE_REDIS_DBINDEX);
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
			jedis.select(Constants.DEFAULT_REDIS_DBINDEX);
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
			jedis.select(Constants.INCREASE_REDIS_DBINDEX);
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
    public static long delFromDispatchDB(final String... keys) {
        JedisPool pool = null;
        Jedis jedis = null;
        try {
            pool = RedisUtils.getPool();
            jedis = pool.getResource();
            jedis.select(Constants.DISPATCH_REDIS_DBINDEX);
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
			jedis.select(Constants.DEFAULT_REDIS_DBINDEX);
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
	public static void saveTemplateToDefaultDB(TemplateResult templateResult, String templateGuid) {
		RedisUtils.setTemplateResult(templateResult, templateGuid, Constants.DEFAULT_REDIS_DBINDEX);
	}

	/**
	 * 
	 * 保存模板到增量库
	 * */
	public static void saveTemplateToIncreaseDB(TemplateResult templateResult, String templateGuid) {
		RedisUtils.setTemplateResult(templateResult, templateGuid, Constants.INCREASE_REDIS_DBINDEX);
	}

	/**
	 * 
	 * 保存ParseResult到标准库
	 * */
	public static ParseResult getParseResultFromDefaultDB(byte[] input, String encoding, String url) {
		ParseResult parseResult = TemplateFactory.process(input, encoding, url, Constants.DEFAULT_REDIS_DBINDEX);
		return parseResult;
	}

	/**
	 * 
	 * 保存ParseResult到标准库
	 * */
	public static ParseResult getParseResultFromIncreaseDB(byte[] input, String encoding, String url) {
		ParseResult parseResult = TemplateFactory.process(input, encoding, url, Constants.INCREASE_REDIS_DBINDEX);
		return parseResult;
	}

	/**
	 * 
	 * 从标准库中获取模板
	 * */
	public static TemplateResult getTemplateResultFromDefaultDB(String templateGuid) {
		TemplateResult templateResult = RedisUtils.getTemplateResult(templateGuid, Constants.DEFAULT_REDIS_DBINDEX);
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

	public  static List<DispatchVo> getDispatchResult(List<String> guid,int dbIndex){
		JedisPool pool = null;
		Jedis jedis = null;
		List<DispatchVo> result= new ArrayList<>();

		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.select(dbIndex);
			List<String> json = jedis.mget(guid.toArray(new String[0]));
			if (json != null) {
				for(String js : json)
					result.add(JSON.parseObject(js, DispatchVo.class));
			}
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			LOG.error("get dispatch result from redis failed", e);
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		return result;
	}

	public static void setDispatchResult(DispatchVo dispatchVo, String guid, int dbindex) {
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
