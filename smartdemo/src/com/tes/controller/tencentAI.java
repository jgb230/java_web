package com.tes.controller;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.alibaba.fastjson.JSONObject;

public class tencentAI {
	private static String url = "https://api.ai.qq.com/fcgi-bin/aai/aai_tts";
	private static String appId = "1106844169";
	private static String appKey = "MbR3PDlBdAv3wH9d";
	private static final int TIMEOUT = 45000;
    public static final String ENCODING = "UTF-8";
    
	tencentAI()
	{
		
	}
	
	public String tts(String ch, String filePath) throws Exception
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
        params.put("speed", "100");
        params.put("text", ch);
        params.put("aht", "6");
        params.put("apc", "55");
        params.put("time_stamp", Long.toString(time/1000));
        params.put("nonce_str", Long.toString(nonce));
        params.put("sign", "");

        params.replace("sign", getReqSign(params, appKey));
        System.out.println(params);
        String res = post(url, params, null);
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
	
	public String getReqSign(Map<String, String> params, String appKey) 
			throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		Map<String, String> sortMap = new TreeMap<String, String>(new MapKeyComparator());
        sortMap.putAll(params);
        
        String urlencode=getRequestBody(sortMap) + "&" + "app_key=" + appKey;
        System.out.println(urlencode);
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] bytes = md.digest(urlencode.getBytes("utf-8"));
        
        return toHex(bytes);
	}
	
	private static String toHex(byte[] bytes) {

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
    /**
     * 创建HTTP连接
     * 
     * @param url
     *            地址
     * @param method
     *            方法
     * @param headerParameters
     *            头信息
     * @param body
     *            请求内容
     * @return
     * @throws Exception
     */
    private static HttpURLConnection createConnection(String url,
            String method, Map<String, String> headerParameters, String body)
            throws Exception {

        URL Url = new URL(url);
        trustAllHttpsCertificates();
        HttpURLConnection httpConnection = (HttpURLConnection) Url.openConnection();
        // 设置请求时间
        httpConnection.setConnectTimeout(TIMEOUT);
        // 设置 header
        if (headerParameters != null) {
            Iterator<String> iteratorHeader = headerParameters.keySet().iterator();
            while (iteratorHeader.hasNext()) {
                String key = iteratorHeader.next();
                httpConnection.setRequestProperty(key,
                        headerParameters.get(key));
            }
        }
        httpConnection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded;charset=" + ENCODING);

        // 设置请求方法
        httpConnection.setRequestMethod(method);
        httpConnection.setDoOutput(true);
        httpConnection.setDoInput(true);
        // 写query数据流
        if (!(body == null || body.trim().equals(""))) {
            OutputStream writer = httpConnection.getOutputStream();
            try {
                writer.write(body.getBytes(ENCODING));
            } finally {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            }
        }

        // 请求结果
        int responseCode = httpConnection.getResponseCode();
        if (responseCode != 200) {
            throw new Exception(responseCode
                    + ":"
                    + inputStream2String(httpConnection.getErrorStream(),
                            ENCODING));
        }

        return httpConnection;
    }

    /**
     * POST请求
     * @param address 请求地址
     * @param headerParameters 参数
     * @param body
     * @return
     * @throws Exception
     */
    public static String post(String address,
            Map<String, String> headerParameters, String body) throws Exception {

        return proxyHttpRequest(address, "POST", null,
                getRequestBody(headerParameters));
    }
    public static String getRequestBody(Map<String, String> params) {
        return getRequestBody(params, true);
    }
    public static String getRequestBody(Map<String, String> params,
            boolean urlEncode) {
        StringBuilder body = new StringBuilder();

        Iterator<String> iteratorHeader = params.keySet().iterator();
        while (iteratorHeader.hasNext()) {
            String key = iteratorHeader.next();
            String value = params.get(key);
            if (value.isEmpty()) {
            	continue;
            }
            if (urlEncode) {
                try {
                    body.append(key + "=" + URLEncoder.encode(value, ENCODING)
                            + "&");
                } catch (UnsupportedEncodingException e) {
                    // e.printStackTrace();
                }
            } else {
                body.append(key + "=" + value + "&");
            }
        }

        if (body.length() == 0) {
            return "";
        }
        return body.substring(0, body.length() - 1);
    }
    /**
     * GET请求
     * @param address
     * @param headerParameters
     * @param body
     * @return
     * @throws Exception
     */
    public static String get(String address,
            Map<String, String> headerParameters, String body) throws Exception {

        return proxyHttpRequest(address + "?"
                + getRequestBody(headerParameters), "GET", null, null);
    }

    /**
     * 设置 https 请求
     * @throws Exception
     */
    private static void trustAllHttpsCertificates() throws Exception {
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String str, SSLSession session) {
                return true;
            }
        });
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext
                .getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc
                .getSocketFactory());
        
    }
    static class miTM implements javax.net.ssl.TrustManager,javax.net.ssl.X509TrustManager {

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public boolean isServerTrusted(
                java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public boolean isClientTrusted(
                java.security.cert.X509Certificate[] certs) {
            return true;
        }

        public void checkServerTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }

        public void checkClientTrusted(
                java.security.cert.X509Certificate[] certs, String authType)
                throws java.security.cert.CertificateException {
            return;
        }


    }
    private static String inputStream2String(InputStream input, String encoding)
            throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input,
                encoding));
        StringBuilder result = new StringBuilder();
        String temp = null;
        while ((temp = reader.readLine()) != null) {
            result.append(temp);
        }

        return result.toString();

    }
    
    public static String proxyHttpRequest(String address, String method,
            Map<String, String> headerParameters, String body) throws Exception {
        String result = null;
        HttpURLConnection httpConnection = null;

        try {
            httpConnection = createConnection(address, method,
                    headerParameters, body);

            String encoding = "UTF-8";
            if (httpConnection.getContentType() != null
                    && httpConnection.getContentType().indexOf("charset=") >= 0) {
                encoding = httpConnection.getContentType()
                        .substring(
                                httpConnection.getContentType().indexOf(
                                        "charset=") + 8);
            }
            result = inputStream2String(httpConnection.getInputStream(),
                    encoding);
            // logger.info("HTTPproxy response: {},{}", address,
            // result.toString());

        } catch (Exception e) {
            // logger.info("HTTPproxy error: {}", e.getMessage());
            throw e;
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
        return result;
    }
}
