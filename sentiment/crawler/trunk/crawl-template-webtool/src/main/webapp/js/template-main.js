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

/**
 *
 * 加载模板
 * */
function loadTemplateBySelector(jquerySelector,templateFile){
    switch (jquerySelector){
        case "#list_tab":
        case "#news_tab":
        {
            $(jquerySelector).load(templateFile,function(){
                //自定义属性model
                var customerAttrModel=function(id,target,selector,attr,filter,formater){
                    this.id=id;
                    this.target=target;
                    this.selector=selector;
                    this.attr=attr;
                    this.filter=filter;
                    this.formater=formater;
                };

                //自定义属性model集合
                var customerAttrModelArray=[];
                //索引器集合
                var parseEngine=['jsoup','xpath'];

                var customerAttrViewModel={
                    regions:ko.observableArray(customerAttrModelArray),
                    parseEngines:ko.observableArray(parseEngine),
                    /*添加解析域*/
                    addItem:function(){
                        this.regions.push(new customerAttrModel('','','','','',''));
                    },
                    getAllItems:function(){
                        //console.dir(this.regions());
                    }
                };

                //绑定到UI显示
                ko.applyBindings(customerAttrViewModel,document.getElementById('customer_attr'));
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