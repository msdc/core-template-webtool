/**
 * Created by wang on 2014/12/9.
 */
$(function(){

    /*单条webContent列表类*/
    var webContent=function(listIndex,id,pageDes,pageUrl){
        //页面中的行索引值
        this.listIndex=listIndex;
        //数据数据库中的行id
        this.id=id;
        this.pageDes=pageDes;
        this.pageUrl=pageUrl;

        //配置模板
        this.clickTemplateConfig=function(){
            window.open("pages/template-main.html?id="+id+"&pagedes="+pageDes);
        }.bind(this);
        //删除
        this.clickDelete=function(item){
            listViewModel.urls.remove(item);
        }.bind(this);
    };

    //页面中显示的数据
    var urlArray=[
        new webContent(1,'5','山东政府采购网','http://www.ccgp-shandong.gov.cn/fin_info/site/index.jsp'),
        new webContent(2,'8','甘肃政府采购网','http://www.ccgp-gansu.gov.cn/templet/default/index.html'),
        new webContent(3,'9','福建省政府采购网','http://www.ccgp-fujian.gov.cn/')
    ];

    //view-model
    var listViewModel={
        urls:ko.observableArray(urlArray),
        //添加新的爬取的url
        addNew:function(listIndex,id,pageDes,pageUrl){
            this.urls.push(new webContent(listIndex,id,pageDes,pageUrl));
        },
        //在指定位置添加元素
        addItemInPosition:function(listIndex,id,pageDes,pageUrl){
            this.urls.splice((listIndex-1),1,new webContent(listIndex,id, pageDes,pageUrl));
            //通过这种方式来获取数组中的元素
            //console.log(this.urls().length);
        },
        //[修改]对应需要的值
        getItem:function(listIndex){
            return this.urls()[Number(listIndex)-1];
        }

    };

    //绑定并显示
    ko.applyBindings(listViewModel);

    //初始化[添加]模式对话框
    initModalAdd(listViewModel);
});

/**
 *
 * 初始化添加爬虫页面对话框
 * @param {Object} listViewModel 页面的view-model对象
 * */
function initModalAdd(listViewModel){
    $('#modal-addNew').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);// Button that triggered the modal
        //操作类型
        var optionType=button.data('optiontype');
        // If necessary, you could initiate an AJAX request here (and then do the updating in a callback).
        // Update the modal's content. We'll use jQuery here, but you could use a data binding library or other methods instead.
        var modal = $(this);
        //标题
        var modalTitle=modal.find('.modal-title');
        //[确定]按钮
        var btnAddNew=modal.find('#btn_modalAddNew_sure');
        switch (optionType){
            case "addNew":
            {
                modalTitle.text('添加');
                //清空[添加]对话框中控件的内容
                clearAddNewModalControls();
                //首先移除之前注册的所有事件
                btnAddNew.unbind();
                //模式确定按钮
                btnAddNew.click(function(){
                    var pageUrl=$('#pageUrl').val();
                    var pageDes=$('#pageDes').val();
                    //产生自动增长的序号
                    //todo:

                    //上一行的索引
                    var lastListIndex=$('#table_urlList>tbody>tr:last>td:first').text();
                    //构造新行表格UI行号
                    var currentListIndex='';
                    if(lastListIndex!=undefined&&lastListIndex!=null&&lastListIndex!=''){
                        //需要添加的新行的索引值
                        currentListIndex=Number(lastListIndex)+1;
                    }

                    //产生一个演示的ID,模仿数据库操作
                    //todo:实际操作的ID,应该从数据库中取,在添加完新记录后,返回新添加记录的ID值
                    var id=currentListIndex;

                    //更新界面的UI显示,添加一条新记录
                    //todo:实际操作过程,需要先添加到物理数据库,然后根据添加的结果再决定是否要更新UI显示
                    listViewModel.addNew(currentListIndex,id,pageDes,pageUrl);
                    //关闭[添加]模式对象框
                    $('#modal-addNew').modal('hide');
                });
            }
                break;
            case "update":
            {
                modalTitle.text('修改');
                //特别注意,data-* 后面的值不能用大写,否则获取不到
                var uiListIndex=button.data('listindex');
                var rowSelector = "#table_urlList tbody tr:eq(" + (uiListIndex - 1) + ")";
                var listIndex = $(rowSelector).children('td:eq(0)').text();
                var currentItem=listViewModel.getItem(listIndex);
                //填充[修改]对话框中控件的内容
                updateFillModalControls(currentItem);
                //首先移除之前注册的所有事件
                btnAddNew.unbind();
                btnAddNew.click(function () {
                    //todo:实际操作时,先更新数据库内容,然后根据更新的结果再更新UI显示
                    var rowid=currentItem.id;
                    var listIndex=currentItem.listIndex;
                    var pageDes = $('#pageDes').val();
                    var pageUrl = $('#pageUrl').val();
                    listViewModel.addItemInPosition(listIndex, rowid, pageDes, pageUrl);
                    //关闭[添加]模式对象框
                    $('#modal-addNew').modal('hide');
                });
            }
                break;
        }
    })
}

/**
 *
 *填充修改模式对话框中的控件内容
 * */
function updateFillModalControls(currentItem){
    $('#pageDes').val(currentItem.pageDes);
    $('#pageUrl').val(currentItem.pageUrl);
}

/**
*
 * 清空[添加]对话框中控件的内容
* */
function clearAddNewModalControls(){
    $('#pageUrl').val('');
    $('#pageDes').val('');
}