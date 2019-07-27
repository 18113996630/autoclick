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

//	private static Connection connection = getConnection();
	private static Logger logger = Logger.getLogger("ConnectionUtil");


	public static void saveOrUpdateConf(Connection connection) {
		
		String times = PropertyUtil.get("times").trim();
		String account = PropertyUtil.get("account").trim();
		logger.info(times);
		PreparedStatement statement = null;
		try {
			int id = queryCount(connection,"select id as cnt from conf t where t.account='" + account + "';");
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

	public static void main(String[] args) {
		boolean refreshTime = validRefreshTime(null, 1, "测试测试测试");
		System.out.println(refreshTime);
	}

	public static boolean validRefreshTime(Connection connection, int success, String msg) {
		SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");
		if(connection == null){
			connection = getConnection();
		}
		try {
			int max = queryCount(connection, "select max as cnt from `autoclick`.`user_info` t where t.account='" + PropertyUtil.get("account") + "'");
			int cnt = queryCount(connection, "SELECT count( 1 ) AS cnt FROM `autoclick`.`logs` t WHERE t.account = '"+PropertyUtil.get("account")+"' AND LEFT (time, 10) = '"+sdfDay.format(new Date())+"' AND t.msg = '刷新成功'");
			log(connection, success, msg);
			if (cnt >= max) {
				log(connection, 999, "已到达最大刷新次数");
				return false;
			}
			return true;
		} catch (Exception e) {
			return true;
		}
	}


	public static void log(Connection connection, int success, String msg) {
		logger.info(msg);
		SimpleDateFormat sdfDetail = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		PreparedStatement statement = null;
		boolean useOnce = false;
		try {
			if(connection == null){
				connection = getConnection();
				useOnce = true;
			}
			statement = connection.prepareStatement("INSERT INTO `autoclick`.`logs` (`account`, `time`, `success`, `msg`) VALUES (?, ?, ?, ?);");
			statement.setString(1, PropertyUtil.get("account"));
			statement.setString(2, sdfDetail.format(new Date()));
			statement.setInt(3, success);
			statement.setString(4, msg);
			statement.executeUpdate();
		} catch (Exception e) {
			return;
		} finally {
			if(useOnce){
				close(connection);
			}
		}
	}

	public static void validLog(Connection connection, int success, String msg) {
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

	public static void insert(Connection connection, String sql) {
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String queryIdentifier(Connection connection, String sql) {
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

	public static int queryCount(Connection connection, String sql) {
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

	public static ResultSet query(Connection connection, String sql) {
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
			connection = DriverManager.getConnection("jdbc:mysql://39.106.190.74:3306/autoclick?useUnicode=true&characterEncoding=utf-8&useSSL=false", "root", "aHVhbmdyb25n");
//			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/autoclick", "root", "123456");
		} catch (Exception e) {

		}
		return connection;
	}

	public static void close(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
