package com.hrong.click;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * @Author hrong
 **/
public class ConnectionUtil {

	private static Connection connection = getConnection();
	private static Logger logger = Logger.getLogger("ConnectionUtil");


	public static void saveOrUpdateConf() {
		String times = PropertyUtil.get("times").trim();
		String account = PropertyUtil.get("account").trim();
		logger.info(times);
		PreparedStatement statement = null;
		try {
			int id = queryCount("select id as cnt from conf t where t.account='" + account + "';");
			if (id == 0) {
				statement = connection.prepareStatement("INSERT INTO `autoclick`.`conf` (`account`, `times`) VALUES (?, ?);");
				statement.setString(1, PropertyUtil.get("account"));
				statement.setString(2, times);
			} else {
				statement = connection.prepareStatement("update `autoclick`.`conf` set `times`='" + times + "' where id=" + id);
			}
			statement.executeUpdate();
		} catch (Exception e) {
			return;
		}
	}

	public static void log(int success, String msg) {
		logger.info(msg);
		SimpleDateFormat sdfDetail = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement("INSERT INTO `autoclick`.`logs` (`account`, `time`, `success`, `msg`) VALUES (?, ?, ?, ?);");
			statement.setString(1, PropertyUtil.get("account"));
			statement.setString(2, sdfDetail.format(new Date()));
			statement.setInt(3, success);
			statement.setString(4, msg);
			statement.executeUpdate();
		} catch (Exception e) {
			return;
		}
	}

	public static void validLog(int success, String msg) {
		logger.info(msg);
		SimpleDateFormat sdfDetail = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement("INSERT INTO `autoclick`.`valid_log` (`account`, `valid`, `time`, `msg`) VALUES (?, ?, ?, ?);");
			statement.setString(1, PropertyUtil.get("account"));
			statement.setInt(2, success);
			statement.setString(3, sdfDetail.format(new Date()));
			statement.setString(4, msg);
			statement.executeUpdate();
		} catch (Exception e) {
			return;
		}
	}

	public static void insert(String sql) {
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String queryIdentifier(String sql) {
		String identifier = null;
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet resultSet = statement.executeQuery();
			while (resultSet.next()) {
				identifier = resultSet.getString("identifier");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return identifier;
	}

	public static int queryCount(String sql) {
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

	public static ResultSet query(String sql) {
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			return statement.executeQuery();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	public static Connection getConnection() {
		Connection connection = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://xx.xxx.xxx.xx:3306/autoclick?useUnicode=true&characterEncoding=utf-8&useSSL=false", "root", "xxxxxx");
//			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/autoclick", "root", "123456");
		} catch (Exception e) {

		}
		return connection;
	}

	public static void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
