/**
 * Created by wang on 2014/12/9.
 */
//web网站的虚拟路径
var virtualWebPath="/crawl-template-webtool";
//列表中每页显示记录数
var paginationItemCounts=10;

$(function(){
    $.ajax({
        url: virtualWebPath + '/webapi/crawlToolResource/getTemplateList',
        type: 'GET',
        success: function (data) {
            var json=JSON.parse(data);
            if(json.success){
                if(json.data.templateList!=null){
                    pageInit(json.data.templateList);
                }else{
                    pageInit([]);
                }
            }else{
                pageInit([]);
            }
        },
        error: function (error) {
            pageInit([]);
        }
    });
    

});

/**
 *
 * 页面初始化
 * */
function pageInit(templateList){
   var pageViewModel=new templateViewModel(templateList);
    //绑定并显示
    ko.applyBindings(pageViewModel);
    //加载分页
    loadPaginationComponent(pageViewModel);
    //【导入导出】模式对话框注册事件
    registerExportModalEvent();
}

function registerExportModalEvent(){
    $('#model_export').on('hidden', function () {
        $('#export_result').text('');//清空错误信息
    });

    $('#model_export').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget); // Button that triggered the modal
        var optionType = button.data('optiontype');// Extract info from data-* attributes
        var modal = $(this);
        var modalTitle=modal.find('#model_export_title');
        var btnConfirm=modal.find('#btn_modalexport_confirm');
        btnConfirm.unbind();//移除按钮之前绑定的事件
        if(optionType=="file_export"){//导出
            modalTitle.text('导出模板到文件');
            btnConfirm.click(btnModalExportConfirmHandler);
        }
        if(optionType=="file_import"){//导入
            modalTitle.text('导入模板文件');
            btnConfirm.click(btnModalImportConfirmHandler);
        }
    });
}

function btnModalImportConfirmHandler(){
    var filePath=$('#file_path').val();
    $.ajax({
        url: virtualWebPath + '/webapi/crawlToolResource/importAllTemplates',
        type: 'POST',
        data: {
            filePath:filePath
        },
        success: function (data) {
            var json=JSON.parse(data);
            if(json.success){
                optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;模板导入操作成功！请刷新该页面！");
            }else{
                if(json.errorMsg=="pathInvalid"){
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;模板导入操作失败！导入文件路径无效！");
                }else{
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;模板导入操作失败！");
                }
            }
            $('#model_export').modal('hide');
        },
        error: function (error) {
            $('#export_result').text('');
            $('#export_result').text("操作失败！错误信息:"+error.responseText);
        }
    });
}

function btnModalExportConfirmHandler(){
    var filePath=$('#file_path').val();
    $.ajax({
        url: virtualWebPath + '/webapi/crawlToolResource/exportAllTemplates',
        type: 'POST',
        data: {
            filePath:filePath
        },
        success: function (data) {
            var json=JSON.parse(data);
            if(json.success){
                optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;导出模板操作成功！");
            }else{
                if(json.errorMsg=="pathInvalid"){
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;导出模板操作失败！导出文件路径无效！");
                }else{
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;导出模板操作失败！");
                }
            }
            $('#model_export').modal('hide');
        },
        error: function (error) {
            $('#export_result').text('');
            $('#export_result').text("操作失败！错误信息:"+error.responseText);
        }
    });
}

/**
 *
 * 加载分页组件
 * @param {Object} result
 * */
function loadPaginationComponent(listViewModel) {
    var totalCount = listViewModel.urls().length;
    $('#data_pagination').pagination({
        items: totalCount,
        itemsOnPage: paginationItemCounts,
        cssStyle: 'light-theme',
        prevText: '上一页',
        nextText: '下一页',
        onPageClick: function (pageNumber, eTarget) {
            var originalUrls=listViewModel.urls();
            var dataIndex=(pageNumber-1)*paginationItemCounts;
            var itemsCounts=dataIndex+paginationItemCounts;
            var nextPageUrls=originalUrls.slice(dataIndex,itemsCounts);
            listViewModel.paginationUrls(nextPageUrls);
        }
    });
}

/*****************View-Model***********************/
function templateViewModel(templateList){
    var self=this;
    var templateListInitData=updateTemplateListInitData(templateList);
    self.urls=ko.observableArray(templateListInitData);
    self.searchString=ko.observable();
    //分页显示的url列表
    self.paginationUrls=ko.observableArray(templateListInitData.slice(0,paginationItemCounts));
    self.addNew=function(){
        window.location.href="pages/template-main.html";
    };
    //搜索
    self.search=function(){
        $.ajax({
            url:virtualWebPath + '/webapi/crawlToolResource/searchTemplateList',
            type:'POST',
            data:{
                searchString:self.searchString()
            },
            success:function(data){
                var json=JSON.parse(data);
                if(json.success){
                    if(json.data.templateList!=null){
                        var templateLists=json.data.templateList;
                        var searchData=updateTemplateListInitData(templateLists);
                        self.urls(searchData);
                        self.paginationUrls(searchData.slice(0,paginationItemCounts));
                        //重新加载分页组件
                        loadPaginationComponent(self);
                    }
                }
            },
            error:function(error){
                self.urls([]);
                if(error){
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;搜索操作执行失败！");
                }
            }
        });
    };

    //删除对话框中的【确定】按钮
    self.modalDelete=function(){
        var item=self.tempData();
        if(item){
            var templateUrl=item.url;
            $.ajax({
                url: virtualWebPath + '/webapi/crawlToolResource/deleteTemplate',
                type: 'POST',
                data:{
                    templateUrl:templateUrl
                },
                success: function (data) {
                    var json=JSON.parse(data);
                    if(json.success){
                        self.paginationUrls.remove(item);
                        self.urls.remove(item);
                        optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;删除成功！");
                    }else{
                        optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;删除失败！");
                    }
                    //手工关闭对话框
                    $('#modal_delete_info').modal('hide');
                },
                error: function (error) {
                    if(error){
                        optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;删除失败！");
                    }
                    //手工关闭对话框
                    $('#modal_delete_info').modal('hide');
                }
            });
        }
    };
    self.tempData=ko.observable();
    //显示删除对话框
    self.showDeleteModal=function(item){
        $('#modal_delete_info').modal('show');
        self.tempData(item);
    };
    self.updateItem=function(){
        var that=this;
        var templateUrl=that.url;
        $.ajax({
            url: virtualWebPath + '/webapi/crawlToolResource/getTemplateGuid',
            type: 'POST',
            data:{
                templateUrl:templateUrl
            },
            success: function (data) {
                var json=JSON.parse(data);
                if(json.success){
                    window.location.href="pages/template-main.html?templateGuid="+json.data;
                }else{
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;修改操作失败！");
                }
            },
            error: function (error) {
                if(error){
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;修改操作失败！");
                }
            }
        });
    };
    self.disableTemplate=function(){
        var that=this;
        var templateUrl=that.url;
        var name=that.name;
        $.ajax({
            url: virtualWebPath + '/webapi/crawlToolResource/disableTemplate',
            type: 'POST',
            data:{
                templateUrl:templateUrl,
                name:name
            },
            success: function (data) {
                that.statusText('停用');
                that.status(false);
            },
            error: function (error) {
                if(error){
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;停用模板操作失败！");
                }
            }
        });
    };
    self.enableTemplate=function(){
        var that=this;
        var templateUrl=that.url;
        var name=that.name;
        $.ajax({
            url: virtualWebPath + '/webapi/crawlToolResource/enableTemplate',
            type: 'POST',
            data:{
                templateUrl:templateUrl,
                name:name
            },
            success: function (data) {
                that.statusText('启用');
                that.status(true);
            },
            error: function (error) {
                if(error){
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;启用模板操作失败！");
                }
            }
        });
    };
}
/*****************View-Model***********************/

/**
 *
 * ajax post请求
 * */
function ajaxPostRequest(url,postData,successHandler,errorHandler){
    $.ajax({
        url: virtualWebPath + url,
        type: 'POST',
        data:postData,
        success: function(data){
            successHandler(data);
        },
        error: function(error){
            errorHandler(error);
        }
    });
}

/**
 *
 * ajax Get 请求
 * */
function ajaxGetRequest(url,successHandler,errorHandler){
    $.ajax({
        url: virtualWebPath + url,
        type: 'GET',
        success: function(data){
            successHandler(data);
        },
        error: function(error){
            errorHandler(error);
        }
    });
}

/**
 *
 * 初始化模板列表数据
 * @param {Array} templateList 模板列表
 * */
function updateTemplateListInitData(templateList){
    var templateListInitData=[];
    if(templateList){
        for(var i=0;i<templateList.length;i++){
            var model=templateList[i];
            if(model==null){
                continue;
            }
            if(model.status=="true"){
                model.statusText=ko.observable("启用");
                model.status=ko.observable(true);
            }else{
                model.statusText=ko.observable("停用");
                model.status=ko.observable(false);
            }
            model.updateUrl="pages/template-main.html?templateGuid="+model.templateId;
            model.targetWindow="_blank";
            templateListInitData.push(model);
        }
    }
    return templateListInitData;
}

/**
 * 操作提示
 * */
function optionExecuteInfo(title,message){
    $('#option_alert').html('')
        .html("<div class=\"alert alert-warning alert-dismissible\" role=\"alert\"><button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\"><span aria-hidden=\"true\">&times;</span></button><strong>"+title+"</strong>"+message+"</div>");
}