package com.isoftstone.crawl.template.utils;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import com.isoftstone.crawl.template.consts.WebtoolConstants;

public class HdfsCommon {

    public static void upFileToHdfs(String fileName) throws IOException {
        // 将本地文件上传到hdfs。
        //"hdfs://192.168.100.231:8020/user/hdfs/tmp1/"
        String target = Config.getValue(WebtoolConstants.KEY_HDFS_ROOT_FOLDER);
        if (StringUtils.isBlank(target)) {
            return;
        }
        Configuration config = new Configuration();
        FileSystem fs = FileSystem.get(URI.create(target), config);
        //--拷贝全量种子文件夹.
        fs.copyFromLocalFile(false, true, new Path(fileName),
            new Path(target));
        // // copy
        // IOUtils.copyBytes(fis, os, 4096, true);
    }

}
