package com.tes.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import com.alibaba.fastjson.JSONObject;


public class tencentAI {
	private static String url = "https://api.ai.qq.com/fcgi-bin/aai/aai_tts";
	private static String appId = "1106844169";
	private static String appKey = "MbR3PDlBdAv3wH9d";
    public static final String ENCODING = "UTF-8";
    
    tencentAI()
	{
		
	}
	
	public String tts(String ch, String filePath, String speed, String aht, String apc) throws Exception
	{
		Random rand = new Random();
		rand.setSeed((new Date()).getTime());
		long nonce = rand.nextLong();
		long time = (new Date()).getTime();
		String fileName = filePath + "_" + time + ".wav";
		
		Map<String, String> params = new TreeMap<String, String>();
        params.put("app_id", appId);
        params.put("speaker", "5");
        params.put("format", "2");
        params.put("volume", "7");
        params.put("speed", speed);
        params.put("text", ch);
        params.put("aht", aht);
        params.put("apc", apc);
        params.put("time_stamp", Long.toString(time/1000));
        params.put("nonce_str", Long.toString(nonce));
        params.put("sign", "");

        params.replace("sign", getReqSign(params, appKey));
        System.out.println(params);
        String res = HttpClient.post(url, params, null, "UTF-8");
        System.out.println("fileName" + fileName);
        writeFile(res, fileName);
		return fileName;
	}
	
	
	public boolean writeFile(String res, String fileName)
	{
		JSONObject jsonres = JSONObject.parseObject(res);
        JSONObject jsondata = jsonres.getJSONObject("data");
        String speech = (String) jsondata.get("speech");

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] encodedText = decoder.decode(speech);
        File file = new File(fileName);
        if(!file.exists()){
            //先得到文件的上级目录，并创建上级目录，在创建文件
            file.getParentFile().mkdir();
            try {
                //创建文件
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileOutputStream outSTr;
		try {
			outSTr = new FileOutputStream(file);
	        BufferedOutputStream Buff = new BufferedOutputStream(outSTr);
	        Buff.write(encodedText);
	        Buff.flush();
	        Buff.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
        return true;
	}
	
	public  String getReqSign(Map<String, String> params, String appKey) 
			throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		Map<String, String> sortMap = new TreeMap<String, String>(new MapKeyComparator());
        sortMap.putAll(params);
        
        String urlencode=HttpClient.getRequestBody(sortMap, "UTF-8") + "&" + "app_key=" + appKey;
        System.out.println(urlencode);
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(urlencode.getBytes("utf-8"));
        
        return toHex(bytes);
	}
	
	public static String toHex(byte[] bytes) {

	    final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
	    StringBuilder ret = new StringBuilder(bytes.length * 2);
	    for (int i=0; i<bytes.length; i++) {
	        ret.append(HEX_DIGITS[(bytes[i] >> 4) & 0x0f]);
	        ret.append(HEX_DIGITS[bytes[i] & 0x0f]);
	    }
	    return ret.toString();
	}
	
	class MapKeyComparator implements Comparator<String>{

	    @Override
	    public int compare(String str1, String str2) {
	        
	        return str1.compareTo(str2);
	   }
	}
	
}
