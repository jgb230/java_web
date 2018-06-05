package com.tes.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.alibaba.druid.pool.DruidPooledConnection;

public class ConnectionPool {
	private static ConnectionPool dbPoolConnection = null;
    private static DruidDataSource druidDataSourceGL = null;
    private static DruidDataSource druidDataSourceHJ = null;
    
    static {
        Properties properties = loadPropertiesFile("db_server.properties");
        try {
        	
        	String GLurl = properties.getProperty("GLurl");
        	String GLdriverClassName = properties.getProperty("GLdriverClassName");
        	String GLusername = properties.getProperty("GLusername");
        	String GLpassword = properties.getProperty("GLpassword");

            //设置连接参数
        	druidDataSourceGL=new DruidDataSource();                
        	druidDataSourceGL.setUrl(GLurl);
        	druidDataSourceGL.setDriverClassName(GLdriverClassName);
        	druidDataSourceGL.setUsername(GLusername);
        	druidDataSourceGL.setPassword(GLpassword);
            //配置初始化大小、最小、最大
        	druidDataSourceGL.setInitialSize(10);
        	druidDataSourceGL.setMinIdle(10);
        	druidDataSourceGL.setMaxActive(100);
            //连接泄漏监测
        	druidDataSourceGL.setRemoveAbandoned(true);
        	druidDataSourceGL.setRemoveAbandonedTimeout(30);
            //配置获取连接等待超时的时间
        	druidDataSourceGL.setMaxWait(20000);
            //配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        	druidDataSourceGL.setTimeBetweenEvictionRunsMillis(20000);
            //防止过期
        	druidDataSourceGL.setValidationQuery("SELECT 'x'");
        	druidDataSourceGL.setTestWhileIdle(true);
        	druidDataSourceGL.setTestOnBorrow(true);
            
        	
        	String HJurl = properties.getProperty("HJurl");
        	String HJdriverClassName = properties.getProperty("HJdriverClassName");
        	String HJusername = properties.getProperty("HJusername");
        	String HJpassword = properties.getProperty("HJpassword");
        	//设置连接参数
        	druidDataSourceHJ=new DruidDataSource();      
        	druidDataSourceHJ.setUrl(HJurl);
        	druidDataSourceHJ.setDriverClassName(HJdriverClassName);
        	druidDataSourceHJ.setUsername(HJusername);
        	druidDataSourceHJ.setPassword(HJpassword);
            //配置初始化大小、最小、最大
        	druidDataSourceHJ.setInitialSize(10);
        	druidDataSourceHJ.setMinIdle(10);
        	druidDataSourceHJ.setMaxActive(2000);
            //连接泄漏监测
        	druidDataSourceHJ.setRemoveAbandoned(true);
        	druidDataSourceHJ.setRemoveAbandonedTimeout(30);
            //配置获取连接等待超时的时间
        	druidDataSourceHJ.setMaxWait(20000);
            //配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        	druidDataSourceHJ.setTimeBetweenEvictionRunsMillis(20000);
            //防止过期
        	druidDataSourceHJ.setValidationQuery("SELECT 'x'");
        	druidDataSourceHJ.setTestWhileIdle(true);
        	druidDataSourceHJ.setTestOnBorrow(true);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 数据库连接池单例
     * @return
     */
    public static synchronized ConnectionPool getInstance(){
        if (null == dbPoolConnection){
            dbPoolConnection = new ConnectionPool();
        }
        return dbPoolConnection;
    }

    /**
     * 返回druid数据库连接
     * @return
     * @throws SQLException
     */
    public DruidPooledConnection getConnection(String db) throws SQLException{
    	DruidPooledConnection con = null;
    	try {

            if(db.equals("GL")){
                con=druidDataSourceGL.getConnection();
            }else if(db.equals("HJ")){
                con=druidDataSourceHJ.getConnection();  
            }
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return con; 
    }
    /**
     * @param string 配置文件名
     * @return Properties对象
     */
    public static Properties loadPropertiesFile(String fullFile) {
        String webRootPath = null;
        if (null == fullFile || fullFile.equals("")){
            throw new IllegalArgumentException("Properties file path can not be null" + fullFile);
        }
        webRootPath = ConnectionPool.class.getClassLoader().getResource("").getPath();
        webRootPath = new File(webRootPath).getParent();
        InputStream inputStream = null;
        Properties p =null;
        try {
            inputStream = new FileInputStream(new File(webRootPath + File.separator + fullFile));
            p = new Properties();
            p.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != inputStream){
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        return p;
    }
    
}
