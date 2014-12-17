/**
 * Created by wang on 2014/12/9.
 */
$(function(){
    //默认加载列表模板
    loadTemplate('列表模板','template-list.html');
    //注册页签事件
    registerTabPanelEvent();

    //解决viewmodel的嵌套问题
    ko.bindingHandlers.stopBinding = {
        init: function() {
            return { controlsDescendantBindings: true };
        }
    };

    ko.virtualElements.allowedBindings.stopBinding = true;

//    var pageDes=getUrlParameter('pagedes');
//    var id=getUrlParameter('id');
//    //$('#title_config').text(decodeURIComponent(pageDes)+"--爬虫模板配置");
});

/**
 *
 * 加载主内容模板
 * @param {String} title 模板标题
 * @param {String} templateFile 模板文件相对路径
 * */
function loadTemplate(title,templateFile){
    var main_content=$('#main_content');
    main_content.html('');
    main_content.load(templateFile,function(){
        $('#title_config').text(title);

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

/**
 *
 * 注册页签事件
 * */
function registerTabPanelEvent(){
    $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        if(e.target.hash=="#list"){
            loadTemplate('列表模板','template-list.html');
        }else if(e.target.hash=="#news"){
            loadTemplate('内容模板','template-news.html');
        }else if(e.target.hash=="#pagitation"){
            loadTemplate('分页模板','template-pagitation.html');
        }
    });
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