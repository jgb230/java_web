package com.tes.controller;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.alibaba.druid.pool.DruidPooledConnection;

public class Dao {
	 public void insert(String sql){
		 ConnectionPool dbp = ConnectionPool.getInstance();    //获取数据连接池单例
	        DruidPooledConnection conn = null;
	        PreparedStatement ps = null;
	        try {
	            conn = dbp.getConnection();    //从数据库连接池中获取数据库连接
	            ps = conn.prepareStatement(sql);
	            ps.executeUpdate();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        } finally {
	            try {
	                if (null != ps){
	                    ps.close();
	                }
	                if (null != conn){
	                    conn.close();
	                }
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	    }
}
