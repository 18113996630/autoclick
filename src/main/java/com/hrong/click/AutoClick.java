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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @Author hrong
 **/
public class AutoClick {
	private static Logger logger = Logger.getLogger("AutoClick");
	private static String account = "yinkang";
	private static String pwd = "xbit2018QCKJ";
	private static List<String> times;
	private static ThreadPoolExecutor executor = new ThreadPoolExecutor(3,
			5,
			0L,
			TimeUnit.SECONDS, new LinkedBlockingQueue<>(10), r -> new Thread(r, "thread-%d"));
	/**
	 * 最大刷新次数
	 */
	private static int max;

	static {
		account = PropertyUtil.get("account") == null ? account : PropertyUtil.get("account");
		pwd = PropertyUtil.get("password") == null ? pwd : PropertyUtil.get("password");
		times = Arrays.asList(PropertyUtil.get("times").trim().split("\\W+"));
		Collections.sort(times);
		max = times.size();
	}


	public static void main(String[] args) throws InterruptedException {
		runJob();
	}

	private static void runJob() throws InterruptedException {
		SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");
		try {
			//将最新的配置上传至db
			ConnectionUtil.saveOrUpdateConf();

			ChromeOptions options = new ChromeOptions();
			//设置chrome及驱动地址
			options.setBinary("D:\\chrome\\Application\\chrome.exe");
			System.setProperty("webdriver.chrome.driver", "D:\\chrome\\Application\\chromedriver.exe");
			ChromeDriver driver = new ChromeDriver(options);

			DriverUtil instance = DriverUtil.getInstance(driver);
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
				ConnectionUtil.log(1, "刷新任务开始");
				today = sdfDay.format(new Date());
				int cnt = 0;
				boolean isFinish = false;
				while (true) {
					//当天刷新任务是否完成
					if (isFinish) {
						break;
					}
					//当前时间与刷新时间点间隔小于5分钟时，不允许刷新
					if (canRefresh()) {
						Thread.sleep(2 * 60 * 1000);
						driver.navigate().refresh();
						instance.closeLayer();
					}
					//刷新操作
					if (times.contains(getHourMinute())) {
						ConnectionUtil.log(1, "到达刷新时间点:"+getHourMinute());
						instance.refresh(account, cnt);
						cnt++;
						isFinish = instance.isFinish();
						Thread.sleep(3 * 60 * 1000);
					}
				}
				ConnectionUtil.log(1, "刷新任务结束");
				ConnectionUtil.log(1, "开启页面定时刷新，刷新间隔：5分钟");
				while (sdfDay.format(new Date()).equalsIgnoreCase(today)) {
					Thread.sleep(5 * 60 * 1000);
					driver.navigate().refresh();
					instance.closeLayer();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "请联系软件提供方[QQ:1011486768]", "出现问题", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * 验证软件是否过期
	 */
	private static void valid() {
		SimpleDateFormat sdfDetail = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");
		Connection connection = ConnectionUtil.getConnection();
		if (connection != null) {
			try {
				int exists = ConnectionUtil.queryCount("select count(1) as cnt from user_info t where t.account='" + account + "'");
				//账号未购买
				if (exists == 0) {
					JOptionPane.showMessageDialog(null, "请联系[QQ:1011486768]购买", "该账号无购买记录", JOptionPane.ERROR_MESSAGE);
					ConnectionUtil.validLog(0, "无购买记录");
					System.exit(0);
				}
				String identifier = ConnectionUtil.queryIdentifier("select identifier from user_info t where t.account='" + account + "'");
				//有购买记录，首次使用
				if ("".equals(identifier) || null == identifier) {
					ConnectionUtil.insert("update user_info set identifier='" + getIdentifierByWindows() + "', password='" + pwd + "' where account='" + account + "'");
					ConnectionUtil.insert("INSERT INTO `autoclick`.`regist` (`account`, `password`, `regist_time`, `identifier`) VALUES ('" + account + "','" + pwd + "','" + sdfDetail.format(new Date()) + "','" + getIdentifierByWindows() + "')");
					ConnectionUtil.validLog(1, "首次使用");
					return;
				}

				ResultSet resultSet = ConnectionUtil.query("select t.expire as expire, t.valid as valid from user_info t where t.account='" + account + "'");
				int expire = 0;
				int valid = 0;
				while (resultSet.next()) {
					expire = resultSet.getInt("expire");
					valid = resultSet.getInt("valid");
				}
				//不可用状态
				if (valid == 0) {
					JOptionPane.showMessageDialog(null, "请联系[QQ:1011486768]解锁", "软件被限制", JOptionPane.ERROR_MESSAGE);
					ConnectionUtil.validLog(0, "软件被限制");
					System.exit(0);
				} else {
					if (expire != 20000101) {
						//已过期
						if (Integer.parseInt(sdfDay.format(new Date()).replace("-", "")) > expire) {
							JOptionPane.showMessageDialog(null, "请联系软件提供方[QQ:1011486768]", "软件已过期", JOptionPane.ERROR_MESSAGE);
							ConnectionUtil.validLog(0, "软件已过期");
							System.exit(0);
						}
					}
				}

				ConnectionUtil.validLog(1, "验证通过");
			} catch (SQLException e) {
				ConnectionUtil.validLog(0, "验证过程发生错误，略过此次验证：" + e.getMessage());
			} finally {
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
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
