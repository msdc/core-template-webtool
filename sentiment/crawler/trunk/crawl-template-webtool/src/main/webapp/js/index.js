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
    self.urls=ko.observableArray(templateList);
    self.addNew=function(){
        window.location.href="pages/template-main.html";
    };
    self.deleteItem=function(){
        var templateUrl=this.url;
        var item=this;
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
}
/*****************View-Model***********************/
/**
 * 操作提示
 * */
function optionExecuteInfo(title,message){
    $('#option_alert').html('')
        .html("<div class=\"alert alert-warning alert-dismissible\" role=\"alert\"><button type=\"button\" class=\"close\" data-dismiss=\"alert\" aria-label=\"Close\"><span aria-hidden=\"true\">&times;</span></button><strong>"+title+"</strong>"+message+"</div>");
}