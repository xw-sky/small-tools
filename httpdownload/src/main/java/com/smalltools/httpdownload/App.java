package com.smalltools.httpdownload;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * http 代理文件目录地址批量下载
 * 
 */
public class App {

	/******************************用户配置s****************************************/
	//下载网络目录地址
	private static String baseUrl = "http://112.126.64.32:8081/HDP/";
	//本地存储磁盘路径
	private String basePath = "d:/wph/";

	//同时下载文件数
	private int maxThread = 10;
	
	//下载结束后尝试下载失败后的文件次数
	private int maxRetries = 100;
	public static void main(String[] args) {
		App app = new App();

		// 获取需要下载的文件列表
		Set<String> filesSet = new TreeSet<>();
		app.serchFiles(baseUrl, filesSet);
		total = filesSet.size();
		System.out.println("本次所需下载文件数：" + total);
		
		stime = System.currentTimeMillis();
		// 多线程下载
		app.multiDownload(filesSet);
	}
	
	/**
	 * 获取需要下载的文件列表
	 * 
	 * @param url
	 * @param filesSet
	 */
	private void serchFiles(String url, Set<String> filesSet) {
		Document doc = null;
		try {
			// 获取当前网页
			doc = Jsoup.connect(url).get();
			// 获取当前网页的a链接
			Elements links = doc.select("td a");
			String href = null;
			for (Element em : links) {
				href = em.attr("abs:href");
				// 非父级地址跳过
				if (!url.contains(href) && href.startsWith(baseUrl)) {
					// 目录继续递归，文件下载
					if (href.endsWith("/")) {
						serchFiles(href, filesSet);
					} else {
						filesSet.add(href);
					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("获取文件下载列表失败");
			throw new RuntimeException("获取文件下载列表失败");
		}

	}
	/******************************用户配置e****************************************/
	
	
	
	
	
	
	private ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxThread);
	private static Set<String> failFiles = Collections.synchronizedSet(new TreeSet<>());;
	
	/**
	 * 本次需要下载的文件总数
	 */
	private static int total = 0;
	//下载开始时间
	private static long stime;
	//加锁  
    private byte[] lock = new byte[0];
    //开始下载文件数
    public int startNum = 0; 
    //下载完成文件数
    public int finishNum = 0; 
    //是否是重新下载已经失败了的文件
    public boolean isRetry = false; 

	/**
	 * 多线程下载文件
	 * 
	 * @param filesSet
	 */
	public void multiDownload(Set<String> filesSet) {
		int tempTotal = filesSet.size();
		for (String url : filesSet) {
			pool.submit(new Runnable() {
				@Override
				public void run() {
					synchronized (lock) {  
						startNum++;  
					}
					boolean result = true;
					try {
						downloadFile(url);
						failFiles.remove(url);
					} catch (IOException e) {
						result = false;
						failFiles.add(url);
					}
					synchronized (lock) {  
						finishNum++;
						printMeg(total,startNum,finishNum,failFiles.size(),result,url);
						if (tempTotal==finishNum) {
							//重试下载失败的文件
							if (maxRetries>0&&failFiles.size()>0) {
								finishNum = 0;
								isRetry=true;
								try {Thread.sleep(1000);} catch (InterruptedException e) {}
								multiDownload(failFiles);
								maxRetries--;
							}else{
								if (failFiles.size()>0) {
									System.out.println("==========下载结束：失败文件地址："+failFiles);
								}else{
									System.out.println("==========下载结束成功下载：" +(total-failFiles.size())+";  失败："+failFiles.size() + "; 用时："+(System.currentTimeMillis()-stime)+"毫秒");
								}
								//关闭线程池
								pool.shutdown();
							}
						}
						
					}  
				}
			});
		}
	}
	
	/**
	 * 打印下载消息
	 * @param total
	 * @param startNum
	 * @param finishNum
	 * @param i
	 * @param stime 
	 * @param result 
	 * @param url 
	 */
	private void printMeg(int total, int startNum, int finishNum, int failNum, boolean result, String url) {
		String msg = "待下载："+(total-startNum)+"; 下载中："+(startNum - finishNum)+"; 成功下载："+(finishNum-failNum)+"; 失败："+failNum +"; "+Thread.currentThread().getName();
		if (isRetry) {
			msg = "失败文件处理中...，成功下载："+(total-failNum)+"; 失败："+failNum +"; "+Thread.currentThread().getName();
		}
		
		if (result) {
			System.out.println(msg+"; ---成功下载："+url);
		}else{
			System.err.println(msg+"; ---下载失败："+url);
		}
		
	}

	private void downloadFile(String href) throws MalformedURLException, IOException {
		String path = href.substring(baseUrl.length());
		FileUtils.copyURLToFile(new URL(href), new File(basePath + path));
	}
}
