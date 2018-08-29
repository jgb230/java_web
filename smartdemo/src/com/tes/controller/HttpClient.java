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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import com.alibaba.fastjson.JSON;

public class HttpClient {

	private static final int TIMEOUT = 45000;
	public static final String ENCODING = "UTF-8";	
	
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
            String method, Map<String, String> headerParameters, String body, String encode, String type)
            throws Exception {
    	if (encode == null) {
    		encode = "UTF-8";
    	}
    	if (type == null) {
    		type = "application/x-www-form-urlencoded";
    	}
    	
    	System.out.println("url:" + url + " method:" + method + " body:" + body);
    	
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
                type + ";charset=" + encode);
        // 设置请求方法
        httpConnection.setRequestMethod(method);
        httpConnection.setDoOutput(true);
        httpConnection.setDoInput(true);
        // 写query数据流
        System.out.println("encode:" + encode);
        if (!(body == null || body.trim().equals(""))) {
            OutputStream writer = httpConnection.getOutputStream();
            try {
                writer.write(body.getBytes(encode));
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
                    		encode));
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
            Map<String, String> bodyParameters, Map<String, String> headerParameters, String encode,  String type) throws Exception {
    	String body = "";
    	if (type !=null && type.equals("application/json")) {
    		body = getJsonBody(bodyParameters, encode);
    	}else {
    		body = getRequestBody(bodyParameters, encode);
    	}
    	
    	
        return proxyHttpRequest(address, "POST", headerParameters, body, encode, type);
    }
    private static String getJsonBody(Map<String, String> bodyParameters, String encode) {
    	return JSON.toJSONString(bodyParameters);
	}

	public static String getRequestBody(Map<String, String> params, String encode) {
        return getRequestBody(params, true, encode);
    }
    public static String getRequestBody(Map<String, String> params,
            boolean urlEncode, String encode) {
        StringBuilder body = new StringBuilder();
        if (encode == null) {
        	encode = "UTF-8";
        }
        Iterator<String> iteratorHeader = params.keySet().iterator();
        while (iteratorHeader.hasNext()) {
            String key = iteratorHeader.next();
            String value = params.get(key);
            if (value == null || value.isEmpty()) {
            	continue;
            }
            if (urlEncode) {
                try {
                    body.append(key + "=" + URLEncoder.encode(value, encode)
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
            Map<String, String> headerParameters, String body, String encode, String type) throws Exception {

        return proxyHttpRequest(address + "?"
                + getRequestBody(headerParameters, encode), "GET", null, null, encode, type);
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
            Map<String, String> headerParameters, String body, String encode, String type) throws Exception {
        String result = null;
        HttpURLConnection httpConnection = null;
        if (encode == null) {
        	encode = "UTF-8";
        }
        try {
            httpConnection = createConnection(address, method,
                    headerParameters, body, encode, type);

            System.out.println("#############" + httpConnection.getContentType());
            if (httpConnection.getContentType() != null
                    && httpConnection.getContentType().indexOf("charset=") >= 0) {
            	encode = httpConnection.getContentType()
                        .substring(
                                httpConnection.getContentType().indexOf(
                                        "charset=") + 8);
            }
            result = inputStream2String(httpConnection.getInputStream(),
            		encode);
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
