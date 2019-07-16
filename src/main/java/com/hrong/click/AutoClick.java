package com.hrong.click;

import org.apache.commons.codec.binary.Base64;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.ErrorHandler;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.SessionId;

import javax.crypto.Cipher;
import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @Author hrong
 **/
public class AutoClick {
	private static Logger logger = Logger.getLogger("AutoClick");
	private static String account = "";
	private static String pwd = "";
	private static SimpleDateFormat sdfDetail = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");
	private static int max = 8;
	private static int interval = 35;
	private static List<String> times;
	private static ThreadPoolExecutor executor = new ThreadPoolExecutor(3,
			5,
			0L,
			TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), r -> new Thread(r, "thread-%d"));
	/**
	 * 开始刷新的整点时间
	 */
	private static int startTime = 9;

	static {
		account = PropertyUtil.get("account") == null ? account : PropertyUtil.get("account");
		pwd = PropertyUtil.get("password") == null ? pwd : PropertyUtil.get("password");
		max = PropertyUtil.get("max") == null ? max : Integer.valueOf(PropertyUtil.get("max"));
		interval = PropertyUtil.get("interval") == null ? interval : Integer.valueOf(PropertyUtil.get("interval"));
		times = Arrays.asList(PropertyUtil.get("times").trim().split("\\W+"));
		Collections.sort(times);
	}


	public static void main(String[] args) throws InterruptedException {
		for (String time : times) {
			System.out.println(getDateValue(time));
		}
//		runJob();
	}

	private static void runJob() throws InterruptedException {
		try {
			valid();

			ChromeOptions options = new ChromeOptions();
			//设置chrome及驱动地址
			options.setBinary("D:\\chrome\\Application\\chrome.exe");
			System.setProperty("webdriver.chrome.driver", "D:\\chrome\\Application\\chromedriver.exe");
			ChromeDriver driver = new ChromeDriver(options);

			//检测浏览器是否关闭
			executor.execute(() -> {
				validChromeIsClosed(driver);
			});
			driver.get("http://dealer.zol.com.cn");
			Thread.sleep(2000L);
			WebElement userName = findById(driver, "loginUsername");
			WebElement password = findById(driver, "loginPassword");
			WebElement btn = findByCssSelector(driver, "#merchantLogin > div > input");
			userName.sendKeys(account);
			password.sendKeys(pwd);
			btn.click();
			logger.info("登录成功");
			Thread.sleep(1000L);

			String today;
			while (true) {
				valid();
				logger.info("***************" + sdfDay.format(new Date()) + " 刷新任务开始***************");
				today = sdfDay.format(new Date());
				int cnt = 0;
				while (true) {
					// || canRefreshOuter(true, times.get(0)) || canRefreshOuter(false, times.get(times.size() - 1))
					if (canRefreshInner()) {
						Thread.sleep(3 * 60 * 1000);
						driver.navigate().refresh();
						closeLayer(driver);
					}
					if (times.contains(getHourMinute())) {
						refresh(driver, cnt);
						cnt++;
					}
					if (cnt == max) {
						break;
					}
				}
				logger.info("***************" + sdfDay.format(new Date()) + " 刷新任务结束***************");

				logger.info("开启页面定时刷新，刷新间隔：5分钟");
				while (sdfDay.format(new Date()).equalsIgnoreCase(today)) {
					Thread.sleep(5 * 60 * 1000);
					driver.navigate().refresh();
					closeLayer(driver);
				}
			}

		} catch (
				Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "请联系软件提供方[QQ:1011486768]", "error", JOptionPane.ERROR_MESSAGE);
		}

	}

	private static void refresh(RemoteWebDriver driver, int cnt) {
		closeLayer(driver);
		try {
			//刷新按钮
			WebElement refreshBtn = findById(driver, "oneKeyRefreshPirce");
			refreshBtn.click();

			//确认刷新
			WebElement sure = findByCssSelector(driver, "#layerbox-border > div > div.layerbox-foot > a.layerbox-button-true");
			sure.click();
			Thread.sleep(500);
			//刷新成功确认
			WebElement exit = findByCssSelector(driver, "#layerbox-border > div > div.layerbox-foot > a");
			exit.click();
			logger.info("-----------完成第" + cnt + "次刷新-----------当前时间：" + sdfDetail.format(new Date()));
		} catch (Exception e) {
			if (e.getMessage().contains("#layerbox-border")) {
				logger.info("检测到当日刷新已完成:" + sdfDay.format(new Date()));
//				isFinish = true;
			}
		}
	}

	private static void validChromeIsClosed(RemoteWebDriver driver) {
		while (true) {
			try {
				driver.getCurrentUrl();
				Thread.sleep(100);
			} catch (Exception e) {
				//弹窗会导致exception
				if (!e.getMessage().contains("alert")) {
					logger.info("检测到浏览器已关闭，即将退出程序");
					System.exit(1);
				}
			}
		}
	}

	private static void print(RemoteWebDriver driver, String msg) {
		String windowHandle = driver.getWindowHandle();
		ErrorHandler errorHandler = driver.getErrorHandler();
		String title = driver.getTitle();
		String currentUrl = driver.getCurrentUrl();
		SessionId sessionId = driver.getSessionId();
		System.out.println("********************" + msg + "***************************");
		System.out.println(windowHandle);
		System.out.println(errorHandler);
		System.out.println(title);
		System.out.println(currentUrl);
		System.out.println(sessionId);
		System.out.println("***********************************************************");
	}

	/**
	 * 关闭弹出框
	 *
	 * @param driver driver
	 */
	private static void closeLayer(RemoteWebDriver driver) {
		try {
			//关闭弹出框
			WebElement noTips = findByCssSelector(driver, "#noTips");
			noTips.click();
			Thread.sleep(2000);
			Alert alert = driver.switchTo().alert();
			alert.accept();
			WebElement closeBtn = findByClass(driver, "layerbox-close");
			closeBtn.click();
		} catch (Exception e) {
			if (!e.getMessage().contains("noTips")) {
				logger.info(e.getLocalizedMessage());
			}
		}
	}

	/**
	 * 验证软件是否过期
	 */
	private static void valid() {
		Connection connection = getConnection();
		if (connection != null) {
			try {
				int exists = queryCount(connection, "select count(1) as cnt from user t where t.identifier='" + getIdentifierByWindows() + "'");
				int cnt = queryCount(connection, "select cnt from count");
				int cntUser = queryCount(connection, "select count(1) as cnt from user");
				//确保拷贝软件后无法使用
				//code存在
				if (exists == 1 && cnt == cntUser) {
					ResultSet resultSet = query(connection, "select t.expire as expire, t.valid as valid from user t where t.identifier='" + getIdentifierByWindows() + "'");
					int expire = 0;
					int valid = 0;
					while (resultSet.next()) {
						expire = resultSet.getInt("expire");
						valid = resultSet.getInt("valid");
					}
					if (valid == 0) {
						JOptionPane.showMessageDialog(null, "请联系[QQ:1011486768]解锁", "软件被限制", JOptionPane.ERROR_MESSAGE);
						System.exit(0);
					} else {
						if (expire != 20000101) {
							//是否过期
							if (Integer.parseInt(sdfDay.format(new Date()).replace("-", "")) > expire) {
								JOptionPane.showMessageDialog(null, "请联系软件提供方[QQ:1011486768]", "软件已过期", JOptionPane.ERROR_MESSAGE);
								System.exit(0);
							}
						}
					}
					logger.info(sdfDay.format(new Date()) + " 通过校验");
					return;
					//code不存在且不允许新增
				} else if (exists == 0 && cnt - cntUser == 0) {
					JOptionPane.showMessageDialog(null, "请联系[QQ:1011486768]购买软件", "无购买记录", JOptionPane.ERROR_MESSAGE);
					System.exit(0);
					//code不存在且允许新增
				} else if (exists == 0 && cnt - cntUser != 0) {
					insert(connection, "insert into user(account, identifier) values('" + account + "','" + getIdentifierByWindows() + "')");
					logger.info("首次使用:" + account + "	" + getIdentifierByWindows());
				}
			} catch (SQLException e) {
				e.printStackTrace();
				logger.info(sdfDay.format(new Date()) + " 验证过程发生错误，略过此次验证");
			} finally {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		logger.info(sdfDay.format(new Date()) + " 略过此次验证");
	}

	private static void insert(Connection connection, String sql) {
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int queryCount(Connection connection, String sql) {
		int cnt = 0;
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next()) {
				cnt = resultSet.getInt("cnt");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cnt;
	}

	private static ResultSet query(Connection connection, String sql) {
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			return statement.executeQuery();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private static String decrypt(String str, String privateKey) {
		String outStr = null;
		try {
			byte[] inputByte = Base64.decodeBase64(str.getBytes("UTF-8"));
			byte[] decoded = Base64.decodeBase64(privateKey);
			RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, priKey);
			outStr = new String(cipher.doFinal(inputByte));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return outStr;
	}

	private static WebElement findByClass(RemoteWebDriver driver, String clazz) {
		return driver.findElement(new By.ByClassName(clazz));
	}

	private static WebElement findById(RemoteWebDriver driver, String id) {
		return driver.findElement(new By.ById(id));
	}

	private static WebElement findByCssSelector(RemoteWebDriver driver, String selector) {
		return driver.findElement(new By.ByCssSelector(selector));
	}

	private static Connection getConnection() {
		Connection connection = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://xx.xxx.xxx.xx:3306/autoclick", "root", "");
		} catch (Exception e) {

		}
		return connection;
	}

	private static String getIdentifierByWindows() {
		String result = "";
		try {
			Process process = Runtime.getRuntime().exec("cmd /c dir C:");
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"));

			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("卷的序列号")) {
					result = line.substring(line.indexOf("卷的序列号是 ") + "卷的序列号是 ".length());
					break;
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	private static String getHourMinute() {
		String date = sdfDetail.format(new Date()).replace("-", "").replace(":", "").replace(" ", "");
		System.out.println(date);
		return date.substring(8, 12);
	}

	private static boolean canRefreshInner() {
		int now = getDateValue(getHourMinute());
		for (String time : times) {
			int value = getDateValue(time);
			if (Math.abs(now - value) <= 3) {
				return false;
			}
		}
		return true;
	}

	private static boolean canRefreshOuter(boolean before, String time) {
		int now = getDateValue(getHourMinute());
		int param = getDateValue(time);
		//当前时间早于给定时间，给定时间-当前时间的差值大于4即可以刷新
		if (before) {
			return param - now >= 4;
		}
		//当前时间晚于给定时间，当前时间-给定时间的差值大于4即可以刷新
		return now - param >= 4;
	}

	private static int getDateValue(String value) {
		if ((int) value.charAt(0) == 0) {
			if ((int) value.charAt(1) == 0) {
				if ((int) value.charAt(2) == 0) {
					return Integer.parseInt(value.substring(3));
				}
				return Integer.parseInt(value.substring(2));
			}
			return Integer.parseInt(value.substring(1));
		}
		return Integer.parseInt(value);
	}
}
