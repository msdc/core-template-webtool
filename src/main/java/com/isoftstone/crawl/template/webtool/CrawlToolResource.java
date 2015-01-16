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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import com.isoftstone.crawl.template.model.CustomerAttrModel;
import com.isoftstone.crawl.template.model.PageModel;
import com.isoftstone.crawl.template.model.TemplateModel;
import com.isoftstone.crawl.template.model.TemplateTagModel;
import com.isoftstone.crawl.template.utils.Config;
import com.isoftstone.crawl.template.utils.DownloadHtml;
import com.isoftstone.crawl.template.utils.MD5Utils;
import com.isoftstone.crawl.template.utils.RedisUtils;
import com.isoftstone.crawl.template.utils.ShellUtils;
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

	private static final Log LOG = LogFactory.getLog(CrawlToolResource.class);

	/**
	 * 保存到本地文件
	 * */
	@POST
	@Path("/saveToLocalFile")
	@Produces(MediaType.TEXT_PLAIN)
	public String saveToLocalFile(@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = GetPageModelByJsonString(data);
		String domain = pageModel.getScheduleDispatchViewModel().getDomain();
		String period = pageModel.getScheduleDispatchViewModel().getPeriod();
		String sequence = pageModel.getScheduleDispatchViewModel().getSequence();
		if (sequence == null || sequence.equals("")) {
			return "保存失败，请输入时序.";
		}
		String folderName = domain + "_" + "1" + period + "_" + sequence;
		ParseResult parseResult = saveTemplateAndParseResult(pageModel);
		if (parseResult == null) {
			return "请先保存模板!再执行此操作!";
		}
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		String redisKey = templateGuid + key_partern;
		TemplateModel templateModel = getTemplateModel(redisKey);
		String status = templateModel.getStatus();
		// ArrayList<String> seeds = TemplateFactory.getOutlink(parseResult);
		ArrayList<String> seeds = new ArrayList<String>();
		seeds.add(templateUrl);
		saveSeedsValueToFile(folderName, seeds, status);
		return "文件保存成功!";
	}

	/**
	 * 保存种子到本地文件. 并将文件夹相关信息存入redis.
	 */
	private void saveSeedsValueToFile(String folderName, List<String> seeds, String status) {
		// --1.保存到本地文件.
		contentToTxt(folderName, seeds, status);

		// --2.保存到redis中.
		String redisKey = folderName + WebtoolConstants.DISPATCH_REIDIS_POSTFIX;
		DispatchVo dispatchVo = new DispatchVo();
		dispatchVo.setStatus(WebtoolConstants.DISPATCH_STATIS_START);
		List<Seed> seedList = new ArrayList<Seed>();
		for (Iterator<String> it = seeds.iterator(); it.hasNext();) {
			String seedStr = it.next();
			Seed seed = new Seed(seedStr, status);
			seedList.add(seed);
		}
		dispatchVo.setSeed(seedList);
		setDispatchResult(dispatchVo, redisKey);
	}

	private void setDispatchResult(DispatchVo dispatchVo, String guid) {
		JedisPool pool = null;
		Jedis jedis = null;
		try {
			StringBuilder str = new StringBuilder();
			str.append(JSON.toJSONString(dispatchVo));
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
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
			putSeedsFolder(folderName, "local");
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
		Runmanager runmanager = new Runmanager();
		runmanager.setHostIp("192.168.100.236");
		runmanager.setUsername("root");
		runmanager.setPassword("Password1");
		runmanager.setPort(22);
		String folderRoot = Config.getValue(WebtoolConstants.FOLDER_NAME_ROOT);
		LOG.info("文件根目录" + folderRoot);
		String command = "";
		if ("local".equals(type)) {
			command = "scp -r " + folderRoot + "/" + folderName + " root@192.168.100.231:/home/" + folderName;
		} else {
			// FIXME:集群模式，执行的命令.
			command = "";
		}
		LOG.info("命令：" + command);
		runmanager.setCommand(command);
		ShellUtils.execCmd(runmanager);
		LOG.info("命令执行完毕：" + command);
	}

	/**
	 * 验证内容页
	 * */
	@POST
	@Path("/verifyNewContent")
	@Produces(MediaType.TEXT_PLAIN)
	public String VerifyNewContent(@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = GetPageModelByJsonString(data);
		ParseResult parseResult = saveParseResult(pageModel);		
		if (parseResult == null) {
			return "请先保存模板!再执行此操作!";
		}
		// 获取内容页链接
		ArrayList<String> contentOutLinkArrayList = TemplateFactory.getContentOutlink(parseResult);
		if (contentOutLinkArrayList.size() == 0) {
			return "列表外链接配置信息不正确！";
		}
		String contentOutLink = contentOutLinkArrayList.get(0);
		byte[] input = DownloadHtml.getHtml(contentOutLink);
		String encoding = sniffCharacterEncoding(input);
		try {
			parseResult = TemplateFactory.process(input, encoding, contentOutLink);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

		return parseResult.toJSON();
	}

	/**
	 * 验证列表页
	 * */
	@POST
	@Path("/verifyListContent")
	@Produces(MediaType.TEXT_PLAIN)
	public String VerifyListContent(@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = GetPageModelByJsonString(data);
		ParseResult parseResult = saveParseResult(pageModel);
		if (parseResult == null) {
			return "请先保存模板!再执行此操作!";
		}
		return parseResult.toJSON();
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
		TemplateResult templateResult = GetTemplateResult(pageModel);
		return templateResult.toJSON();
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
		saveTemplateResultToRedis(pageModel);
		return "模板保存成功!";
	}

	/**
	 * 
	 * 查看HTML内容按钮
	 * */
	@POST
	@Path("/viewHtmlContent")
	@Produces(MediaType.TEXT_PLAIN)
	public String viewHtmlContent(@DefaultValue("") @FormParam("webUrl") String webUrl) {
		String htmlContent = DownloadHtml.getHtml(webUrl, "UTF-8");
		return htmlContent;
	}

	/**
	 * 
	 * 删除模板
	 * */
	@POST
	@Path("/deleteTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public Boolean DeleteTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl) {
		String templateGuid = MD5Utils.MD5(templateUrl);
		JedisPool pool = null;
		Jedis jedis = null;
		Boolean executeResult = true;
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.del(templateGuid + key_partern);
			jedis.del(templateGuid);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
			executeResult = false;
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		return executeResult;
	}

	/**
	 * 
	 * 修改模板
	 * */
	@POST
	@Path("/updateTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String UpdateTemplate(@DefaultValue("") @FormParam("templateGuid") String templateGuid) {
		String json = "";
		TemplateResult templateResult = RedisUtils.getTemplateResult(templateGuid);
		json = templateResult.toJSON();
		return json;
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
		return templateGuid;
	}

	/**
	 * 
	 * 获取所有的模板列表
	 * */
	@GET
	@Path("/getTemplateList")
	@Produces(MediaType.TEXT_PLAIN)
	public String GetTemplateList() {
		JedisPool pool = null;
		Jedis jedis = null;
		TemplateList templateList = new TemplateList();
		List<TemplateModel> templateListArrayList = new ArrayList<TemplateModel>();
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			Set<String> listKeys = jedis.keys("*" + key_partern);
			for (String key : listKeys) {
				String templateString = jedis.get(key);
				TemplateModel templateModel = GetTemplateModel(templateString);
				templateListArrayList.add(templateModel);
			}
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		templateList.setTemplateList(templateListArrayList);
		String templateListJSONString = GetTemplateListJSONString(templateList);
		return templateListJSONString;
	}

	/**
	 * 
	 * 停用模板
	 * */
	@POST
	@Path("/disableTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String DisableTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl, @DefaultValue("") @FormParam("name") String name) {
		setTemplateStatus(templateUrl, name, "false");
		return "success";
	}

	/**
	 * 
	 * 启用模板
	 * */
	@POST
	@Path("/enableTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String EnableTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl, @DefaultValue("") @FormParam("name") String name) {
		setTemplateStatus(templateUrl, name, "true");
		return "success";
	}

	/**
	 * 
	 * 获取模板列表中单个模板对象
	 * */
	@POST
	@Path("/getSingleTemplateModel")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSingleTemplateModel(@DefaultValue("") @FormParam("templateGuid") String templateGuid) {
		JedisPool pool = null;
		Jedis jedis = null;
		String json = "";
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			json = jedis.get(templateGuid + key_partern);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		return json;
	}

	/**
	 * 
	 * 导出所有模板到文件
	 * */
	@POST
	@Path("/exportAllTemplates")
	@Produces(MediaType.TEXT_PLAIN)
	public String exportAllTemplates(@DefaultValue("") @FormParam("filePath") String filePath) {
		String newFilePath="";
		try{
			newFilePath=getFilePathByOSPlatForm(filePath);
		}catch(Exception e){
			return "pathInvalid";
		}
				
		JedisPool pool = null;
		Jedis jedis = null;	
		String resultStatus = "true";
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			Set<String> listKeys = jedis.keys("*" + key_partern);
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
			}
		} catch (Exception e) {
			resultStatus = "false";
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		return resultStatus;
	}

	/**
	 * 
	 * 导入所有模板文件
	 * */
	@POST
	@Path("/importAllTemplates")
	@Produces(MediaType.TEXT_PLAIN)
	public String importAllTemplates(@DefaultValue("") @FormParam("filePath") String dirPath) {
		String newFilePath="";
		try {
			newFilePath=getFilePathByOSPlatForm(dirPath);
		} catch (Exception e) {
			return "pathInvalid";
		}
		
		JedisPool pool = null;
		Jedis jedis = null;
		String resultStatus = "true";
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			File file = new File(newFilePath);
			File[] files = file.listFiles();
			for (File f : files) {
				if (f.isFile()) {
					String fileName = f.getName();
					String templateString = importTemplateJSONString(newFilePath + fileName);
					String templateGuid = fileName.substring(0, fileName.lastIndexOf("."));
					jedis.set(templateGuid, templateString);
				}
			}
		} catch (Exception e) {
			resultStatus = "false";
			e.printStackTrace();
			pool.returnBrokenResource(jedis);
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}

		return resultStatus;
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

	/**
	 * 将对象转换为JSON-string形式
	 * */
	private String GetTemplateListJSONString(TemplateList templateList) {
		String json = null;

		ObjectMapper objectmapper = new ObjectMapper();
		try {
			json = objectmapper.writeValueAsString(templateList);
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
		RedisUtils.setTemplateResult(templateResult, templateGuid);
		SaveTemplateToList(pageModel, "true");// 保存数据源列表所需要的key值
		System.out.println("templateGuid=" + templateGuid);
		parseResult = TemplateFactory.process(input, encoding, templateUrl);
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
		parseResult = TemplateFactory.process(input, encoding, templateUrl);
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
		RedisUtils.setTemplateResult(templateResult, templateGuid);
		// 保存数据源列表所需要的key值 模板默认为启用状态
		SaveTemplateToList(pageModel, "true");
	}

	/**
	 * 将redis中模板的id和数据源列表做关联
	 * */
	private void SaveTemplateToList(PageModel pageModel, String status) {
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		JedisPool pool = null;
		Jedis jedis = null;
		TemplateModel templateModel = setTemplateStatus(pageModel, status);
		try {
			StringBuilder str = new StringBuilder();
			str.append(GetTemplateModelJSONString(templateModel));
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.set(templateGuid + key_partern, str.toString());
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
	}

	/**
	 * 设置模板列表中单个模板的状态
	 * */
	private TemplateModel setTemplateStatus(PageModel pageModel, String status) {
		TemplateModel templateModel = new TemplateModel();
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		templateModel.setTemplateId(templateGuid);
		templateModel.setName(pageModel.getBasicInfoViewModel().getName());
		templateModel.setDescription(pageModel.getBasicInfoViewModel().getName());
		templateModel.setUrl(pageModel.getBasicInfoViewModel().getUrl());
        templateModel.setSchedulePeriod(pageModel.getScheduleDispatchViewModel().getPeriod());
        templateModel.setScheduleSequence(pageModel.getScheduleDispatchViewModel().getSequence());
        templateModel.setIncreasePeriod(pageModel.getTemplateIncreaseViewModel().getPeriod());
        templateModel.setIncreasePageCounts(pageModel.getTemplateIncreaseViewModel().getPageCounts());
		templateModel.setStatus(status);
		return templateModel;
	}

	/**
	 * 设置模板列表中单个模板的状态
	 * */
	private void setTemplateStatus(String templateUrl, String name, String status) {
		String templateGuid = MD5Utils.MD5(templateUrl);
		JedisPool pool = null;
		Jedis jedis = null;
		TemplateModel templateModel = new TemplateModel();
		templateModel.setTemplateId(templateGuid);
		templateModel.setName(name);
		templateModel.setDescription(name);
		templateModel.setUrl(templateUrl);
		templateModel.setStatus(status);
		try {
			pool = RedisUtils.getPool();
            jedis = pool.getResource();            
            String templateResult=jedis.get(templateGuid);
            PageModel pageModel=GetPageModelByJsonString(templateResult);
            templateModel.setSchedulePeriod(pageModel.getScheduleDispatchViewModel().getPeriod());
            templateModel.setScheduleSequence(pageModel.getScheduleDispatchViewModel().getSequence());
            templateModel.setIncreasePeriod(pageModel.getTemplateIncreaseViewModel().getPeriod());
            templateModel.setIncreasePageCounts(pageModel.getTemplateIncreaseViewModel().getPageCounts());
            
            StringBuilder str = new StringBuilder();
            str.append(GetTemplateModelJSONString(templateModel));
            jedis.set(templateGuid + key_partern, str.toString());
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
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
		} else {
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
			selector.initPagitationSelector(paginationType, pageModel.getListPaginationViewModel().getCurrentString(), pageModel.getListPaginationViewModel().getReplaceTo(), pageModel.getListPaginationViewModel().getPaginationUrl(), pageModel.getListPaginationViewModel().getStart(), pageModel.getListPaginationViewModel().getRecords(), paginationInterval, indexer, filter, null);
		} else {
			// Constants.PAGINATION_TYPE_PAGENUMBER 需要取值
			selector.initPagitationSelector(paginationType, pageModel.getListPaginationViewModel().getCurrentString(), pageModel.getListPaginationViewModel().getReplaceTo(), pageModel.getListPaginationViewModel().getPaginationUrl(), pageModel.getListPaginationViewModel().getStart(), pageModel.getListPaginationViewModel().getRecords(), indexer, filter, null);
		}

		if (!pageModel.getListPaginationViewModel().getSelector().equals("")) {
			pagination.add(selector);
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
}
