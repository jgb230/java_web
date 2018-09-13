package com.tes.controller;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.util.jdbc.PreparedStatementBase;

public class Dao {
	 public void insert(String sql,String db){
		 System.out.println(smartTes.getCurrentTime() + sql);
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
	
	public void execute(String sql,List<List<Object>> list,String db){
		System.out.println(smartTes.getCurrentTime() + sql);
		ConnectionPool dbp = ConnectionPool.getInstance();    //获取数据连接池单例
		DruidPooledConnection conn = null;
		PreparedStatement  statement = null;
		try {
		    conn = dbp.getConnection(db);    //从数据库连接池中获取数据库连接
		    statement = conn.prepareStatement(sql);
		    for (int i = 0; i < list.size(); i++) {
		    	for (int j = 0; j < list.get(i).size(); j++) {
		    		System.out.println(smartTes.getCurrentTime() + list.get(i).get(j).getClass().toString());
		    		if (list.get(i).get(j).getClass().toString().equals("class java.lang.String")) {
		    			statement.setString(j+1, (String) list.get(i).get(j));
		    		}else if (list.get(i).get(j).getClass().toString().equals("class java.lang.Integer")) {
		    			statement.setInt(j+1, (int) list.get(i).get(j));
		    		}else if (list.get(i).get(j).getClass().toString().equals("class java.lang.Long")) {
		    			statement.setLong(j+1, (long) list.get(i).get(j));
		    		}
		    	}
		    }
		    statement.executeUpdate();
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
		System.out.println(smartTes.getCurrentTime() + sql);
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
	        		ret += ("A:"+tmp.replaceAll("\\d+\\.", "").replaceAll(";", "")+"|");
	        	}else if (type == 1) {
	        		ret += ("Q:"+buildContent(tmp, conn)+"|");
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
		System.out.println(smartTes.getCurrentTime() + "ret:"+ret);
		return ret;
	}
	
	public String trimTxt(String tmp) {
		String ret="";
		String [] ct = tmp.split(";");
		for (int i=0; i<ct.length; i++) {
			ret += ct[i].substring(ct[i].indexOf(".")+1);
		}
		return ret;
	}

	public String buildContent(String tmp, DruidPooledConnection conn) {
		String ret = "";
		if (tmp.indexOf("|") != -1) {
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

	public String getContent(String tmp, DruidPooledConnection conn) {
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

	public String getSmsContent(String msg,String db) {
		
		String sql = String.format("select smsg_content from tb_smsmsg where smsg_uuid = '%s'", msg);
		ConnectionPool dbp = ConnectionPool.getInstance();    //获取数据连接池单例
		DruidPooledConnection conn = null;
		ResultSet resultSet = null;
		Statement statement = null;
		String ret = "";
		try {
		    conn = dbp.getConnection(db);    //从数据库连接池中获取数据库连接
		    statement = conn.createStatement();
	        resultSet = statement.executeQuery(sql);
	        if (resultSet.next()) {
	        	ret = resultSet.getString("smsg_content");
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
		System.out.println(smartTes.getCurrentTime() + "smsg_content:"+ret);
		return ret;
	}
	
	public String getSmsUser(String msg,String db) {
		
		String sql = String.format("select smsg_userid from tb_smsmsg where smsg_uuid = '%s'", msg);
		ConnectionPool dbp = ConnectionPool.getInstance();    //获取数据连接池单例
		DruidPooledConnection conn = null;
		ResultSet resultSet = null;
		Statement statement = null;
		String ret = "";
		try {
		    conn = dbp.getConnection(db);    //从数据库连接池中获取数据库连接
		    statement = conn.createStatement();
	        resultSet = statement.executeQuery(sql);
	        if (resultSet.next()) {
	        	ret = resultSet.getString("smsg_userid");
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
		System.out.println(smartTes.getCurrentTime() + "smsg_userid:"+ret);
		return ret;
	}
	
	public String getSmsGWType(String userid,String db) {
		String sql = String.format("select sgw_type from tb_smsgateway where sgw_userid = '%s'", userid);
		ConnectionPool dbp = ConnectionPool.getInstance();    //获取数据连接池单例
		DruidPooledConnection conn = null;
		ResultSet resultSet = null;
		Statement statement = null;
		String ret = "";
		try {
		    conn = dbp.getConnection(db);    //从数据库连接池中获取数据库连接
		    statement = conn.createStatement();
	        resultSet = statement.executeQuery(sql);
	        if (resultSet.next()) {
	        	ret = resultSet.getString("sgw_type");
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
		System.out.println(smartTes.getCurrentTime() + "sgw_type:"+ret);
		return ret;
	}
	
	public Map<String,String> getSmsGW(String userid,String db) {
		Map<String, String> retMap = new HashMap<String, String>();
		String sql = String.format("select sgw_appid, sgw_appkey, sgw_url, sgw_sign from tb_smsgateway where sgw_userid = '%s'", userid);
		ConnectionPool dbp = ConnectionPool.getInstance();    //获取数据连接池单例
		DruidPooledConnection conn = null;
		ResultSet resultSet = null;
		Statement statement = null;
		try {
		    conn = dbp.getConnection(db);    //从数据库连接池中获取数据库连接
		    statement = conn.createStatement();
	        resultSet = statement.executeQuery(sql);
	        if (resultSet.next()) {
	        	retMap.put("sgw_appid", resultSet.getString("sgw_appid"));
	        	retMap.put("sgw_appkey", resultSet.getString("sgw_appkey"));
	        	retMap.put("sgw_url", resultSet.getString("sgw_url"));
	        	retMap.put("sgw_sign", resultSet.getString("sgw_sign"));
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
		System.out.println(smartTes.getCurrentTime() + "retMap:"+retMap);
		return retMap;
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
