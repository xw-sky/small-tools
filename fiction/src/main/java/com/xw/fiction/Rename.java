package com.xw.fiction;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 重命名规则类
 * 
 */
class ReplacementChain {
	
	private Map<String, String> map;

	public ReplacementChain() {
		this.map = new HashMap<String, String>();
	}

	public Map<String, String> getMap() {
		return map;
	}

	// 添加新的替换规则(字符串替换)
	public ReplacementChain addRegulation(String oldStr, String newStr) {
		this.map.put(oldStr, newStr);
		return this;
	}
}

/**
 * 重命名类
 * 
 */
public class Rename {
	/**
	 * 批量重命名
	 * 
	 * @param path
	 * @param replacementChain
	 */
	public static void multiRename(String path, ReplacementChain replacementChain) {
		File file = new File(path);
		if (!file.isDirectory()) {
			System.out.println(path + "不是一个文件夹！");
			return;
		}
		File f = null;
		/** 循环遍历所有文件* */
		for (String fileName : file.list()) {
			f = new File(path + File.separatorChar + fileName);
			Map<String, String> map = replacementChain.getMap();
			for (Entry<String, String> entry : map.entrySet()) {
				fileName = fileName.replace(entry.getKey(), entry.getValue());
			}
			f.renameTo(new File(path + File.separatorChar + fileName));
		}
	}

	public static void main(String[] args) {
		ReplacementChain r = new ReplacementChain();
		r.addRegulation("笑傲江湖0", "笑傲江湖");
		String path = "D:\\迅雷下载\\笑傲江湖.40集全.国语中字.2001￡CMCT暮雨潇潇";
		multiRename(path, r);
		System.out.println("恭喜，批量重命名成功！");
	}
}
