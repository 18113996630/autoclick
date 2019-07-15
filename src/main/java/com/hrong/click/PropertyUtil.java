package com.hrong.click;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @Author hrong
 **/
public class PropertyUtil {
	private static String basePath = System.getProperty("user.dir");
	private static Properties properties;

	static {
		properties = new Properties();
		InputStream inputStream;
		try {
			inputStream = new FileInputStream(basePath + "/conf/conf.properties");
			properties.load(inputStream);
		} catch (IOException e) {
			inputStream = PropertyUtil.class.getClassLoader().getResourceAsStream("conf.properties");
			try {
				properties.load(inputStream);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	static String get(String key) {
		return properties.getProperty(key);
	}

	public static void main(String[] args) {
		System.out.println(basePath);
	}
}
