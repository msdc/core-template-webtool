/**
 * Created by wang on 2014/12/9.
 */
/**************Models****************/
function customerAttrModel(selector,attr,filter,filterCategory){
    //this.parseEngine=['jsoup','xpath'];
    //this.id=id;
    //this.target=target;
    //this.selectorFunction=selectorFunction;
    this.selector=selector;
    this.attr=attr;
    this.filter=filter;
    this.filterCategory=filterCategory;
    //this.formater=formater;
    //this.formatCategory=formatCategory;
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
    this.filterCategory = ko.observableArray([ '匹配', '移除']);
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
    this.url=ko.observable('http://www.ccgp-gansu.gov.cn/votoonadmin/article/classlist.jsp?pn=1&class_id=');
    this.name=ko.observable();
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
    this.filterCategory=ko.observableArray(['匹配','移除']);
    this.filterCategorySelected=ko.observable('匹配');
    this.formater=ko.observable();
    this.formatCategory=ko.observableArray([]);
    this.paginationType=ko.observableArray(['分页的末尾页数','分页步进数','获取分页的记录数']);
    this.paginationTypeSelected=ko.observable('分页的末尾页数');
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
    this.filterCategory=ko.observable(['匹配','移除']);
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
                //新闻内容标题test
                this.newsTitleViewModel.selector('body > table:nth-child(2) > tbody > tr > td > table > tbody > tr > td > table > tbody > tr > td > table > tbody > tr > td > table:nth-child(1) > tbody > tr > td');
                this.newsTitleViewModel.selectorAttrSelected('text');

                this.newsPublishTimeViewModel=new commonAttrViewMode();
                this.newsSourceViewModel=new commonAttrViewMode();

                this.newsContentViewModel=new commonAttrViewMode();
                //新闻内容test
                this.newsContentViewModel.selector('body > table:nth-child(2) > tbody > tr > td > table > tbody > tr > td > table > tbody > tr > td > table > tbody > tr > td > table:nth-child(2) > tbody > tr > td');
                this.newsContentViewModel.selectorAttrSelected('text');

                this.listCustomerAttrViewModel=new customerAttrViewModel();
                this.listOutLinkViewModel=new commonAttrViewMode();
                //列表页test
                this.listOutLinkViewModel.selector('body > form > table:nth-child(2) > tbody > tr > td:nth-child(2) > table:nth-child(2) > tbody > tr > td > table > tbody > tr > td > table:nth-child(2) > tbody > tr:nth-child(1) > td:nth-child(2) > table > tbody > tr:nth-child(2n-1) > td:nth-child(2) > a');
                this.listOutLinkViewModel.selectorAttrSelected('href');

                this.listPaginationViewModel=new paginationViewModel();
                //分页test
                this.listPaginationViewModel.paginationUrl('http://www.ccgp-gansu.gov.cn/votoonadmin/article/classlist.jsp?pn=##&class_id=213');
                this.listPaginationViewModel.selector('body > form > table:nth-child(2) > tbody > tr > td:nth-child(2) > table:nth-child(2) > tbody > tr > td > table > tbody > tr > td > table:nth-child(3) > tbody > tr > td:nth-child(1) > font:nth-child(2)');
                this.listPaginationViewModel.selectorAttrSelected('text');
                this.listPaginationViewModel.currentString('##');
                this.listPaginationViewModel.start('2');
                this.listPaginationViewModel.filter('\\d+');

                this.newsAttrModels=function(){
                    var attrModels=[];
                    var modelArray=this.newsCustomerAttrViewModel.regions();
                    for(var i=0;i<modelArray.length;i++){
                        var model=modelArray[i];
                        var temp=new customerAttrModel(
                            model.selector(),model.attrSelected(),model.filter(),model.filterCategorySelected()
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
                           model.selector(),model.attrSelected(),model.filter(),model.filterCategorySelected()
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
                            data:getJSONString(this)
                        },
                        success: function (result) {
                            alert(result);
                        },
                        error: function () {
                        }
                    });
                }.bind(this);

                this.saveTemplate=function(){
                    $.ajax({
                        url: '/webapi/crawlToolResource/saveTemplate',
                        type: 'POST',
                        data: {
                            data: getJSONString(this)
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
 * @param {Object} obj=masterVM
 * */
function getJSONString(obj){
    var jsonString=JSON.stringify({
        basicInfoViewModel:{
            url:obj.basicInfoViewModel.url(),
            name:obj.basicInfoViewModel.name(),
            tag:obj.basicInfoViewModel.tagsSelected()
        },
        newsCustomerAttrViewModel:obj.newsAttrModels(),
        newsTitleViewModel:{
            selector:obj.newsTitleViewModel.selector(),
            selectorAttr:obj.newsTitleViewModel.selectorAttrSelected()
        },
        newsPublishTimeViewModel:{
            selector:obj.newsPublishTimeViewModel.selector(),
            selectorAttr:obj.newsPublishTimeViewModel.selectorAttrSelected()
        },
        newsSourceViewModel:{
            selector:obj.newsSourceViewModel.selector(),
            selectorAttr:obj.newsSourceViewModel.selectorAttrSelected()
        },
        newsContentViewModel:{
            selector:obj.newsContentViewModel.selector(),
            selectorAttr:obj.newsContentViewModel.selectorAttrSelected()
        },
        listCustomerAttrViewModel:obj.listAttrModels(),
        listOutLinkViewModel:{
            selector:obj.listOutLinkViewModel.selector(),
            selectorAttr:obj.listOutLinkViewModel.selectorAttrSelected()
        },
        listPaginationViewModel:{
            selector:obj.listPaginationViewModel.selector(),
            selectorAttr:obj.listPaginationViewModel.selectorAttrSelected(),
            filterCategory:obj.listPaginationViewModel.filterCategorySelected(),
            filter:obj.listPaginationViewModel.filter(),
            paginationType:obj.listPaginationViewModel.paginationTypeSelected(),
            paginationUrl:obj.listPaginationViewModel.paginationUrl(),
            currentString:obj.listPaginationViewModel.currentString(),
            replaceTo:obj.listPaginationViewModel.replaceTo(),
            start:obj.listPaginationViewModel.start(),
            records:obj.listPaginationViewModel.records()
        }
    });
    return jsonString;
}

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