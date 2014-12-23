/**
 * Created by wang on 2014/12/9.
 */
/**************Models****************/
function customerAttrModel(id,target,selectorFunction,selector,attr,filter,filterCategory,formater,formatCategory){
    //this.parseEngine=['jsoup','xpath'];
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
 * 自定义属性的单个Model视图
 * */
function singleCustomerViewModel() {
    this.parseEngine = ko.observableArray(['jsoup', 'xpath']);
    this.id = ko.observable();
    this.target = ko.observable();
    this.selectorFunction = ko.observableArray(['label','field','content', 'pagination']);
    this.selectorFunctionSelected=ko.observable('label');
    this.selector = ko.observable();
    this.attr = ko.observableArray(['href', 'text', 'src', 'html']);
    this.attrSelected = ko.observable('text');
    this.filterCategory = ko.observableArray([ '匹配', '移除','替换']);
    this.filterCategorySelected=ko.observable('匹配');
    this.filter = ko.observable();
    this.formater = ko.observable();
    this.formatCategory = ko.observableArray([]);
    this.formatCategorySelected = ko.observable();
}

/**
 *
 * 基本信息View-Model
 * */
function basicInfoViewModel(){
    this.id=ko.observable('');
    this.url=ko.observable('http://www.ccgp-shandong.gov.cn/fin_info/site/index.jsp');
    this.name=ko.observable('山东政府采购网');
    this.tags=ko.observableArray(['财经','体育','经济']);
    this.tagsSelected=ko.observable('财经');
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
 * 自定义属性View-Model
 * */
function customerAttrViewModel(){
    this.regions=ko.observableArray();
    this.parseEngines=ko.observableArray(['jsoup','xpath']);
    /*添加解析域*/
    this.addItem=function(){
        this.regions.push(new singleCustomerViewModel());
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
    this.selectorAttr=ko.observableArray(['href','text','src','html']);
    this.selectorAttrSelected=ko.observable('href');
    this.filter=ko.observable();
    this.filterCategory=ko.observableArray(['匹配','移除','替换']);
    this.filterCategorySelected=ko.observable('匹配');
    this.formater=ko.observable();
    this.formatCategory=ko.observableArray([]);
    this.paginationType=ko.observableArray(['分页步进数','分页的末尾页数','分页的末尾页数','获取分页的记录数']);
    this.paginationTypeSelected=ko.observable('分页步进数');
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
    this.selectorAttrSelected=ko.observable('text');
    this.filter=ko.observable();
    this.filterCategory=ko.observable(['匹配','移除','替换']);
    this.filterCategorySelected=ko.observable('匹配');
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
                this.newsPublishTimeViewModel=new commonAttrViewMode();
                this.newsSourceViewModel=new commonAttrViewMode();
                this.newsContentViewModel=new commonAttrViewMode();

                this.listCustomerAttrViewModel=new customerAttrViewModel();
                this.listOutLinkViewModel=new commonAttrViewMode();
                this.listPaginationViewModel=new paginationViewModel();

                this.newsAttrModels=function(){
                    var attrModels=[];
                    var modelArray=this.newsCustomerAttrViewModel.regions();
                    for(var i=0;i<modelArray.length;i++){
                        var model=modelArray[i];
                        var temp=new customerAttrModel(
                            model.id(),model.target(),model.selectorFunctionSelected(),model.selector(),model.attrSelected(),model.filter(),model.filterCategorySelected(),model.formater(),model.formatCategorySelected()
                        );
                        attrModels.push(temp);
                    }
                    return attrModels;
                };

                this.listAttrModels=function(){
                    var attrModels=[];
                    var modelArray=this.listCustomerAttrViewModel.regions();
                    for(var i=0;i<modelArray.length;i++){
                        var model=modelArray[i];
                        var temp=new customerAttrModel(
                            model.id(),model.target(),model.selectorFunctionSelected(),model.selector(),model.attrSelected(),model.filter(),model.filterCategorySelected(),model.formater(),model.formatCategorySelected()
                        );
                        attrModels.push(temp);
                    }
                    return attrModels;
                };

                this.templateTest=function(){
                    $.ajax({
                        url: '/webapi/crawlToolResource/getJSONString',
                        type: 'POST',
                        data: {
                            data:JSON.stringify({
                                pageModel: {
                                    basicInfoViewModel:{
                                        url:this.basicInfoViewModel.url(),
                                        name:this.basicInfoViewModel.name(),
                                        tagsSelected:this.basicInfoViewModel.tagsSelected()
                                    },
                                    newsCustomerAttrViewModel:{
                                        newsCustomerModels:this.newsAttrModels()
                                    },
                                    newsTitleViewModel:{
                                        selector:this.newsTitleViewModel.selector(),
                                        selectorAttr:this.newsTitleViewModel.selectorAttrSelected()
                                    },
                                    newsPublishTimeViewModel:{
                                        selector:this.newsPublishTimeViewModel.selector(),
                                        selectorAttr:this.newsPublishTimeViewModel.selectorAttrSelected()
                                    },
                                    newsSourceViewModel:{
                                        selector:this.newsSourceViewModel.selector(),
                                        selectorAttr:this.newsSourceViewModel.selectorAttrSelected()
                                    },
                                    newsContentViewModel:{
                                        selector:this.newsContentViewModel.selector(),
                                        selectorAttr:this.newsContentViewModel.selectorAttrSelected()
                                    },
                                    listCustomerAttrViewModel:{
                                        listCustomerModels:this.listAttrModels()
                                    },
                                    listOutLinkViewModel:{
                                        selector:this.listOutLinkViewModel.selector(),
                                        selectorAttr:this.listOutLinkViewModel.selectorAttrSelected()
                                    },
                                    listPaginationViewModel:{
                                        selector:this.listPaginationViewModel.selector(),
                                        selectorAttr:this.listPaginationViewModel.selectorAttrSelected(),
                                        filterCategory:this.listPaginationViewModel.filterCategorySelected(),
                                        filter:this.listPaginationViewModel.filter(),
                                        paginationType:this.listPaginationViewModel.paginationTypeSelected(),
                                        paginationUrl:this.listPaginationViewModel.paginationUrl(),
                                        currentString:this.listPaginationViewModel.currentString(),
                                        replaceTo:this.listPaginationViewModel.replaceTo(),
                                        start:this.listPaginationViewModel.start(),
                                        records:this.listPaginationViewModel.records()
                                    }
                                }
                            })
                        },
                        success: function (result) {
                            alert(result);
                        },
                        error: function () {
                        }
                    });
                }.bind(this);
            })();
            ko.applyBindings(masterVM);
        })
    });

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