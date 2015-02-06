/**
 * Created by lenovo on 2015/2/6.
 */

//web网站的虚拟路径
var virtualWebPath="/crawl-template-webtool";

$(function(){

   //事件注册
   RegisterEvent();
});

/**
 *
 * 事件注册
 * */
function RegisterEvent(){
   $('#md5_encrypt').click(md5EncryptBtnHandler);
}

/**
 *
 * MD5加密按钮
 * */
function md5EncryptBtnHandler(){
   var md5_template_url=$('#md5_template_url').val();
   var md5_textAreaControl=$('#md5_textArea');

    if(md5_template_url==""){
        md5_textAreaControl.val("模板url不能为空");
        return;
    }

    $.ajax({
        url: virtualWebPath + '/webapi/crawlToolResource/getTemplateGuid',
        type: 'POST',
        data: {
            templateUrl: md5_template_url
        },
        success: function (data) {
            var json=JSON.parse(data);
            if(json.success){
                md5_textAreaControl.val(json.data);
            }else{
                md5_textAreaControl.val("接口调用出错！");
            }
        },
        error: function (error) {
            md5_textAreaControl.val(error.responseText);
        }
    });    
}