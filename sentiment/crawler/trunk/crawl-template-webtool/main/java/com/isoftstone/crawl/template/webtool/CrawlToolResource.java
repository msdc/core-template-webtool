package com.isoftstone.crawl.template.webtool;

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

@Path("crawlToolResource")
public class CrawlToolResource {
	/*根据生成的JSON模板，得到相关的页面内容*/
	@POST @Path("/testTemplateJsonString")    
    @Produces("application/json")    
	public ParseResult testTemplateJsonString(
			@DefaultValue("") @FormParam("webUrl") String webUrl,
    		@DefaultValue("") @FormParam("jsoupSelector") String jsoupSelector,
    		@DefaultValue("") @FormParam("attribute") String attribute,
    		@DefaultValue("") @FormParam("filterString") String filterString,
    		@DefaultValue("") @FormParam("formaterString") String formaterString){
		byte[] input = DownloadHtml.getHtml(webUrl);
		TemplateResult templateResult = getTemplateResult(webUrl,jsoupSelector,attribute,filterString,formaterString);
		ParseResult parseResult = null;
		String encoding = "gb2312";
		parseResult = TemplateFactory.process(input, encoding, webUrl);
		return parseResult;
	}
	
	/*产生页面模板JSON串，并将结果保存到REDIS中*/
	public TemplateResult getTemplateResult(String webUrl,String jsoupSelector,String attribute,String filterString,String formaterString){
    	TemplateResult template = new TemplateResult();
		template.setType(Constants.TEMPLATE_LIST);
		String templateUrl = webUrl;
		String templateGuid = MD5Utils.MD5(templateUrl);
		template.setTemplateGuid(templateGuid);
		
		List<Selector> list = new ArrayList<Selector>();
		
		SelectorIndexer indexer = null;
		Selector selector = null;
		SelectorFilter filter = null;
		SelectorFormat format = null;
		
		indexer = new SelectorIndexer();
		selector = new Selector();
		indexer.initJsoupIndexer(jsoupSelector, attribute);
		selector.initContentSelector(indexer, null);
		list.add(selector);
		template.setList(list);
		
		RedisUtils.setTemplateResult(template, templateGuid);
		return template;
	}
	
	/*查看生成的模板JSON串*/
    @POST @Path("/viewTemplateJsonString")    
    @Produces("application/json")    
    public String viewTemplateJsonString(
    		@DefaultValue("") @FormParam("webUrl") String webUrl,
    		@DefaultValue("") @FormParam("jsoupSelector") String jsoupSelector,
    		@DefaultValue("") @FormParam("attribute") String attribute,
    		@DefaultValue("") @FormParam("filterString") String filterString,
    		@DefaultValue("") @FormParam("formaterString") String formaterString){
    		byte[] input = DownloadHtml.getHtml(webUrl);
    		String templateJsonString = GetTemplateJsonString(webUrl,jsoupSelector,attribute,filterString,formaterString);
    		return templateJsonString;
    	}     
   
    /* 产生模板的JSON串 */
    private String GetTemplateJsonString(String webUrl,String jsoupSelector,String attribute,String filterString,String formaterString){
    	TemplateResult template = new TemplateResult();
		template.setType(Constants.TEMPLATE_LIST);
		String templateUrl = webUrl;
		String templateGuid = MD5Utils.MD5(templateUrl);
		template.setTemplateGuid(templateGuid);
		
		List<Selector> list = new ArrayList<Selector>();
		
		SelectorIndexer indexer = null;
		Selector selector = null;
		SelectorFilter filter = null;
		SelectorFormat format = null;
		
		indexer = new SelectorIndexer();
		selector = new Selector();
		indexer.initJsoupIndexer(jsoupSelector, attribute);
		selector.initContentSelector(indexer, null);
		list.add(selector);
		template.setList(list);
		
		String JSONString=JSONUtils.getTemplateResultJSON(template);
	    return JSONString;
    }
}
