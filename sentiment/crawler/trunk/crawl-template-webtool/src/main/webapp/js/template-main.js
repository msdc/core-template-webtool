/**
 * Created by wang on 2014/12/9.
 */

/*********************************************Models*************************************/
/**
 *
 * 索引器集合
 * */
var parseEngine=['jsoup','xpath'];

/**
 *
 * 自定义属性的model
 * */
function customerAttrModel(id,target,selectorFunction,selector,attr,filter,filterCategory,formater,formatCategory){
    this.parseEngine=['jsoup','xpath'];
    this.id=id;
    this.target=target;
    this.selectorFunction=selectorFunction;
    this.selector=selector;
    this.attr=attr;
    this.filter=filter;
    this.filterCategory=filterCategory;
    this.formater=formater;
    this.formatCategory=formatCategory;
}

/**
 *
 * 模板基本信息属性对象
 * */
function templateBasicInfoModel(id,url,name){
    this.id = id;
    this.url=url;
    this.name=name;
    this.tags=['财经','体育','经济'];
    this.viewHtmlContent=function(){
        var url=this.url;
        $('#modal-viewHtml').modal('show');
        $.ajax({
            url:'/webapi/crawlToolResource/viewHtmlContent',
            type:'POST',
            data:{
                webUrl:url
            },
            success:function(result){
                var modalBody=$('#modal-viewHtml-body');
                modalBody.text('');//清空
                modalBody.text(result);
            },
            error:function(){}
        });
    }.bind(this);
}

/**
 *
 * 列表页和内容页中的单个属性实体 ,例如:标题
 * @param {String} selector
 * @param {Array} selectorAttr
 * @param {String} filter
 * @param {Array} filterCategory
 * @param {String} formater
 * @param {Array} formatCategory
 * */
function commonPanelModel(selector,selectorAttr,filter,filterCategory,formater,formatCategory){
    this.parseEngine=['jsoup','xpath'];
    this.selector=selector;
    this.selectorAttr=selectorAttr;
    this.filter=filter;
    this.filterCategory=filterCategory;
    this.formater=formater;
    this.formatCategory=formatCategory;
}

/**
 *
 * 分页属性model实体
 * */
function paginationModel(selector,selectorAttr,filter,filterCategory,formater,formatCategory,paginationUrl,currentString,replaceTo,start,records){
    this.parseEngine=['jsoup','xpath'];
    this.selector=selector;
    this.selectorAttr=selectorAttr;
    this.filter=filter;
    this.filterCategory=filterCategory;
    this.formater=formater;
    this.formatCategory=formatCategory;
    this.paginationType=['PAGINATION_TYPE_PAGE','PAGINATION_TYPE_PAGENUMBER','PAGINATION_TYPE_PAGENUMBER_INTERVAL'];
    this.paginationUrl=paginationUrl;
    this.currentString=currentString;
    this.replaceTo=replaceTo;
    this.start=start;
    this.records=records;
}
 /*********************************************Models*************************************/

/********************************************View-Models*********************************/

//配置页面整个视图View-Model
function templateViewModel(basicInfo){
    this.basicInfo=ko.observable(basicInfo);
    //测试模板方法
    this.testTemplate=function(){
        alert('testTemplate');
    };
    //保存模板方法
    this.saveTemplate=function(){
        alert('saveTemplate');
    };
}

/**
 * 通过load方式加载的局部视图View-Model
 * 自定义属性视图中的view-model
 * @param {Array} modelArray
 * @param {Array} parseEngine
 * */
function customerAttrViewModel(modelArray,parseEngine){
    this.regions=ko.observableArray(modelArray);
    this.parseEngines=ko.observableArray(parseEngine);
    /*添加解析域*/
    this.addItem=function(){
        this.regions.push(new customerAttrModel('','',['field','label','content','pagination'],'',['href','text','src','html'],'',['移除','匹配','替换'],'',['时间','日期']));
    }.bind(this);
    //删除元素 .bind(this)改变作用域,始终绑定当前对象
    this.removeItem=function(item){
        this.regions.remove(item);
    }.bind(this);
    this.getAllItems=function(){}.bind(this);
}

/**
 *
 * 内容页局部View-Model
 * */
function newsPartialViewModel(title,content){
    this.title=ko.observable(title);
    this.content=ko.observable(content);
}

/**
 *
 * 列表页局部View-Model
 * */
function listPartialViewModel(outlink,pagination){
    this.outlink=outlink;
    this.pagination=pagination;
}
/********************************************View-Models*********************************/

/*********************************************Functions**********************************/
$(function(){
    //解决多个View-Model的嵌套问题
    ko.bindingHandlers.stopBinding = {
        init: function() {
            return { controlsDescendantBindings: true };
        }
    };

    //初始化模板内容
    initTemplateContent();

    //模态对话框事件
    registerModalViewContentEvent();
});

/**
 *
 * 给模态对话框注册事件
 * */
function registerModalViewContentEvent(){
    $('#modal-viewHtml').on('hidden.bs.modal', function (e) {
        var modalBody=$('#modal-viewHtml-body');
        modalBody.html('');//清空
        modalBody.html('<div class=\"text-center\"><img src=\"../image/load.gif\"></div>');
    })
}

/**
 *
 * 初始化页面内容
 * */
function initTemplateContent(){
    //模板基本信息对象
    var basicInfo=new templateBasicInfoModel('','http://www.ccgp-shandong.gov.cn/fin_info/site/index.jsp','山东政府采购网');
    //模板基本信息初始化
    var pageViewModel=new templateViewModel(basicInfo);
    ko.applyBindings(pageViewModel);
    //列表区域内容初始化
    loadTemplateBySelector('#list_tab','template-list.html');
    //内容区域内容初始化
    loadTemplateBySelector('#news_tab','template-news.html');
};

/**
 *
 * knockout.js binding the ui element.
 * @param {String} jquerySelector
 * @param {String} templateFile
 * @param {Object} attrViewModel
 * @param {Object} domElement
 * */
function customerViewModelBinding(jquerySelector,templateFile,attrViewModel,domElement){
    $(jquerySelector).load(templateFile,function(){
        //绑定到UI显示
        ko.applyBindings(attrViewModel,domElement);
    });
}

/**
 *
 * 加载模板
 * @param {String} jquerySelector
 * @param {String} templateFile
 * */
function loadTemplateBySelector(jquerySelector,templateFile){
    switch (jquerySelector){
        case "#list_tab":
        {
            //列表页中model集合
            var listCustomerArray=[];
            var listCustomerViewModel=new customerAttrViewModel(listCustomerArray,parseEngine);

            var listOutLink=new commonPanelModel('body>tbody>',['href','text','src','html'],'',['移除','匹配','替换'],'YYYY-MM-DD',['时间','日期']);
            var listPagination=new paginationModel('body>tbody>',['href','text','src','html'],'',['移除','匹配','替换'],'YYYY-MM-DD',['时间','日期'],'http://www.ccgp-fujian.gov.cn/secpag.cfm?PageNum=##&&caidan=采购公告&caidan2=公开招标&level=province&yqgg=0','##','',2,'');
            var listPartial=new listPartialViewModel(listOutLink,listPagination);

            $(jquerySelector).load(templateFile,function(){
                ko.applyBindings(listPartial,document.getElementById('list_tab'));
                ko.applyBindings(listCustomerViewModel,document.getElementById('list_customer_attr'));
            });
        }
            break;
        case "#news_tab":
        {
            //内容页中自定义属性集合
            var newsCustomerArray=[];
            var newsCustomerViewModel=new customerAttrViewModel(newsCustomerArray,parseEngine);

            var newsTitle=new commonPanelModel('body>tbody>',['href','text','src','html'],'',['移除','匹配','替换'],'YYYY-MM-DD',['时间','日期']);
            var newsContent=new commonPanelModel('body>tbody>',['href','text','src','html'],'',['移除','匹配','替换'],'YYYY-MM-DD',['时间','日期']);
            var newsPartial=new newsPartialViewModel(newsTitle,newsContent);

            $(jquerySelector).load(templateFile,function(){
                ko.applyBindings(newsPartial,document.getElementById('news_tab'));
                ko.applyBindings(newsCustomerViewModel,document.getElementById('news_customer_attr'));
            });
        }
            break;
    }
}

/**
 *
 * 得到URL中的参数
 * */
function getUrlParameter(sParam){
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++)
    {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam)
        {
            return sParameterName[1];
        }
    }
}