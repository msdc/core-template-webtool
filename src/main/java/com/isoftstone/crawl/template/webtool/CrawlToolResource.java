package com.isoftstone.crawl.template.webtool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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
import com.isoftstone.crawl.template.utils.DownloadHtml;
import com.isoftstone.crawl.template.utils.MD5Utils;
import com.isoftstone.crawl.template.utils.RedisUtils;

/**
 * 
 * 爬虫工具restful-services服务类
 * */
@Path("crawlToolResource")
public class CrawlToolResource {
	//列表的key的后缀
	public static final String key_partern = "_templatelist";
	
	/**
	 * 保存到本地文件
	 * */
	@POST
	@Path("/saveToLocalFile")
	@Produces(MediaType.TEXT_PLAIN)
	public String SaveToLocalFile(
			@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = GetPageModelByJsonString(data);
		ParseResult parseResult = SaveTemplateToRedis(pageModel);			
		if (parseResult == null) {
			return "请先保存模板!再执行此操作!";
		}
		// HashMap<String, String> seeds = parseResult.getResult();
		// ArrayList<String> seeds=TemplateFactory.getOutlink(parseResult);
		ArrayList<String> seeds = TemplateFactory.getOutlink(parseResult);
		SaveSeedsValueToFile(seeds);
		return "文件保存成功!";
	}

	/**
	 * 验证内容页
	 * */
	@POST
	@Path("/verifyNewContent")
	@Produces(MediaType.TEXT_PLAIN)
	public String VerifyNewContent(
			@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = GetPageModelByJsonString(data);
		ParseResult parseResult = SaveTemplateToRedis(pageModel);
		// ParseResult parseResult = GetParseResultFromRedis(pageModel);
		if (parseResult == null) {
			return "请先保存模板!再执行此操作!";
		}
		// 获取内容页链接
		ArrayList<String> contentOutLinkArrayList = TemplateFactory
				.getContentOutlink(parseResult);
		String contentOutLink = contentOutLinkArrayList.get(0);
		byte[] input = DownloadHtml.getHtml(contentOutLink);
		String encoding = "gb2312";
		try {
			parseResult = TemplateFactory.process(input, encoding,
					contentOutLink);
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
	public String VerifyListContent(
			@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = GetPageModelByJsonString(data);
		ParseResult parseResult = SaveTemplateToRedis(pageModel);
		if (parseResult == null) {
			return "请先保存模板!再执行此操作!";
		}

		// //测试单个页面列表中的content内容
		// ArrayList<String> paginationOutLinkArrayList = TemplateFactory
		// .getPaginationOutlink(parseResult);
		// String paginationOutLink = paginationOutLinkArrayList.get(0);
		// byte[] input = DownloadHtml.getHtml(paginationOutLink);
		// String encoding = "gb2312";
		// try {
		// parseResult = TemplateFactory.process(input, encoding,
		// paginationOutLink);
		// } catch (Exception e) {
		// // TODO: handle exception
		// e.printStackTrace();
		// }

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
		ParseResult parseResult = SaveTemplateToRedis(pageModel);
		if (parseResult != null) {
			return "模板保存成功!";
		}
		return "模板保存失败!请检查模板配置是否正确!";
	}

	/**
	 * 
	 * 查看HTML内容按钮
	 * */
	@POST
	@Path("/viewHtmlContent")
	@Produces(MediaType.TEXT_PLAIN)
	public String viewHtmlContent(
			@DefaultValue("") @FormParam("webUrl") String webUrl) {
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
	public Boolean DeleteTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl){
		String templateGuid = MD5Utils.MD5(templateUrl);
		JedisPool pool = null;
		Jedis jedis = null;		
		Boolean executeResult=true;
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();	
			jedis.del(templateGuid+key_partern);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
			executeResult=false;
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
	public String UpdateTemplate(@DefaultValue("") @FormParam("templateGuid") String templateGuid){
		String json="";
		TemplateResult templateResult = RedisUtils.getTemplateResult(templateGuid);
		json=templateResult.toJSON();
		return json;
	}
	
	/**
	 * 
	 * 修改模板
	 * */
	@POST
	@Path("/getTemplateGuid")
	@Produces(MediaType.TEXT_PLAIN)
	public String getTemplateGuid(@DefaultValue("") @FormParam("templateUrl") String templateUrl){
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
	public String GetTemplateList(){
		JedisPool pool = null;
		Jedis jedis = null;
		TemplateList templateList = new TemplateList();
		List<TemplateModel> templateListArrayList=new ArrayList<TemplateModel>();
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			Set<String> listKeys = jedis.keys("*"+key_partern);
			for (String key : listKeys) {  
			      String templateString=jedis.get(key);
			      TemplateModel templateModel=GetTemplateModel(templateString);
			      templateListArrayList.add(templateModel);			      
			} 					
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}	
		templateList.setTemplateList(templateListArrayList);
		String templateListJSONString=GetTemplateListJSONString(templateList);
		return templateListJSONString;	
	}
	
	/**
	 * 
	 * 获取模板列表中单个模板对象
	 * */
	@POST
	@Path("/getSingleTemplateModel")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSingleTemplateModel(@DefaultValue("") @FormParam("templateGuid") String templateGuid){
		JedisPool pool = null;
		Jedis jedis = null;	
		String json="";
		try {
			pool = RedisUtils.getPool();
			jedis = pool.getResource();	
			json=jedis.get(templateGuid+key_partern);
		} catch (Exception e) {
			pool.returnBrokenResource(jedis);
			e.printStackTrace();
		} finally {
			RedisUtils.returnResource(pool, jedis);
		}
		return json;
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
	 * 保存种子到本地文件
	 * */
	private void SaveSeedsValueToFile(HashMap<String, String> seeds) {
		StringBuffer stringBuilder = new StringBuffer();
		try {
			for (Iterator<Entry<String, String>> it = seeds.entrySet()
					.iterator(); it.hasNext();) {
				Entry<String, String> entry = it.next();
				String seed = entry.getValue();
				stringBuilder.append(seed
						+ System.getProperty("line.separator"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (System.getProperty("os.name").indexOf("Linux") > -1) {
			contentToTxt("/home/linux/drcnet.com.cn_1day_01/seed.txt",
					stringBuilder.toString());
		} else if (System.getProperty("os.name").indexOf("Windows") > -1) {
			contentToTxt("C:\\drcnet.com.cn_1day_01\\seed.txt",
					stringBuilder.toString());
		}

	}

	/**
	 * 保存种子到本地文件
	 * */
	private void SaveSeedsValueToFile(ArrayList<String> seeds) {
		StringBuffer stringBuilder = new StringBuffer();
		try {
			for (String seed : seeds) {
				stringBuilder.append(seed
						+ System.getProperty("line.separator"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (System.getProperty("os.name").indexOf("Linux") > -1) {
			contentToTxt("/home/linux/drcnet.com.cn_1day_01/seed.txt",
					stringBuilder.toString());
		} else if (System.getProperty("os.name").indexOf("Windows") > -1) {
			contentToTxt("C:\\drcnet.com.cn_1day_01\\seed.txt",
					stringBuilder.toString());
		}
	}

	/**
	 * 
	 * 从redis中获取ParseResult结果
	 * */
	private ParseResult GetParseResultFromRedis(PageModel pageModel) {
		String guid = MD5Utils.MD5(pageModel.getBasicInfoViewModel().getUrl());
		ParseResult parseResult = null;
		TemplateResult templateResult = RedisUtils.getTemplateResult(guid);
		String parseResultGuid = templateResult.getParseResultGuid(); // 获取中间结果
		parseResult = RedisUtils.getParseResult(parseResultGuid);
		return parseResult;
	}

	/**
	 * 返回ParseResult
	 * */
	private ParseResult GetParseResultByLocalProcess(String encoding,
			String templateUrl, PageModel pageModel, String parseType) {
		ParseResult parseResult = null;
		TemplateResult templateResult = null;
		byte[] input = DownloadHtml.getHtml(templateUrl);
		try {
			templateResult = GetTemplateResult(pageModel);
			parseResult = TemplateFactory.localProcess(input, encoding,
					templateUrl, templateResult, parseType);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return parseResult;
	}

	/**
	 * 
	 * 保存模板配置到redis
	 * */
	private ParseResult SaveTemplateToRedis(PageModel pageModel) {
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		TemplateResult templateResult = GetTemplateResult(pageModel);
		String templateGuid = MD5Utils.MD5(templateUrl);
		ParseResult parseResult = null;
		String encoding = "gb2312";
		byte[] input = DownloadHtml.getHtml(templateUrl);
		RedisUtils.setTemplateResult(templateResult, templateGuid);
		SaveTemplateToList(pageModel);//保存数据源列表所需要的key值
		System.out.println("templateGuid=" + templateGuid);
		parseResult = TemplateFactory.process(input, encoding, templateUrl);
		return parseResult;
	}
	
	/**
	 * 将redis中模板的id和数据源列表做关联
	 * */
	private void SaveTemplateToList(PageModel pageModel) {
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		JedisPool pool = null;
		Jedis jedis = null;
		TemplateModel templateModel=new TemplateModel();
		templateModel.setTemplateId(templateGuid);
		templateModel.setName(pageModel.getBasicInfoViewModel().getName());
		templateModel.setDescription(pageModel.getBasicInfoViewModel().getName());
		templateModel.setUrl(pageModel.getBasicInfoViewModel().getUrl());
		templateModel.setStatus("true");
		try {
			StringBuilder str = new StringBuilder();
			str.append(GetTemplateModelJSONString(templateModel));
			pool = RedisUtils.getPool();
			jedis = pool.getResource();
			jedis.set(templateGuid+key_partern, str.toString());
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
	 * 生成模板对象
	 * */
	private TemplateResult GetTemplateResult(PageModel pageModel) {
		TemplateResult template = new TemplateResult();
		template.setType(Constants.TEMPLATE_LIST);
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		template.setTemplateGuid(templateGuid);

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
			indexer.initJsoupIndexer(pageModel.getListOutLinkViewModel()
					.getSelector(), pageModel.getListOutLinkViewModel()
					.getSelectorAttr());
			selector.initContentSelector(indexer, null);
			list.add(selector);
			template.setList(list);
		}

		// 处理列表自定义属性 以时间为例
		List<CustomerAttrModel> listCustomerAttrViewModel = pageModel
				.getListCustomerAttrViewModel();
		for (CustomerAttrModel model : listCustomerAttrViewModel) {
			Selector label = new Selector();
			label.setType(Constants.SELECTOR_LABEL);
			indexer = new SelectorIndexer();
			indexer.initJsoupIndexer(model.getSelector(), model.getAttr());
			filter = new SelectorFilter();
			String filterString = model.getFilter();
			String filterCategory = model.getFilterCategory();
			if (filterCategory.equals("匹配")) {
				filter.initMatchFilter(filterString);
			} else if (filterCategory.equals("替换")) {
				filter.initReplaceFilter(model.getReplaceBefore(),
						model.getReplaceTo());
			} else if (filterCategory.equals("移除")) {
				filter.initRemoveFilter(filterString);
			}
			label.initLabelSelector(model.getTarget(), "", indexer, filter,
					null);
			selector.setLabel(label);
			list.add(selector);
			template.setList(list);
		}

		// pagitation outlink js翻页无法处理
		indexer = new SelectorIndexer();
		selector = new Selector();
		indexer.initJsoupIndexer(pageModel.getListPaginationViewModel()
				.getSelector(), pageModel.getListPaginationViewModel()
				.getSelectorAttr());

		filter = new SelectorFilter();
		String paginationFilter = pageModel.getListPaginationViewModel()
				.getFilter();
		String paginationFilterCategory = pageModel
				.getListPaginationViewModel().getFilterCategory();
		// 替换前
		String replaceBefore = pageModel.getListPaginationViewModel()
				.getReplaceBefore();
		// 替换后
		String replaceToString = pageModel.getListPaginationViewModel()
				.getReplaceTo();
		if (paginationFilterCategory.equals("匹配")) {
			filter.initMatchFilter(paginationFilter);
		} else if (paginationFilterCategory.equals("替换")) {
			filter.initReplaceFilter(replaceBefore, replaceToString);
		} else if (paginationFilterCategory.equals("移除")) {
			filter.initRemoveFilter(paginationFilter);
		}

		String paginationType = pageModel.getListPaginationViewModel()
				.getPaginationType();
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
				paginationInterval = Integer.parseInt(pageModel
						.getListPaginationViewModel().getInterval());
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				paginationInterval = 0;
			}
		}

		// 按照是否使用分页进步数,调用不同的方法
		if (paginationInterval != 0) {
			selector.initPagitationSelector(paginationType, pageModel
					.getListPaginationViewModel().getCurrentString(), pageModel
					.getListPaginationViewModel().getReplaceTo(), pageModel
					.getListPaginationViewModel().getPaginationUrl(), pageModel
					.getListPaginationViewModel().getStart(), pageModel
					.getListPaginationViewModel().getRecords(),
					paginationInterval, indexer, filter, null);
		} else {
			// Constants.PAGINATION_TYPE_PAGENUMBER 需要取值
			selector.initPagitationSelector(paginationType, pageModel
					.getListPaginationViewModel().getCurrentString(), pageModel
					.getListPaginationViewModel().getReplaceTo(), pageModel
					.getListPaginationViewModel().getPaginationUrl(), pageModel
					.getListPaginationViewModel().getStart(), pageModel
					.getListPaginationViewModel().getRecords(), indexer,
					filter, null);
		}

		if (!pageModel.getListPaginationViewModel().getSelector().equals("")) {
			pagination.add(selector);
			template.setPagination(pagination);
		}

		// title
		indexer = new SelectorIndexer();
		selector = new Selector();
		if (!pageModel.getNewsTitleViewModel().getSelector().equals("")) {
			indexer.initJsoupIndexer(pageModel.getNewsTitleViewModel()
					.getSelector(), pageModel.getNewsTitleViewModel()
					.getSelectorAttr());
			selector.initFieldSelector("title", "", indexer, null, null);
			news.add(selector);
		}

		// content
		indexer = new SelectorIndexer();
		selector = new Selector();
		if (!pageModel.getNewsContentViewModel().getSelector().equals("")) {
			indexer.initJsoupIndexer(pageModel.getNewsContentViewModel()
					.getSelector(), pageModel.getNewsContentViewModel()
					.getSelectorAttr());
			selector.initFieldSelector("content", "", indexer, null, null);
			news.add(selector);
			template.setNews(news);
		}

		// public time
		indexer = new SelectorIndexer();
		selector = new Selector();
		if (!pageModel.getNewsPublishTimeViewModel().getSelector().equals("")) {
			indexer.initJsoupIndexer(pageModel.getNewsPublishTimeViewModel()
					.getSelector(), pageModel.getNewsPublishTimeViewModel()
					.getSelectorAttr());
			selector.initFieldSelector("publisTime", "", indexer, null, null);
			news.add(selector);
			template.setNews(news);
		}

		// source
		indexer = new SelectorIndexer();
		selector = new Selector();
		if (!pageModel.getNewsSourceViewModel().getSelector().equals("")) {
			indexer.initJsoupIndexer(pageModel.getNewsSourceViewModel()
					.getSelector(), pageModel.getNewsSourceViewModel()
					.getSelectorAttr());
			selector.initFieldSelector("source", "", indexer, null, null);
			news.add(selector);
			template.setNews(news);
		}

		// 处理内容自定义属性 以时间为例
		List<CustomerAttrModel> newsCustomerAttrViewModel = pageModel
				.getNewsCustomerAttrViewModel();
		for (CustomerAttrModel model : newsCustomerAttrViewModel) {
			indexer = new SelectorIndexer();
			selector = new Selector();
			if (!model.getSelector().equals("")) {
				indexer.initJsoupIndexer(model.getSelector(), model.getAttr());
				selector.initFieldSelector(model.getTarget(), "", indexer,
						null, null);
				news.add(selector);
				template.setNews(news);
			}
		}

		return template;
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
	 * 保存内容到文件
	 * */
	private void contentToTxt(String filePath, String content) {
		String str = new String(); // 原有txt内容
		String s1 = new String();// 内容更新
		try {
			File f = new File(filePath);
			File parentDir = f.getParentFile();
			if (parentDir != null && !parentDir.exists()) {
				parentDir.mkdirs();
			}
			if (f.exists()) {
				// System.out.print("文件已经存在");
			} else {
				f.createNewFile();// 不存在则创建
			}
			BufferedReader input = new BufferedReader(new FileReader(f));

			while ((str = input.readLine()) != null) {
				s1 += str + "\n";
			}
			// System.out.println(s1);
			input.close();
			s1 += content;

			BufferedWriter output = new BufferedWriter(new FileWriter(f));
			output.write(s1);
			output.close();
			System.out.println("文件保存路径:" + filePath);
		} catch (Exception e) {
			e.printStackTrace();

		}
	}
}
