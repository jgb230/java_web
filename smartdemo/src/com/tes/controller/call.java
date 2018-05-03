package com.tes.controller;

import com.alibaba.fastjson.JSONObject;

public class call {
	public String calleeid; // 被叫号码
	public String callerid; // 主叫号码
	public String callid; // 每个通话的唯一I
	public String flow;
	public String action;

	public String robotId;
	public String userId;
	
	call(String json) {
		JSONObject jsonDate = null;

		jsonDate = JSONObject.parseObject(json);
		this.action = String.valueOf(jsonDate.get("notify"));
		this.flow = String.valueOf(jsonDate.get("flowdata"));
		this.calleeid = String.valueOf(jsonDate.get("calleeid"));
		this.callerid = String.valueOf(jsonDate.get("callerid"));
		this.callid = String.valueOf(jsonDate.get("callid"));
		this.robotId = "";
		this.userId = "";

	}

	public void setRobotId(String robotId) {
		this.robotId = robotId;
	}
	
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
}