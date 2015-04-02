package com.isoftstone.crawl.template.crawlstate;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.isoftstone.crawl.template.consts.WebtoolConstants;
import com.isoftstone.crawl.template.utils.Config;
import com.isoftstone.crawl.template.utils.ShellUtils;
import com.isoftstone.crawl.template.vo.Runmanager;

/**
 * Created by Administrator on 2015/1/14.
 */

public class ReParseAndIndex {

	private static final Log LOG = LogFactory.getLog(ReParseAndIndex.class);

	public static void reParseAndIndex(String nutch_root, String data_folder, String solr_index, boolean filter) {
		List<String> segParseList = new ArrayList<String>();
		List<String> segIndexList = new ArrayList<String>();

		File[] fis = new File(data_folder).listFiles();
		for (File tpFile : fis) {
			LOG.info("tpFile:" + tpFile);
			if (tpFile.isDirectory()) {
				File[] secfis = tpFile.listFiles();
				for (File sectpFile : secfis) {
					if (sectpFile.isDirectory() && sectpFile.getName().equals(new String("segments"))) {
						String tpindexPath = sectpFile.getPath();
						tpindexPath = tpindexPath.substring(0, tpindexPath.indexOf(new String("segments")));
						segIndexList.add(tpindexPath);
						File[] thirdFile = sectpFile.listFiles();
						for (File thirdtpFile : thirdFile) {
							segParseList.add(thirdtpFile.getPath());
							File[] finalFile = thirdtpFile.listFiles(new FileFilter() {
								@Override
								public boolean accept(File pathname) {
									if (pathname.isDirectory()) {
										if (pathname.getName().equals(new String("crawl_parse")) || pathname.getName().equals(new String("parse_data")) || pathname.getName().equals(new String("parse_text"))) {
											return true;
										} else {
											return false;
										}
									} else {
										return false;
									}
								}
							});
							for (int l = 0; l < finalFile.length; l++) {
								if (filter) {
									removeDir(finalFile[l]);
								}
							}
						}
					}
				}
			}
		}
		if (filter) {
			for (String segment : segParseList) {
				// parse
				String tpStrParse = nutch_root + " parse %s";
				excuteCmd(String.format(tpStrParse, segment));

				String temp = segment.substring(0, segment.indexOf("segments"));
				// 更新crawldb
				String updatedbStr = nutch_root + " updatedb %scrawldb %s";
				excuteCmd(String.format(updatedbStr, temp, segment));

				// 更新linkdb
				String invertlinksStr = nutch_root + " invertlinks %slinkdb %s -noFilter";
				excuteCmd(String.format(invertlinksStr, temp, segment));
				// 过滤重复
				String dedupStr = nutch_root + " dedup %scrawldb";
				excuteCmd(String.format(dedupStr, temp));
			}
		}

		for (String segs : segIndexList) {
			// 索引
			String tpStrIndex = nutch_root + " solrindex " + solr_index + " %scrawldb -linkdb %slinkdb -dir %ssegments";
			excuteCmd(String.format(tpStrIndex, segs, segs, segs));
		}
	}

	public static void excuteCmd(String cmd) {
		Runmanager runmanager = new Runmanager();
		runmanager.setHostIp(Config.getValue(WebtoolConstants.KEY_NUTCH_HOST_IP));
		runmanager.setUsername(Config.getValue(WebtoolConstants.KEY_NUTCH_HOST_USERNAME));
		runmanager.setPassword(Config.getValue(WebtoolConstants.KEY_NUTCH_HOST_PASSWORD));
		runmanager.setPort(22);
		runmanager.setCommand(cmd);
		ShellUtils.execCmd(runmanager);
	}

	public static void removeDir(File dir) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				removeDir(file);
			} else
				LOG.info(file + ":" + file.delete());
		}
		LOG.info(dir + "----" + dir.delete());
	}
}
