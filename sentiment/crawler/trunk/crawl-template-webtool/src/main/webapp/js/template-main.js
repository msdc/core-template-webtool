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
var customerAttrModel=function(id,target,selector,attr,filter,formater){
    this.id=id;
    this.target=target;
    this.selector=selector;
    this.attr=attr;
    this.filter=filter;
    this.formater=formater;
};

/**
 *
 * 模板基本信息属性对象
 * */
var templateBasicInfoModel=function(id,url,name){
    this.id = id;
    this.url=url;
    this.name=name;
    this.tags=['财经','体育','经济'];
};

/**
 *
 * 列表页和内容页中的单个属性实体 ,例如:标题
 * */
var commonPanelModel=function(id,selector,selectorAttr,filter,filterCategory,formater,formatCategory){
    this.id = id;
    this.selector=selector;
    this.selectorAttr=selectorAttr;
    this.filter=filter;
    this.filterCategory=filterCategory;
    this.formater=formater;
    this.formatCategory=formatCategory;
};

/**
 *
 * 分页属性model实体
 * */
/*********************************************Models*************************************/

/********************************************View-Models*********************************/

//整个页面视图View-Model
var templateViewModel=function(basicInfo){
    this.basicInfo=ko.observable(basicInfo);
};

/**
 * 局部视图的View-Model
 * 自定义属性视图中的view-model
 * @param {Array} modelArray
 * @param {Array} parseEngine
 * */
var customerAttrViewModel=function(modelArray,parseEngine){
    this.regions=ko.observableArray(modelArray);
    this.parseEngines=ko.observableArray(parseEngine);
    /*添加解析域*/
    this.addItem=function(){
        this.regions.push(new customerAttrModel('','','','','',''));
    }.bind(this);
    //删除元素 .bind(this)改变作用域,始终绑定当前对象
    this.removeItem=function(item){
        this.regions.remove(item);
    }.bind(this);
    this.getAllItems=function(){}.bind(this);
};


/********************************************View-Models*********************************/

/*********************************************Functions**********************************/
$(function(){
    //初始化模板内容
    initTemplateContent();

    //解决多个View-Model的嵌套问题
    ko.bindingHandlers.stopBinding = {
        init: function() {
            return { controlsDescendantBindings: true };
        }
    };

    ko.virtualElements.allowedBindings.stopBinding = true;
});

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
            var listAttrModelArray=[];
            var listAttrViewModel=new customerAttrViewModel(listAttrModelArray,parseEngine);
            customerViewModelBinding(jquerySelector,templateFile,listAttrViewModel,document.getElementById('list_tab'))
        }
            break;
        case "#news_tab":
        {
            //内容页中model集合
            var newsAttrModelArray=[];
            var newsAttrViewModel=new customerAttrViewModel(newsAttrModelArray,parseEngine);
            customerViewModelBinding(jquerySelector,templateFile,newsAttrViewModel,document.getElementById('news_tab'));
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