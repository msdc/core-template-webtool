/**
 * Created by linux on 14-12-5.
 */
$(function(){
    //view template button event register.
    registeViewTemplateEvent();

    //test template
    registeTestTemplateEvent();
});

function registeTestTemplateEvent(){
    $('#btn_test_template').click(btnTestButtonEventHandler);
}

function btnTestButtonEventHandler(){
    sendRequest('/webapi/crawlToolResource/testTemplateJsonString','POST',function(result){
        $('#txt_json_template').val(JSON.stringify(result,null,4));
    });
}

function registeViewTemplateEvent(){
    $('#btn_view_template').click(btnSaveButtonEventHandler);
}

//保存按钮的单击事件
function btnSaveButtonEventHandler(){
    sendRequest('/webapi/crawlToolResource/viewTemplateJsonString','POST',function(result){
        $('#txt_json_template').val(JSON.stringify(result,null,4));
    });
}

/**
 *
 * 向Java servlet发送ajax请求获取数据
 * @param {String} url servlet中对应资源的url
 * @param {String} method ajax请求的类型
 * @param {Function} callback 回调函数，包含请求的结果
 * */
function sendRequest(url,method,callback){
    var webUrl=$('#txt_webpage_url').val();
    var jsoupSelector=$('#txt_jsoup_selector').val();
    var attribute=$('#txt_jsoup_attribute').val();
    var filterString=$('#txt_filter_string').val();
    var formaterString=$('#txt_formater_string').val();

    $.ajax({
        url:url,
        type:method,
        data:{
            webUrl:webUrl,
            jsoupSelector:jsoupSelector,
            attribute:attribute,
            filterString:filterString,
            formaterString:formaterString
        },
        success:function(result){
            callback(result);
        },
        error:function(err){
            if(err){
                callback(err);
            }
        }
    });
}

/**
 *
 * 格式化Json数据并输出
 * */
function FormatJSON(oData, sIndent) {
    if (arguments.length < 2) {
        var sIndent = "";
    }
    var sIndentStyle = "    ";
    var sDataType = RealTypeOf(oData);

    // open object
    if (sDataType == "array") {
        if (oData.length == 0) {
            return "[]";
        }
        var sHTML = "[";
    } else {
        var iCount = 0;
        $.each(oData, function() {
            iCount++;
            return;
        });
        if (iCount == 0) { // object is empty
            return "{}";
        }
        var sHTML = "{";
    }

    // loop through items
    var iCount = 0;
    $.each(oData, function(sKey, vValue) {
        if (iCount > 0) {
            sHTML += ",";
        }
        if (sDataType == "array") {
            sHTML += ("\n" + sIndent + sIndentStyle);
        } else {
            sHTML += ("\n" + sIndent + sIndentStyle + "\"" + sKey + "\"" + ": ");
        }

        // display relevant data type
        switch (RealTypeOf(vValue)) {
            case "array":
            case "object":
                sHTML += FormatJSON(vValue, (sIndent + sIndentStyle));
                break;
            case "boolean":
            case "number":
                sHTML += vValue.toString();
                break;
            case "null":
                sHTML += "null";
                break;
            case "string":
                sHTML += ("\"" + vValue + "\"");
                break;
            default:
                sHTML += ("TYPEOF: " + typeof(vValue));
        }

        // loop
        iCount++;
    });

    // close object
    if (sDataType == "array") {
        sHTML += ("\n" + sIndent + "]");
    } else {
        sHTML += ("\n" + sIndent + "}");
    }

    // return
    return sHTML;
}

function RealTypeOf(v) {
    if (typeof(v) == "object") {
        if (v === null) return "null";
        if (v.constructor == (new Array).constructor) return "array";
        if (v.constructor == (new Date).constructor) return "date";
        if (v.constructor == (new RegExp).constructor) return "regex";
        return "object";
    }
    return typeof(v);
}