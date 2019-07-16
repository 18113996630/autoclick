package com.hrong.click;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * @Author hrong
 **/
public class DriverUtil {
	private static Logger logger = Logger.getLogger("DriverUtil");
	private static SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");
	private static RemoteWebDriver driver;


	public static DriverUtil getInstance(RemoteWebDriver remoteWebDriver) {
		driver = remoteWebDriver;
		return new DriverUtil();
	}

	public WebElement findByClass(String clazz) {
		return driver.findElement(new By.ByClassName(clazz));
	}

	public WebElement findById(String id) {
		return driver.findElement(new By.ById(id));
	}

	public WebElement findByCssSelector(String selector) {
		return driver.findElement(new By.ByCssSelector(selector));
	}

	public boolean refresh(String account, int cnt) {
		closeLayer();
		try {
			//刷新按钮
			WebElement refreshBtn = findById("oneKeyRefreshPirce");
			refreshBtn.click();
			//确认刷新
			WebElement sure = findByCssSelector("#layerbox-border > div > div.layerbox-foot > a.layerbox-button-true");
			sure.click();
			Thread.sleep(500);
			//刷新成功确认
			WebElement exit = findByCssSelector("#layerbox-border > div > div.layerbox-foot > a");
			exit.click();
			ConnectionUtil.log(1, "刷新成功");
		} catch (Exception e) {
			if (e.getMessage().contains("#layerbox-border")) {
				ConnectionUtil.log(1, "检测到当日刷新已完成");
			}
			ConnectionUtil.log(0, e.getMessage());
		}
		return false;
	}

	public void validChromeIsClosed() {
		while (true) {
			try {
				driver.getCurrentUrl();
				Thread.sleep(100);
			} catch (Exception e) {
				//弹窗会导致exception
				if (!e.getMessage().contains("alert")) {
					ConnectionUtil.log(0, "检测到浏览器已关闭，即将退出程序");
					ConnectionUtil.close();
					System.exit(1);
				}
			}
		}
	}

	public void validPwd() {
		try {
			driver.switchTo().alert();
		} catch (Exception e) {
			try {
				ConnectionUtil.log(1, "登录成功");
				return;
				//防止服务器失效
			} catch (Exception e1) {
				return;
			}
		}
		ConnectionUtil.log(0, "登录失败，密码错误");
		System.exit(0);
	}

	/**
	 * 关闭弹出框
	 */
	public void closeLayer() {
		try {
			//关闭弹出框
			WebElement noTips = findByCssSelector("#noTips");
			noTips.click();
			Thread.sleep(2000);
			Alert alert = driver.switchTo().alert();
			alert.accept();
			WebElement closeBtn = findByClass("layerbox-close");
			closeBtn.click();
		} catch (Exception e) {
			if (!e.getMessage().contains("noTips")) {
				logger.info(e.getLocalizedMessage());
			}
		}
	}

	public boolean isFinish() {
		closeLayer();
		try {
			//刷新按钮
			WebElement refreshBtn = findById("oneKeyRefreshPirce");
			refreshBtn.click();
			Thread.sleep(500);
			//确认刷新按钮
			findByCssSelector("#layerbox-border > div > div.layerbox-foot > a.layerbox-button-true");
			WebElement cancel = findByCssSelector("#layerbox-border > div > div.layerbox-foot > a.layerbox-button-false");
			cancel.click();
		} catch (Exception e) {
			if (e.getMessage().contains("#layerbox-border")) {
				ConnectionUtil.log(1, "检测到当日刷新已完成:" + sdfDay.format(new Date()));
				return true;
			}
		}
		return false;
	}
}
