/**
 * Created by wang on 2014/12/9.
 */

/**************Models****************/
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
/**************Models****************/

/**************View-Models****************/
/**
 *
 * 基本信息View-Model
 * */
function basicInfoViewModel(){
    this.id=ko.observable('');
    this.url=ko.observable('http://www.ccgp-shandong.gov.cn/fin_info/site/index.jsp');
    this.name=ko.observable('山东政府采购网');
    this.tags=ko.observableArray(['财经','体育','经济']);
}

/**
 *
 * 自定义属性View-Model
 * */
function customerAttrViewModel(){
    this.regions=ko.observableArray([]);
    this.parseEngines=ko.observableArray(['jsoup','xpath']);
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
 * 分页属性View-Model
 * */
function paginationViewModel(){
    this.parseEngine=ko.observableArray(['jsoup','xpath']);
    this.selector=ko.observable();
    this.selectorAttr=ko.observableArray([]);
    this.filter=ko.observable();
    this.filterCategory=ko.observableArray([]);
    this.formater=ko.observable();
    this.formatCategory=ko.observableArray([]);
    this.paginationType=ko.observableArray(['PAGINATION_TYPE_PAGE','PAGINATION_TYPE_PAGENUMBER','PAGINATION_TYPE_PAGENUMBER_INTERVAL']);
    this.paginationUrl=ko.observable();
    this.currentString=ko.observable();
    this.replaceTo=ko.observable();
    this.start=ko.observable();
    this.records=ko.observable();
}

/**
 *
 * 通用属性View-Model
 * */
function commonAttrViewMode(){
    this.parseEngine=ko.observableArray(['jsoup','xpath']);
    this.selector=ko.observable();
    this.selectorAttr=ko.observable(['href','text','src','html']);
    this.filter=ko.observable();
    this.filterCategory=ko.observable(['移除','匹配','替换']);
    this.formater=ko.observable();
    this.formatCategory=ko.observable(['时间','日期']);
}
/**************View-Models****************/

/*********************************************Functions**********************************/
$(function(){
    //解决多个View-Model的嵌套问题
    ko.bindingHandlers.stopBinding = {
        init: function() {
            return { controlsDescendantBindings: true };
        }
    };

    $('#news_tab').load('template-news.html',function(){
        $('#list_tab').load('template-list.html',function(){
            //master view model with instances of both the view models.
            var masterVM = (function(){
                this.basicInfoViewModel = new basicInfoViewModel();

                this.newsCustomerAttrViewModel=new customerAttrViewModel();
                this.newsTitleViewModel=new commonAttrViewMode();
                this.newsContentViewModel=new commonAttrViewMode();

                this.listCustomerAttrViewModel=new customerAttrViewModel();
                this.listOutLinkViewModel=new commonAttrViewMode();
                this.listPaginationViewModel=new paginationViewModel();
            })();
            ko.applyBindings(masterVM);
        })
    });

    //模态对话框事件
    //registerModalViewContentEvent();
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