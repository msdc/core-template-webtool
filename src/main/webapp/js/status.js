/**
 * Created by lenovo on 2015/3/5.
 */
//web网站的虚拟路径
var virtualWebPath = "/crawl-template-webtool";
//列表中每页显示记录数
var paginationItemCounts = 10;

/*************************View-Model Definition Start**********************************/
/**
 *
 * @summary 种子有效性View-Model
 * */
var seedEffectiveVM = function (urlData) {
    var urlInitData = updateUrlData(urlData);
    this.urls = ko.observableArray(urlInitData);
};

/**
 *
 * @summary 页面主体View-Model类
 * */
var masterVM = function (urlData) {
    this.seedEffectiveVM = new seedEffectiveVM(urlData);
    //分页显示的url列表
    self.paginationUrls = ko.observableArray(urlData.slice(0, paginationItemCounts));
};
/*************************View-Model Definition End**********************************/

/**
 *
 * 初始化列表数据
 * @param {Array} urlData url列表
 * */
function updateUrlData(urlData) {
    var seedsUrlInitData = [];
    if (urlData) {
        for (var i = 0; i < urlData.length; i++) {
            var model = urlData[i];
            if (model == null) {
                continue;
            }
            model.updateUrl = "../template-main.html?templateGuid=" + model.templateId;
            model.targetWindow = "_blank";
            seedsUrlInitData.push(model);
        }
    }
    return seedsUrlInitData;
}

$(function () {
    $.ajax2({
        url: virtualWebPath + '/webapi/crawlToolResource/getSeedsEffectiveStatusList',
        type: 'GET',
        success: function (data) {
            var json = JSON.parse(data);
            if (json.success) {
                if (json.data.seedsEffectiveStatusList != null) {
                    initPageContent(json.data.seedsEffectiveStatusList);
                } else {
                    initPageContent([]);
                }
            } else {
                initPageContent([]);
            }
        },
        error: function (error) {
            initPageContent([]);
        }
    }, '#seedEffective_table');
});

/**
 *
 * @summary 加载页面主体内容
 * @param {Object} urlData 初始化数据
 * */
function initPageContent(urlData) {
    $('#seeds_effective').load('status-seeds.html', function () {
        var mainViewModel = new masterVM(urlData);
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
    var totalCount = seedEffectiveVM.urls().length;
    $('#seeds_effective_pagination').pagination({
        items: totalCount,
        itemsOnPage: paginationItemCounts,
        cssStyle: 'light-theme',
        prevText: '上一页',
        nextText: '下一页',
        onPageClick: function (pageNumber, eTarget) {
            var originalUrls = seedEffectiveVM.urls();
            var dataIndex = (pageNumber - 1) * paginationItemCounts;
            var itemsCounts = dataIndex + paginationItemCounts;
            var nextPageUrls = originalUrls.slice(dataIndex, itemsCounts);
            seedEffectiveVM.paginationUrls(nextPageUrls);
        }
    });
}

/******重写Ajax操作,做成通用Loading操作*******/
$.ajax2 = function (options, aimDiv) {
    var img = $("<img id=\"progressImgage\"  src=\"../../image/load.gif\" />"); //Loading小图标
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
        "margin-top": function () {
            return -1 * img.height() / 2;
        },
        "margin-left": function () {
            return -1 * img.width() / 2;
        }
    });
    mask.show().css("opacity", "0.1");
    $.ajax(options);
};