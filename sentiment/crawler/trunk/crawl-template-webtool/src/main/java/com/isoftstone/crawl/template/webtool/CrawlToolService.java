package com.isoftstone.crawl.template.webtool;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.mortbay.log.Log;

import com.isoftstone.crawl.template.consts.WebtoolConstants;
import com.isoftstone.crawl.template.crawlstate.CrawlState;
import com.isoftstone.crawl.template.impl.ParseResult;
import com.isoftstone.crawl.template.impl.TemplateFactory;
import com.isoftstone.crawl.template.impl.TemplateResult;
import com.isoftstone.crawl.template.model.BasicInfoViewModel;
import com.isoftstone.crawl.template.model.CrawlDataModel;
import com.isoftstone.crawl.template.model.CrawlDataModelList;
import com.isoftstone.crawl.template.model.CrawlStateBean;
import com.isoftstone.crawl.template.model.CrawlStatusModel;
import com.isoftstone.crawl.template.model.CrawlStatusModelList;
import com.isoftstone.crawl.template.model.ListPaginationViewModel;
import com.isoftstone.crawl.template.model.PageModel;
import com.isoftstone.crawl.template.model.ResponseJSONProvider;
import com.isoftstone.crawl.template.model.SearchKeyWordDataModel;
import com.isoftstone.crawl.template.model.SearchKeyWordModel;
import com.isoftstone.crawl.template.model.SeedsEffectiveStatusList;
import com.isoftstone.crawl.template.model.SeedsEffectiveStatusModel;
import com.isoftstone.crawl.template.model.TemplateList;
import com.isoftstone.crawl.template.model.TemplateModel;
import com.isoftstone.crawl.template.model.TemplateTagModel;
import com.isoftstone.crawl.template.utils.Config;
import com.isoftstone.crawl.template.utils.CrawlDataModelComparator;
import com.isoftstone.crawl.template.utils.CrawlStatusModelComparator;
import com.isoftstone.crawl.template.utils.DownloadHtml;
import com.isoftstone.crawl.template.utils.EncodeUtils;
import com.isoftstone.crawl.template.utils.MD5Utils;
import com.isoftstone.crawl.template.utils.RedisOperator;
import com.isoftstone.crawl.template.utils.SeedsEffectiveModelComparator;
import com.isoftstone.crawl.template.utils.SolrSerach;
import com.isoftstone.crawl.template.utils.StatusMonitorCache;
import com.isoftstone.crawl.template.utils.TemplateModelComparator;

/**
 * 
 * 爬虫工具restful-services服务类
 * */
@Path("crawlToolResource")
public class CrawlToolService {

	/**
	 * 保存到本地文件
	 * */
	@POST
	@Path("/saveToLocalFile")
	@Produces(MediaType.TEXT_PLAIN)
	public String saveToLocalFile(@DefaultValue("") @FormParam("data") String data) {
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		CrawlToolResource serviceHelper = new CrawlToolResource();
		PageModel pageModel = serviceHelper.getPageModelByJsonString(data);
		String domain = pageModel.getScheduleDispatchViewModel().getDomain();
		// String period = pageModel.getScheduleDispatchViewModel().getPeriod();
		String period = pageModel.getTemplateIncreaseViewModel().getPeriod();
		String sequence = pageModel.getScheduleDispatchViewModel().getSequence();
		boolean userProxy = pageModel.getScheduleDispatchViewModel().getUseProxy();
		if (sequence == null || sequence.equals("")) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("保存失败，请输入时序.");
			return jsonProvider.toJSON();
		}
		String folderName = domain + "_" + "1" + period + "_" + sequence;
		String incrementFolderName = domain + "_" + "1" + period + "_" + WebtoolConstants.INCREMENT_FILENAME_SIGN + "_" + sequence;
		ParseResult parseResult = serviceHelper.saveTemplateAndParseResult(pageModel);
		if (parseResult == null) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("请先保存模板!再执行此操作!");
			return jsonProvider.toJSON();
		}
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		String redisKey = templateGuid + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN;
		TemplateModel templateModel = serviceHelper.getTemplateModel(redisKey);
		String status = templateModel.getStatus();

		// --增量相关.
		String incrementPageCountStr = pageModel.getTemplateIncreaseViewModel().getPageCounts();
		if (incrementPageCountStr == null || "".equals(incrementPageCountStr)) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("保存失败，请输入增量需要爬取的页数");
			return jsonProvider.toJSON();
		}
		int incrementPageCount = Integer.valueOf(incrementPageCountStr);
		if (incrementPageCount > 0) {
			incrementPageCount = incrementPageCount - 1;
		}
		String pageSort = pageModel.getTemplateIncreaseViewModel().getPageSort();
		ArrayList<String> seedsTemp = TemplateFactory.getPaginationOutlink(parseResult);
		ArrayList<String> seeds = new ArrayList<String>();
		seeds.add(templateUrl);
		if ("升序".equals(pageSort)) {
			for (int i = 0; i < incrementPageCount && i < seedsTemp.size(); i++) {
				seeds.add(seedsTemp.get(i));
			}
		} else {
			for (int i = seedsTemp.size() - 1; i >= 0 && incrementPageCount > 0; i--, incrementPageCount--) {
				seeds.add(seedsTemp.get(i));
			}
		}
		String paginationUrl = pageModel.getListPaginationViewModel().getPaginationUrl();
		String currentString = pageModel.getListPaginationViewModel().getCurrentString();
		String start = pageModel.getListPaginationViewModel().getStart();

		serviceHelper.saveSeedsValueToFile(folderName, incrementFolderName, templateUrl, seeds, status, userProxy, paginationUrl, currentString, start);
		jsonProvider.setSuccess(true);
		jsonProvider.setData("文件保存成功!");
		return jsonProvider.toJSON();
	}

	/**
	 * 验证内容页
	 * */
	@POST
	@Path("/verifyNewContent")
	@Produces(MediaType.TEXT_PLAIN)
	public String verifyNewContent(@DefaultValue("") @FormParam("data") String data) {
		ResponseJSONProvider<ParseResult> jsonProvider = new ResponseJSONProvider<ParseResult>();
		CrawlToolResource serviceHelper = new CrawlToolResource();
		PageModel pageModel = serviceHelper.getPageModelByJsonString(data);
		ParseResult parseResult = serviceHelper.saveParseResult(pageModel);
		if (parseResult == null) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("无法完成页面解析，请检查选择器和过滤器正确性，确认后重新保存模板后，重试！");
			return jsonProvider.toJSON();
		}
		// 获取内容页链接
		ArrayList<String> contentOutLinkArrayList = TemplateFactory.getContentOutlink(parseResult);
		if (contentOutLinkArrayList == null) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("列表外链接配置信息不正确！");
			return jsonProvider.toJSON();
		}

		if (contentOutLinkArrayList.size() == 0) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("列表外链接配置信息不正确！");
			return jsonProvider.toJSON();
		}

		int maxRandomNumber = contentOutLinkArrayList.size() - 1;
		if (maxRandomNumber < 0 || maxRandomNumber == 0) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("列表外链接配置信息不正确！");
			return jsonProvider.toJSON();
		}

		int contentOutLinkIndex = serviceHelper.getRandomNumber(0, maxRandomNumber);
		String contentOutLink = contentOutLinkArrayList.get(contentOutLinkIndex);
		byte[] input = null;
		try {
			input = DownloadHtml.getHtml(contentOutLink);
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("访问内容页链接： [" + contentOutLink + "] 失败，网络访问异常！");
			e.printStackTrace();
			return jsonProvider.toJSON();
		}

		String encoding = CrawlToolResource.sniffCharacterEncoding(input);
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
	public String verifyListContent(@DefaultValue("") @FormParam("data") String data) {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		PageModel pageModel = serviceHelper.getPageModelByJsonString(data);
		ResponseJSONProvider<ParseResult> jsonProvider = new ResponseJSONProvider<ParseResult>();
		ParseResult parseResult = serviceHelper.saveParseResult(pageModel);
		if (parseResult == null) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("无法完成页面解析，请检查选择器和过滤器正确性，确认后重新保存模板后，重试！");
		} else {
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
	public String getJSONString(@DefaultValue("") @FormParam("data") String data) {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		PageModel pageModel = serviceHelper.getPageModelByJsonString(data);
		ResponseJSONProvider<TemplateResult> jsonProvider = new ResponseJSONProvider<TemplateResult>();
		TemplateResult templateResult = serviceHelper.getTemplateResult(pageModel);
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
	public String saveTemplate(@DefaultValue("") @FormParam("data") String data) {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		PageModel pageModel = serviceHelper.getPageModelByJsonString(data);
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		// 保存常规模板
		jsonProvider = serviceHelper.saveTemplateResultToRedis(pageModel);
		if (jsonProvider.getSuccess() == false) {
			return jsonProvider.toJSON();
		}
		// 保存增量模板
		jsonProvider = serviceHelper.saveIncreaseTemplateResult(pageModel);
		if (jsonProvider.getSuccess() == false) {
			return jsonProvider.toJSON();
		}
		// 保存到文件
		jsonProvider = serviceHelper.getResponseJSONProvider(saveToLocalFile(data));
		if (jsonProvider.getSuccess() == false) {
			return jsonProvider.toJSON();
		}
		jsonProvider.setSuccess(true);
		jsonProvider.setData("模板保存成功！");
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 保存增量模板
	 * */
	@POST
	@Path("/saveIncreaseTemplate")
	@Produces(MediaType.TEXT_PLAIN)
	public String saveIncreaseTemplate(@DefaultValue("") @FormParam("data") String data) {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		PageModel pageModel = serviceHelper.getPageModelByJsonString(data);
		ResponseJSONProvider<String> jsonProvider = serviceHelper.saveIncreaseTemplateResult(pageModel);
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
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		String htmlContent = "";
		try {
			byte[] input = DownloadHtml.getHtml(webUrl);
			String encoding = CrawlToolResource.sniffCharacterEncoding(input);
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
	public String deleteTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl) {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		String templateGuid = MD5Utils.MD5(templateUrl);
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		jsonProvider.setSuccess(true);
		jsonProvider.setData("删除成功！");
		long effectCounts = -1;

		// 先删除增量模板
		String jsonString = RedisOperator.getFromDefaultDB(templateGuid + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
		TemplateModel templateModel = serviceHelper.getTemplateModelByJSONString(jsonString);
		List<String> increaseTemplateIdList = templateModel.getTemplateIncreaseIdList();
		if (increaseTemplateIdList != null) {
			for (String increaseTemplateId : increaseTemplateIdList) {
				effectCounts = RedisOperator.delFromIncreaseDB(increaseTemplateId);
				if (effectCounts < 0) {
					jsonProvider.setSuccess(false);
					jsonProvider.setErrorMsg("删除模板失败");
					jsonProvider.setData(null);
				}
			}
		}

		// 最后删除模板列表
		effectCounts = RedisOperator.delFromDefaultDB((templateGuid + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN), templateGuid);
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
	public String updateTemplate(@DefaultValue("") @FormParam("templateGuid") String templateGuid) {
		ResponseJSONProvider<TemplateResult> jsonProvider = new ResponseJSONProvider<TemplateResult>();
		TemplateResult templateResult = RedisOperator.getTemplateResultFromDefaultDB(templateGuid);
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
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
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
	public String getTemplateList() {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<TemplateList> jsonProvider = new ResponseJSONProvider<TemplateList>();
		jsonProvider.setSuccess(true);
		TemplateList templateList = new TemplateList();
		List<TemplateModel> templateListArrayList = new ArrayList<TemplateModel>();
		try {
			Set<String> listKeys = RedisOperator.searchKeysFromDefaultDB("*" + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
			if (listKeys != null) {
				for (String key : listKeys) {
					String templateString = RedisOperator.getFromDefaultDB(key);
					TemplateModel templateModel = serviceHelper.getTemplateModelByJSONString(templateString);
					templateListArrayList.add(templateModel);
				}
			}
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("Redis操作异常！");
			e.printStackTrace();
		}
		// 列表按名称排序
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
	public String searchTemplateList(@DefaultValue("") @FormParam("searchString") String searchString) {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<TemplateList> jsonProvider = new ResponseJSONProvider<TemplateList>();
		jsonProvider.setSuccess(true);
		if (searchString.equals("启用")) {
			searchString = "true";
		} else if (searchString.equals("停用")) {
			searchString = "false";
		}

		TemplateList templateList = new TemplateList();
		List<TemplateModel> templateListArrayList = new ArrayList<TemplateModel>();
		try {
			Set<String> listKeys = RedisOperator.searchKeysFromDefaultDB("*" + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
			if (listKeys != null) {
				for (String key : listKeys) {
					String templateString = RedisOperator.getFromDefaultDB(key);
					TemplateModel templateModel = serviceHelper.getTemplateModelByJSONString(templateString);
					if (StringUtils.isBlank(searchString)) {
						templateListArrayList.add(templateModel);
					} else if (searchString.equals("false") || searchString.equals("true")) {
						if (templateModel.getStatus().equals(searchString)) {
							templateListArrayList.add(templateModel);
						}
					} else {
						if (StringUtils.contains(templateString, searchString)) {
							templateListArrayList.add(templateModel);
						}
					}
				}
			}
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("Redis操作异常！");
			e.printStackTrace();
		}
		// 列表按名称排序
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
	public String disableTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl, @DefaultValue("") @FormParam("name") String name) {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		serviceHelper.setTemplateStatus(templateUrl, name, "false");
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
	public String enableTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl, @DefaultValue("") @FormParam("name") String name) {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		serviceHelper.setTemplateStatus(templateUrl, name, "true");
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
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		String json = RedisOperator.getFromDefaultDB(templateGuid + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
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
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		jsonProvider.setSuccess(true);
		jsonProvider.setData("导出模板操作成功！");
		String newFilePath = "";
		try {
			newFilePath = serviceHelper.getFilePathByOSPlatForm(filePath);
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("pathInvalid");
			jsonProvider.setData(null);
			return jsonProvider.toJSON();
		}

		try {
			Set<String> listKeys = RedisOperator.searchKeysFromDefaultDB("*" + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
			if (listKeys != null) {
				for (String key : listKeys) {
					String templateString = RedisOperator.getFromDefaultDB(key);
					TemplateModel templateModel = serviceHelper.getTemplateModelByJSONString(templateString);
					String templateGuid = templateModel.getTemplateId();
					String templateJsonString = RedisOperator.getFromDefaultDB(templateGuid);
					String templateFileName = templateGuid + WebtoolConstants.TEMPLATE_FILE_EXTENTIONS_NAME;
					String templateListName = key + WebtoolConstants.TEMPLATE_FILE_EXTENTIONS_NAME;
					// 保存模板
					serviceHelper.exportTemplateJSONStringToFile(newFilePath + templateFileName, templateJsonString);
					// 保存模板列表
					serviceHelper.exportTemplateJSONStringToFile(newFilePath + templateListName, templateString);
					// 增量模板
					List<String> increaseTemplateIdList = templateModel.getTemplateIncreaseIdList();
					if (increaseTemplateIdList != null) {
						for (String increaseTemplateId : increaseTemplateIdList) {
							String increaseTemplateJsonString = RedisOperator.getFromIncreaseDB(increaseTemplateId);
							if (StringUtils.isEmpty(increaseTemplateJsonString)) {
								continue;
							}
							String increaseTemplateFileName = increaseTemplateId + WebtoolConstants.INCREASE_TEMPLATE_PARTERN + WebtoolConstants.TEMPLATE_FILE_EXTENTIONS_NAME;
							// 导出增量模板
							serviceHelper.exportTemplateJSONStringToFile(newFilePath + increaseTemplateFileName, increaseTemplateJsonString);
						}
					}
				}
			}
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("导出模板操作失败！");
			jsonProvider.setData(null);
			e.printStackTrace();
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
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		jsonProvider.setSuccess(true);
		jsonProvider.setData("导入模板操作成功！");
		String newFilePath = "";
		try {
			newFilePath = serviceHelper.getFilePathByOSPlatForm(dirPath);
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("pathInvalid");
			jsonProvider.setData(null);
			return jsonProvider.toJSON();
		}

		try {
			File file = new File(newFilePath);
			File[] files = file.listFiles();
			for (File f : files) {
				if (f.isFile()) {
					String fileName = f.getName();
					String templateString = serviceHelper.importTemplateJSONString(newFilePath + fileName);
					String templateGuid = null;
					if (fileName.contains(WebtoolConstants.INCREASE_TEMPLATE_PARTERN)) {
						templateGuid = fileName.substring(0, fileName.lastIndexOf(WebtoolConstants.INCREASE_TEMPLATE_PARTERN));
						RedisOperator.setToIncreaseDB(templateGuid, templateString);
					} else {
						templateGuid = fileName.substring(0, fileName.lastIndexOf("."));
						RedisOperator.setToDefaultDB(templateGuid, templateString);
					}
				}
			}
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("导入模板操作失败！");
			jsonProvider.setData(null);
			e.printStackTrace();
		}

		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 根据关键字，自动批量生成搜索引擎模板
	 * */
	@POST
	@Path("/bulkSearchTemplates")
	@Produces(MediaType.TEXT_PLAIN)
	public String bulkSearchTemplates(@DefaultValue("") @FormParam("data") String data) {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		StringBuilder sbString = new StringBuilder();
		int failedTemplateCount = 0;
		jsonProvider.setSuccess(true);
		jsonProvider.setData("关键字对应的搜索引擎模板，已全部生成！请回到列表页面，并刷新!");
		SearchKeyWordModel searchKeyWordModel = serviceHelper.getSearchKeyWordModel();
		String keyWordURL = Config.getValue(WebtoolConstants.SEARCH_KEYWORD_API_URL);
		if (StringUtils.isBlank(keyWordURL)) {
			jsonProvider.setSuccess(false);
			jsonProvider.setData("配置文件中缺少，关键字服务地址，请检查相关配置！");
			return jsonProvider.toJSON();
		}
		if (searchKeyWordModel == null) {
			jsonProvider.setSuccess(false);
			jsonProvider.setData("未能成功获取到关键字信息，请检查关键字获取地址是否有效，地址如下：" + keyWordURL + "<br/>");
			return jsonProvider.toJSON();
		}
		if (searchKeyWordModel.getData().size() == 0) {
			jsonProvider.setSuccess(false);
			jsonProvider.setData("关键字服务中没有可以提取的关键字信息，关键字服务地址如下：" + keyWordURL + "<br/>");
			return jsonProvider.toJSON();
		}

		String searchEngineType = serviceHelper.getSearchEngineType(serviceHelper.getPageModelByJsonString(data));
		List<SearchKeyWordDataModel> originalKeyWordModelList = searchKeyWordModel.getData();
		// 得到当前搜索引擎需要搜索的关键字信息
		List<SearchKeyWordDataModel> keyWordModelList = serviceHelper.getKeyWordModelList(originalKeyWordModelList, searchEngineType);

		for (SearchKeyWordDataModel model : keyWordModelList) {
			// 搜索关键字
			String searchKeyWord = model.getTagWords();
			PageModel pageModel = serviceHelper.getPageModelByJsonString(data);
			jsonProvider = serviceHelper.validPageModelBeforeSave(pageModel);
			if (jsonProvider.getSuccess() == false) {
				return jsonProvider.toJSON();
			}

			// 根据words关键字，处理pageModel中的相关信息:URL,名称,Tags,查询关键字,并重新赋值
			String templateURL = pageModel.getBasicInfoViewModel().getUrl();
			String currentString = pageModel.getBasicInfoViewModel().getCurrentString();
			templateURL = templateURL.replace(currentString, searchKeyWord);
			String encodeTemplateUrl = "";
			try {
				encodeTemplateUrl = EncodeUtils.formatUrl(templateURL, "");
			} catch (Exception e) {
				e.printStackTrace();
			}
			String templateGuid = MD5Utils.MD5(encodeTemplateUrl);

			// 处理URL及名称
			BasicInfoViewModel basicInfoViewModel = pageModel.getBasicInfoViewModel();
			String templateType = basicInfoViewModel.getTemplateType();
			if (templateType.equals(WebtoolConstants.BAIDU_SEARCH_NAME)) {
				basicInfoViewModel.setName(WebtoolConstants.BAIDU_SEARCH_NAME + "-" + searchKeyWord);
			} else if (templateType.equals(WebtoolConstants.BING_SEARCH_NAME)) {
				basicInfoViewModel.setName(WebtoolConstants.BING_SEARCH_NAME + "-" + searchKeyWord);
			} else if (templateType.equals(WebtoolConstants.SOUGOU_SEARCH_NAME)) {
				basicInfoViewModel.setName(WebtoolConstants.SOUGOU_SEARCH_NAME + "-" + searchKeyWord);
			}
			String encodedSearchKeyWord = "";
			try {
				encodedSearchKeyWord = EncodeUtils.formatUrl(searchKeyWord, "");
			} catch (Exception e) {
				e.printStackTrace();
			}

			basicInfoViewModel.setCurrentString(encodedSearchKeyWord);
			basicInfoViewModel.setUrl(encodeTemplateUrl);

			// 处理列表分页链接中的URL
			ListPaginationViewModel paginationViewModel = pageModel.getListPaginationViewModel();
			if (paginationViewModel == null) {
				jsonProvider.setSuccess(false);
				jsonProvider.setData("列表分页配置不正确，请检查！");
				return jsonProvider.toJSON();
			}
			String paginationURL = paginationViewModel.getPaginationUrl();
			paginationURL = paginationURL.replace(currentString, searchKeyWord);
			String encodepaginationURL = "";
			try {
				encodepaginationURL = EncodeUtils.formatUrl(paginationURL, "");
			} catch (Exception e) {
				e.printStackTrace();
			}
			paginationViewModel.setPaginationUrl(encodepaginationURL);

			// 处理静态属性Tags，无论是否配置静态属性tags值，都需要处理
			List<TemplateTagModel> templateTagsViewModel = pageModel.getTemplateTagsViewModel();
			if (templateTagsViewModel != null) {
				if (templateTagsViewModel.size() > 0) {
					serviceHelper.getSearchEngineTagsViewModel(templateTagsViewModel, searchKeyWord, templateType);
				} else {
					templateTagsViewModel = serviceHelper.getSearchEngineTagsViewModel(searchKeyWord);
				}
			} else {// 没有设置模板静态属性的时候
				templateTagsViewModel = serviceHelper.getSearchEngineTagsViewModel(searchKeyWord);
			}

			// 构造新的pageModel
			pageModel.setBasicInfoViewModel(basicInfoViewModel);
			pageModel.setTemplateTagsViewModel(templateTagsViewModel);
			TemplateResult templateResult = serviceHelper.getTemplateResult(pageModel);
			RedisOperator.saveTemplateToDefaultDB(templateResult, templateResult.getTemplateGuid());
			// 保存数据源列表所需要的key值 模板默认为启用状态
			serviceHelper.saveTemplateToList(pageModel, "true");
			// 同时导出到文件
			saveToLocalFile(pageModel.toJSON());
			// 同时生成增量模板
			String templateModelJSONString = RedisOperator.getFromDefaultDB(templateGuid + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
			TemplateModel templateModel = serviceHelper.getTemplateModelByJSONString(templateModelJSONString);
			ResponseJSONProvider<String> saveResult = serviceHelper.saveIncreaseTemplateResult(templateModel, "../");
			if (saveResult.getErrorMsg() != null) {
				failedTemplateCount++;
				sbString.append("<div class=\"alert alert-danger\" role=\"alert\"><span class=\"glyphicon glyphicon-exclamation-sign\" aria-hidden=\"true\"></span><span class=\"sr-only\">Error:</span>"
						+ saveResult.getErrorMsg() + "</div>");
			}

		}

		if (failedTemplateCount > 0) {
			sbString.append("<div class=\"bg-success\">&nbsp;&nbsp;&nbsp;汇总结果：成功生成" + keyWordModelList.size() + "个模板，其中" + failedTemplateCount + "个未成功生成增量模板，请根据上述模板名称，检查相应的模板配置！</div>");
		} else {
			sbString.append("<div class=\"bg-success\">&nbsp;&nbsp;&nbsp;汇总结果：成功生成" + keyWordModelList.size() + "个模板，和这些模板相关联的增量模板也全部生成成功!</div>");
		}
		jsonProvider.setData(sbString.toString());

		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 根据关键字，自动批量生成增量模板
	 * */
	@GET
	@Path("/generateAllIncreaseTemplates")
	@Produces(MediaType.TEXT_PLAIN)
	public String generateAllIncreaseTemplates() {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		jsonProvider.setSuccess(true);
		StringBuilder sbString = new StringBuilder();
		Set<String> templateListKeys = RedisOperator.searchKeysFromDefaultDB("*" + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
		int failedTemplateCount = 0;
		for (String listKey : templateListKeys) {
			String templateModelJSONString = RedisOperator.getFromDefaultDB(listKey);
			TemplateModel templateModel = serviceHelper.getTemplateModelByJSONString(templateModelJSONString);
			// TemplateResult templateResult =
			// RedisOperator.getTemplateResultFromDefaultDB(templateModel.getTemplateId());
			// PageModel pageModel =
			// serviceHelper.convertTemplateResultToPageModel(templateModel,
			// templateResult);
			// 同时导出到文件
			// Log.info("开始批量导入文件.");
			// saveToLocalFile(pageModel.toJSON());
			// Log.info("批量导入文件完成.");
			ResponseJSONProvider<String> saveResult = serviceHelper.saveIncreaseTemplateResult(templateModel, "");
			if (saveResult.getErrorMsg() != null) {
				failedTemplateCount++;
				sbString.append("<div class=\"alert alert-danger\" role=\"alert\"><span class=\"glyphicon glyphicon-exclamation-sign\" aria-hidden=\"true\"></span><span class=\"sr-only\">Error:</span>"
						+ saveResult.getErrorMsg() + "</div>");
			}
		}
		if (failedTemplateCount > 0) {
			sbString.append("<div class=\"bg-success\">&nbsp;&nbsp;&nbsp;汇总结果：共" + templateListKeys.size() + "个模板，其中" + failedTemplateCount + "个未成功生成增量模板，请根据上述模板名称，检查相应的模板配置！</div>");
		} else {
			sbString.append("<div class=\"bg-success\">&nbsp;&nbsp;&nbsp;汇总结果：共" + templateListKeys.size() + "个模板,全部成功生成增量模板!</div>");
		}

		jsonProvider.setData(sbString.toString());
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 批量检查种子有效性
	 * */
	@GET
	@Path("/getSeedsEffectiveStatusList")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSeedsEffectiveStatusList() {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<SeedsEffectiveStatusList> jsonProvider = new ResponseJSONProvider<SeedsEffectiveStatusList>();

		SeedsEffectiveStatusList seedsEffectiveStatusList = new SeedsEffectiveStatusList();
		List<SeedsEffectiveStatusModel> seedsEffectiveStatusModelList = new ArrayList<SeedsEffectiveStatusModel>();
		try {
			Set<String> listKeys = RedisOperator.searchKeysFromDefaultDB("*" + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
			if (listKeys != null) {
				for (String key : listKeys) {
					String templateString = RedisOperator.getFromDefaultDB(key);
					TemplateModel templateModel = serviceHelper.getTemplateModelByJSONString(templateString);
					Date currentDate = new Date();
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String nowDateString = dateFormat.format(currentDate);
					SeedsEffectiveStatusModel seedsEffectiveStatusModel = new SeedsEffectiveStatusModel();
					seedsEffectiveStatusModel.setTemplateId(templateModel.getTemplateId());
					seedsEffectiveStatusModel.setUrl(templateModel.getBasicInfoViewModel().getUrl());
					seedsEffectiveStatusModel.setDescription(templateModel.getDescription());
					seedsEffectiveStatusModel.setName(templateModel.getBasicInfoViewModel().getName());
					seedsEffectiveStatusModel.setCheckTime(nowDateString);
					TemplateResult templateResult = RedisOperator.getTemplateResultFromDefaultDB(templateModel.getTemplateId());
					PageModel pageModel = serviceHelper.convertTemplateResultToPageModel(templateModel, templateResult);
					// 检查列表页
					ResponseJSONProvider<ParseResult> listJsonProvider = serviceHelper.getResponseJSONProviderObj(verifyListContent(serviceHelper.getPageModeJSONString(pageModel)));
					if (listJsonProvider.getSuccess() == false) {
						seedsEffectiveStatusModel.setEffectiveStatus(WebtoolConstants.TEMPLATE_INVALID_STATUS);
						seedsEffectiveStatusModelList.add(seedsEffectiveStatusModel);
						continue;
					}
					// 检查内容页
					ResponseJSONProvider<ParseResult> newsContentJsonProvider = serviceHelper.getResponseJSONProviderObj(verifyNewContent(serviceHelper.getPageModeJSONString(pageModel)));
					if (newsContentJsonProvider.getSuccess() == false) {
						seedsEffectiveStatusModel.setEffectiveStatus(WebtoolConstants.TEMPLATE_INVALID_STATUS);
						seedsEffectiveStatusModelList.add(seedsEffectiveStatusModel);
						continue;
					}
					// 检查增量
					ResponseJSONProvider<String> increaseJsonProvider = new ResponseJSONProvider<String>();
					increaseJsonProvider = serviceHelper.saveIncreaseTemplateResult(pageModel);
					if (increaseJsonProvider.getSuccess() == false) {
						seedsEffectiveStatusModel.setEffectiveStatus(WebtoolConstants.TEMPLATE_INVALID_STATUS);
						seedsEffectiveStatusModelList.add(seedsEffectiveStatusModel);
						continue;
					}
					seedsEffectiveStatusModel.setEffectiveStatus(WebtoolConstants.TEMPLATE_VALID_STATUS);
					seedsEffectiveStatusModelList.add(seedsEffectiveStatusModel);
				}
			}
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("Redis操作异常！");
			e.printStackTrace();
		}
		// 列表按名称排序
		Collections.sort(seedsEffectiveStatusModelList, new SeedsEffectiveModelComparator());
		seedsEffectiveStatusList.setSeedsEffectiveStatusList(seedsEffectiveStatusModelList);
		// 添加到缓存
		StatusMonitorCache.setSeedsEffectiveStatusListCache(seedsEffectiveStatusList);
		jsonProvider.setSuccess(true);
		jsonProvider.setData(seedsEffectiveStatusList);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 获取上次检查种子有效性列表
	 * */
	@GET
	@Path("/getSeedsEffectiveStatusCache")
	@Produces(MediaType.TEXT_PLAIN)
	public String getSeedsEffectiveStatusCache() {
		ResponseJSONProvider<SeedsEffectiveStatusList> jsonProvider = new ResponseJSONProvider<SeedsEffectiveStatusList>();
		jsonProvider.setSuccess(true);
		SeedsEffectiveStatusList seedsEffectiveStatusList = new SeedsEffectiveStatusList();
		List<SeedsEffectiveStatusModel> seedsEffectiveStatusModelList = StatusMonitorCache.getSeedsEffectiveStatusListCache().getSeedsEffectiveStatusList();
		seedsEffectiveStatusList.setSeedsEffectiveStatusList(seedsEffectiveStatusModelList);
		if (seedsEffectiveStatusModelList != null) {
			// 列表按名称排序
			Collections.sort(seedsEffectiveStatusModelList, new SeedsEffectiveModelComparator());
		}
		jsonProvider.setData(seedsEffectiveStatusList);
		if (seedsEffectiveStatusModelList != null) {
			jsonProvider.setTotal(seedsEffectiveStatusModelList.size());
		}

		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 刷新单条抓取数据
	 * */
	@POST
	@Path("/refreshSeedEffectiveStatus")
	@Produces(MediaType.TEXT_PLAIN)
	public String refreshSeedEffectiveStatus(@DefaultValue("") @FormParam("templateId") String templateId) {
		ResponseJSONProvider<SeedsEffectiveStatusModel> jsonProvider = new ResponseJSONProvider<SeedsEffectiveStatusModel>();
		jsonProvider.setSuccess(true);
		CrawlToolResource serviceHelper = new CrawlToolResource();
		// 先取之前的模板列表JSON字符串
		String singleTemplateListModel = RedisOperator.getFromDefaultDB(templateId + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
		TemplateModel templateModel = serviceHelper.getTemplateModelByJSONString(singleTemplateListModel);
		TemplateResult templateResult = RedisOperator.getTemplateResultFromDefaultDB(templateId);
		PageModel pageModel = serviceHelper.convertTemplateResultToPageModel(templateModel, templateResult);

		Date currentDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String nowDateString = dateFormat.format(currentDate);

		SeedsEffectiveStatusModel seedsEffectiveStatusModel = new SeedsEffectiveStatusModel();
		seedsEffectiveStatusModel.setCheckTime(nowDateString);
		// 检查列表页
		ResponseJSONProvider<ParseResult> listJsonProvider = serviceHelper.getResponseJSONProviderObj(verifyListContent(serviceHelper.getPageModeJSONString(pageModel)));
		if (listJsonProvider.getSuccess() == false) {
			seedsEffectiveStatusModel.setEffectiveStatus(WebtoolConstants.TEMPLATE_INVALID_STATUS);
			jsonProvider.setData(seedsEffectiveStatusModel);
			serviceHelper.updateSeedsEffectiveStatusCache(templateId, nowDateString, WebtoolConstants.TEMPLATE_INVALID_STATUS);
			return jsonProvider.toJSON();
		}
		// 检查内容页
		ResponseJSONProvider<ParseResult> newsContentJsonProvider = serviceHelper.getResponseJSONProviderObj(verifyNewContent(serviceHelper.getPageModeJSONString(pageModel)));
		if (newsContentJsonProvider.getSuccess() == false) {
			seedsEffectiveStatusModel.setEffectiveStatus(WebtoolConstants.TEMPLATE_INVALID_STATUS);
			jsonProvider.setData(seedsEffectiveStatusModel);
			serviceHelper.updateSeedsEffectiveStatusCache(templateId, nowDateString, WebtoolConstants.TEMPLATE_INVALID_STATUS);
			return jsonProvider.toJSON();
		}

		seedsEffectiveStatusModel.setEffectiveStatus(WebtoolConstants.TEMPLATE_VALID_STATUS);
		// 同时更新缓存中想对应的数据
		serviceHelper.updateSeedsEffectiveStatusCache(templateId, nowDateString, WebtoolConstants.TEMPLATE_VALID_STATUS);
		jsonProvider.setData(seedsEffectiveStatusModel);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 获取种子爬取状态
	 * */
	@GET
	@Path("/getCrawlStatusList")
	@Produces(MediaType.TEXT_PLAIN)
	public String getCrawlStatusList() {
		ResponseJSONProvider<CrawlStatusModelList> jsonProvider = new ResponseJSONProvider<CrawlStatusModelList>();

		CrawlStatusModelList crawlStatusModelList = new CrawlStatusModelList();
		List<CrawlStatusModel> crawlStatusModelArrayList = new ArrayList<CrawlStatusModel>();

		CrawlState crawlState = new CrawlState();
		List<CrawlStateBean> crawlStateResult = crawlState.getCrawlState();
		for (CrawlStateBean crawlStateBean : crawlStateResult) {
			CrawlStatusModel crawlStatusModel = new CrawlStatusModel();
			Date currentDate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String nowDateString = dateFormat.format(currentDate);
			crawlStatusModel.setUrl(crawlStateBean.getDispatchName());
			crawlStatusModel.setCrawlStatus(crawlStateBean.getCrawlState());
			crawlStatusModel.setCheckTime(nowDateString);
			crawlStatusModelArrayList.add(crawlStatusModel);
		}
		// 列表按名称排序
		Collections.sort(crawlStatusModelArrayList, new CrawlStatusModelComparator());
		crawlStatusModelList.setCrawlStatusModelList(crawlStatusModelArrayList);
		// 添加到缓存
		StatusMonitorCache.setCrawlStatusModelListCache(crawlStatusModelList);
		jsonProvider.setSuccess(true);
		jsonProvider.setData(crawlStatusModelList);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 获取种子爬取状态缓存数据
	 * */
	@GET
	@Path("/getCrawlStatusCache")
	@Produces(MediaType.TEXT_PLAIN)
	public String getCrawlStatusCache() {
		ResponseJSONProvider<CrawlStatusModelList> jsonProvider = new ResponseJSONProvider<CrawlStatusModelList>();
		CrawlStatusModelList crawlStatusModelList = new CrawlStatusModelList();
		List<CrawlStatusModel> crawlStatusModelArrayList = StatusMonitorCache.getCrawlStatusModelListCache().getCrawlStatusModelList();

		if (crawlStatusModelArrayList != null) {
			// 列表按名称排序
			Collections.sort(crawlStatusModelArrayList, new CrawlStatusModelComparator());
		}
		crawlStatusModelList.setCrawlStatusModelList(crawlStatusModelArrayList);

		jsonProvider.setSuccess(true);
		jsonProvider.setData(crawlStatusModelList);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 停止爬虫.
	 * */
	@POST
	@Path("/stopCrawl")
	@Produces(MediaType.TEXT_PLAIN)
	public String stopCrawl(@DefaultValue("") @FormParam("folderName") String folderName) {
		ResponseJSONProvider<CrawlStatusModel> jsonProvider = new ResponseJSONProvider<CrawlStatusModel>();
		CrawlState crawlState = new CrawlState();
		String stopStatus = crawlState.stopCrawl(folderName);

		Date currentDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String nowDateString = dateFormat.format(currentDate);

		CrawlStatusModel crawlStatusModel = new CrawlStatusModel();
		crawlStatusModel.setCheckTime(nowDateString);
		if (stopStatus.equals("success")) {
			crawlStatusModel.setCrawlStatus("已停止");
		} else {
			crawlStatusModel.setCrawlStatus("停止失败");
		}

		// 同时更新缓存中想对应的数据
		List<CrawlStatusModel> crawlStatusModelList = StatusMonitorCache.getCrawlStatusModelListCache().getCrawlStatusModelList();
		for (CrawlStatusModel model : crawlStatusModelList) {
			if (model.getUrl().equals(folderName)) {
				model.setCheckTime(nowDateString);
				model.setCrawlStatus(stopStatus);
				break;
			}
		}

		jsonProvider.setSuccess(true);
		jsonProvider.setData(crawlStatusModel);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 重新索引
	 * */
	@POST
	@Path("/reParse")
	@Produces(MediaType.TEXT_PLAIN)
	public String reParse(@DefaultValue("") @FormParam("folderName") String folderName) {
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		CrawlState crawlState = new CrawlState();
		String reParseResult = crawlState.reParse(folderName, null);
		jsonProvider.setSuccess(true);
		if (reParseResult.equals("success")) {
			jsonProvider.setData("操作成功！");
		} else {
			jsonProvider.setData("操作失败！");
		}
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 爬虫增量.
	 * */
	@POST
	@Path("/crawlIncrement")
	@Produces(MediaType.TEXT_PLAIN)
	public String crawlIncrement(@DefaultValue("") @FormParam("folderName") String folderName) {
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		CrawlState crawlState = new CrawlState();
		String crawlIncrementResult = crawlState.crawlIncrement(folderName);
		jsonProvider.setSuccess(true);
		if (crawlIncrementResult.equals("success")) {
			jsonProvider.setData("操作成功！");
		} else {
			jsonProvider.setData("操作失败！");
		}
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 爬虫全量.
	 * */
	@POST
	@Path("/crawlFull")
	@Produces(MediaType.TEXT_PLAIN)
	public String crawlFull(@DefaultValue("") @FormParam("folderName") String folderName) {
		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
		CrawlState crawlState = new CrawlState();
		String crawlFullResult = crawlState.crawlFull(folderName);
		jsonProvider.setSuccess(true);
		if (crawlFullResult.equals("success")) {
			jsonProvider.setData("操作成功！");
		} else {
			jsonProvider.setData("操作失败！");
		}
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 获取种子爬取状态按分类查询 1、爬虫 2、搜索引擎
	 * */
	@GET
	@Path("/getCrawlDataListByDataSource")
	@Produces(MediaType.TEXT_PLAIN)
	public String getCrawlDataListByDataSource() {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<CrawlDataModelList> jsonProvider = new ResponseJSONProvider<CrawlDataModelList>();

		CrawlDataModelList crawlDataModelList = new CrawlDataModelList();
		List<CrawlDataModel> crawlDataModelArrayList = new ArrayList<CrawlDataModel>();

		CrawlDataModel searchEngineCrawlDataModel = new CrawlDataModel();
		searchEngineCrawlDataModel.setDataSource("2");
		// 搜索引擎
		serviceHelper.fillCrawlDataModelArrayList(searchEngineCrawlDataModel, crawlDataModelArrayList, "tags", "datasource=2", "搜索引擎");
		CrawlDataModel normalCrawlDataModel = new CrawlDataModel();
		// 常规引擎
		normalCrawlDataModel.setDataSource("1");
		serviceHelper.fillCrawlDataModelArrayList(normalCrawlDataModel, crawlDataModelArrayList, "tags", "datasource=1", "定向站点");

		// 列表按名称排序
		Collections.sort(crawlDataModelArrayList, new CrawlDataModelComparator());
		crawlDataModelList.setCrawlDataModelList(crawlDataModelArrayList);
		// 添加到缓存
		StatusMonitorCache.setCrawlDataModelListCache(crawlDataModelList);
		jsonProvider.setSuccess(true);
		jsonProvider.setData(crawlDataModelList);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 获取种子爬取状态
	 * */
	@GET
	@Path("/getCrawlDataList")
	@Produces(MediaType.TEXT_PLAIN)
	public String getCrawlDataList() {
		CrawlToolResource serviceHelper = new CrawlToolResource();
		ResponseJSONProvider<CrawlDataModelList> jsonProvider = new ResponseJSONProvider<CrawlDataModelList>();

		CrawlDataModelList crawlDataModelList = new CrawlDataModelList();
		List<CrawlDataModel> crawlDataModelArrayList = new ArrayList<CrawlDataModel>();
		// 需要查询的Domain列表
		List<String> domainList = new ArrayList<String>();

		try {
			Set<String> listKeys = RedisOperator.searchKeysFromDefaultDB("*" + WebtoolConstants.TEMPLATE_LIST_KEY_PARTERN);
			// 产生Domain列表
			serviceHelper.fillDomainList(listKeys, domainList);
			SolrSerach search = new SolrSerach();
			HashMap<String, Long> queryResult = search.getQueryResultCount(domainList);
			if (queryResult != null) {
				Iterator<Entry<String, Long>> it = queryResult.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, Long> entry = (Map.Entry<String, Long>) it.next();
					CrawlDataModel crawlDataModel = new CrawlDataModel();
					Date currentDate = new Date();
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String nowDateString = dateFormat.format(currentDate);
					// 查询今日索引
					long todayIndexCount = search.getQueryResultCount("host", entry.getKey(), WebtoolConstants.CRAWL_DATA_QUERY_FIELD, serviceHelper.getTimeOfZero(), new Date());
					crawlDataModel.setUrl(entry.getKey());
					// 今日索引
					crawlDataModel.setTodayIndexCounts(todayIndexCount);
					crawlDataModel.setIndexCounts(entry.getValue());
					crawlDataModel.setCheckTime(nowDateString);
					crawlDataModelArrayList.add(crawlDataModel);
				}
			}
		} catch (Exception e) {
			jsonProvider.setSuccess(false);
			jsonProvider.setErrorMsg("Redis操作异常！");
			e.printStackTrace();
		}
		// 列表按名称排序
		Collections.sort(crawlDataModelArrayList, new CrawlDataModelComparator());
		crawlDataModelList.setCrawlDataModelList(crawlDataModelArrayList);
		// 添加到缓存
		StatusMonitorCache.setCrawlDataModelListCache(crawlDataModelList);
		jsonProvider.setSuccess(true);
		jsonProvider.setData(crawlDataModelList);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 获取种子爬取数据缓存数据
	 * */
	@GET
	@Path("/getCrawlDataCache")
	@Produces(MediaType.TEXT_PLAIN)
	public String getCrawlDataCache() {
		ResponseJSONProvider<CrawlDataModelList> jsonProvider = new ResponseJSONProvider<CrawlDataModelList>();
		CrawlDataModelList crawlDataModelList = new CrawlDataModelList();
		List<CrawlDataModel> crawlDataModelArrayList = StatusMonitorCache.getCrawlDataModelListCache().getCrawlDataModelList();

		if (crawlDataModelArrayList != null) {
			// 列表按名称排序
			Collections.sort(crawlDataModelArrayList, new CrawlDataModelComparator());
		}
		crawlDataModelList.setCrawlDataModelList(crawlDataModelArrayList);
		jsonProvider.setSuccess(true);
		jsonProvider.setData(crawlDataModelList);
		return jsonProvider.toJSON();
	}

	/**
	 * 
	 * 刷新单条抓取数据 typeName=2 搜索引擎，typeName=1 常規網站
	 * */
	@POST
	@Path("/refreshCrawlData")
	@Produces(MediaType.TEXT_PLAIN)
	public String refreshCrawlData(@DefaultValue("") @FormParam("domain") String domain, @DefaultValue("") @FormParam("typeName") String typeName,
			@DefaultValue("") @FormParam("dataSource") String dataSource) {
		ResponseJSONProvider<CrawlDataModel> jsonProvider = new ResponseJSONProvider<CrawlDataModel>();
		CrawlToolResource serviceHelper = new CrawlToolResource();
		SolrSerach search = new SolrSerach();
		CrawlDataModel crawlDataModel = new CrawlDataModel();

		Date currentDate = new Date();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String nowDateString = dateFormat.format(currentDate);
		long dataCount = -1;
		long todayDataCount = -1;

		if (typeName.equals("1")) {// 按域统计
			dataCount = search.getQueryResultCount("host", domain);
			todayDataCount = search.getQueryResultCount("host", domain, WebtoolConstants.CRAWL_DATA_QUERY_FIELD, serviceHelper.getTimeOfZero(), new Date());
		} else if (typeName.equals("2")) {// 按分类统计
			dataCount = search.getQueryResultCount("tags", "datasource=" + dataSource);
			todayDataCount = search.getQueryResultCount("tags", "datasource=" + dataSource, WebtoolConstants.CRAWL_DATA_QUERY_FIELD, serviceHelper.getTimeOfZero(), new Date());
		}

		crawlDataModel.setCheckTime(nowDateString);
		crawlDataModel.setIndexCounts(dataCount);
		crawlDataModel.setTodayIndexCounts(todayDataCount);

		// 同时更新缓存中想对应的数据
		List<CrawlDataModel> crawlDataModelList = StatusMonitorCache.getCrawlDataModelListCache().getCrawlDataModelList();

		for (CrawlDataModel model : crawlDataModelList) {
			if (model.getUrl().equals(domain)) {
				model.setCheckTime(nowDateString);
				model.setIndexCounts(dataCount);
				model.setTodayIndexCounts(todayDataCount);
				break;
			}
		}

		jsonProvider.setSuccess(true);
		jsonProvider.setData(crawlDataModel);
		return jsonProvider.toJSON();
	}
}
