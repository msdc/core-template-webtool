/**
 * Created by wang on 2014/12/9.
 */
//web网站的虚拟路径
var virtualWebPath="/crawl-template-webtool";

/**************Models****************/
/**
 *
 * 自定义属性初始化model对象
 * */
function customerAttrModel(target,selector,attr,filter,filterCategory,formatCategory,formatter,filterReplaceTo){
    this.target=target;
    this.selector=selector;
    this.attr=attr;
    this.filter=filter;
    this.filterCategory=filterCategory;
    this.formatCategory=formatCategory;
    this.formatter=formatter;
    this.filterReplaceTo=filterReplaceTo;
}

/**
 *
 * 模板静态属性Tag Model对象
 * */
function templateTagModel(tagKey,tagValue){
    this.tagKey=tagKey;
    this.tagValue=tagValue;
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
    this.selector = ko.observable();
    this.attr = ko.observableArray(['href', 'text', 'src', 'html']);
    this.attrSelected = ko.observable('text');
    this.filterCategory = ko.observableArray([ '匹配','替换', '移除']);
    this.filterCategorySelected=ko.observable('匹配');
    this.filter = ko.observable();
    this.formatter=ko.observable();
    this.formatCategory=ko.observableArray(['日期']);
    this.formatCategorySelected=ko.observable('日期');
    this.showMatchFilter=ko.computed(function(){
        if(this.filterCategorySelected()=='匹配'){
            return true;
        }else{
            return false;
        }
    },this);
    this.showRemoveFilter=ko.computed(function(){
        if(this.filterCategorySelected()=='移除'){
            return true;
        }else{
            return false;
        }
    },this);
    this.showReplaceFilter=ko.computed(function(){
        if(this.filterCategorySelected()=='替换'){
            return true;
        }else{
            return false;
        }
    },this);
    this.filterReplaceTo=ko.observable();
}

/**
 *
 * 调度任务的View-Model
 * */
function scheduleDispatchViewModel(){
    this.periods=ko.observableArray(['hour','day','week']);
    this.periodsSelected=ko.observable('hour');
    this.sequence=ko.observable();
}

/**
 *
 * 单个Tag的View-Model
 * */
function singleTemplateTagViewModel(){
    this.tagKey=ko.observable();
    this.tagValue=ko.observable();
}

/**
 *
 * 模板静态属性的View-Model
 * */
function templateTagsViewModel(){
    this.tags=ko.observableArray();
    this.addTag=function(){
        this.tags.push(new singleTemplateTagViewModel());
    }.bind(this);
    this.removeTag=function(item){
        this.tags.remove(item);
    }.bind(this);
}

/**
 *
 * 增量配置的View-Model
 * */
function templateIncreaseViewModel(){
    this.periods=ko.observableArray(['hour','day','week']);
    this.periodsSelected=ko.observable('hour');
    this.pageCounts=ko.observable();
    this.pageSort=ko.observableArray(['升序','倒序']);
    this.pageSortSelected=ko.observable('升序');
}

/**
 *
 * 基本信息View-Model
 * */
function basicInfoViewModel(){
    this.id=ko.observable('');
    this.url=ko.observable();
    this.name=ko.observable();
    this.tags=ko.observableArray(['财经','体育','经济']);
    this.tagsSelected=ko.observable('财经');
    this.viewHtmlContent=function(){
        var url=this.url;
        $('#modalHtmlTitle').text('Html内容');
        $('#modal-viewHtml').modal('show');
        $.ajax({
            url:virtualWebPath+'/webapi/crawlToolResource/viewHtmlContent',
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
    this.filterCategory=ko.observableArray(['匹配','替换','移除']);
    this.filterCategorySelected=ko.observable('匹配');
    this.formatter=ko.observable();
    this.formatCategory=ko.observableArray([]);
    this.paginationType=ko.observableArray(['分页的末尾页数','分页步进数','获取分页的记录数','获取分页URL','自定义分页']);
    this.paginationTypeSelected=ko.observable('分页的末尾页数');
    this.paginationUrl=ko.observable();
    this.currentString=ko.observable();
    this.replaceTo=ko.observable();
    this.filterReplaceTo=ko.observable();
    this.start=ko.observable();
    this.records=ko.observable();
    this.interval=ko.observable();
    this.lastNumber=ko.observable();
    this.showStart=ko.computed(function(){
        if(this.paginationTypeSelected()=='分页的末尾页数'||this.paginationTypeSelected()=='获取分页的记录数'||this.paginationTypeSelected()=='自定义分页'){
            return true;
        }else{
            return false;
        }
    },this);
    this.showInterval=ko.computed(function(){
        if(this.paginationTypeSelected()=='分页步进数'||this.paginationTypeSelected()=='自定义分页'){
            //进步数默认值
            if(this.paginationTypeSelected()=='分页步进数'){
                this.interval(2);
            }else if(this.paginationTypeSelected()=='自定义分页'){
                this.interval(1);
            }
            return true;
        }else{
            return false;
        }
    },this);
    this.showRecords=ko.computed(function(){
        if(this.paginationTypeSelected()=='获取分页的记录数'){
            return true;
        }else{
            return false;
        }
    },this);
    this.showLastNumber=ko.computed(function(){
        if(this.paginationTypeSelected()=='自定义分页'){
            return true;
        }else{
            return false;
        }
    },this);
    this.showSelector=ko.computed(function(){
        if(this.paginationTypeSelected()=='自定义分页'){
            return false;
        }else{
            return true;
        }
    },this);
    this.showFilter=ko.computed(function(){
        if(this.paginationTypeSelected()=='自定义分页'){
            return false;
        }else{
            return true;
        }
    },this);
    this.showMatchFilter=ko.computed(function(){
        if(this.filterCategorySelected()=='匹配'){
            return true;
        }else{
            return false;
        }
    },this);
    this.showRemoveFilter=ko.computed(function(){
        if(this.filterCategorySelected()=='移除'){
            return true;
        }else{
            return false;
        }
    },this);
    this.showReplaceFilter=ko.computed(function(){
        if(this.filterCategorySelected()=='替换'){
            return true;
        }else{
            return false;
        }
    },this);
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
    this.filterCategory=ko.observable([ '匹配','替换', '移除']);
    this.filterCategorySelected=ko.observable('匹配');
    this.formatter=ko.observable();
    this.formatCategory=ko.observable(['日期']);
    this.formatCategorySelected=ko.observable('日期');
    this.showMatchFilter=ko.computed(function(){
        if(this.filterCategorySelected()=='匹配'){
            return true;
        }else{
            return false;
        }
    },this);
    this.showRemoveFilter=ko.computed(function(){
        if(this.filterCategorySelected()=='移除'){
            return true;
        }else{
            return false;
        }
    },this);
    this.showReplaceFilter=ko.computed(function(){
        if(this.filterCategorySelected()=='替换'){
            return true;
        }else{
            return false;
        }
    },this);
    this.filterReplaceTo=ko.observable();
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

    var templateGuid=getUrlParameter("templateGuid");
    //执行的是update操作
    if(templateGuid!=undefined&&templateGuid!=''&&templateGuid!=null){
        $.ajax({
            url: virtualWebPath + '/webapi/crawlToolResource/updateTemplate',
            type: 'POST',
            data: {
                templateGuid: templateGuid
            },
            success: function (data) {
                var json=JSON.parse(data);
                loadPageContext(json);
            },
            error: function (error) {
            }
        });
    }else{//添加操作页面初始化
        loadPageContext(null);
    }


    //模态对话框事件
    registerModalViewContentEvent();

    //$('#modal_showErrorMessage').on('hidden.bs.modal', function (e) {
       // $('#modal_body_showError').html('');//清空错误信息
    //});
});

/**
 *
 * 页面内容初始化
 * @param {Object} initData 模板结果的JSON对象
 * */
function loadPageContext(initData){
    $('#news_tab').load('template-news.html',function(){
        $('#schedule_tab').load('template-schedule.html',function(){
            $('#list_tab').load('template-list.html',function(){
                //master view model with instances of both the view models.
                var masterVM = (function(){
                    this.basicInfoViewModel = new basicInfoViewModel();
                    this.scheduleDispatchViewModel=new scheduleDispatchViewModel();
                    this.templateTagsViewModel=new templateTagsViewModel();
                    this.templateIncreaseViewModel=new templateIncreaseViewModel();

                    this.newsCustomerAttrViewModel=new customerAttrViewModel();
                    this.newsTitleViewModel=new commonAttrViewMode();
                    this.newsPublishTimeViewModel=new commonAttrViewMode();
                    this.newsSourceViewModel=new commonAttrViewMode();
                    this.newsContentViewModel=new commonAttrViewMode();

                    this.listCustomerAttrViewModel=new customerAttrViewModel();
                    this.listOutLinkViewModel=new commonAttrViewMode();
                    this.listPaginationViewModel=new paginationViewModel();

                    if(initData!=null){
                        updateTemplate(initData,this);
                    }else{
                        addNewTemplateDataInit(this);
                        //测试 添加模板
                        //testAddNewTemplate(this);
                    }

                    //内容页自定义属性
                    this.newsAttrModels=function(){
                        var attrModels=[];
                        var modelArray=this.newsCustomerAttrViewModel.regions();
                        for(var i=0;i<modelArray.length;i++){
                            var model=modelArray[i];
                            var temp=new customerAttrModel(
                                model.target(),model.selector(),model.attrSelected(),model.filter(),model.filterCategorySelected(),model.formatCategorySelected(),model.formatter(),model.filterReplaceTo()
                            );
                            attrModels.push(temp);
                        }
                        return attrModels;
                    };

                    //列表页自定义属性
                    this.listAttrModels=function(){
                        var attrModels=[];
                        var modelArray=this.listCustomerAttrViewModel.regions();
                        for(var i=0;i<modelArray.length;i++){
                            var model=modelArray[i];
                            var temp=new customerAttrModel(
                                model.target(),model.selector(),model.attrSelected(),model.filter(),model.filterCategorySelected(),model.formatCategorySelected(),model.formatter(),model.filterReplaceTo()
                            );
                            attrModels.push(temp);
                        }
                        return attrModels;
                    };

                    //模板静态属性列表
                    this.templateTagModels=function(){
                        var attrModels=[];
                        var modelArray=this.templateTagsViewModel.tags();
                        for(var i=0;i<modelArray.length;i++){
                            var model=modelArray[i];
                            var temp=new templateTagModel(model.tagKey(),model.tagValue());
                            attrModels.push(temp);
                        }
                        return attrModels;
                    };

                    //验证内容页
                    this.verifyNewContent=function(){
                        $('#lbl_result_title').text('内容页验证结果');
                        $('#txt_testResult').val('程序正在执行,请稍后...').css({color:'red',fontSize:'20px'});
                        ajaxPostRequest(virtualWebPath+'/webapi/crawlToolResource/verifyNewContent', this, showResultInTextArea, showErrorsInTextArea);
                    };

                    //验证列表页
                    this.verifyListContent=function(){
                        $('#lbl_result_title').text('列表页验证结果');
                        $('#txt_testResult').val('程序正在执行,请稍后...').css({color:'red',fontSize:'20px'});
                        ajaxPostRequest(virtualWebPath+'/webapi/crawlToolResource/verifyListContent', this, showResultInTextArea, showErrorsInTextArea);
                    };

                    /*查看模板JSON*/
                    this.templateTest=function(){
                        $('#lbl_result_title').text('查看模板JSON');
                        $('#txt_testResult').val('程序正在执行,请稍后...').css({color:'red'});
                        ajaxPostRequest(virtualWebPath+'/webapi/crawlToolResource/getJSONString', this, showResultInTextArea, showErrorsInTextArea);
                    }.bind(this);

                    /*保存模板配置*/
                    this.saveTemplate=function(){
                        $('#modalHtmlTitle').text('保存结果');
                        $('#modal-viewHtml').modal('show');
                        ajaxPostRequest(virtualWebPath+'/webapi/crawlToolResource/saveTemplate', this, showResultInModal, showResultInModal);
                    }.bind(this);

                    /*保存增量模板*/
                    this.saveIncreaseTemplate=function(){
                        $('#modalHtmlTitle').text('保存结果');
                        $('#modal-viewHtml').modal('show');
                        ajaxPostRequest(virtualWebPath+'/webapi/crawlToolResource/saveIncreaseTemplate', this, showResultInModal, showResultInModal);
                    }.bind(this);

                    /*保存到本地文件*/
                    this.saveToLocalFile=function(){
                        $('#modalHtmlTitle').text('保存结果');
                        $('#modal-viewHtml').modal('show');
                        ajaxPostRequest(virtualWebPath+'/webapi/crawlToolResource/saveToLocalFile',this,showResultInModal,showResultInModal);
                    }.bind(this);

                })();
                ko.applyBindings(masterVM);
            })
        });
    });
}

/**
 *
 *添加模板 数据初始化
 *@param {Object} pageViewModel 当前页面的View-Model
 * */
function addNewTemplateDataInit(pageViewModel){
    pageViewModel.newsTitleViewModel.selectorAttrSelected('text');
    pageViewModel.newsPublishTimeViewModel.selectorAttrSelected('text');
    pageViewModel.newsContentViewModel.selectorAttrSelected('text');
    pageViewModel.listOutLinkViewModel.selectorAttrSelected('href');
    pageViewModel.listPaginationViewModel.selectorAttrSelected('text');
    pageViewModel.listPaginationViewModel.currentString('##');
    pageViewModel.listPaginationViewModel.start('2');
}

/**
 *
 *[测试] 添加模板
 *@param {Object} pageViewModel 当前页面的View-Model
 * */
function testAddNewTemplate(pageViewModel){
    //基本信息测试
    pageViewModel.basicInfoViewModel.url('http://www.drcnet.com.cn/www/Integrated/Leaf.aspx?uid=040401&version=integrated&chnid=1017&leafid=3018');
    pageViewModel.basicInfoViewModel.name('国研网-银行信托');

    //调度参数测试test
    pageViewModel.scheduleDispatchViewModel.sequence('3');

    //新闻内容标题test
    pageViewModel.newsTitleViewModel.selector('#docSubject');

    //新闻内容时间test
    pageViewModel.newsPublishTimeViewModel.selector('#docDeliveddate');

    //新闻内容test
    pageViewModel.newsContentViewModel.selector('#docSummary');

    //列表页链接test
    pageViewModel.listOutLinkViewModel.selector('#Content_WebPageDocumentsByUId1 > li > div.sub > a');
    pageViewModel.listPaginationViewModel.filter('\\d+');

    //分页test
    pageViewModel.listPaginationViewModel.paginationUrl('http://www.drcnet.com.cn/www/Integrated/Leaf.aspx?uid=040401&version=integrated&chnid=1017&leafid=3018&curpage=##');
    pageViewModel.listPaginationViewModel.selector('#Content_WebPageDocumentsByUId1_span_totalpage');
}

/**
 *
 * 模板修改
 * @param {Object} initData 页面初始化数据对象
 * @param {Object} pageViewModel 当前页面的View-Model
 * */
function updateTemplate(initData,pageViewModel){
    var templateGuid=initData.templateGuid;
    $.ajax({
        url: virtualWebPath + '/webapi/crawlToolResource/getSingleTemplateModel',
        type: 'POST',
        data: {
            templateGuid: templateGuid
        },
        success: function (data) {
            updateTemplateDataInit(initData,pageViewModel,data);
        },
        error: function (error) {
        }
    });
}

/**
 *
 * 模板修改 数据初始化
 * @param {Object} initData 页面初始化数据对象
 * @param {Object} pageViewModel 当前页面的View-Model
 * @param {String}　singleTemplateListJSON　单个模板对象的JSON字符
 * */
function updateTemplateDataInit(initData,pageViewModel,singleTemplateListJSON){
    var templateModel=null;
    if(singleTemplateListJSON!=''&&singleTemplateListJSON!=undefined&&singleTemplateListJSON!=null){
        templateModel=JSON.parse(singleTemplateListJSON);
    }
    //基本信息、调度配置信息、增量配置信息
    if(templateModel!=null){
        //页面标题
        $('title').text(templateModel.name);
        $('#title_config').text(templateModel.name);
        pageViewModel.basicInfoViewModel.name(templateModel.name);
        pageViewModel.basicInfoViewModel.url(templateModel.url);
        //调度配置信息
        pageViewModel.scheduleDispatchViewModel.periodsSelected(templateModel.scheduleDispatchViewModel.period);
        pageViewModel.scheduleDispatchViewModel.sequence(templateModel.scheduleDispatchViewModel.sequence);
        //增量配置信息
        pageViewModel.templateIncreaseViewModel.periodsSelected(templateModel.templateIncreaseViewModel.period);
        pageViewModel.templateIncreaseViewModel.pageCounts(templateModel.templateIncreaseViewModel.pageCounts);
        pageViewModel.templateIncreaseViewModel.pageSortSelected(templateModel.templateIncreaseViewModel.pageSort);
    }

    //列表页外链接
    var listOutLinkArray=initData.list;
    if(listOutLinkArray!=null){
        var listOutLink=listOutLinkArray[0];
        if(listOutLink.indexers!=null){
            var listOutLinkIndexer=listOutLink.indexers[0];
            var listOutLinkSelector=listOutLinkIndexer.value;
            var listOutLinkSelectorAttr=listOutLinkIndexer.attribute;
            pageViewModel.listOutLinkViewModel.selector(listOutLinkSelector);
            pageViewModel.listOutLinkViewModel.selectorAttrSelected(listOutLinkSelectorAttr);
        }

        //列表页的自定义属性
        var listOutLinkLabels=listOutLink.labels;
        if(listOutLinkLabels!=null){
            for(var i=0;i<listOutLinkLabels.length;i++){
                var listCustomerAttrObj=listOutLinkLabels[i];
                var customerViewModel=new singleCustomerViewModel();
                //自定义属性索引器
                if(listCustomerAttrObj.indexers!=null){
                    var customerViewModelIndexer=listCustomerAttrObj.indexers[0];
                    customerViewModel.target(listCustomerAttrObj.name);
                    customerViewModel.selector(customerViewModelIndexer.value);
                    customerViewModel.attrSelected(customerViewModelIndexer.attribute);
                }

                //列表自定义属性过滤器
                if(listCustomerAttrObj.filters!=null){
                    var customerViewModelFilter=listCustomerAttrObj.filters[0];
                    setViewModelFilter(customerViewModel,customerViewModelFilter);
                }

                //列表自定义属性格式化器
                if(listCustomerAttrObj.formats!=null){
                    var customerViewModelFormatter=listCustomerAttrObj.formats[0];
                    setViewModelFormatter(customerViewModel,customerViewModelFormatter);
                }

                pageViewModel.listCustomerAttrViewModel.regions.push(customerViewModel);
            }
        }
    }

    //列表分页
    var listPaginationArray=initData.pagination;
    if(listPaginationArray!=null){
        var listPagination=listPaginationArray[0];
        //分页索引器
        if(listPagination.indexers!=null){
            var listPaginationIndexer=listPagination.indexers[0];
            var listPaginationSelector=listPaginationIndexer.value;
            var listPaginationSelectorAttr=listPaginationIndexer.attribute;
            pageViewModel.listPaginationViewModel.selector(listPaginationSelector);
            pageViewModel.listPaginationViewModel.selectorAttrSelected(listPaginationSelectorAttr);
        }
        //过滤器
        if(listPagination.filters!=null){
            var listPaginationFilter=listPagination.filters[0];
            var listPaginationFilterCategory=listPaginationFilter.type;
            if(listPaginationFilterCategory=="match"){
                pageViewModel.listPaginationViewModel.filterCategorySelected('匹配');
                pageViewModel.listPaginationViewModel.filter(listPaginationFilter.value);
            }
            if(listPaginationFilterCategory=="remove"){
                pageViewModel.listPaginationViewModel.filterCategorySelected('移除');
                pageViewModel.listPaginationViewModel.filter(listPaginationFilter.value);
            }
            if(listPaginationFilterCategory=="replace"){
                pageViewModel.listPaginationViewModel.filterCategorySelected('替换');
                pageViewModel.listPaginationViewModel.filter(listPaginationFilter.value);
                pageViewModel.listPaginationViewModel.filterReplaceTo(listPaginationFilter.replaceTo);
            }

        }
        pageViewModel.listPaginationViewModel.paginationUrl(listPagination.pagitationUrl);
        var paginationType=listPagination.pagitationType;
        if(paginationType=="number"){//分页的末尾页数
            pageViewModel.listPaginationViewModel.paginationTypeSelected('分页的末尾页数');
        }else if(paginationType=="interval"){//分页步进数
            pageViewModel.listPaginationViewModel.paginationTypeSelected('分页步进数');
            pageViewModel.listPaginationViewModel.interval(listPagination.interval);
        }else if(paginationType=="record"){//获取分页的记录数
            pageViewModel.listPaginationViewModel.paginationTypeSelected('获取分页的记录数');
            pageViewModel.listPaginationViewModel.records(listPagination.recordNumber);
        }else if(paginationType=="page"){//获取分页URL
            pageViewModel.listPaginationViewModel.paginationTypeSelected('获取分页URL');
        }else if(paginationType=="custom"){//自定义分页
            pageViewModel.listPaginationViewModel.paginationTypeSelected('自定义分页');
            pageViewModel.listPaginationViewModel.interval(listPagination.interval);
            pageViewModel.listPaginationViewModel.lastNumber(listPagination.lastNumber);
        }
        pageViewModel.listPaginationViewModel.currentString(listPagination.current);
        pageViewModel.listPaginationViewModel.replaceTo(listPagination.replaceTo);
        pageViewModel.listPaginationViewModel.start(listPagination.startNumber);
    }

    //内容页 标题
    var newsArray=initData.news;
    if(newsArray!=null){
        for(var i=0;i<newsArray.length;i++){
            var newsField=newsArray[i];
            var newsFieldName=newsField.name;
            if(newsField.indexers==null){
                continue;
            }
            var newsFieldIndexer=newsField.indexers[0];
            if(newsFieldName=="title"){
                pageViewModel.newsTitleViewModel.selector(newsFieldIndexer.value);
                pageViewModel.newsTitleViewModel.selectorAttrSelected(newsFieldIndexer.attribute);
            }else if(newsFieldName=="content"){
                pageViewModel.newsContentViewModel.selector(newsFieldIndexer.value);
                pageViewModel.newsContentViewModel.selectorAttrSelected(newsFieldIndexer.attribute);
            }else if(newsFieldName=="tstamp"){
                pageViewModel.newsPublishTimeViewModel.selector(newsFieldIndexer.value);
                pageViewModel.newsPublishTimeViewModel.selectorAttrSelected(newsFieldIndexer.attribute);
                if(newsField.filters!=null){
                    var newsPublishTimeViewModelFilter=newsField.filters[0];
                    setViewModelFilter(pageViewModel.newsPublishTimeViewModel,newsPublishTimeViewModelFilter);
                }
                if(newsField.formats!=null){
                    var newsPublishTimeViewModelFormatter=newsField.formats[0];
                    setViewModelFormatter(pageViewModel.newsPublishTimeViewModel,newsPublishTimeViewModelFormatter);
                }
            }else if(newsFieldName=="source"){
                pageViewModel.newsSourceViewModel.selector(newsFieldIndexer.value);
                pageViewModel.newsSourceViewModel.selectorAttrSelected(newsFieldIndexer.attribute);
            }else{
                //内容页的自定义属性
                var customerViewModel=new singleCustomerViewModel();
                //自定义属性索引器
                customerViewModel.target(newsField.name);
                customerViewModel.selector(newsFieldIndexer.value);
                customerViewModel.attrSelected(newsFieldIndexer.attribute);

                //内容页自定义属性过滤器
                if(newsField.filters!=null){
                    var customerViewModelFilter=newsField.filters[0];
                    setViewModelFilter(customerViewModel,customerViewModelFilter);
                }

                //内容页自定义属性格式化器
                if(newsField.formats!=null){
                    var customerViewModelFormatter=newsField.formats[0];
                    setViewModelFormatter(customerViewModel,customerViewModelFormatter);
                }

                pageViewModel.newsCustomerAttrViewModel.regions.push(customerViewModel);
            }
        }
    }

    //模板的静态属性tag
    var templateTags=initData.tags;
    if(templateTags!=null){
        for(var tagKey in templateTags){
            var tagValue=templateTags[tagKey];
            var templateTagModel=new singleTemplateTagViewModel();
            templateTagModel.tagKey(tagKey);
            templateTagModel.tagValue(tagValue);
            pageViewModel.templateTagsViewModel.tags.push(templateTagModel);
        }
    }
}

/**
 *
 * 设置View-Model的过滤器
 * @param {Object} viewModel view-model 对象
 * @param {Object} viewModelFilter viewModel的过滤器
 * */
function setViewModelFilter(viewModel,viewModelFilter){
    var filterCategory=viewModelFilter.type;
    if(filterCategory=="match"){
        viewModel.filterCategorySelected('匹配');
        viewModel.filter(viewModelFilter.value);
    }
    if(filterCategory=="remove"){
        viewModel.filterCategorySelected('移除');
        viewModel.filter(viewModelFilter.value);
    }
    if(filterCategory=="replace"){
        viewModel.filterCategorySelected('替换');
        viewModel.filter(viewModelFilter.value);
        viewModel.filterReplaceTo(viewModelFilter.replaceTo);
    }
}

/**
 *
 * 设置View-Model的格式化器
 * @param {Object} viewModel view-model 对象
 * @param {Object} viewModelFormatter viewModel的格式化器
 * */
function setViewModelFormatter(viewModel,viewModelFormatter){
    var formatterCategory=viewModelFormatter.type;
    if(formatterCategory=="date"){
        viewModel.formatCategorySelected('日期');
        viewModel.formatter(viewModelFormatter.value);
    }
}

/**
 *
 * 在textArea 中显示请求的结果
 * */
function showResultInTextArea(data){
    //$('#modal_body_showError').html('');
    //$('#btn_showErrorMessage').hide();
    var jsonString="";
    try{
        jsonString=JSON.parse(data);
        $('#txt_testResult').val(JSON.stringify(jsonString,null,4)).css({color:'#000000',fontSize:'14px'});
    }catch(e){
        $('#txt_testResult').val(data).css({color:'#000000',fontSize:'14px'});
    }

}

/**
 * 
 * 在textArea中显示请求的错误信息
 * */
function showErrorsInTextArea(error){
	$('#txt_testResult').val("错误信息:"+error.responseText).css({color:'#000000',fontSize:'14px'});
    //$('#modal_body_showError').html(error.responseText);
    //$('#btn_showErrorMessage').show();
}

/**
 *
 * 在模式对话框中显示结果
 * */
function showResultInModal(data){
    var modalBody=$('#modal-viewHtml-body');
    modalBody.text('');//清空
    if(data.responseText){
        modalBody.text("错误信息:"+data.responseText);
    }else{
        modalBody.text(data);
    }
}

/**
 *
 * ajax post 请求
 * @param {String} url
 * @param {Object} data
 * @param {Function} successCallback
 * @param {Function} errorCallback
 * */
function ajaxPostRequest(url,data,successCallback,errorCallback){
    $.ajax({
        url: url,
        type: 'POST',
        data: {
            data: getJSONString(data)
        },
        success: function(data){successCallback(data)},
        error: function(error){errorCallback(error)}
    });
}

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
            selectorAttr:obj.newsPublishTimeViewModel.selectorAttrSelected(),
            filterCategory:obj.newsPublishTimeViewModel.filterCategorySelected(),
            filter:obj.newsPublishTimeViewModel.filter(),
            filterReplaceTo:obj.newsPublishTimeViewModel.filterReplaceTo(),
            formatter:obj.newsPublishTimeViewModel.formatter(),
            formatCategory:obj.newsPublishTimeViewModel.formatCategorySelected()
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
            filterReplaceTo:obj.listPaginationViewModel.filterReplaceTo(),
            start:obj.listPaginationViewModel.start(),
            records:obj.listPaginationViewModel.records(),
            interval:obj.listPaginationViewModel.interval(),
            lastNumber:obj.listPaginationViewModel.lastNumber()
        },
        scheduleDispatchViewModel:{
            domain:getDomainByUrl(obj.basicInfoViewModel.url()),
            period:obj.scheduleDispatchViewModel.periodsSelected(),
            sequence:obj.scheduleDispatchViewModel.sequence()
        },
        templateTagsViewModel:obj.templateTagModels(),
        templateIncreaseViewModel:{
            period:obj.templateIncreaseViewModel.periodsSelected(),
            pageCounts:obj.templateIncreaseViewModel.pageCounts(),
            pageSort:obj.templateIncreaseViewModel.pageSortSelected()
        }
    });
    return jsonString;
}

/**
 *
 * 根据url获取domain
 * */
function getDomainByUrl(url) {
    var url = $.trim(url);
    if(url.search(/^https?\:\/\//) != -1){
        url = url.match(/^https?\:\/\/([^\/?#]+)(?:[\/?#]|$)/i, "");
    }else{
        url = url.match(/^([^\/?#]+)(?:[\/?#]|$)/i, "");
    }

    if(url!=null){
        if(url.length>0){
            return url[1];
        }
    }

    return "";
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