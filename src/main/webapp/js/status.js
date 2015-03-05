/**
 * Created by lenovo on 2015/3/5.
 */
//web网站的虚拟路径
var virtualWebPath="/crawl-template-webtool";
//列表中每页显示记录数
var paginationItemCounts=10;

/*************************View-Model Definition Start**********************************/
/**
 *
 * @summary 种子有效性View-Model
 * */
var seedEffectiveVM=function(urlData){
    this.urls=ko.observableArray(urlData);
};

/**
 *
 * @summary 页面主体View-Model类
 * */
var masterVM=function(urlData){
    this.seedEffectiveVM=new seedEffectiveVM(urlData);
};
/*************************View-Model Definition End**********************************/

$(function(){
    //加载页面主体内容
    initPageContent();
});

function initPageContent(urlData){
    $('#seeds_effective').load('status-seeds.html',function(){
        var mainViewModel=new masterVM(urlData);
        ko.applyBindings(mainViewModel);
        //加载分页控件
        loadPaginationComponent(mainViewModel.seedEffectiveVM);
    });
}

/**
 *
 * 加载分页组件
 * @param {Object} result
 * */
function loadPaginationComponent(seedEffectiveVM) {
    //var totalCount = seedEffectiveVM.urls().length;
    $('#seeds_effective_pagination').pagination({
        items: 10,
        itemsOnPage: paginationItemCounts,
        cssStyle: 'light-theme',
        prevText: '上一页',
        nextText: '下一页',
        onPageClick: function (pageNumber, eTarget) {
        }
    });
}

/******重写Ajax操作,做成通用Loading操作*******/
$.ajax2 = function (options, aimDiv) {
    var img = $("<img id=\"progressImgage\"  src=\"../image/load.gif\" />"); //Loading小图标
    var mask = $("<div id=\"maskOfProgressImage\"></div>").addClass("mask"); //Div遮罩
    var PositionStyle = "fixed";
    //是否将Loading固定在aimDiv中操作,否则默认为全屏Loading遮罩
    if (aimDiv != null && aimDiv != "" && aimDiv != undefined) {
        $(aimDiv).css("position", "relative").append(img).append(mask);
        PositionStyle = "absolute";
    }
    else {
        $("body").append(img).append(mask);
    }
    img.css({
        "z-index": "2000",
        "display": "none"
    });
    mask.css({
        "position": PositionStyle,
        "top": "0",
        "right": "0",
        "bottom": "0",
        "left": "0",
        "z-index": "1000",
        "background-color": "#000000",
        "display": "none"
    });
    var complete = options.complete;
    options.complete = function (httpRequest, status) {
        img.hide();
        mask.hide();
        if (complete) {
            complete(httpRequest, status);
        }
    };
    options.async = true;
    img.show().css({
        "position": PositionStyle,
        "top": "40%",
        "left": "50%",
        "margin-top": function () { return -1 * img.height() / 2; },
        "margin-left": function () { return -1 * img.width() / 2; }
    });
    mask.show().css("opacity", "0.1");
    $.ajax(options);
};