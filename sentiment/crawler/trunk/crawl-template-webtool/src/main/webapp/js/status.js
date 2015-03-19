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
var seedEffectiveVM = function (mainViewModel, urlData) {
    var that = this;
    var urlInitData = updateSeedsEffectiveData(urlData);
    that.urls = ko.observableArray(urlInitData);
    //分页显示的url列表
    that.paginationUrls = ko.observableArray(urlData.slice(0, paginationItemCounts));
    //注册modal中确定按钮事件 检查种子有效性
    mainViewModel.checkSeedsEffective = function () {
        $('#modal_seedsEffectiveInfo').modal('hide');
        fillPageList('seedsEffectiveStatusList', '/webapi/crawlToolResource/getSeedsEffectiveStatusList', mainViewModel, false, initSeedsEffectiveList);
    };
    //检查单个种子有效性
    that.checkSingleSeedEffective = function () {
        var self = this;
        self.effectiveStatusString('执行中..');
        self.checkTimeString('执行中..');
        sendPostRequest('/webapi/crawlToolResource/refreshSeedEffectiveStatus', self, {templateId: self.templateId}, refreshSingleSeedHandler, refreshSingleSeedErrorHandler);
    };
    //显示提示
    that.showSeedEffectiveInfo = function () {
        $('#modal_seedsEffectiveInfo').modal('show');
    };
};

/**
 *
 * @summary 爬取状态View-model
 * */
var crawlStatusVM = function (mainViewModel, urlData) {
    var that = this;
    that.urls = ko.observableArray(urlData);
    //分页显示的url列表
    that.paginationUrls = ko.observableArray(urlData.slice(0, paginationItemCounts));
    //刷新所有的爬取状态
    that.refreshCrawStatus = function () {
        fillPageList('crawlStatusModelList', '/webapi/crawlToolResource/getCrawlStatusList', mainViewModel, false, initCrawlStatusList);
    };
    //停止爬虫
    that.stopCrawl = function () {
        var self = this;
        self.crawlStatusString('执行中..');
        self.checkTimeString('执行中..');
        sendPostRequest('/webapi/crawlToolResource/stopCrawl', self, {folderName: self.url}, function (data, dataModel) {
            var json = JSON.parse(data);
            if (json.success) {
                var crawlStatusModel = json.data;
                dataModel.crawlStatusString(crawlStatusModel.crawlStatus);
                dataModel.checkTimeString(crawlStatusModel.checkTime);
            }
        }, function (error) {
        });
    };
    //重新索引
    that.reParse = function () {
        var self = this;
        sendAjax2PostRequest('/webapi/crawlToolResource/reParse', self, {folderName: self.url}, null, crawlStatusSuccessHandler, crawlStatusErrorHandler);
    };
    //爬虫增量.
    that.crawlIncrement = function () {
        var self = this;
        sendAjax2PostRequest('/webapi/crawlToolResource/crawlIncrement', self, {folderName: self.url}, null, crawlStatusSuccessHandler, crawlStatusErrorHandler);
    };
    // 爬虫全量.
    that.crawlFull = function () {
        var self = this;
        sendAjax2PostRequest('/webapi/crawlToolResource/crawlFull', self, {folderName: self.url}, null, crawlStatusSuccessHandler, crawlStatusErrorHandler);
    };
};

/**
 *
 * 爬取状态api调用成功回调函数
 * @param {Object} data
 * @param {Object} dataModel
 * */
function crawlStatusSuccessHandler(data, dataModel) {
    $('#modal_showOptionMessageTitle').text('操作结果');
    var modalBody = $('#modal_body_showOptionMessage');
    modalBody.text('');//清空
    if (data.responseText) {
        modalBody.text("错误信息:" + data.responseText);
    } else {
        var result = JSON.parse(data);
        if (result.success) {
            modalBody.text(result.data);
        } else {
            modalBody.text(result.errorMsg);
        }
    }
    $('#modal_showOptionMessage').modal('show');
}

/**
 *
 * 爬取状态api调用失败回调函数
 * */
function crawlStatusErrorHandler(error) {
}

/**
 *
 * @summary 爬取数据View-model
 * */
var crawlDataVM = function (mainViewModel, urlData) {
    var that = this;
    that.urls = ko.observableArray(urlData);
    that.typeName = ko.observable('域名');
    //分页显示的url列表
    that.paginationUrls = ko.observableArray(urlData.slice(0, paginationItemCounts));
    //按照域统计
    that.refreshCrawlData = function () {
        that.typeName('域名');
        fillPageList('crawlDataModelList', '/webapi/crawlToolResource/getCrawlDataList', mainViewModel, false, initCrawlDataList);
    };
    //按分类查询统计
    that.queryByDataSource = function () {
        that.typeName('分类名称');
        fillPageList('crawlDataModelList', '/webapi/crawlToolResource/getCrawlDataListByDataSource', mainViewModel, false, initCrawlDataList);
    };
    //刷新单条抓取数据
    that.refreshSingleData = function () {
        var self = this;
        self.indexCountsString('查询中..');
        self.checkTimeString('查询中..');
        self.todayIndexCountsString('查询中..');
        var postData = {domain: self.url, typeName: '1', dataSource: '1'};
        if (that.typeName() == '域名') {
            postData.typeName = '1';
            postData.dataSource = self.dataSource;
        } else if (that.typeName() == '分类名称') {
            postData.typeName = '2';
            postData.dataSource = self.dataSource;
        }
        sendPostRequest('/webapi/crawlToolResource/refreshCrawlData', self, postData, refreshSingleDataHandler, refreshSingleDataErrorHandler);
    };
};

/**
 *
 * @summary 页面主体View-Model类
 * */
var masterVM = function (urlData) {
    var that = this;
    that.seedEffectiveVM = new seedEffectiveVM(that, urlData);
    that.crawlStatusVM = new crawlStatusVM(that, urlData);
    that.crawlDataVM = new crawlDataVM(that, urlData);
};
/*************************View-Model Definition End**********************************/

/**
 *
 * 刷新种子有效性 回调函数
 * @param {Object} data
 * @param {Object} dataModel
 * */
function refreshSingleSeedHandler(data, dataModel) {
    var json = JSON.parse(data);
    if (json.success) {
        var seedsEffective = json.data;
        dataModel.effectiveStatusString(seedsEffective.effectiveStatus);
        dataModel.checkTimeString(seedsEffective.checkTime);
    }
}


/**
 *
 * 刷新数据失败 回调函数
 * */
function refreshSingleSeedErrorHandler(error) {
}

/**
 *
 * 刷新爬取状态数据成功 回调函数
 * @param {Object} data
 * @param {Object} dataModel
 * */
function refreshSingleDataHandler(data, dataModel) {
    var json = JSON.parse(data);
    if (json.success) {
        var crawlDataModel = json.data;
        dataModel.indexCountsString(crawlDataModel.indexCounts);
        dataModel.checkTimeString(crawlDataModel.checkTime);
        dataModel.todayIndexCountsString(crawlDataModel.todayIndexCounts);
    }
}

/**
 *
 * 刷新数据失败 回调函数
 * */
function refreshSingleDataErrorHandler(error) {
}

/**
 *
 * 发送post请求
 * @param {String} url
 * @param {Object} dataModel
 * @param {Object} data
 * @param {Function} successHandler
 * @param {Function} errorHandler
 * */
function sendPostRequest(url, dataModel, data, successHandler, errorHandler) {
    $.ajax({
        url: virtualWebPath + url,
        type: 'POST',
        data: data,
        success: function (data) {
            successHandler(data, dataModel);
        },
        error: function (error) {
            errorHandler(error, dataModel);
        }
    });
}

/**
 *
 * 发送post请求
 * @param {String} url
 * @param {Object} dataModel
 * @param {Object} data
 * @param {String} targetElement
 * @param {Function} successHandler
 * @param {Function} errorHandler
 * */
function sendAjax2PostRequest(url, dataModel, data, targetElement, successHandler, errorHandler) {
    $.ajax2({
        url: virtualWebPath + url,
        type: 'POST',
        data: data,
        success: function (data) {
            successHandler(data, dataModel);
        },
        error: function (error) {
            errorHandler(error, dataModel);
        }
    }, targetElement);
}

/**
 *
 * ajax get请求填充页面列表数据
 * @param {String} listType 需要填充的页面列表类型
 * @param {String} url 接口调用url
 * @param {Object} mainViewModel 页面View-Model
 * @param {Boolean} isPageLoad 页面是否第一次加载
 * @param {Function} callback 回调函数
 * */
function fillPageList(listType, url, mainViewModel, isPageLoad, callback) {
    $.ajax2({
        url: virtualWebPath + url,
        type: 'GET',
        success: function (data) {
            var json = JSON.parse(data);
            if (json.success) {
                switch (listType) {
                    case 'seedsEffectiveStatusList'://种子有效性列表
                    {
                        if (json.data.seedsEffectiveStatusList != null) {
                            callback(isPageLoad, mainViewModel, json.data.seedsEffectiveStatusList);
                        } else {
                            callback(isPageLoad, mainViewModel, []);
                        }
                    }
                        break;
                    case 'crawlStatusModelList'://爬取状态
                    {
                        if (json.data.crawlStatusModelList != null) {
                            callback(isPageLoad, mainViewModel, json.data.crawlStatusModelList);
                        } else {
                            callback(isPageLoad, mainViewModel, []);
                        }
                    }
                        break;
                    case 'crawlDataModelList'://爬取数据
                    {
                        if (json.data.crawlDataModelList != null) {
                            callback(isPageLoad, mainViewModel, json.data.crawlDataModelList);
                        } else {
                            callback(isPageLoad, mainViewModel, []);
                        }
                    }
                        break;
                }
            } else {
                callback(isPageLoad, mainViewModel, []);
            }
        },
        error: function (error) {
            callback(isPageLoad, mainViewModel, []);
        }
    });
}

/**
 *
 * 初始化列表数据
 * @param {Array} urlData url列表
 * */
function updateSeedsEffectiveData(urlData) {
    var seedsUrlInitData = [];
    if (urlData) {
        for (var i = 0; i < urlData.length; i++) {
            var model = urlData[i];
            if (model == null) {
                continue;
            }
            model.updateUrl = "../template-main.html?templateGuid=" + model.templateId;
            model.targetWindow = "_blank";
            //添加ko绑定 种子有效性
            model.effectiveStatusString = ko.observable(model.effectiveStatus);
            //添加ko 绑定 检查时间
            model.checkTimeString = ko.observable(model.checkTime);
            seedsUrlInitData.push(model);
        }
    }
    return seedsUrlInitData;
}

$(function () {
    var mainViewModel = new masterVM([]);
    ko.applyBindings(mainViewModel);

    //初始化种子有效性列表测试数据
    fillPageList('seedsEffectiveStatusList', '/webapi/crawlToolResource/getSeedsEffectiveStatusCache', mainViewModel, true, initSeedsEffectiveList);
    //初始化爬取状态列表
    fillPageList('crawlStatusModelList', '/webapi/crawlToolResource/getCrawlStatusCache', mainViewModel, true, initCrawlStatusList);
    //初始化爬取数据列表
    fillPageList('crawlDataModelList', '/webapi/crawlToolResource/getCrawlDataCache', mainViewModel, true, initCrawlDataList);

    //注册Tab显示事件
    registerTabShownEvent(mainViewModel);
});

/**
 *
 * 注册Tab显示事件
 * */
function registerTabShownEvent(mainViewModel) {
    $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
        var target = e.target.hash; // newly activated tab
        if (target == "#seeds_effective") {
            //var sampleData = seedEffectiveSampleData();//测试数据
            //初始化种子有效性列表
            //initSeedsEffectiveList(mainViewModel, null);
        }
        else if (target == '#crawl_status') {
            //var sampleData = crawlStatusSampleData();//测试数据
            //初始化爬取状态列表
            //initCrawlStatusList(mainViewModel, []);
        } else if (target == '#crawl_data') {
            //var sampleData = crawlDataSampleData();//测试数据
            //初始化爬取数据列表
            //initCrawlDataList(mainViewModel, []);
        }
    });
}

/**
 *
 * @summary 种子有效性列表
 * @param {Boolean} isPageLoad 判断页面是否第一次加载
 * @param {Object} mainViewModel 整个页面的View-Model对象
 * @param {Object|Array} initData 初始化数据
 * */
function initSeedsEffectiveList(isPageLoad, mainViewModel, initData) {
    var updatedSampleData = updateSeedsEffectiveData(initData);
    mainViewModel.seedEffectiveVM.urls(updatedSampleData);
    mainViewModel.seedEffectiveVM.paginationUrls(mainViewModel.seedEffectiveVM.urls().slice(0, paginationItemCounts));
    //加载种子有效性页面分页控件
    loadPaginationComponent('#seeds_effective_pagination', mainViewModel.seedEffectiveVM);
    if (!isPageLoad) {
        //提示
        showOptionModalMessage('操作结果', '检查种子有效性操作已执行完毕！');
    }
}

/**
 *
 * 显示操作提示
 * @param {String}　title 提示框标题
 * @param {String} message 提示信息
 * */
function showOptionModalMessage(title, message) {
    //显示操作成功提示
    $('#modal_showOptionMessageTitle').text(title);
    var modalBody = $('#modal_body_showOptionMessage');
    modalBody.text('');//清空
    modalBody.text(message);//清空
    $('#modal_showOptionMessage').modal('show');
}

/**
 *
 * 构造新的数据ko数据
 * */
function updateInitCrawlStatus(initData) {
    var crawlStatusList = [];
    if (initData) {
        for (var i = 0; i < initData.length; i++) {
            var model = initData[i];
            if (model == null) {
                continue;
            }
            //添加ko绑定 爬取状态
            model.crawlStatusString = ko.observable(model.crawlStatus);
            //添加ko 绑定 检查时间
            model.checkTimeString = ko.observable(model.checkTime);
            crawlStatusList.push(model);
        }
    }

    return crawlStatusList;
}

/**
 *
 * @summary 爬取状态列表
 * @param {Boolean} isPageLoad 判断页面是否第一次加载
 * @param {Object} mainViewModel 整个页面的View-Model对象 *
 * @param {Object|Array} initData 初始化数据
 * */
function initCrawlStatusList(isPageLoad, mainViewModel, initData) {
    var updatedData = updateInitCrawlStatus(initData);
    mainViewModel.crawlStatusVM.urls(updatedData);
    mainViewModel.crawlStatusVM.paginationUrls(mainViewModel.crawlStatusVM.urls().slice(0, paginationItemCounts));
    //加载爬取数据页面分页控件
    loadPaginationComponent('#crawl_status_pagination', mainViewModel.crawlStatusVM);
    if (!isPageLoad) {
        //提示
        showOptionModalMessage('操作结果', '爬取状态查询完毕！');
    }
}

/**
 *
 * 构造新的数据ko数据
 * */
function updateInitCrawlData(initData) {
    var crawlDataList = [];
    if (initData) {
        for (var i = 0; i < initData.length; i++) {
            var model = initData[i];
            if (model == null) {
                continue;
            }
            //添加ko绑定 索引条数
            model.indexCountsString = ko.observable(model.indexCounts);
            //添加ko 绑定 检查时间
            model.checkTimeString = ko.observable(model.checkTime);
            //今日索引
            model.todayIndexCountsString = ko.observable(model.todayIndexCounts);
            crawlDataList.push(model);
        }
    }

    return crawlDataList;
}

/**
 *
 * @summary 爬取数据列表
 * @param {Boolean} isPageLoad 判断页面是否第一次加载
 * @param {Object} mainViewModel 整个页面的View-Model对象
 * @param {Object|Array} initData 初始化数据
 * */
function initCrawlDataList(isPageLoad, mainViewModel, initData) {
    var updatedData = updateInitCrawlData(initData);
    mainViewModel.crawlDataVM.urls(updatedData);
    mainViewModel.crawlDataVM.paginationUrls(mainViewModel.crawlDataVM.urls().slice(0, paginationItemCounts));
    //加载爬取状态页面分页控件
    loadPaginationComponent('#crawl_data_pagination', mainViewModel.crawlDataVM);
    if (!isPageLoad) {
        //提示
        showOptionModalMessage('操作结果', '爬取数据查询完毕！');
    }
}

/**
 *
 * 种子有效性测试数据
 * */
function seedEffectiveSampleData() {
    var sampleData = [
        {name: '上海证券报-信托研究', url: 'http://caifu.cnstock.com/list/xingtuo_yanjiu', effectiveStatus: '有效'},
        {name: '上海证券报-信托调查', url: 'http://caifu.cnstock.com/list/xingtuo_diaocha', effectiveStatus: '有效'}
    ];
    return sampleData;
}

/**
 *
 * 爬取状态测试数据
 * */
function crawlStatusSampleData() {
    var sampleData = [
        {name: '上海证券报-信托研究', url: 'http://caifu.cnstock.com/list/xingtuo_yanjiu', crawlStatus: '爬去中'},
        {name: '上海证券报-信托调查', url: 'http://caifu.cnstock.com/list/xingtuo_diaocha', crawlStatus: '注入'}
    ];
    return sampleData;
}

/**
 *
 * 爬取数据测试数据
 * */
function crawlDataSampleData() {
    var sampleData = [
        {name: '上海证券报-信托研究', url: 'http://caifu.cnstock.com/list/xingtuo_yanjiu', indexCounts: '2890', todayIndexCounts: '289'},
        {name: '上海证券报-信托调查', url: 'http://caifu.cnstock.com/list/xingtuo_diaocha', indexCounts: '2234', todayIndexCounts: '123'}
    ];
    return sampleData;
}

/**
 *
 * 加载分页组件
 * @param {String} paginationDomElement 加载分页的DOM元素
 * @param {Object} viewModel 需要分页的ViewModel
 * *
 * */
function loadPaginationComponent(paginationDomElement, viewModel) {
    var totalCount = viewModel.urls().length;
    $(paginationDomElement).pagination({
        items: totalCount,
        itemsOnPage: paginationItemCounts,
        cssStyle: 'light-theme',
        prevText: '上一页',
        nextText: '下一页',
        onPageClick: function (pageNumber, eTarget) {
            var originalUrls = viewModel.urls();
            var dataIndex = (pageNumber - 1) * paginationItemCounts;
            var itemsCounts = dataIndex + paginationItemCounts;
            var nextPageUrls = originalUrls.slice(dataIndex, itemsCounts);
            viewModel.paginationUrls(nextPageUrls);
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