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

//列表页中model集合
var listAttrModelArray=[];
//内容页中model集合
var newsAttrModelArray=[];

//自定义属性model
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
 * 加载模板
 * */
function loadTemplateBySelector(jquerySelector,templateFile){
    switch (jquerySelector){
        case "#list_tab":
        {
            $(jquerySelector).load(templateFile,function(){
                //索引器集合
                var listParseEngine=['jsoup','xpath'];
                var listAttrViewModel={
                    listRegions:ko.observableArray(listAttrModelArray),
                    listParseEngines:ko.observableArray(listParseEngine),
                    /*添加解析域*/
                    listAddItem:function(){
                        this.listRegions.push(new customerAttrModel('','','','','',''));
                    },
                    listGetAllItems:function(){
                        //console.dir(this.regions());
                    }
                };

                //绑定到UI显示
                ko.applyBindings(listAttrViewModel,document.getElementById('list_tab'));
            });
        }
        case "#news_tab":
        {
            $(jquerySelector).load(templateFile,function(){
                //索引器集合
                var newsParseEngine=['jsoup','xpath'];
                var newsAttrViewModel={
                    newsRegions:ko.observableArray(newsAttrModelArray),
                    newsParseEngines:ko.observableArray(newsParseEngine),
                    /*添加解析域*/
                    newsAddItem:function(){
                        this.newsRegions.push(new customerAttrModel('','','','','',''));
                    },
                    newsGetAllItems:function(){
                        //console.dir(this.regions());
                    }
                };

                //绑定到UI显示
                ko.applyBindings(newsAttrViewModel,document.getElementById('news_tab'));
            });
        }
            break;
    }
}

/**
 *
 * 得到URL中的参数
 * */
function getUrlParameter(sParam)
{
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