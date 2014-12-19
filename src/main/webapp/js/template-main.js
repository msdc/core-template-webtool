/**
 * Created by wang on 2014/12/9.
 */
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
    loadTemplateBySelector('#list_tab','template-list.html');
    loadTemplateBySelector('#news_tab','template-news.html');
};

//索引器集合
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

/**
 *
 * knockout.js binding the ui element.
 * @param {String} jquerySelector
 * @param {String} templateFile
 * @param {Object} attrViewModel
 * @param {Object} domElement
 * */
function commonViewModelBinding(jquerySelector,templateFile,attrViewModel,domElement){
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
            commonViewModelBinding(jquerySelector,templateFile,listAttrViewModel,document.getElementById('list_tab'))
        }
            break;
        case "#news_tab":
        {
            //内容页中model集合
            var newsAttrModelArray=[];
            var newsAttrViewModel=new customerAttrViewModel(newsAttrModelArray,parseEngine);
            commonViewModelBinding(jquerySelector,templateFile,newsAttrViewModel,document.getElementById('news_tab'));
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