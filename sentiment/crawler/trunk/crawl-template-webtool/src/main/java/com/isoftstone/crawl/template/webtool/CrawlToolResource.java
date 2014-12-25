package com.isoftstone.crawl.template.webtool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.isoftstone.crawl.template.global.Constants;
import com.isoftstone.crawl.template.impl.ParseResult;
import com.isoftstone.crawl.template.impl.Selector;
import com.isoftstone.crawl.template.impl.SelectorFilter;
import com.isoftstone.crawl.template.impl.SelectorFormat;
import com.isoftstone.crawl.template.impl.SelectorIndexer;
import com.isoftstone.crawl.template.impl.TemplateFactory;
import com.isoftstone.crawl.template.impl.TemplateResult;
import com.isoftstone.crawl.template.utils.JSONUtils;
import com.isoftstone.crawl.template.utils.MD5Utils;
import com.isoftstone.crawl.template.utils.RedisUtils;
import com.lj.util.http.DownloadHtml;

/**
 * 
 * 爬虫工具restful-services服务类
 * */
@Path("crawlToolResource")
public class CrawlToolResource {
	@POST
	@Path("/getJSONString")
	@Produces(MediaType.TEXT_PLAIN)
	public String GetJSONString(@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = null;
		try {
			ObjectMapper objectmapper = new ObjectMapper();
			pageModel = objectmapper.readValue(data, PageModel.class);
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

		String encoding = "gb2312";
		ParseResult parseResult = null;
		byte[] input = DownloadHtml.getHtml(pageModel.getBasicInfoViewModel()
				.getUrl());
		TemplateResult templateResult = SaveTemplateResult(pageModel);
		parseResult = TemplateFactory.localProcess(input, encoding, pageModel
				.getBasicInfoViewModel().getUrl(), templateResult,
				Constants.TEMPLATE_LIST);
		String templateJsonString = GetTemplateJsonString(pageModel);
		return templateJsonString;
	}

	@POST
	@Path("/saveTemplate")
	@Produces("text/plain")
	public String SaveTemplate(@DefaultValue("") @FormParam("data") String data) {
		PageModel pageModel = null;
		try {
			ObjectMapper objectmapper = new ObjectMapper();
			pageModel = objectmapper.readValue(data, PageModel.class);
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

		return "";
	}

	public ParseResult testTemplateJsonString(PageModel pageModel) {
		byte[] input = DownloadHtml.getHtml(pageModel.getBasicInfoViewModel()
				.getUrl());
		TemplateResult templateResult = SaveTemplateResult(pageModel);
		ParseResult parseResult = null;
		String encoding = "gb2312";
		parseResult = TemplateFactory.process(input, encoding, pageModel
				.getBasicInfoViewModel().getUrl());
		return parseResult;
	}

	private TemplateResult SaveTemplateResult(PageModel pageModel) {
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
		indexer.initJsoupIndexer(pageModel.getListOutLinkViewModel()
				.getSelector(), pageModel.getListOutLinkViewModel()
				.getSelectorAttr());
		selector.initContentSelector(indexer, null);
		list.add(selector);
		template.setList(list);

		// // tstamp 自定义属性
		// Selector label = new Selector();
		// label.setType(Constants.SELECTOR_LABEL);
		// indexer = new SelectorIndexer();
		// indexer.initJsoupIndexer(
		// "body > form > table:nth-child(2) > tbody > tr > td:nth-child(2) > table:nth-child(2) > tbody > tr > td > table > tbody > tr > td > table:nth-child(2) > tbody > tr:nth-child(1) > td:nth-child(2) > table > tbody > tr:nth-child(2n-1) > td:nth-child(3)",
		// Constants.ATTRIBUTE_TEXT);
		// filter = new SelectorFilter();
		// filter.initMatchFilter(Constants.YYYYMMDD);
		// label.initLabelSelector("tstamp", "", indexer, filter, null);
		// selector.setLabel(label);
		// list.add(selector);
		// template.setList(list);

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
		if (paginationFilterCategory == "匹配") {
			filter.initMatchFilter(paginationFilter);
		} else if (paginationFilterCategory == "替换") {
			// filter.initReplaceFilter(value, replaceTo);
		} else if (paginationFilterCategory == "移除") {
			// filter.initRemoveFilter(value);
		}

		String paginationType = pageModel.getListPaginationViewModel()
				.getPaginationType();
		if (paginationType == "分页的末尾页数") {
			paginationType = Constants.PAGINATION_TYPE_PAGENUMBER;
		} else if (paginationType == "分页步进数") {
			paginationType = Constants.PAGINATION_TYPE_PAGENUMBER_INTERVAL;
		} else if (paginationType == "获取分页的记录数") {
			paginationType = Constants.PAGINATION_TYPE_PAGERECORD;
		} else {
			paginationType = Constants.PAGINATION_TYPE_PAGENUMBER;
		}

		// Constants.PAGINATION_TYPE_PAGENUMBER 需要取值
		selector.initPagitationSelector(paginationType, pageModel
				.getListPaginationViewModel().getCurrentString(), pageModel
				.getListPaginationViewModel().getReplaceTo(), pageModel
				.getListPaginationViewModel().getPaginationUrl(), pageModel
				.getListPaginationViewModel().getStart(), null, indexer,
				filter, null);
		pagination.add(selector);
		template.setPagination(pagination);

		// title
		indexer = new SelectorIndexer();
		selector = new Selector();
		indexer.initJsoupIndexer(pageModel.getNewsTitleViewModel()
				.getSelector(), pageModel.getNewsTitleViewModel()
				.getSelectorAttr());
		selector.initFieldSelector("title", "", indexer, null, null);
		news.add(selector);

		// content
		indexer = new SelectorIndexer();
		selector = new Selector();
		indexer.initJsoupIndexer(pageModel.getNewsContentViewModel()
				.getSelector(), pageModel.getNewsContentViewModel()
				.getSelectorAttr());
		selector.initFieldSelector("content", "", indexer, null, null);
		news.add(selector);
		template.setNews(news);

		RedisUtils.setTemplateResult(template, templateGuid);
		return template;
	}

	private String GetTemplateJsonString(PageModel pageModel) {
		TemplateResult template = new TemplateResult();
		template.setType(Constants.TEMPLATE_LIST);
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);
		template.setTemplateGuid(templateGuid);

		List<Selector> list = new ArrayList<Selector>();

		SelectorIndexer indexer = null;
		Selector selector = null;
		SelectorFilter filter = null;
		SelectorFormat format = null;

		indexer = new SelectorIndexer();
		selector = new Selector();
		indexer.initJsoupIndexer(pageModel.getListOutLinkViewModel()
				.getSelector(), pageModel.getListOutLinkViewModel()
				.getSelectorAttr());
		selector.initContentSelector(indexer, null);
		list.add(selector);
		template.setList(list);

		String JSONString = JSONUtils.getTemplateResultJSON(template);
		return JSONString;
	}

	/**
	 * 
	 * 查看html内容按钮
	 * */
	@POST
	@Path("/viewHtmlContent")
	@Produces("text/plain")
	public String viewHtmlContent(
			@DefaultValue("") @FormParam("webUrl") String webUrl) {
		String htmlContent = DownloadHtml.getHtml(webUrl, "UTF-8");
		return htmlContent;
	}

	private String convertToJSONString(PageModel pageModel) {
		String json = null;
		ObjectMapper objectmapper = new ObjectMapper();
		try {
			json = objectmapper.writeValueAsString(pageModel);
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

}
