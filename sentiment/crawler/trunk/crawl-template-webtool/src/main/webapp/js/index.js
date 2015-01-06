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
    //view-model
    var pageViewModel={
        urls:ko.observableArray(templateList),
        addNew:function(){
            window.location.href="pages/template-main.html";
        }.bind(this)
    };

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