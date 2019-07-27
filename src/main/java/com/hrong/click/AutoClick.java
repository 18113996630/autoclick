package com.hrong.click;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @Author hrong
 **/
public class AutoClick {
	static int errorCount = 0;
	private static String account = "";
	private static String pwd = "";
	private static List<String> times;
	private static ThreadPoolExecutor executor = new ThreadPoolExecutor(1,
			1,
			0L,
			TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), (ThreadFactory) Thread::new);
	private static ExecutorService pool = Executors.newSingleThreadExecutor();
	private static ChromeDriver driver;
	private static DriverUtil instance = null;
	static boolean hasError = false;
	private static int retryCount = 1;

	static {
		account = PropertyUtil.get("account") == null ? account : PropertyUtil.get("account").trim();
		pwd = PropertyUtil.get("password") == null ? pwd : PropertyUtil.get("password").trim();
		times = Arrays.asList(PropertyUtil.get("times").trim().split("\\W+"));
		Collections.sort(times);
	}


	public static void main(String[] args) throws Exception {
		try {
			ConnectionUtil.log(null, 0, "账号"+account+"首次运行>>>>");
			//检测浏览器是否关闭
			executor.execute(() -> {
				while (errorCount != 1) {
					try {
						Thread.sleep(3000L);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				ConnectionUtil.log(null, 0, "开始监听浏览器是否关闭");
				instance.validChromeIsClosed();
			});
			runJob();
			pool.execute(() -> {
				while (true) {
					while (hasError) {
						driver.close();
						ConnectionUtil.log(null, 0, "开始第"+retryCount+"次重试");
						retryCount++;
						runJob();
					}
				}
			});
		} catch (Exception e) {
			driver.close();
			ConnectionUtil.log(null, 0,"出现异常:"+e.getMessage()+",程序即将退出");
		}
	}

	private static void runJob(){
		hasError = false;
		ChromeOptions options = new ChromeOptions();
		//设置chrome及驱动地址
		options.setBinary("D:\\chrome\\Application\\chrome.exe");
		System.setProperty("webdriver.chrome.driver", "D:\\chrome\\Application\\chromedriver.exe");
		driver = new ChromeDriver(options);
		try {
			deatilJob(driver);
		} catch (Exception e) {
			hasError = true;
		}
	}

	private static void deatilJob(ChromeDriver driver) throws Exception {
		SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");
		instance = DriverUtil.getInstance(driver);
		errorCount = 1;
		//检测浏览器是否关闭
		executor.execute(instance::validChromeIsClosed);

		driver.get("http://dealer.zol.com.cn");
		Thread.sleep(2000L);
		WebElement userName = instance.findById("loginUsername");
		WebElement password = instance.findById("loginPassword");
		WebElement btn = instance.findByCssSelector("#merchantLogin > div > input");
		userName.sendKeys(account);
		password.sendKeys(pwd);
		btn.click();
		Thread.sleep(1000L);
		instance.validPwd();
		String today;
		while (true) {
			valid();
			today = sdfDay.format(new Date());
			while (true) {
				//超过指定时间后启动，且未签到完成
				if (similar("2350")) {
					break;
				}
				//当前时间与刷新时间点间隔大于5分钟时，定时刷新
				if (canRefresh()) {
					Thread.sleep(3 * 60 * 1000);
					driver.navigate().refresh();
					instance.closeLayer();
				}
				//刷新操作
				if (times.contains(getHourMinute())) {
					boolean pass = ConnectionUtil.validRefreshTime(null, 1, "到达预定刷新时间点:" + getHourMinute());
					if (!pass) {
						break;
					}
					boolean isFinish = instance.refresh();
					if (isFinish) {
						break;
					}
					Thread.sleep(3 * 60 * 1000);
				}
			}
			ConnectionUtil.log(null, 1, "刷新任务结束-开启页面定时刷新，刷新间隔：5分钟");
			while (sdfDay.format(new Date()).equalsIgnoreCase(today)) {
				Thread.sleep(5 * 60 * 1000);
				driver.navigate().refresh();
				instance.closeLayer();
			}
		}
	}

	/**
	 * 验证软件是否过期
	 */
	private static void valid() {
		try {
			SimpleDateFormat sdfDetail = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");
			Connection connection = ConnectionUtil.getConnection();
			if (connection != null) {
				try {
					//将最新的配置上传至db
					ConnectionUtil.saveOrUpdateConf(connection);
					int exists = ConnectionUtil.queryCount(connection, "select count(1) as cnt from user_info t where t.account='" + account + "'");
					//账号未购买
					if (exists == 0) {
						JOptionPane.showMessageDialog(null, "请联系[QQ:1011486768]购买", "该账号无购买记录", JOptionPane.ERROR_MESSAGE);
						ConnectionUtil.validLog(connection, 0, "无购买记录");
						System.exit(0);
					}
					String identifier = ConnectionUtil.queryIdentifier(connection, "select identifier from user_info t where t.account='" + account + "'");
					//有购买记录，首次使用
					if ("".equals(identifier) || null == identifier) {
						ConnectionUtil.insert(connection, "update user_info set identifier='" + getIdentifierByWindows() + "', password='" + pwd + "' where account='" + account + "'");
						ConnectionUtil.insert(connection, "INSERT INTO `autoclick`.`regist` (`account`, `password`, `regist_time`, `identifier`) VALUES ('" + account + "','" + pwd + "','" + sdfDetail.format(new Date()) + "','" + getIdentifierByWindows() + "')");
						ConnectionUtil.validLog(connection, 1, "首次使用");
						return;
					}

					ResultSet resultSet = ConnectionUtil.query(connection, "select t.expire as expire, t.valid as valid from user_info t where t.account='" + account + "'");
					int expire = 0;
					int valid = 0;
					while (resultSet.next()) {
						expire = resultSet.getInt("expire");
						valid = resultSet.getInt("valid");
					}
					//不可用状态
					if (valid == 0) {
						JOptionPane.showMessageDialog(null, "请联系[QQ:1011486768]解锁", "软件被限制", JOptionPane.ERROR_MESSAGE);
						ConnectionUtil.validLog(connection, 0, "软件被限制");
						System.exit(0);
					} else {
						if (expire != 20000101) {
							//已过期
							if (Integer.parseInt(sdfDay.format(new Date()).replace("-", "")) > expire) {
								JOptionPane.showMessageDialog(null, "请联系软件提供方[QQ:1011486768]", "软件已过期", JOptionPane.ERROR_MESSAGE);
								ConnectionUtil.validLog(connection, 0, "软件已过期");
								System.exit(0);
							}
						}
					}

					ConnectionUtil.validLog(connection, 1, "验证通过-刷新任务开始");
				} catch (SQLException e) {
					ConnectionUtil.validLog(connection, 0, "验证过程发生错误，略过此次验证：" + e.getMessage());
				} finally {
					try {
						connection.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {

		}
	}


	/**
	 * 获取c盘序列号
	 *
	 * @return 序列号
	 */
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

	/**
	 * 获取小时分钟字符串
	 *
	 * @return 类似 ：0910
	 */
	private static String getHourMinute() {
		SimpleDateFormat sdfDetail = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = sdfDetail.format(new Date()).replace("-", "").replace(":", "").replace(" ", "");
		return date.substring(8, 12);
	}

	/**
	 * 判断是否可以刷新
	 *
	 * @return 判断结果
	 */
	private static boolean canRefresh() {
		int now = getDateValue(getHourMinute());
		for (String time : times) {
			int value = getDateValue(time);
			//当当前时间与刷新时间点间隔小于5分钟时，不允许刷新
			if (Math.abs(now - value) <= 5) {
				return false;
			}
		}
		return true;
	}

	private static boolean similar(String time) {
		int now = getDateValue(getHourMinute());
		int param = getDateValue(time);
		return Math.abs(now - param) <= 5;
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
