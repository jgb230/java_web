package com.tes.controller;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import com.alibaba.druid.pool.DruidPooledConnection;

public class Dao {
	 public void insert(String sql,String db){
		 System.out.println(sql);
		ConnectionPool dbp = ConnectionPool.getInstance();    //获取数据连接池单例
		DruidPooledConnection conn = null;
		Statement statement = null;
		try {
		    conn = dbp.getConnection(db);    //从数据库连接池中获取数据库连接
		    statement = conn.createStatement();
		    statement.executeUpdate(sql);
	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            if (null != statement){
	            	statement.close();
	            }
	            if (null != conn){
	                conn.close();
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
	}
	 
	public void update(String sql, String db){
		insert(sql, db);
	}
	 
	public String select(String sql, String db){
		System.out.println(sql);
		ConnectionPool dbp = ConnectionPool.getInstance();    //获取数据连接池单例
		DruidPooledConnection conn = null;
		ResultSet resultSet = null;
		Statement statement = null;
		try {
		    conn = dbp.getConnection(db);    //从数据库连接池中获取数据库连接
		    statement = conn.createStatement();
	        resultSet = statement.executeQuery(sql);
	        if (resultSet.next()) {
               return resultSet.getString(1);
            }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            if (null != conn){
	                conn.close();
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
		return "";
	}
	
	public String selectAsr(String callid, String db) {
		String sql = String.format("select content,cttype from tb_call_info "
				+ "where callid='%s' order by id; ", callid);
		ConnectionPool dbp = ConnectionPool.getInstance();    //获取数据连接池单例
		DruidPooledConnection conn = null;
		ResultSet resultSet = null;
		Statement statement = null;
		String ret = "";
		String tmp = "";
		int type = 0;
		try {
		    conn = dbp.getConnection(db);    //从数据库连接池中获取数据库连接
		    statement = conn.createStatement();
	        resultSet = statement.executeQuery(sql);
	        while (resultSet.next()) {
	        	tmp = resultSet.getString("content");
//	        	System.out.println("tmp:"+tmp);
	        	type = resultSet.getInt("cttype");
	        	if (type == 0) {
	        		ret += ("A:"+tmp+"\n");
	        	}else if (type == 1) {
	        		ret += ("Q:"+buildContent(tmp, conn)+"\n");
	        	}
            }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            if (null != conn){
	                conn.close();
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
		System.out.println("ret:"+ret);
		return ret;
	}
	
	private String buildContent(String tmp, DruidPooledConnection conn) {
		String ret = "";
		if (tmp.indexOf("\\|") != -1) {
			String [] ct = tmp.split("\\|");
			for (int i=0; i<ct.length; i++) {
				if (ct[i].endsWith(".wav")) {
					ret += getContent(ct[i], conn);
				}else {
					ret += ct[i];
				}
			}
		}else {
			ret = getContent(tmp, conn);
		}
//		System.out.println("ret:"+ret);
		return ret;
	}

	private String getContent(String tmp, DruidPooledConnection conn) {
		String sql = String.format("select content from tb_ai_content where id = '%s'", tmp);
		ResultSet resultSet = null;
		Statement statement = null;
		try {
		    statement = conn.createStatement();
	        resultSet = statement.executeQuery(sql);
	        if (resultSet.next()) {
	        	return resultSet.getString("content");
            }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    } 
		return "";
	}

	public static void sqltest(call callTemp) {
//		Dao druidDao = new Dao();
//		
//		String sql = String.format("insert into tb_call_info (callid, callerid, calleeid, speaker, content, cttype, indate) "
//				+ "values(\"%s\",\"%s\",\"%s\",%d,\"%s\",%d,\"%s\")", 
//				callTemp.callid, callTemp.callerid, callTemp.calleeid, 0, "你好", 0, smartTes.getCurrentTime());
//		druidDao.insert(sql);
	}
	
	public static void selecttest() {
//		Dao druidDao = new Dao();
//		
//		String sql = String.format("select id from tb_tencent_content"
//				+ " where content='%s'", "你好");
//		System.out.println(druidDao.select(sql));
	}
}
