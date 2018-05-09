package com.tes.controller;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

@Controller
public class smartTes {
	final static String RESUME = "resume";
	final static String ASRSTART = "asrstart";
	final static String INVALID = "invalid";
	final static int INVALIDTIME = 2500;
	final static int ASRTIME = 800;
	final static int OUTIME = 1000;
	final static int LONGOUT = 10 * 1000 - OUTIME;
	
	private static final Logger log = Logger.getLogger(smartTes.class);
	
//	Hashtable<String, Integer> timeTable = new Hashtable<String, Integer>();
//	Hashtable<String, String> robotTable = new Hashtable<String, String>();
	Hashtable<String, CallMsg> callInfo = new Hashtable<String, CallMsg>();
	private boolean TEST = false;
	
	@RequestMapping("/smatrIvr")
	public @ResponseBody Map<String, Object> smartDemo(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket receiver = context.socket(ZMQ.DEALER);
		receiver.connect("tcp://172.16.0.17:3130");
		
		String sendMsg;
		String recvMsg;
		
		String json = new String(getReqMsg(request));
		log.info(json);
		
		System.out.println("\n\n\n----------------------------------------------------------\n");
		System.out.println(getCurrentTime() +" json---" + json);
		JSONObject jsonDate = JSONObject.parseObject(json);;
		call callTemp = null;
		try {
			callTemp = new call(json);
			getRobot(callTemp, receiver);
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		sendMsg = buildSendMsg(jsonDate, callTemp, receiver);
		System.out.println(getCurrentTime() +" sendMsg---" + sendMsg);
		
		if (sendMsg.isEmpty() || callTemp.robotId.isEmpty()){
			System.out.println(getCurrentTime() +" sendMsg is empty" );
			map =  noop(callTemp);
		}else if (sendMsg.equals(RESUME)) {
			System.out.println(getCurrentTime() +" resume play" );
			map =  console(callTemp, RESUME);
		}else if (sendMsg.equals(INVALID)) {
			System.out.println(getCurrentTime() +" invalid "+ INVALIDTIME/1000 +"S之内" );
			map = noop(callTemp);
		}else if (sendMsg.equals(ASRSTART)) {
			String key = callTemp.calleeid + callTemp.callerid;
			System.out.println(getCurrentTime() +" asrstart "+ INVALIDTIME/1000 +"S之内" );
			int time = 100;
			if (callInfo.containsKey(key) && callInfo.get(key).getoutTime() != 0){
				time = callInfo.get(key).getoutTime();
			}
			map = nothingFangyin(time, callTemp);
			setTime(key, time);
		}else {
			if (!TEST){
				boolean ret = receiver.send(sendMsg);
				if (ret){
	
					ZMQ.PollItem []items = {new ZMQ.PollItem(receiver, ZMQ.Poller.POLLIN)};
					ZMQ.poll(items, OUTIME);
					if (!items[0].isReadable()){
						System.out.println(getCurrentTime() +"timeout nothingFangyin:" );
						map = nothingFangyin(LONGOUT, callTemp);
						if (!callTemp.action.equals("leave")) {
							String key = callTemp.calleeid + callTemp.callerid;
							setTime(key, LONGOUT);
						}
					}else {
						recvMsg = new String(receiver.recv());
						System.out.println(getCurrentTime() +" recvMsg:" + recvMsg);
						map = buildMap(recvMsg, callTemp);
					}
				}
			}else {
				System.out.println(getCurrentTime() + " TEST");
				if ("leave".equals(callTemp.action)){
					recvMsg = testRcvhangup(callTemp);
				}else {
					recvMsg = testRcv(callTemp);
				}
				System.out.println(getCurrentTime() +" recvMsg:" + recvMsg);
				map = buildMap(recvMsg, callTemp);
			}
			
		}
		
		log.info("map=" + map);
		System.out.println(getCurrentTime() +" map=" + map);
		
//		System.out.println(getCurrentTime() +" timeTable:" + timeTable);
//		System.out.println(getCurrentTime() +" robotTable:" + robotTable);
		System.out.println(getCurrentTime() +" callInfo:" + callInfo);
		receiver.close();
		context.close();
		
		return map;
	}

	private void getRobot(call callTemp, ZMQ.Socket receiver) throws Exception {
		String key = callTemp.calleeid + callTemp.callerid;
		String phone = "";
		String recvMsg = "";
		String sendMsg = "";
		String robotId = "";
		if (isSmart(callTemp.calleeid)) {
			phone = callTemp.callerid;
		}else {
			phone = callTemp.calleeid;
		}
		callTemp.setUserId(phone);
//		if (robotTable.containsKey(key)) {
//			robotId = robotTable.get(key);
//			callTemp.setRobotId(robotId);
//			return;
//		}
		if (callInfo.containsKey(key) && callInfo.get(key).getRobotId() != "") {
			robotId = callInfo.get(key).getRobotId();
			callTemp.setRobotId(robotId);
			return;
		}
		
		sendMsg = build602(phone);
		System.out.println(getCurrentTime() +" sendMsg---" + sendMsg);
		if (!TEST){
			boolean ret = receiver.send(sendMsg);
			if (ret){
	
				ZMQ.PollItem []items = {new ZMQ.PollItem(receiver, ZMQ.Poller.POLLIN)};
				ZMQ.poll(items, OUTIME);
				if (!items[0].isReadable()){
					throw new Exception(getCurrentTime() +" 602 no return!" );
				}
				
				recvMsg = new String(receiver.recv());
				System.out.println(getCurrentTime() + " recvMsg:" + recvMsg);
				JSONObject jsonDate;
				try{
					jsonDate = JSONObject.parseObject(recvMsg);
				}catch(Exception e){
					throw new Exception(getCurrentTime() + "can not convert recvMsg to jsonDate!");
				}
				
				int cmd = (Integer)jsonDate.get("cmd");
				System.out.println(getCurrentTime() +"  cmd:" + cmd + " action:" + callTemp.action);
				if (cmd == 700){
					if (!jsonDate.containsKey("robotList")){
						throw new Exception(getCurrentTime() + "没有robotList项");
					}
					String robotList = JSONObject.toJSONString(jsonDate.get("robotList"));
					@SuppressWarnings("unchecked")
					List<Map<String,String>> list = (List<Map<String,String>>) JSONArray.parse(robotList);
					if (list.size() <= 0) {
						int errNum = (int) jsonDate.get("robotFlag");
						throw new Exception(getCurrentTime() + "没有robotList项" + errInfo(errNum));
					}
					robotId = (String) list.get(0).get("robotID");
//					robotTable.put(key, robotId);
					if (callInfo.containsKey(key)) {
						callInfo.get(key).setRobotId(robotId);
					}else {
						callInfo.put(key, new CallMsg(robotId));
					}
					
					callTemp.setRobotId(robotId);
				}
			}
		}else {
			System.out.println(getCurrentTime() + " TEST");
			recvMsg = testRcv700(callTemp);
			System.out.println(getCurrentTime() + " recvMsg:" + recvMsg);
			JSONObject jsonDate;
			try{
				jsonDate = JSONObject.parseObject(recvMsg);
			}catch(Exception e){
				throw new Exception(getCurrentTime() + "can not convert recvMsg to jsonDate!");
			}
			
			int cmd = (Integer)jsonDate.get("cmd");
			System.out.println(getCurrentTime() +"  cmd:" + cmd + " action:" + callTemp.action);
			if (cmd == 700){
				if (!jsonDate.containsKey("robotList")){
					throw new Exception(getCurrentTime() + "没有robotList项");
				}
				String robotList = JSONObject.toJSONString(jsonDate.get("robotList"));
				System.out.println(getCurrentTime() + " robotList:" + robotList);
				@SuppressWarnings("unchecked")
				List<Map<String,String>> list = (List<Map<String,String>>) JSONArray.parse(robotList);
				robotId = (String) list.get(0).get("robotID");
//				robotTable.put(key, robotId);
				if (callInfo.containsKey(key)) {
					callInfo.get(key).setRobotId(robotId);
				}else {
					callInfo.put(key, new CallMsg(robotId));
				}
				callTemp.setRobotId(robotId);
			}
		}
		
	}

	private String errInfo(int errNum) {
		String errInfo = "";
		if (errNum == 0) {
			errInfo = "没有机器人！";
		}else if (errNum > 0 && errNum < 10000) {
			errInfo = "已存在机器人：" + errNum;
		}else if (errNum == 10001) {
			errInfo = "机器人创建失败！";
		}else if (errNum == 10002) {
			errInfo = "数据库错误！";
		}else if (errNum == 10003) {
			errInfo = "不允许创建机器人！";
		}else if (errNum == 10004) {
			errInfo = "配置文件错误！";
		}
		return errInfo + " （找力豪）";
	}

	private String build602(String phone) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 602);
		map.put("uid", phone);
		map.put("playerID", 0);
		map.put("robotType", 1);
		return JSON.toJSONString(map);
	}

	private boolean isSmart(String calleeid) {
		if (calleeid.equals("8888") || calleeid.equals("9999")) {
			return true;
		}else {
			return false;
		}
	}

	private Map<String, Object> buildMap(String recvMsg, call callTemp) throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonDate;
		try{
			jsonDate = JSONObject.parseObject(recvMsg);
		}catch(Exception e){
			e.printStackTrace();
			return map;
		}
		
		int cmd = (Integer)jsonDate.get("cmd");
		System.out.println(getCurrentTime() +"  cmd:" + cmd + " action:" + callTemp.action);
		String key = callTemp.calleeid + callTemp.callerid;
		if (cmd == 901){
			if (!jsonDate.containsKey("timeout")){
				throw new Exception("没有timeout项");
			}
			int outTime = (Integer)jsonDate.get("timeout");
			String message = String.valueOf(jsonDate.get("content"));
			System.out.println(getCurrentTime() +" outTime:" + outTime + " message:" + message );
			if ("enter".equals(callTemp.action)){// 接通
				map = defaultfangyin(message, 0, callTemp);
			}else {
				map = fangyin(message, 0, callTemp);
				setLastedPlay(callTemp);
			}
			setTime(key, outTime);
		}else if (cmd == 1020){
			map = hangUp("");
			clear(callTemp);
		}else if (cmd == 1021) {
			int time = 100;
//			if (timeTable.containsKey(key)){
//				time = timeTable.get(key);
//			}
			if (callInfo.containsKey(key) && callInfo.get(key).getoutTime() != 0){
				time = callInfo.get(key).getoutTime();
			}
			map = nothingFangyin(time, callTemp);
			setTime(key, time);
		}
		return map;
	}

	private void setLastedPlay(call callTemp) {
		String key = callTemp.calleeid + callTemp.callerid;
		if (callInfo.containsKey(key)) {
			callInfo.get(key).setlastedPlay(new Date());
		}else {
			callInfo.put(key, new CallMsg());
		}
	}

	private void clear(call callTemp) {
		String key = callTemp.calleeid + callTemp.callerid;
//		if (timeTable.containsKey(key)){
//			timeTable.remove(key);
//		}
		
		if (callInfo.containsKey(key) ){
			callInfo.remove(key);
		}
		
//		if (robotTable.containsKey(key)) {
//			robotTable.remove(key);
//		}
	}

	private void setTime(String key, int outTime) {
//		if (timeTable.containsKey(key)){
//			timeTable.replace(key, outTime);
//		}else {
//			timeTable.put(key, outTime);
//		}
		
		if (callInfo.containsKey(key)){
			callInfo.get(key).setoutTime(outTime);
		}else {
			callInfo.put(key, new CallMsg(outTime));
		}
		
	}

	private String buildSendMsg(JSONObject jsonDate, call callTemp, ZMQ.Socket receiver) {
		String key = callTemp.calleeid + callTemp.callerid;
		if ("enter".equals(callTemp.action)) { // 接通
			return buildEndterMsg(callTemp);
		} else if (("asrprogress_notify".equals(callTemp.action))) { // 停顿识别（逗号识别）
			return RESUME;
		} else if ("asrmessage_notify".equals(callTemp.action)) {
			Date now = new Date();
			sendInterupt(jsonDate, callTemp, receiver);
			String message = String.valueOf(jsonDate.get("message"));
			boolean isPlay = (boolean) jsonDate.get("playstate");
			if (callInfo.containsKey(key)) {
				if (callInfo.get(key).getlastedPlay().getTime() + INVALIDTIME + ASRTIME > now.getTime()) {
					// 播放前1.5秒之内为无效  500ms为asr识别估计时间
					return INVALID;
				}
			}
			if (message.isEmpty() ){
				// ase识别为空
				if (isPlay) {
					// 正在播放，返回继续播放
					return RESUME;
				}else {
					// 不在播放，发送空到TES
					return buildMsgMsg("", callTemp);
				}
			}else {
				if (isPlay && callInfo.containsKey(key) && callInfo.get(key).isFrequent(now)) {
					// 频繁打断 暂时不用
					//return buildFrequentMsg(callTemp);
					return buildMsgMsg(message, callTemp);
				}else {
					return buildMsgMsg(message, callTemp);
				}
			}
			
		} else if ("playback_result".equals(callTemp.action)) { // 放音超时结果
			return buildPlayback(callTemp);
		} else if ("leave".equals(callTemp.action)){ // 挂断
			clear(callTemp);
			return buildLeaveMsg(callTemp);
		} else if ("wait_result".equals(callTemp.action)){ // wait超时
			return buildTimeoutMsg(callTemp);
		} else if ("start_asr_result".equals(callTemp.action)) {
			return ASRSTART;
		}
		return "";
	}

	private String buildFrequentMsg(call callTemp) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 1230);
		map.put("robotid", callTemp.robotId);
		map.put("uid", callTemp.userId);
		map.put("content", "用户频繁打断");
		return JSON.toJSONString(map);
	}

	private String buildPlayback(call callTemp) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 1403);
		map.put("robotid", callTemp.robotId);
		//map.put("uid", calleeid);
		map.put("uid", callTemp.userId);
		map.put("result", 0);
		map.put("play_time", 0);
		return JSON.toJSONString(map);
	}

	private void sendInterupt(JSONObject jsonDate, call callTemp, Socket receiver) {
		int playTime = (int) jsonDate.get("playms");
		boolean playStat = (boolean) jsonDate.get("playstate");
		if (playStat == false) {
			return;
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 1403);
		map.put("robotid", callTemp.robotId);
		//map.put("uid", calleeid);
		map.put("uid", callTemp.userId);
		map.put("result", 1);
		map.put("play_time", playTime);
		String sendMsg = JSON.toJSONString(map);
		String recvMsg = "";
		System.out.println(getCurrentTime() +" sendMsg---" + sendMsg);
		boolean ret = receiver.send(sendMsg);
		if (ret){
			ZMQ.PollItem []items = {new ZMQ.PollItem(receiver, ZMQ.Poller.POLLIN)};
			ZMQ.poll(items, OUTIME);
			if (!items[0].isReadable()){
				System.out.println(getCurrentTime() +" nothingFangyin:" );
			}else {
				recvMsg = new String(receiver.recv());
				System.out.println(getCurrentTime() +" recvMsg:" + recvMsg);
			}
		}
	}

	private String buildTimeoutMsg(call callTemp) {
		String key = callTemp.calleeid + callTemp.callerid;
//		if (timeTable.containsKey(key)){
//			int outTime = (Integer)(timeTable.get(key));
//			if (outTime< 1000){
//				return buildTimeoutShortMsg(callTemp);
//			}else {
//				return buildTimeoutLongMsg(callTemp);
//			}
//		}
//		
		if (callInfo.containsKey(key) && callInfo.get(key).getoutTime() != 0){
			int outTime = callInfo.get(key).getoutTime();
			if (outTime< 1000){
				return buildTimeoutShortMsg(callTemp);
			}else {
				return buildTimeoutLongMsg(callTemp);
			}
		}
		return "";
	}
	
	private String buildTimeoutShortMsg(call callTemp) {
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 1401);
		map.put("robotid", callTemp.robotId);
		//map.put("uid", calleeid);
		map.put("uid", callTemp.userId);
		return JSON.toJSONString(map);

		
//		String sendMsg = String.format("{" 
//				+ "\"cmd\":%d,"
//				+ "\"robotid\":\"%s\"," 
//				+ "\"uid\":%s"
//				+ "}", 
//				1401 , "180418105225a217ea9a0ecdRI000010", calleeid);
//		
//		return sendMsg;
	}

	private String buildTimeoutLongMsg(call callTemp) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 1402);
		map.put("robotid", callTemp.robotId );
		//map.put("uid", calleeid);
		map.put("uid", callTemp.userId );
		return JSON.toJSONString(map);
		
//		String sendMsg = String.format("{" 
//				+ "\"cmd\":%d,"
//				+ "\"robotid\":\"%s\"," 
//				+ "\"uid\":%s"
//				+ "}", 
//				1402 , "180418105225a217ea9a0ecdRI000010", calleeid);
//		
//		return sendMsg;
	}

	private String buildLeaveMsg(call callTemp) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 601);
		map.put("robotid", callTemp.robotId );
		//map.put("uid", calleeid);
		map.put("uid", callTemp.userId );
		return JSON.toJSONString(map);
		
//		String sendMsg = String.format("{" 
//				+ "\"cmd\":%d"
//				+ "\"robotid\":\"%s\"," 
//				+ "\"uid\":%s}", 
//				601 , "180418105225a217ea9a0ecdRI000010", calleeid);
//		
//		return sendMsg;
	}

	private String buildMsgMsg(String message, call callTemp) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 801);
		map.put("channel", "dialog");
		map.put("content", message);
		map.put("robotid", callTemp.robotId );
		map.put("scene", "");
		//map.put("uid", calleeid);
		map.put("uid", callTemp.userId );
		return JSON.toJSONString(map);
		
//		String sendMsg = String.format("{" 
//				+ "\"cmd\":%d,"
//				+ "\"channel\":\"%s\"," 
//				+ "\"content\":\"%s\","
//				+ "\"robotid\":\"%s\"," 
//				+ "\"scene\":\"%s\"," 
//				+ "\"uid\":%s"
//				+ "}", 
//				801 , "dialog", message, "180418105225a217ea9a0ecdRI000010", "",calleeid);
//		
//		return sendMsg;
	}

	private String buildEndterMsg(call callTemp) {
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 600);
		map.put("robotid", callTemp.robotId );
		//map.put("uid", calleeid);
		map.put("uid", callTemp.userId);
		return JSON.toJSONString(map);
		
//		String sendMsg = String.format("{" 
//				+ "\"cmd\":%d,"
//				+ "\"robotid\":\"%s\"," 
//				+ "\"uid\":%s"
//				+ "}", 
//				600 , "180418105225a217ea9a0ecdRI000010", calleeid);
//		
//		return sendMsg;
	}

	/**
	 * 根据流程，组装对应的放音json
	 * 
	 * @param flow
	 *            放音音频
	 * @author cxy
	 * @version 2018-04-15
	 */
	public Map<String, Object> fangyin(String prompt, int outTime, call callTemp) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (!prompt.isEmpty()) {
			map.put("action", "playback");
			// 缓存中取出这步的业务名称
			map.put("flowdata", callTemp.flow);
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("prompt", "/home/galaxyeye/Downloads/" + prompt);
			params.put("wait", outTime);
			params.put("retry", 0);
			params.put("allow_interrupt", INVALIDTIME);//播放前1.5秒不能打断
			
			map.put("params", params);
			return map;
		}

		return null;
	}

	public Map<String, Object> nothingFangyin(int outTime, call callTemp) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (!callTemp.action.isEmpty()) {
			map.put("action", "wait");
			// 缓存中取出这步的业务名称
			map.put("flowdata", "");
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("timeout", outTime);

			map.put("params", params);
			return map;
		}

		return null;
	}
	
	public Map<String, Object> noop(call callTemp) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (!callTemp.action.isEmpty()) {
			map.put("action", "noop");
			// 缓存中取出这步的业务名称
			map.put("flowdata", "");
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("usermsg", "");

			map.put("params", params);
			return map;
		}

		return null;
	}
	
	public Map<String, Object> console(call callTemp, String op) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (!callTemp.action.isEmpty()) {
			map.put("action", "console_playback");
			// 缓存中取出这步的业务名称
			map.put("flowdata", "");
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("command", op);

			map.put("params", params);
			return map;
		}

		return null;
	}
	
	/**
	 * 进入流程
	 * 
	 * @param flow
	 *            欢迎ivr
	 * @author cxy
	 * @version 2018-04-15
	 * @return
	 */
	public Map<String, Object> defaultfangyin(String prompt, int outTime, call callTemp) {
		Map<String, Object> map = new HashMap<String, Object>();
		if (!prompt.isEmpty()) {
			Map<String, Object> params = new HashMap<String, Object>();
			Map<String, Object> after_params = new HashMap<String, Object>();
			map.put("action", "playback");
			map.put("after_action", "start_asr");
			map.put("flowdata", callTemp.flow);
			map.put("after_ignore_error", false);
			params.put("min_speak_ms", 100);
			params.put("max_speak_ms", 10000);
			params.put("min_pause_ms", 300);
			params.put("max_pause_ms", 600);
			params.put("pause_play_ms", 400);// 暂停播放毫秒
			params.put("threshold", 500);// VAD阈值，默认0，建议不要设置，如果一定要设置，建议
											// 2000以下的值。
			params.put("volume", 50);
			params.put("recordpath", "");
			params.put("filter_level", 0.5);
			
			after_params.put("prompt", "/home/galaxyeye/Downloads/" + prompt);
			after_params.put("wait", outTime);
			after_params.put("retry", 0);
			map.put("params", after_params);
			map.put("after_params", params);
			return map;
		}
		return null;
	}

	/**
	 * 放音后挂断
	 * 
	 * @param voice
	 *            结束语
	 * @author cxy
	 * @version 2018-04-15
	 * @return
	 */
	public Map<String, Object> hangUp(String voice) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("action", "hangup");
		Map<String, Object> after_params = new HashMap<String, Object>();
		after_params.put("cause", 0);
		after_params.put("usermsg", "");
		map.put("params", after_params);
		return map;
	}

	/**
	 * @Description(解析request转为json) @param request
	 * @author guodong
	 * @return
	 */
	public static String getReqMsg(HttpServletRequest request) {
		InputStream in;
		StringBuffer json = null;
		try {
			in = request.getInputStream();
			json = new StringBuffer();
			byte[] b = new byte[4096];
			for (int n; (n = in.read(b)) != -1;) {
				json.append(new String(b, 0, n));
			}
			String msg = new String(json.toString().getBytes(), "utf-8");
			//System.out.println("msg=" + msg);
			if (msg.indexOf("text=\"") > -1) {
				msg = msg.substring(0, msg.indexOf("text=\"") + 5) + "\\\""
						+ msg.substring(msg.indexOf("text=\"") + 6, msg.indexOf("\"\"")) + "\\\"\""
						+ msg.substring(msg.indexOf("\"\"") + 2, msg.length());
			}
			//System.out.println("after msg=" + msg);
			return msg;
		} catch (Exception e) {
			e.printStackTrace();
			json.append("");
		}
		return json.toString();
	}

	public String testRcv(call callTemp){
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 901);
		map.put("result", 1);
		map.put("uid", callTemp.userId );
		map.put("robotid", callTemp.robotId );
		map.put("delay", 0);
		map.put("channel", "dialog");
		map.put("content", "你好啊！我是可可");
		map.put("timeout", 500);
		
		return JSON.toJSONString(map);
		
	}
	
	public String testRcvhangup(call callTemp){
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("cmd", 1020);
		map.put("uid", callTemp.userId );
		map.put("robotid", callTemp.robotId );
		
		return JSON.toJSONString(map);

	}
	
	public String testRcv700(call callTemp){
		Map<String, Object> map = new HashMap<String, Object>();
		HashMap<String, Object> robotmap = new HashMap<String, Object>();
		@SuppressWarnings("rawtypes")
		List<HashMap> robotList = new ArrayList<HashMap>();
		map.put("cmd", 700);
		map.put("uid", callTemp.userId );
		robotmap.put("robotID", "1803131024027cc6f33d1d79RI001908");
		robotList.add(robotmap);
		
		map.put("robotList", robotList);
		
		return JSON.toJSONString(map);
		
	}
	
	public String getEnv(String env){
		String value = "";
		try{
			Map<String, String> map = System.getenv();
			value = map.get(env);
		}catch (Exception e){
			e.printStackTrace();
		}
		return value;
	}
	
	public String getCurrentTime(){
		SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 
		String myTime = sdFormat.format(new Date());
		
		return myTime;
	}
}
