package com.isoftstone.crawl.template.webtool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
import com.isoftstone.crawl.template.utils.MD5Utils;
import com.isoftstone.crawl.template.utils.RedisUtils;
import com.lj.util.http.DownloadHtml;

/**
 * 
 * 爬虫工具restful-services服务类
 * */
@Path("crawlToolResource")
public class CrawlToolResource {
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
		HashMap<String, String> seeds = parseResult.getResult();
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
		return "暂未实现";
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
		return "模板保存失败!";
	}

	/**
	 * 
	 * 查看HTML内容按钮
	 * */
	@POST
	@Path("/viewHtmlContent")
	@Produces("text/plain")
	public String viewHtmlContent(
			@DefaultValue("") @FormParam("webUrl") String webUrl) {
		String htmlContent = DownloadHtml.getHtml(webUrl, "UTF-8");
		return htmlContent;
	}

	/**
	 * 保存种子到本地文件
	 * */
	private void SaveSeedsValueToFile(HashMap<String, String> seeds) {
		try {
			for (Iterator<Entry<String, String>> it = seeds.entrySet()
					.iterator(); it.hasNext();) {
				Entry<String, String> entry = it.next();
				String seed = entry.getValue();
				contentToTxt("/home/linux/drcnet.com.cn_1day_01/seed.txt", seed);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * 保存模板配置到redis
	 * */
	private ParseResult SaveTemplateToRedis(PageModel pageModel) {
		ParseResult parseResult = null;
		String encoding = "gb2312";
		byte[] input = DownloadHtml.getHtml(pageModel.getBasicInfoViewModel()
				.getUrl());
		TemplateResult templateResult = GetTemplateResult(pageModel);
		String templateUrl = pageModel.getBasicInfoViewModel().getUrl();
		String templateGuid = MD5Utils.MD5(templateUrl);

		RedisUtils.setTemplateResult(templateResult, templateGuid);
		parseResult = TemplateFactory.process(input, encoding, pageModel
				.getBasicInfoViewModel().getUrl());
		return parseResult;
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
				// filter.initReplaceFilter(value, replaceTo);
			} else if (filterCategory.equals("移除")) {
				// filter.initRemoveFilter(value);
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
		if (paginationFilterCategory.equals("匹配")) {
			filter.initMatchFilter(paginationFilter);
		} else if (paginationFilterCategory.equals("替换")) {
			// filter.initReplaceFilter(value, replaceTo);
		} else if (paginationFilterCategory.equals("移除")) {
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
				// filter.initReplaceFilter(value, replaceTo);
			} else if (filterCategory.equals("移除")) {
				// filter.initRemoveFilter(value);
			}
			label.initLabelSelector(model.getTarget(), "", indexer, filter,
					null);
			selector.setLabel(label);
			news.add(selector);
			template.setNews(news);
		}

		return template;
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
			if (f.exists()) {
				System.out.print("文件存在");
			} else {
				System.out.print("文件不存在");
				f.createNewFile();// 不存在则创建
			}
			BufferedReader input = new BufferedReader(new FileReader(f));

			while ((str = input.readLine()) != null) {
				s1 += str + "\n";
			}
			System.out.println(s1);
			input.close();
			s1 += content;

			BufferedWriter output = new BufferedWriter(new FileWriter(f));
			output.write(s1);
			output.close();
		} catch (Exception e) {
			e.printStackTrace();

		}
	}
}
