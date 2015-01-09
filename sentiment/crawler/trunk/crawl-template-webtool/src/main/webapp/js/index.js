/**
 * Created by wang on 2014/12/9.
 */
//web网站的虚拟路径
var virtualWebPath="/crawl-template-webtool";

$(function(){
    $.ajax({
        url: virtualWebPath + '/webapi/crawlToolResource/getTemplateList',
        type: 'GET',
        success: function (data) {
            var json=JSON.parse(data);
            if(json.templateList!=null){
                pageInit(json.templateList);
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
        itemsOnPage: 8,
        cssStyle: 'light-theme',
        prevText: '上一页',
        nextText: '下一页',
        onPageClick: function (pageNumber, eTarget) {}
    });
}

/*****************View-Model***********************/
function templateViewModel(templateList){
    var self=this;
    var templateListInitData=updateTemplateListInitData(templateList);
    self.urls=ko.observableArray(templateListInitData);
    self.addNew=function(){
        window.location.href="pages/template-main.html";
    };
    self.deleteItem=function(){
        var item=this;
        var templateUrl=item.url;
        $.ajax({
            url: virtualWebPath + '/webapi/crawlToolResource/deleteTemplate',
            type: 'POST',
            data:{
                templateUrl:templateUrl
            },
            success: function (data) {
                if(data=="true"){
                    self.urls.remove(item);
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;删除成功！");
                }else{
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;删除失败！");
                }
            },
            error: function (error) {
                if(error){
                    optionExecuteInfo("操作信息","&nbsp;&nbsp;&nbsp;&nbsp;删除失败！");
                }
            }
        });
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
                window.location.href="pages/template-main.html?templateGuid="+data;
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
                $('#btn_disable').css({display:'none'});
                $('#btn_enable').removeAttr('style');
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
                $('#btn_enable').css({display:'none'});
                $('#btn_disable').removeAttr('style');
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
                model.status=true;
            }else{
                model.statusText=ko.observable("停用");
                model.status=false;
            }
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