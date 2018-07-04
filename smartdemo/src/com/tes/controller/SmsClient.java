package com.tes.controller;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONException;

import com.aliyuncs.exceptions.ClientException;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.httpclient.HTTPException;

public class SmsClient {

	private static String smurl = "http://114.55.25.138/msg/HttpBatchSendSM";
	private static String smAccount = "bmxm2018";
	private static String smPwd = "Bmxm2018@";
	

	public static int sendSms(String phone, String msg) throws Exception
	{
		Dao druidDao = new Dao();
		String content = druidDao.getSmsContent(msg, "HJ");
		String userid = druidDao.getSmsUser(msg, "HJ");
		String type = druidDao.getSmsGWType(userid, "HJ").toUpperCase();
		if (type.equals("BATCH")) {
			return sendSmsBatch(phone, content, userid);
		}else if (type.equals("SMSSERVICE")) {
			return sendSmsService(phone, content, userid);
		}
		return sendSmsBatch(phone, content, userid);
		
	}
	
	private static int sendSmsService(String phone, String msg, String userid) throws Exception {
		Dao druidDao = new Dao();
	    Map<String,String> map = druidDao.getSmsGW(userid, "HJ");
	    String account = map.get("sgw_appid");
	    String appkey = map.get("sgw_appkey");
	    String url = map.get("sgw_url");
	    
		Map<String, String> params = new TreeMap<String, String>();
        params.put("UserId", account);
        params.put("Password", appkey);
        params.put("Mobiles", phone);
        params.put("Content", msg);
        
        String res = HttpClient.get(url, params, null, "GBK");
        int position = res.indexOf("Status=");
        int position2 = res.indexOf("&Description=");
        String ret = res.substring(position + 7, position2);
        System.out.println("res:" + res + " ret:" + ret);
        if (ret.toUpperCase().equals("SUCC")) {
        	return 0;
        }else {
        	int position3 = res.indexOf("&GatewayId=");
        	String err = res.substring(position2 + 13, position3);
        	System.out.println( err);
        	return -1;
        }
	}

	public static int sendSmsBatch(String phone, String msg, String userid) throws Exception
	{
		Dao druidDao = new Dao();
	    Map<String,String> map = druidDao.getSmsGW(userid, "HJ");
	    String account = map.get("sgw_appid");
	    String appkey = map.get("sgw_appkey");
	    String url = map.get("sgw_url");
	    
		Map<String, String> params = new TreeMap<String, String>();
        params.put("account", account);
        params.put("pswd", appkey);
        params.put("mobile", phone);
        params.put("msg", msg);
        params.put("needstatus", "true");
        
        String res = HttpClient.post(url, params, null, "UTF-8");
        int position = res.indexOf(",");
        String ret = res.substring(position + 1, position + 2);
        System.out.println("res:" + res + " ret:" + ret);
        if (ret.equals("0")) {
        	return 0;
        }else {
        	String err = res.substring(position + 1, position + 4);
        	return Integer.parseInt(err);
        }
	}
	
	public static int sendsmsTx(String phone, String msg, String userid) throws JSONException, HTTPException, IOException
	{

	    SmsSingleSender ssender = new SmsSingleSender(1400105326, "d61a7a002ca05d0d6a37f4528baae58a");
	    SmsSingleSenderResult result = ssender.send(0, "86", phone,
	        msg, "", "");
	    System.out.print(new String(result.toString().getBytes("utf-8")));
	    return 1;
	}
	
	public static int sendsmsAli(String phone, String msg, String userid) 
	{
		try {
			Alisms.Alisend(phone, msg);
		} catch (ClientException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return 1;
	}
	
}
