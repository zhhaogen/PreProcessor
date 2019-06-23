/**
 * 创建于 2019-04-09 22:30:53
 */
package cn.zhg.preprocessor.demo;


import xiaogen.util.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;

/**
 * @author zzz
 *
 */
public class Main
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try {
			Enumeration<URL> urls = loader.getResources("./");
			Logger.d("currentThread loader");
			if(urls!=null){
				Logger.d(Collections.list(urls));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		new SimpleService().sayHello();
		new HelloService().sayHello();
	}
}
