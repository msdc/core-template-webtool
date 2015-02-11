package com.isoftstone.crawl.template.webtool;

import javax.ws.rs.Path;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Path("crawlToolService")
public class CrawlToolService {
	private static final Log LOG = LogFactory.getLog(CrawlToolService.class);
	
//	@POST
//	@Path("/saveToLocalFile")
//	@Produces(MediaType.TEXT_PLAIN)
//	public String saveToLocalFile(@DefaultValue("") @FormParam("data") String data) {
//		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
//		PageModel pageModel = CrawlToolResource.GetPageModelByJsonString(data);
//		String domain = pageModel.getScheduleDispatchViewModel().getDomain();
//		// String period = pageModel.getScheduleDispatchViewModel().getPeriod();
//		String period = pageModel.getTemplateIncreaseViewModel().getPeriod();
//		String sequence = pageModel.getScheduleDispatchViewModel().getSequence();
//		boolean userProxy = pageModel.getScheduleDispatchViewModel().getUseProxy();
//		if (sequence == null || sequence.equals("")) {
//			jsonProvider.setSuccess(false);
//			jsonProvider.setErrorMsg("保存失败，请输入时序.");
//			return jsonProvider.toJSON();
//		}
//		String folderName = domain + "_" + "1" + period + "_" + sequence;
//		String incrementFolderName = domain + "_" + "1" + period + "_" + WebtoolConstants.INCREMENT_FILENAME_SIGN + "_" + sequence;
//		ParseResult parseResult = CrawlToolResource.saveTemplateAndParseResult(pageModel);
//		if (parseResult == null) {
//			jsonProvider.setSuccess(false);
//			jsonProvider.setErrorMsg("请先保存模板!再执行此操作!");
//			return jsonProvider.toJSON();
//		}
//		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
//		String templateGuid = MD5Utils.MD5(templateUrl);
//		String redisKey = templateGuid + WebtoolConstants.key_partern;
//		TemplateModel templateModel = CrawlToolResource.getTemplateModel(redisKey);
//		String status = templateModel.getStatus();
//
//		// --增量相关.
//		String incrementPageCountStr = pageModel.getTemplateIncreaseViewModel().getPageCounts();
//		if (incrementPageCountStr == null || "".equals(incrementPageCountStr)) {
//			jsonProvider.setSuccess(false);
//			jsonProvider.setErrorMsg("保存失败，请输入增量需要爬取的页数");
//			return jsonProvider.toJSON();
//		}
//		int incrementPageCount = Integer.valueOf(incrementPageCountStr);
//		if (incrementPageCount > 0) {
//			incrementPageCount = incrementPageCount - 1;
//		}
//		String pageSort = pageModel.getTemplateIncreaseViewModel().getPageSort();
//		ArrayList<String> seedsTemp = TemplateFactory.getPaginationOutlink(parseResult);
//		ArrayList<String> seeds = new ArrayList<String>();
//		seeds.add(templateUrl);
//		if ("升序".equals(pageSort)) {
//			for (int i = 0; i < incrementPageCount && i < seedsTemp.size(); i++) {
//				seeds.add(seedsTemp.get(i));
//			}
//		} else {
//			for (int i = seedsTemp.size() - 1; i >= 0 && incrementPageCount > 0; i--, incrementPageCount--) {
//				seeds.add(seedsTemp.get(i));
//			}
//		}
//		String paginationUrl = pageModel.getListPaginationViewModel().getPaginationUrl();
//		String currentString = pageModel.getListPaginationViewModel().getCurrentString();
//		String start = pageModel.getListPaginationViewModel().getStart();
//
//		CrawlToolResource.saveSeedsValueToFile(folderName, incrementFolderName, templateUrl, seeds, status, userProxy, paginationUrl, currentString, start);
//		jsonProvider.setSuccess(true);
//		jsonProvider.setData("文件保存成功!");
//		return jsonProvider.toJSON();
//	}
//	
//	/**
//	 * 
//	 * 删除模板
//	 * */
//	@POST
//	@Path("/deleteTemplate")
//	@Produces(MediaType.TEXT_PLAIN)
//	public String DeleteTemplate(@DefaultValue("") @FormParam("templateUrl") String templateUrl) {
//		String templateGuid = MD5Utils.MD5(templateUrl);
//		ResponseJSONProvider<String> jsonProvider = new ResponseJSONProvider<String>();
//		jsonProvider.setSuccess(true);
//		jsonProvider.setData("删除成功！");
//		long effectCounts = -1;
//
//		// 先删除增量模板
//		String jsonString = RedisOperator.getFromDefaultDB(templateGuid + WebtoolConstants.key_partern);
//		TemplateModel templateModel = CrawlToolResource.GetTemplateModel(jsonString);
//		List<String> increaseTemplateIdList = templateModel.getTemplateIncreaseIdList();
//		if (increaseTemplateIdList != null) {
//			for (String increaseTemplateId : increaseTemplateIdList) {
//				effectCounts = RedisOperator.delFromIncreaseDB(increaseTemplateId);
//				if (effectCounts < 0) {
//					jsonProvider.setSuccess(false);
//					jsonProvider.setErrorMsg("删除模板失败");
//					jsonProvider.setData(null);
//				}
//			}
//		}
//
//		// 最后删除模板列表
//		effectCounts = RedisOperator.delFromDefaultDB((templateGuid + WebtoolConstants.key_partern), templateGuid);
//		if (effectCounts < 0) {
//			jsonProvider.setSuccess(false);
//			jsonProvider.setErrorMsg("删除模板失败");
//			jsonProvider.setData("");
//		}
//
//		return jsonProvider.toJSON();
//	}
}
