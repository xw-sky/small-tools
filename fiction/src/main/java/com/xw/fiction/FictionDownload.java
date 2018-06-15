package com.xw.fiction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

public class FictionDownload {
	
	/******************************用户配置s****************************************/
	//章节列表
	String url = "https://www.xxbiquge.com/23_23396/";
	//小说名称
	private static String fictionName = "十方神王.txt";
	//同一章节下载失败后重复尝试次数
	private int maxRetries = 100;
	//同时下载文件数
	private int maxThread = 10;
	/******************************用户配置e****************************************/
	
	private ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThread);
	public static void main(String[] args) {
		FictionDownload app = new FictionDownload();
		//获取章节列表
		List<Chapter> chapterList = app.getChapterList();
		//下载小说内容放入list中
		app.multiDownLoadToList(chapterList);
		
	}
	
	/**
	 * 获取章节列表
	 * @return
	 */
	private List<Chapter> getChapterList(){
		Document doc;
		List<Chapter> list = new ArrayList<Chapter>();
		try {
			doc = Jsoup.connect(url).get();
			Elements links = doc.select("dd a");
			System.out.println("获取列表："+links.size());
			String href;
			for (Element a : links) {
				href = a.attr("abs:href");
				if (href!=null && href.startsWith(url) && href.endsWith(".html")) {
					list.add(new Chapter(a.text(), href));
				}else {
					System.out.println(href);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("章节列表获取失败");
		}
		return list;
	}
	
	//加锁  
    private byte[] lock = new byte[0];
    //下载完成章节数
    public int finishNum = 0; 
    //下载失败的章节数
    public int failNum = 0; 

	/**
	 * 多线程下载章节放入list中
	 * @param chapterList
	 */
	private void multiDownLoadToList(final List<Chapter> chapterList) {
		final int total = chapterList.size();
		System.out.println("需要下载章节总数："+total);
		final long stime = System.currentTimeMillis();
		for (final Chapter chap : chapterList) {
			pool.submit(new Runnable() {
				public void run() {
					chap.content=getContent(chap.url,1);
					synchronized (lock) { 
						boolean flag = true;
						if (chap.content!=null) {
							finishNum++;
						}else {
							failNum++;
							flag = false;
						}
						String msg = "章节总数："+total+"; 已下载完成："+finishNum+"; 下载失败章节数："+failNum+"; 地址:"+chap.url+";  "+chap.name;
						if (flag) {
							System.out.println(msg);
						}else {
							System.err.println(msg);
						}
						if (total == (finishNum+failNum)) {
							System.out.println("下载完成,下载用时"+(System.currentTimeMillis()-stime)+"毫秒");
							//将list的小说内容写入文件
							listToFile(chapterList);
							System.out.println("下载完成，并写出到："+fictionName+" 文件里");
							pool.shutdown();
						}
					}
				}
			});
		}
	}
	
	/**
	 * 获取网页内容
	 * @param pageUrl
	 * @param retrieNum 尝试下载次数
	 * @return
	 */
	private String getContent(String pageUrl,int retrieNum) {
		Document doc=null;
		Element content=null;
		try {
			doc = Jsoup.connect(pageUrl).get();
			content = doc.getElementById("content");
			content.html();
		} catch (Exception e) {
			try {
				if (retrieNum>maxRetries) {
					return null;
				}
				Thread.sleep(1000);
				System.out.println("尝试重新下载章节："+pageUrl);
				return getContent(pageUrl,retrieNum++);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		
//		return content.html().replace("<br>","").replace("&nbsp;&nbsp;&nbsp;&nbsp;", "    ");
		return Jsoup.clean(content.html(), new Whitelist()).replaceAll("&nbsp;", "");
	}

	private void listToFile(List<Chapter> chapterList) {
		//将需要写出的内容放入list中
		List<String> lines = new ArrayList<String>();
		for (Chapter ct : chapterList) {
			if (!ct.name.contains("第")&&!ct.name.contains("章")) {
				ct.name = "第"+ct.name.trim().replaceAll("[\\s]+", "章：");
			}
			lines.add(ct.name);
			lines.add(ct.content);
		}
		//清空文件内容
		try {
			FileUtils.writeLines(new File(fictionName), lines);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("文件写出异常");
		}
	}
	
	class Chapter{
		public String name;
		public String url;
		public String content;
		public Chapter(String name, String url) {
			super();
			this.name = name;
			this.url = url;
		}
	}
}
