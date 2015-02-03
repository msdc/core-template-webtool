package com.isoftstone.crawl.template.webtool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.alibaba.fastjson.JSON;
import com.isoftstone.crawl.template.consts.WebtoolConstants;
import com.isoftstone.crawl.template.global.Constants;
import com.isoftstone.crawl.template.impl.ParseResult;
import com.isoftstone.crawl.template.impl.Selector;
import com.isoftstone.crawl.template.impl.SelectorFilter;
import com.isoftstone.crawl.template.impl.SelectorFormat;
import com.isoftstone.crawl.template.impl.SelectorIndexer;
import com.isoftstone.crawl.template.impl.TemplateFactory;
import com.isoftstone.crawl.template.impl.TemplateResult;
import com.isoftstone.crawl.template.model.BasicInfoViewModel;
import com.isoftstone.crawl.template.model.CustomerAttrModel;
import com.isoftstone.crawl.template.model.PageModel;
import com.isoftstone.crawl.template.model.ResponseJSONProvider;
import com.isoftstone.crawl.template.model.ScheduleDispatchViewModel;
import com.isoftstone.crawl.template.model.TemplateIncreaseViewModel;
import com.isoftstone.crawl.template.model.TemplateModel;
import com.isoftstone.crawl.template.model.TemplateTagModel;
import com.isoftstone.crawl.template.utils.Config;
import com.isoftstone.crawl.template.utils.DownloadHtml;
import com.isoftstone.crawl.template.utils.HdfsCommon;
import com.isoftstone.crawl.template.utils.MD5Utils;
import com.isoftstone.crawl.template.utils.RedisOperator;
import com.isoftstone.crawl.template.utils.RedisUtils;
import com.isoftstone.crawl.template.utils.ShellUtils;
import com.isoftstone.crawl.template.utils.TemplateModelComparator;
import com.isoftstone.crawl.template.vo.DispatchVo;
import com.isoftstone.crawl.template.vo.Runmanager;
import com.isoftstone.crawl.template.vo.Seed;

/**
 * 
 * 爬虫工具restful-services服务类
 * */
@Path("crawlToolResource")
public class CrawlToolResource {
	// 列表的key的后缀
	public static final String key_partern = "_templatelist";
	// 文件扩展名
	public static final String file_extensionName = ".txt";
	//-- 增量文件夹命名标识.
	public static final String INCREMENT_FILENAME_SIGN = "increment";
	
	private static final Log LOG = LogFactory.getLog(CrawlToolResource.class);

	/**
	 * 保存到本地文件
	 * */
	@POST
	@Path("/saveToLocalFile")
	@Produces(MediaType.TEXT_PLAIN)
	public String saveToLocalFile(@DefaultValue("") @FormParam("data") String data) {
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		PageModel pageModel = GetPageModelByJsonString(data);
		String domain = pageModel.getScheduleDispatchViewModel().getDomain();
//		String period = pageModel.getScheduleDispatchViewModel().getPeriod();
		String period = pageModel.getTemplateIncreaseViewModel().getPeriod();
		String sequence = pageModel.getScheduleDispatchViewModel().getSequence();
		boolean userProxy = pageModel.getScheduleDispatchViewModel().getUseProxy();
		if (sequence == null || sequence.equals("")) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("保存失败，请输入时序.");	
			return jsonProvider.toJSON();
		}
		String folderName = domain + "_" + "1" + period + "_" + sequence;
		String incrementFolderName = domain + "_" + "1" + period + "_" + INCREMENT_FILENAME_SIGN + "_" + sequence;
		ParseResult parseResult = saveTemplateAndParseResult(pageModel);
		if (parseResult == null) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("请先保存模板!再执行此操作!");
			return jsonProvider.toJSON();
		}
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		String redisKey = templateGuid + key_partern;
		TemplateModel templateModel = getTemplateModel(redisKey);
		String status = templateModel.getStatus();
		
		//--增量相关.
		String incrementPageCountStr = pageModel.getTemplateIncreaseViewModel().getPageCounts();
		if(incrementPageCountStr == null || "".equals(incrementPageCountStr)) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("保存失败，请输入增量需要爬取的页数");
			return jsonProvider.toJSON();			
		}
		int incrementPageCount = Integer.valueOf(incrementPageCountStr);
		if(incrementPageCount > 0) {
		    incrementPageCount = incrementPageCount - 1;
		}
		String pageSort = pageModel.getTemplateIncreaseViewModel().getPageSort();
        ArrayList<String> seedsTemp = TemplateFactory.getPaginationOutlink(parseResult);
        ArrayList<String> seeds = new ArrayList<String>();
        seeds.add(templateUrl);
        if("升序".equals(pageSort)) {
            for(int i = 0; i < incrementPageCount && i < seedsTemp.size(); i++) {
                seeds.add(seedsTemp.get(i));
            }
        }else {
            for(int i = seedsTemp.size(); i > 0 && incrementPageCount > 0; i--, incrementPageCount--) {
                seeds.add(seedsTemp.get(i));
            }
        }
		saveSeedsValueToFile(folderName, incrementFolderName, templateUrl, seeds, status, userProxy);
		jsonProvider.setSuccess(true);
		jsonProvider.setData("文件保存成功!");
		return jsonProvider.toJSON();
	}

	/**
	 * 保存种子到本地文件. 并将文件夹相关信息存入redis.
	 */
	private void saveSeedsValueToFile(String folderName, String incrementFolderName, 
	        String templateUrl, List<String> seeds, String status, boolean userProxy) {
		// --1.1 保存模板url到本地文件.
	    List<String> templateList = new ArrayList<String>();
	    templateList.add(templateUrl);
		contentToTxt(folderName, templateList, status);
		// --1.2 保存增量种子到本地文件.
		contentToTxt(incrementFolderName, seeds, status);

		// --2.保存到redis中.
		String redisKey = folderName + WebtoolConstants.DISPATCH_REIDIS_POSTFIX;
		
		DispatchVo dispatchVo = getDispatchResult(redisKey, WebtoolConstants.DISPATCH_REDIS_DBINDEX);
		if(dispatchVo == null) {
		    dispatchVo = new DispatchVo();
		}
		dispatchVo.setStatus(WebtoolConstants.DISPATCH_STATIS_START);
		dispatchVo.setUserProxy(userProxy);
		List<Seed> seedList = dispatchVo.getSeed();
		if(seedList == null) {
		    seedList = new ArrayList<Seed>();
		}
		for (Iterator<String> it = seeds.iterator(); it.hasNext();) {
			String seedStr = it.next();
			Seed seed = new Seed(seedStr, status);
			if(seedList.contains(seed)) {
			    //-- 更新seed的status.
			    seedList.remove(seed);
			}
			seedList.add(seed);
		}
		dispatchVo.setSeed(seedList);
		setDispatchResult(dispatchVo, redisKey, WebtoolConstants.DISPATCH_REDIS_DBINDEX);
	}

    public DispatchVo getDispatchResult(String guid, int dbindex) {
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
            LOG.error("", e);
        } finally {
            RedisUtils.returnResource(pool, jedis);
        }
        return null;
    }

	private void setDispatchResult(DispatchVo dispatchVo, String guid, int dbindex) {
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
			LOG.error("", e);
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
	}

	public TemplateModel getTemplateModel(String guid) {
		JedisPool pool = null;
		Jedis jedis = null;
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			String json = jedis.get(guid);
			if (json != null) {
				return GetTemplateModel(json);
			}
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			LOG.error("", e);
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		return null;
	}

	/**
	 * 
	 * 保存内容到文件
	 * */
	private void contentToTxt(String folderName, List<String> seeds, String status) {
		String folderRoot = Config.getValue(WebtoolConstants.FOLDER_NAME_ROOT);
		String filePath = folderRoot + File.separator + folderName + File.separator + WebtoolConstants.SEED_FILE_NAME;
		String str = null; // 原有txt内容
		StringBuffer strBuf = new StringBuffer();// 内容更新
		BufferedReader input = null;
		BufferedWriter output = null;
		try {
			File f = new File(filePath);
			File parentDir = f.getParentFile();
			if (parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs();
			}
			if (!f.exists()) {
				f.createNewFile(); // 不存在则创建
			} else {
				input = new BufferedReader(new FileReader(f));

				while ((str = input.readLine()) != null) {
					String temp = str;
					if (str.startsWith("#")) {
						temp = str.substring(1, str.length());
					}
					if (!seeds.contains(temp)) {
						strBuf.append(str + System.getProperty("line.separator"));
					}
				}
				input.close();
			}
			for (Iterator<String> it = seeds.iterator(); it.hasNext();) {
				String seedStr = it.next();
				if (WebtoolConstants.URL_STATUS_FALSE.equals(status)) {
					strBuf.append("#");
				}
				strBuf.append(seedStr + System.getProperty("line.separator"));
			}
			output = new BufferedWriter(new FileWriter(f));
			output.write(strBuf.toString());
			output.close();
			String isCopy = Config.getValue(WebtoolConstants.KEY_IS_COPYFOLDER);
			if("true".equals(isCopy)) {
			    putSeedsFolder(folderName, "local");
			}
			HdfsCommon.upFileToHdfs(filePath);
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

	private static void putSeedsFolder(String folderName, String type) {
		LOG.info("进入方法");
		String hostIp = Config.getValue(WebtoolConstants.KEY_HOST_IP);
		String userName = Config.getValue(WebtoolConstants.KEY_HOST_USERNAME);
		String password = Config.getValue(WebtoolConstants.KEY_HOST_PASSWORD);
		Runmanager runmanager = new Runmanager();
		runmanager.setHostIp(hostIp);
		runmanager.setUsername(userName);
        runmanager.setPassword(password);
        runmanager.setPort(22);
        String folderRoot = Config.getValue(WebtoolConstants.FOLDER_NAME_ROOT);
        LOG.info("文件根目录" + folderRoot);
        String command = "";
        if ("local".equals(type)) {
            //String folderPath = folderRoot + "/" + folderName;
            //new SFTPUtils().copyFile(runmanager, folderPath, folderPath);
            String desCopyRootFolder = Config.getValue(WebtoolConstants.KEY_DES_FOLDER);
            command = "scp -r " + folderRoot + File.separator + folderName
                    + " " + desCopyRootFolder;
        } else {
            // FIXME:集群模式，执行的命令.
        }
        LOG.info("命令：" + command);
        runmanager.setCommand(command);
        ShellUtils.execCmd(runmanager);
	}

	/**
	 * 验证内容页
	 * */
	@POST
	@Path("/verifyNewContent")
	@Produces(MediaType.TEXT_PLAIN)
	public String VerifyNewContent(@DefaultValue("") @FormParam("data") String data) {
		ResponseJSONProvider<ParseResult> jsonProvider=new ResponseJSONProvider<ParseResult>();
		PageModel pageModel = GetPageModelByJsonString(data);
		ParseResult parseResult = saveParseResult(pageModel);		
		if (parseResult == null) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("请先保存模板!再执行此操作!");		
			return jsonProvider.toJSON();
		}
		// 获取内容页链接
		ArrayList<String> contentOutLinkArrayList = TemplateFactory.getContentOutlink(parseResult);
		if (contentOutLinkArrayList.size() == 0) {
			jsonProvider.setSuccess(false);
		 	jsonProvider.setErrorMsg("列表外链接配置信息不正确！");
		 	return jsonProvider.toJSON();
		}
		int contentOutLinkIndex=getRandomNumber(0, contentOutLinkArrayList.size()-1);
		String contentOutLink = contentOutLinkArrayList.get(contentOutLinkIndex);
		byte[] input = DownloadHtml.getHtml(contentOutLink);
		String encoding = sniffCharacterEncoding(input);
		try {
			parseResult = RedisOperator.getParseResultFromDefaultDB(input, encoding, contentOutLink);
			jsonProvider.setSuccess(true);
			jsonProvider.setData(parseResult);
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("redis操作异常！");
			e.printStackTrace();
		}			
		
		return jsonProvider.toJSON();
	}

	/**
	 * 验证列表页
	 * */
	@POST
	@Path("/verifyListContent")
	@Produces(MediaType.TEXT_PLAIN)
	public String VerifyListContent(@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = GetPageModelByJsonString(data);
		ResponseJSONProvider<ParseResult> jsonProvider=new ResponseJSONProvider<ParseResult>();
		ParseResult parseResult = saveParseResult(pageModel);
		if (parseResult == null) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("请先保存模板!再执行此操作!");			
		}else{
			jsonProvider.setSuccess(true);
			jsonProvider.setData(parseResult);
		}
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 测试模板主方法
	 * */
	@POST
	@Path("/getJSONString")
	@Produces(MediaType.TEXT_PLAIN)
	public String GetJSONString(@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = GetPageModelByJsonString(data);
		ResponseJSONProvider<TemplateResult> jsonProvider=new ResponseJSONProvider<TemplateResult>();
		TemplateResult templateResult = GetTemplateResult(pageModel);
		jsonProvider.setSuccess(true);
		jsonProvider.setData(templateResult);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 保存模板的主方法
	 * */
	@POST
	@Path("/saveTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String SaveTemplate(@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = GetPageModelByJsonString(data);
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		saveTemplateResultToRedis(pageModel);
		jsonProvider.setSuccess(true);
		jsonProvider.setData("模板保存成功!");
		return jsonProvider.toJSON();
	}
	
	/**
	 * 
	 * 保存增量模板
	 * */
	@POST
	@Path("/saveIncreaseTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String SaveIncreaseTemplate(@DefaultValue("") @FormParam("data") String data){
		PageModel pageModel = GetPageModelByJsonString(data);
		ResponseJSONProvider<String> jsonProvider=saveIncreaseTemplateResult(pageModel);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 查看HTML内容按钮
	 * */
	@POST
	@Path("/viewHtmlContent")
	@Produces(MediaType.TEXT_PLAIN)
	public String viewHtmlContent(@DefaultValue("") @FormParam("webUrl") String webUrl) {
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		String htmlContent="";
		try {
			byte[] input = DownloadHtml.getHtml(webUrl);
			String encoding = sniffCharacterEncoding(input);
			htmlContent = DownloadHtml.getHtml(webUrl, encoding);	
			jsonProvider.setSuccess(true);
			jsonProvider.setData(htmlContent);
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("无法打开此网站！");
			e.printStackTrace();
		}
			
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 删除模板
	 * */
	@POST
	@Path("/deleteTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String DeleteTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl) {
		String templateGuid = MD5Utils.MD5(templateUrl);
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		jsonProvider.setSuccess(true);
		jsonProvider.setData("删除成功！");
		long effectCounts = -1;
		
		//先删除增量模板
		String jsonString=RedisOperator.getFromDefaultDB(templateGuid + key_partern);
		TemplateModel templateModel = GetTemplateModel(jsonString);
		List<String> increaseTemplateIdList = templateModel
				.getTemplateIncreaseIdList();
		if (increaseTemplateIdList != null) {
			for (String increaseTemplateId : increaseTemplateIdList) {
				effectCounts = RedisOperator
						.delFromIncreaseDB(increaseTemplateId);
				if (effectCounts < 0) {
					jsonProvider.setSuccess(false);
					jsonProvider.setErrorMsg("删除模板失败");
					jsonProvider.setData(null);
				}
			}
		}

		//最后删除模板列表
		effectCounts = RedisOperator.delFromDefaultDB(
				(templateGuid + key_partern), templateGuid);
		if (effectCounts < 0) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("删除模板失败");
			jsonProvider.setData("");
		}	
		
		
		
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 修改模板
	 * */
	@POST
	@Path("/updateTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String UpdateTemplate(@DefaultValue("") @FormParam("templateGuid") String templateGuid) {
	    ResponseJSONProvider<TemplateResult> jsonProvider=new ResponseJSONProvider<TemplateResult>();
		TemplateResult templateResult =RedisOperator.getTemplateResultFromDefaultDB(templateGuid);
		jsonProvider.setSuccess(true);
		jsonProvider.setData(templateResult);		
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 修改模板
	 * */
	@POST
	@Path("/getTemplateGuid")
	@Produces(MediaType.TEXT_PLAIN)
	public String getTemplateGuid(@DefaultValue("") @FormParam("templateUrl") String templateUrl) {
		String templateGuid = MD5Utils.MD5(templateUrl);
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		jsonProvider.setSuccess(true);
		jsonProvider.setData(templateGuid);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 获取所有的模板列表
	 * */
	@GET
	@Path("/getTemplateList")
	@Produces(MediaType.TEXT_PLAIN)
	public String GetTemplateList() {
		ResponseJSONProvider<TemplateList> jsonProvider=new ResponseJSONProvider<TemplateList>();
		jsonProvider.setSuccess(true);
		JedisPool pool = null;
		Jedis jedis = null;
		TemplateList templateList = new TemplateList();
		List<TemplateModel> templateListArrayList = new ArrayList<TemplateModel>();
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.select(RedisOperator.DEFAULT_DBINDEX);
			Set<String> listKeys = jedis.keys("*" + key_partern);
			if(listKeys!=null){
				for (String key : listKeys) {
					String templateString = jedis.get(key);
					TemplateModel templateModel = GetTemplateModel(templateString);
					templateListArrayList.add(templateModel);
				}
			}			
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("Redis操作异常！");
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		//列表按名称排序
        Collections.sort(templateListArrayList, new TemplateModelComparator());		 
		templateList.setTemplateList(templateListArrayList);
		jsonProvider.setData(templateList);	
		jsonProvider.setTotal(templateListArrayList.size());
		return jsonProvider.toJSON();
	}
	
	/**
	 * 
	 * 搜索模板
	 * */
	@POST
	@Path("/searchTemplateList")
	@Produces(MediaType.TEXT_PLAIN)
	public String SearchTemplateList(
			@DefaultValue("") @FormParam("searchString") String searchString) {
		ResponseJSONProvider<TemplateList> jsonProvider=new ResponseJSONProvider<TemplateList>();
		jsonProvider.setSuccess(true);
		if(searchString.equals("启用")){
			searchString="true";
		}else if(searchString.equals("停用")){
			searchString="false";
		}
		JedisPool pool = null;
		Jedis jedis = null;		
		TemplateList templateList = new TemplateList();
		List<TemplateModel> templateListArrayList = new ArrayList<TemplateModel>();
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.select(RedisOperator.DEFAULT_DBINDEX);
			Set<String> listKeys = jedis.keys("*" + key_partern);
			if(listKeys!=null){
				for (String key : listKeys) {
					String templateString = jedis.get(key);
					if(templateString.contains(searchString)){
						TemplateModel templateModel = GetTemplateModel(templateString);
						templateListArrayList.add(templateModel);
					}
				}
			}			
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("Redis操作异常！");
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}	
		//列表按名称排序
		Collections.sort(templateListArrayList, new TemplateModelComparator());	
		templateList.setTemplateList(templateListArrayList);		
		jsonProvider.setData(templateList);	
		jsonProvider.setTotal(templateListArrayList.size());
		return jsonProvider.toJSON();
	}
	/**
	 * 
	 * 停用模板
	 * */
	@POST
	@Path("/disableTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String DisableTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl, @DefaultValue("") @FormParam("name") String name) {
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		setTemplateStatus(templateUrl, name, "false");
		jsonProvider.setSuccess(true);
		jsonProvider.setData("操作成功！");
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 启用模板
	 * */
	@POST
	@Path("/enableTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String EnableTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl, @DefaultValue("") @FormParam("name") String name) {
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		setTemplateStatus(templateUrl, name, "true");
		jsonProvider.setSuccess(true);
		jsonProvider.setData("操作成功！");
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 获取模板列表中单个模板对象
	 * */
	@POST
	@Path("/getSingleTemplateModel")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSingleTemplateModel(@DefaultValue("") @FormParam("templateGuid") String templateGuid) {	
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		String json = RedisOperator.getFromDefaultDB(templateGuid + key_partern);
		jsonProvider.setSuccess(true);
		jsonProvider.setData(json);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 导出所有模板到文件
	 * */
	@POST
	@Path("/exportAllTemplates")
	@Produces(MediaType.TEXT_PLAIN)
	public String exportAllTemplates(@DefaultValue("") @FormParam("filePath") String filePath) {
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		jsonProvider.setSuccess(true);
		jsonProvider.setData("导出模板操作成功！");
		String newFilePath="";
		try{
			newFilePath=getFilePathByOSPlatForm(filePath);
		}catch(Exception e){
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("pathInvalid");
			jsonProvider.setData(null);
			return jsonProvider.toJSON();
		}
				
		JedisPool pool = null;
		Jedis jedis = null;			
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.select(RedisOperator.DEFAULT_DBINDEX);
			Set<String> listKeys = jedis.keys("*" + key_partern);
			if(listKeys!=null){
				for (String key : listKeys) {
					String templateString = jedis.get(key);
					TemplateModel templateModel = GetTemplateModel(templateString);
					String templateGuid = templateModel.getTemplateId();
					String templateJsonString = jedis.get(templateGuid);
					String templateFileName = templateGuid + file_extensionName;
					String templateListName = key + file_extensionName;
					// 保存模板
					exportTemplateJSONStringToFile(newFilePath + templateFileName, templateJsonString);
					// 保存模板列表
					exportTemplateJSONStringToFile(newFilePath + templateListName, templateString);
					//增量模板
					List<String> increaseTemplateIdList=templateModel.getTemplateIncreaseIdList();
					if(increaseTemplateIdList!=null){
						for(String increaseTemplateId:increaseTemplateIdList){
							String increaseTemplateJsonString=jedis.get(increaseTemplateId);
							String increaseTemplateFileName=increaseTemplateId+WebtoolConstants.INCREASE_TEMPLATE_PARTERN+file_extensionName;
							//导出增量模板
							exportTemplateJSONStringToFile(newFilePath + increaseTemplateFileName,increaseTemplateJsonString);
						}
					}
				}
			}			
		} catch (Exception e) {		
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("导出模板操作失败！");
			jsonProvider.setData(null);
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 导入所有模板文件
	 * */
	@POST
	@Path("/importAllTemplates")
	@Produces(MediaType.TEXT_PLAIN)
	public String importAllTemplates(@DefaultValue("") @FormParam("filePath") String dirPath) {
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		jsonProvider.setSuccess(true);
		jsonProvider.setData("导入模板操作成功！");
		String newFilePath="";
		try {
			newFilePath=getFilePathByOSPlatForm(dirPath);
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("pathInvalid");
			jsonProvider.setData(null);
			return jsonProvider.toJSON();
		}
		
		JedisPool pool = null;
		Jedis jedis = null;		
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			File file = new File(newFilePath);
			File[] files = file.listFiles();
			for (File f : files) {
				if (f.isFile()) {
					String fileName = f.getName();
					String templateString = importTemplateJSONString(newFilePath + fileName);
					String templateGuid=null;
					if(fileName.contains(WebtoolConstants.INCREASE_TEMPLATE_PARTERN)){
						templateGuid = fileName.substring(0, fileName.lastIndexOf(WebtoolConstants.INCREASE_TEMPLATE_PARTERN));
						jedis.select(RedisOperator.INCREASE_DBINDEX);	
					}else{
						templateGuid = fileName.substring(0, fileName.lastIndexOf("."));
						jedis.select(RedisOperator.DEFAULT_DBINDEX);						
					}
					jedis.set(templateGuid, templateString);
				}
			}
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("导入模板操作失败！");
			jsonProvider.setData(null);			
			e.printStackTrace();
			pool.returnBrokenResource(jedis);
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}

		return jsonProvider.toJSON();
	}

	
	/**
	 * 
	 * 根据关键字，自动批量生成搜索引擎模板
	 * */	
	@POST
	@Path("/autoGenerateTemplates")
	@Produces(MediaType.TEXT_PLAIN)
	public String AutoGenerateTemplates(@DefaultValue("") @FormParam("data") String data){
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		PageModel pageModel = GetPageModelByJsonString(data);
		//TODO: 获取关键词，根据关键词产生搜索引擎模板
		List<String> wordsList=new ArrayList<String>();
		wordsList.add("政府");
		wordsList.add("公司");
		for(String word: wordsList){			
			//TODO:根据words关键字，处理pageModel中的相关信息:URL,名称,Tags,并重新赋值
			String templateURL=pageModel.getBasicInfoViewModel().getUrl();
			String currentString=pageModel.getBasicInfoViewModel().getCurrentString();
			templateURL=templateURL.replace(currentString,word);
			
			//处理URL及名称
			BasicInfoViewModel basicInfoViewModel=pageModel.getBasicInfoViewModel();
			String templateType=basicInfoViewModel.getTemplateType();
			if(templateType.equals("百度新闻搜索")){
				basicInfoViewModel.setName("百度新闻-"+word);
			}
			basicInfoViewModel.setUrl(templateURL);
			
			
			//处理静态属性tags值
			List<TemplateTagModel> templateTagsViewModel=pageModel.getTemplateTagsViewModel();
			if(templateTagsViewModel!=null){
				for (int i = 0; i < templateTagsViewModel.size(); i++) {
					if(templateTagsViewModel.get(i).getTagKey().equals("分类")){
						templateTagsViewModel.get(i).setTagValue(word); 
					}					
				}
			}			
			
			//构造新的pageModel
			pageModel.setBasicInfoViewModel(basicInfoViewModel);
			pageModel.setTemplateTagsViewModel(templateTagsViewModel);			
			TemplateResult templateResult = GetTemplateResult(pageModel);						
			RedisOperator.saveTemplateToDefaultDB(templateResult, templateResult.getTemplateGuid());		
			// 保存数据源列表所需要的key值 模板默认为启用状态
			SaveTemplateToList(pageModel, "true");
		}
		jsonProvider.setSuccess(true);
		jsonProvider.setData("关键字对应的搜索引擎模板，已全部生成！请回到列表页面，并刷新!");
		return jsonProvider.toJSON();
	}
	
	/**
	 * 
	 * 读取文件
	 * */
	private String importTemplateJSONString(String path) throws IOException {
		File file = new File(path);
		if (!file.exists() || file.isDirectory()) {
			throw new FileNotFoundException();
		}
		BufferedReader br = new BufferedReader(new FileReader(file));
		String temp = null;
		StringBuffer sb = new StringBuffer();
		temp = br.readLine();
		while (temp != null) {
			sb.append(temp + " ");
			temp = br.readLine();
		}
		br.close();
		return sb.toString();
	}

	/**
	 * 
	 * 保存内容到文件
	 * */
	private void exportTemplateJSONStringToFile(String filePath, String content) throws Exception {
		File f = new File(filePath);		 
		File parentDir = f.getParentFile();
		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		if (!f.exists()) {
			f.createNewFile();// 不存在则创建
		}
		BufferedWriter output = new BufferedWriter(new FileWriter(f));
		output.write(content);
		output.close();
		//System.out.println("导出模板文件保存路径:" + filePath);
	}

	private String GetTemplateModelJSONString(TemplateModel templateModel) {
		String json = null;

		ObjectMapper objectmapper = new ObjectMapper();
		try {
			json = objectmapper.writeValueAsString(templateModel);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return json;
	}

	/**
	 * JSON 字符转换为对象
	 * */
	private TemplateModel GetTemplateModel(String jsonString) {
		TemplateModel templateModel = null;
		try {
			ObjectMapper objectmapper = new ObjectMapper();
			templateModel = objectmapper.readValue(jsonString, TemplateModel.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return templateModel;
	}

	/**
	 * 
	 * 同时保存[模板]和[中间结果]到redis
	 * */
	private ParseResult saveTemplateAndParseResult(PageModel pageModel) {
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		TemplateResult templateResult = GetTemplateResult(pageModel);
		String templateGuid = MD5Utils.MD5(templateUrl);
		ParseResult parseResult = null;
		byte[] input = DownloadHtml.getHtml(templateUrl);
		String encoding = sniffCharacterEncoding(input);
		RedisOperator.saveTemplateToDefaultDB(templateResult, templateGuid);		
		SaveTemplateToList(pageModel, "true");// 保存数据源列表所需要的key值
		System.out.println("templateGuid=" + templateGuid);
		parseResult = RedisOperator.getParseResultFromDefaultDB(input, encoding, templateUrl);		
		return parseResult;
	}

	/**
	 * 
	 * 只保存[中间结果]到redis
	 * */
	private ParseResult saveParseResult(PageModel pageModel) {
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		ParseResult parseResult = null;
		byte[] input = DownloadHtml.getHtml(templateUrl);
		String encoding = sniffCharacterEncoding(input);
		parseResult = RedisOperator.getParseResultFromDefaultDB(input, encoding, templateUrl);		
		return parseResult;
	}

	/**
	 * 
	 * 只保存[模板配置]到redis
	 * */
	private void saveTemplateResultToRedis(PageModel pageModel) {
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		TemplateResult templateResult = GetTemplateResult(pageModel);
		String templateGuid = MD5Utils.MD5(templateUrl);
		RedisOperator.saveTemplateToDefaultDB(templateResult, templateGuid);		
		// 保存数据源列表所需要的key值 模板默认为启用状态
		SaveTemplateToList(pageModel, "true");
	}
	
	/**
	 * 
	 * 保存【增量】模板到redis
	 * */
	private ResponseJSONProvider<String> saveIncreaseTemplateResult(PageModel pageModel){	
		ResponseJSONProvider<String> jsonProvider=new ResponseJSONProvider<String>();
		jsonProvider.setSuccess(true);
		TemplateResult templateResult = GetTemplateResult(pageModel);
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		ParseResult parseResult = null;
		byte[] input = DownloadHtml.getHtml(templateUrl);
		String encoding = sniffCharacterEncoding(input);				
		parseResult = RedisOperator.getParseResultFromDefaultDB(input, encoding, templateUrl);
		if(parseResult==null){
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("请先保存常规模板！");
			return jsonProvider;
		}		
		String pageSort=pageModel.getTemplateIncreaseViewModel().getPageSort();
		String pageCounts=pageModel.getTemplateIncreaseViewModel().getPageCounts();	
		
		ArrayList<String> paginationOutlinkArray = TemplateFactory.getPaginationOutlink(parseResult);
		
		if(pageCounts.equals("")){
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("增量配置的中页数值不能为空！");
			return jsonProvider;
		}else{
			int counts=Integer.parseInt(pageCounts);	
			//增量模板移除分页
			templateResult.setPagination(null);
			String templateGuid = MD5Utils.MD5(templateUrl);	
			 //先取之前的模板列表JSON字符串           
	        String singleTemplateListModel=RedisOperator.getFromDefaultDB(templateGuid+key_partern);        
	        TemplateModel singleTemplateModel=GetTemplateModel(singleTemplateListModel);
	        //删除之前的增量模板
	        if(singleTemplateModel.getTemplateIncreaseIdList()!=null){
	        	for(String oldIncreaseTemplateId: singleTemplateModel.getTemplateIncreaseIdList()){
	        		RedisOperator.delFromIncreaseDB(oldIncreaseTemplateId);
	        	}
	        }
			if(paginationOutlinkArray.size()>=counts){
				//记录增量模板id
				List<String> increaseTemplateIdList=new ArrayList<String>();
				if(pageSort.equals("升序")){
					for (int i = 0; i < counts; i++) {
						String paginationUrl=paginationOutlinkArray.get(i);
						String paginationUrlGuid=MD5Utils.MD5(paginationUrl);						
						increaseTemplateIdList.add(paginationUrlGuid);
						//修改模板的guid
						templateResult.setTemplateGuid(paginationUrlGuid);
						RedisOperator.saveTemplateToIncreaseDB(templateResult, paginationUrlGuid);						
					}
				}else{
					for (int i = 0; i < counts; i++) {
						String paginationUrl=paginationOutlinkArray.get(paginationOutlinkArray.size()-(i+1));
						String paginationUrlGuid=MD5Utils.MD5(paginationUrl);	
						increaseTemplateIdList.add(paginationUrlGuid);
						//修改模板的guid
						templateResult.setTemplateGuid(paginationUrlGuid);
						RedisOperator.saveTemplateToIncreaseDB(templateResult, paginationUrlGuid);
					}
				}
				//保存新的增量模板列表
		        singleTemplateModel.setTemplateIncreaseIdList(increaseTemplateIdList);
		        RedisOperator.setToDefaultDB(templateGuid+key_partern, GetTemplateModelJSONString(singleTemplateModel));
			}else{
				jsonProvider.setSuccess(false);
				jsonProvider.setErrorMsg("请检查列表分页配置，未能解析到pagination_outlink！");
				return jsonProvider;
			}
		}
		jsonProvider.setData("增量模板保存成功!");
		return jsonProvider;
	}	


	/**
	 * 将redis中模板的id和数据源列表做关联
	 * */
	private void SaveTemplateToList(PageModel pageModel, String status) {
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		TemplateModel templateModel = setTemplateStatus(pageModel, status);
		StringBuilder str = new StringBuilder();
		str.append(GetTemplateModelJSONString(templateModel));
		RedisOperator.setToDefaultDB(templateGuid + key_partern, str.toString());
	}

	/**
	 * 保存模板，设置模板列表中单个模板的状态
	 * */
	private TemplateModel setTemplateStatus(PageModel pageModel, String status) {
		TemplateModel templateModel = new TemplateModel();
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		templateModel.setTemplateId(templateGuid);		
		templateModel.setDescription(pageModel.getBasicInfoViewModel().getName());		
		
		BasicInfoViewModel basicInfoViewModel=new BasicInfoViewModel();
		basicInfoViewModel.setName(pageModel.getBasicInfoViewModel().getName());
		basicInfoViewModel.setUrl(pageModel.getBasicInfoViewModel().getUrl());
		basicInfoViewModel.setTemplateType(pageModel.getBasicInfoViewModel().getTemplateType());
		basicInfoViewModel.setCurrentString(pageModel.getBasicInfoViewModel().getCurrentString());
		templateModel.setBasicInfoViewModel(basicInfoViewModel);
		
		ScheduleDispatchViewModel scheduleDispatchViewModel=new ScheduleDispatchViewModel();
		scheduleDispatchViewModel.setPeriod(pageModel.getScheduleDispatchViewModel().getPeriod());
		scheduleDispatchViewModel.setSequence(pageModel.getScheduleDispatchViewModel().getSequence());
		scheduleDispatchViewModel.setUseProxy(pageModel.getScheduleDispatchViewModel().getUseProxy());
		 //这里必须先取之前的模板列表JSON字符串，因为增量模板列表可能因为修改操作而被覆盖           
        String singleTemplateListModel=RedisOperator.getFromDefaultDB(templateGuid+key_partern);     
		TemplateModel oldTemplateModel = GetTemplateModel(singleTemplateListModel);	
		if(oldTemplateModel.getTemplateIncreaseIdList()!=null){//修改操作时，保存原来的增量模板列表
			templateModel.setTemplateIncreaseIdList(oldTemplateModel.getTemplateIncreaseIdList());			
		}		
		if(!oldTemplateModel.getAddedTime().equals("")){//修改操作时，时间不变
			templateModel.setAddedTime(oldTemplateModel.getAddedTime());
		}else{
			Date currentDate=new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String nowDateString = dateFormat.format(currentDate);
			templateModel.setAddedTime(nowDateString);
		}
		templateModel.setScheduleDispatchViewModel(scheduleDispatchViewModel);
        
        TemplateIncreaseViewModel templateIncreaseViewModel=new TemplateIncreaseViewModel();
        templateIncreaseViewModel.setPeriod(pageModel.getTemplateIncreaseViewModel().getPeriod());
        templateIncreaseViewModel.setPageCounts(pageModel.getTemplateIncreaseViewModel().getPageCounts());
        templateIncreaseViewModel.setPageSort(pageModel.getTemplateIncreaseViewModel().getPageSort());
        templateModel.setTemplateIncreaseViewModel(templateIncreaseViewModel);

        //修改时状态不变
        if(!oldTemplateModel.getStatus().equals("")){
        	templateModel.setStatus(oldTemplateModel.getStatus());
        }else{
        	 templateModel.setStatus(status);
        }        
       
		return templateModel;
	}

	/**
	 * 更改模板状态，设置模板列表中单个模板的状态
	 * */
	private void setTemplateStatus(String templateUrl, String name, String status) {
		String templateGuid = MD5Utils.MD5(templateUrl);
		 //先取之前的模板列表JSON字符串           
        String singleTemplateListModel=RedisOperator.getFromDefaultDB(templateGuid+key_partern);     
		TemplateModel templateModel = GetTemplateModel(singleTemplateListModel);		
		templateModel.setStatus(status);
		
        StringBuilder str = new StringBuilder();
        str.append(GetTemplateModelJSONString(templateModel));
        RedisOperator.setToDefaultDB(templateGuid + key_partern, str.toString());        
	}

	/**
	 * 
	 * 根据JSON字符串,得到PAGE-MODEL对象
	 * */
	private PageModel GetPageModelByJsonString(String json) {
		PageModel pageModel = null;
		try {
			ObjectMapper objectmapper = new ObjectMapper();
			pageModel = objectmapper.readValue(json, PageModel.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return pageModel;
	}

	/**
	 * 
	 * 产生过滤器
	 * */
	private SelectorFilter getFieldFilter(String filterString,String filterCategory,String filterReplaceTo) {
		SelectorFilter filter = new SelectorFilter();
		if (filterString.equals("")) {
			filter = null;
		} else {
			if (filterCategory.equals("匹配")) {
				filter.initMatchFilter(filterString);
			} else if (filterCategory.equals("替换")) {
				filter.initReplaceFilter(filterString, filterReplaceTo);
			} else if (filterCategory.equals("移除")) {
				filter.initRemoveFilter(filterString);
			}else{
				filter = null;
			}
		}
		
		return filter;
	}
	
	/**
	 * 
	 * 产生格式化器
	 * */
	private SelectorFormat getFieldFormatter(String formatString,String formatCategory) {
		SelectorFormat format = new SelectorFormat();
		if(formatString.equals("")){
			format=null;
		}else{
			if(formatCategory.equals("日期")){
				format.initDateFormat(formatString);
			}else{
				format=null;
			}
		}
		return format;
	}
	
	/**
	 * 
	 * 生成模板对象
	 * */
	private TemplateResult GetTemplateResult(PageModel pageModel) {
		TemplateResult template = new TemplateResult();
		template.setType(Constants.TEMPLATE_LIST);
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		template.setTemplateGuid(templateGuid);
		template.setState(Constants.UN_FETCH);

		// 处理模板tag静态属性
		HashMap<String, String> dictionary = new HashMap<String, String>();
		List<TemplateTagModel> tempalteTags = pageModel.getTemplateTagsViewModel();
		for (TemplateTagModel model : tempalteTags) {
			dictionary.put(model.getTagKey(), model.getTagValue());
		}
		template.setTags(dictionary);

		List<Selector> list = new ArrayList<Selector>();
		List<Selector> news = new ArrayList<Selector>();
		List<Selector> pagination = new ArrayList<Selector>();

		SelectorIndexer indexer = null;
		Selector selector = null;
		SelectorFilter filter = null;
		SelectorFormat format = null;

		// list outlink
		indexer = new SelectorIndexer();
		selector = new Selector();
		if (!pageModel.getListOutLinkViewModel().getSelector().equals("")) {
			indexer.initJsoupIndexer(pageModel.getListOutLinkViewModel().getSelector(), pageModel.getListOutLinkViewModel().getSelectorAttr());
			selector.initContentSelector(indexer, null);
		}

		// 处理列表自定义属性 以时间为例
		List<CustomerAttrModel> listCustomerAttrViewModel = pageModel.getListCustomerAttrViewModel();
		for (CustomerAttrModel model : listCustomerAttrViewModel) {
			Selector label = new Selector();
			label.setType(Constants.SELECTOR_LABEL);
			indexer = new SelectorIndexer();
			indexer.initJsoupIndexer(model.getSelector(), model.getAttr());			
			format = new SelectorFormat();
			
			//处理列表自定义属性过滤器
			String filterString = model.getFilter();
			String filterCategory = model.getFilterCategory();
			String filterReplaceTo=model.getFilterReplaceTo();
			filter=getFieldFilter(filterString, filterCategory, filterReplaceTo);
			
			//处理列表自定义属性格式化器
			String formatString=model.getFormatter();
			String formatCategory=model.getFormatCategory();
			format=getFieldFormatter(formatString, formatCategory);
			
			label.initLabelSelector(model.getTarget(), "", indexer, filter, format);
			selector.setLabel(label);
		}
		list.add(selector);
		template.setList(list);

		// pagitation outlink js翻页无法处理
		indexer = new SelectorIndexer();
		selector = new Selector();
		indexer.initJsoupIndexer(pageModel.getListPaginationViewModel().getSelector(), pageModel.getListPaginationViewModel().getSelectorAttr());
		
		//处理分页过滤器
		String paginationFilter = pageModel.getListPaginationViewModel().getFilter();
		String paginationFilterCategory = pageModel.getListPaginationViewModel().getFilterCategory();		
		String filterReplaceToString = pageModel.getListPaginationViewModel().getFilterReplaceTo();
		filter = getFieldFilter(paginationFilter, paginationFilterCategory, filterReplaceToString);

		//处理分页类型
		String paginationType = pageModel.getListPaginationViewModel().getPaginationType();
		if (paginationType.equals("分页的末尾页数")) {
			paginationType = Constants.PAGINATION_TYPE_PAGENUMBER;
		} else if (paginationType.equals("分页步进数")) {
			paginationType = Constants.PAGINATION_TYPE_PAGENUMBER_INTERVAL;
		} else if (paginationType.equals("获取分页的记录数")) {
			paginationType = Constants.PAGINATION_TYPE_PAGERECORD;
		} else if (paginationType.equals("获取分页URL")) {
			paginationType = Constants.PAGINATION_TYPE_PAGE;
		}else if(paginationType.equals("自定义分页")){
			paginationType = Constants.PAGINATION_TYPE_CUSTOM;
		} 
		else {
			paginationType = Constants.PAGINATION_TYPE_PAGENUMBER;
		}

		// 处理分页进步数
		int paginationInterval = 0;
		if (!pageModel.getListPaginationViewModel().getInterval().equals("")) {
			try {
				paginationInterval = Integer.parseInt(pageModel.getListPaginationViewModel().getInterval());
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				paginationInterval = 0;
			}
		}

		// 按照是否使用分页进步数,调用不同的方法
		if (paginationInterval != 0) {
			if(paginationType==Constants.PAGINATION_TYPE_CUSTOM){
				selector.initPagitationSelector(paginationType,pageModel.getListPaginationViewModel().getCurrentString(),pageModel.getListPaginationViewModel().getReplaceTo(),pageModel.getListPaginationViewModel().getPaginationUrl(),pageModel.getListPaginationViewModel().getStart(),pageModel.getListPaginationViewModel().getLastNumber(),paginationInterval);
			}else if(paginationType==Constants.PAGINATION_TYPE_PAGENUMBER_INTERVAL){
				// Constants.PAGINATION_TYPE_PAGENUMBER 分页步进数
				selector.initPagitationSelector(paginationType, pageModel.getListPaginationViewModel().getCurrentString(), pageModel.getListPaginationViewModel().getReplaceTo(), pageModel.getListPaginationViewModel().getPaginationUrl(), pageModel.getListPaginationViewModel().getStart(), pageModel.getListPaginationViewModel().getRecords(), paginationInterval, indexer, filter, null);
			}
		}
		else {//调用分页进步数方法或分页URL方法			
			selector.initPagitationSelector(paginationType, pageModel.getListPaginationViewModel().getCurrentString(), pageModel.getListPaginationViewModel().getReplaceTo(), pageModel.getListPaginationViewModel().getPaginationUrl(), pageModel.getListPaginationViewModel().getStart(), pageModel.getListPaginationViewModel().getRecords(), indexer, filter, null);
		}

		if (!pageModel.getListPaginationViewModel().getSelector().equals("")) {
			pagination.add(selector);
		}else{
			//当选择自定义分页时，选择器可以为空
			if(paginationType==Constants.PAGINATION_TYPE_CUSTOM){
				pagination.add(selector);
			}
		}
		template.setPagination(pagination);

		// title
		indexer = new SelectorIndexer();
		selector = new Selector();
		if (!pageModel.getNewsTitleViewModel().getSelector().equals("")) {
			indexer.initJsoupIndexer(pageModel.getNewsTitleViewModel().getSelector(), pageModel.getNewsTitleViewModel().getSelectorAttr());
			selector.initFieldSelector("title", "", indexer, null, null);
			news.add(selector);
		}

		// content
		indexer = new SelectorIndexer();
		selector = new Selector();
		if (!pageModel.getNewsContentViewModel().getSelector().equals("")) {
			indexer.initJsoupIndexer(pageModel.getNewsContentViewModel().getSelector(), pageModel.getNewsContentViewModel().getSelectorAttr());
			selector.initFieldSelector("content", "", indexer, null, null);
			news.add(selector);
		}

		// public time
		indexer = new SelectorIndexer();
		selector = new Selector();
		if (!pageModel.getNewsPublishTimeViewModel().getSelector().equals("")) {
			//处理发布时间过滤器
			String filterString = pageModel.getNewsPublishTimeViewModel().getFilter();
			String filterCategory = pageModel.getNewsPublishTimeViewModel().getFilterCategory();
			String filterReplaceTo=pageModel.getNewsPublishTimeViewModel().getFilterReplaceTo();						
			filter =getFieldFilter(filterString, filterCategory, filterReplaceTo);
			
			//处理发布时间格式化器
			String formatString=pageModel.getNewsPublishTimeViewModel().getFormatter();
			String formatCategory=pageModel.getNewsPublishTimeViewModel().getFormatCategory();
			format=getFieldFormatter(formatString, formatCategory);
			
			indexer.initJsoupIndexer(pageModel.getNewsPublishTimeViewModel().getSelector(), pageModel.getNewsPublishTimeViewModel().getSelectorAttr());
			selector.initFieldSelector("tstamp", "", indexer, filter, format);
			news.add(selector);
		}

		// source
		indexer = new SelectorIndexer();
		selector = new Selector();
		if (!pageModel.getNewsSourceViewModel().getSelector().equals("")) {
			indexer.initJsoupIndexer(pageModel.getNewsSourceViewModel().getSelector(), pageModel.getNewsSourceViewModel().getSelectorAttr());
			selector.initFieldSelector("source", "", indexer, null, null);
			news.add(selector);
		}

		// 处理内容自定义属性 以时间为例
		List<CustomerAttrModel> newsCustomerAttrViewModel = pageModel.getNewsCustomerAttrViewModel();
		for (CustomerAttrModel model : newsCustomerAttrViewModel) {
			indexer = new SelectorIndexer();
			selector = new Selector();
			
			//处理内容自定义属性过滤器
			String filterString = model.getFilter();
			String filterCategory = model.getFilterCategory();
			String filterReplaceTo=model.getFilterReplaceTo();
			filter=getFieldFilter(filterString, filterCategory, filterReplaceTo);
						
			//处理内容自定义属性格式化器
			String formatString=model.getFormatter();
			String formatCategory=model.getFormatCategory();
			format=getFieldFormatter(formatString, formatCategory);
						
			if (!model.getSelector().equals("")) {
				indexer.initJsoupIndexer(model.getSelector(), model.getAttr());
				selector.initFieldSelector(model.getTarget(), "", indexer, filter, format);
				news.add(selector);
			}
		}
		template.setNews(news);
		// System.out.println("templateResult:"+template.toJSON());
		return template;
	}

	// I used 1000 bytes at first, but found that some documents have
	// meta tag well past the first 1000 bytes.
	// (e.g. http://cn.promo.yahoo.com/customcare/music.html)
	private static final int CHUNK_SIZE = 8000;
	// NUTCH-1006 Meta equiv with single quotes not accepted
	private static Pattern metaPattern = Pattern.compile("<meta\\s+([^>]*http-equiv=(\"|')?content-type(\"|')?[^>]*)>", Pattern.CASE_INSENSITIVE);
	private static Pattern charsetPattern = Pattern.compile("charset=\\s*([a-z][_\\-0-9a-z]*)", Pattern.CASE_INSENSITIVE);
	private static Pattern charsetPatternHTML5 = Pattern.compile("<meta\\s+charset\\s*=\\s*[\"']?([a-z][_\\-0-9a-z]*)[^>]*>", Pattern.CASE_INSENSITIVE);

	private static String sniffCharacterEncoding(byte[] content) {
		int length = content.length < CHUNK_SIZE ? content.length : CHUNK_SIZE;
		String str = "";
		try {
			// System.out.println("content:"+new String(content,"utf-8"));
			str = new String(content, 0, length, Charset.forName("ASCII").toString());
			// System.out.println("str:"+str);
		} catch (UnsupportedEncodingException e) {
			return null;
		}

		Matcher metaMatcher = metaPattern.matcher(str);
		String encoding = null;
		if (metaMatcher.find()) {
			Matcher charsetMatcher = charsetPattern.matcher(metaMatcher.group(1));
			if (charsetMatcher.find())
				encoding = new String(charsetMatcher.group(1));
		}
		if (encoding == null) {
			// check for HTML5 meta charset
			metaMatcher = charsetPatternHTML5.matcher(str);
			if (metaMatcher.find()) {
				encoding = new String(metaMatcher.group(1));
			}
		}
		if (encoding == null) {
			metaMatcher = charsetPattern.matcher(str);
			if (metaMatcher.find()) {
				encoding = new String(metaMatcher.group(1));
			}
		}
		if (encoding == null) {
			// check for BOM
			if (content.length >= 3 && content[0] == (byte) 0xEF && content[1] == (byte) 0xBB && content[2] == (byte) 0xBF) {
				encoding = "UTF-8";
			} else if (content.length >= 2) {
				if (content[0] == (byte) 0xFF && content[1] == (byte) 0xFE) {
					encoding = "UTF-16LE";
				} else if (content[0] == (byte) 0xFE && content[1] == (byte) 0xFF) {
					encoding = "UTF-16BE";
				} else {
					encoding = "UTF-8";// ”尼玛“个别网站不设定编码
				}
			}
		}
		return encoding;
	}

	/**
	 * 
	 * 输出异常信息
	 * */
	private static String getStackTrace(final Throwable throwable) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		throwable.printStackTrace(pw);
		return sw.getBuffer().toString();
	}
	
	/**
	 * 
	 * 根据操作系统类型生成正确的文件系统路径
	 * */
	private String getFilePathByOSPlatForm(String filePath) throws Exception{
		String tempPath="";
		if (System.getProperty("os.name").indexOf("Windows") > -1) {
			if(filePath.indexOf("\\")<0){
				throw new Exception("输入文件路径不合法");  
			}else{
				if (!filePath.endsWith("\\")) {
					tempPath=filePath+"\\";
				}else{
					tempPath=filePath;
				}
			}			
		}else{
			if(filePath.indexOf("/")<0){
				throw new Exception("输入文件路径不合法");  
			}else{
				if (!filePath.endsWith("/")) {
					tempPath=filePath+"/";
				}else{
					tempPath=filePath;
				}
			}	
		}		
		return tempPath;
	}
	
	/**
	 * 
	 * 产生指定范围内的随机数
	 * */
	private int getRandomNumber(int min, int max) {
		Random random = new Random();
		int s = random.nextInt(max) % (max - min + 1) + min;
		return s;
	}
	
}
