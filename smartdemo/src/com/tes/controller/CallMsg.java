package com.tes.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CallMsg {
	int outTime;
	String robotId;
	List<Date> interrupt;
	Date lastedPlay;
	Date answerTime;
	SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"); 

	CallMsg(){
		outTime = 0;
		robotId = "";
		interrupt = new ArrayList<Date>();
		lastedPlay = new Date();
	}
	
	CallMsg(String robot){
		outTime = 0;
		robotId = robot;
		interrupt = new ArrayList<Date>();
		lastedPlay = new Date();
	}
	
	CallMsg(int out){
		outTime = out;
		robotId = "";
		interrupt = new ArrayList<Date>();
		lastedPlay = new Date();
	}
	
	public void setoutTime(int out) {
		outTime = out;
	}
	
	public int getoutTime() {
		return outTime;
	}
	
	public void setAnswerTime(Date answer) {
		answerTime = answer;
	}
	
	public Date getAnswerTime() {
		return answerTime;
	}
	
	public void setRobotId(String robot) {
		robotId = robot;
	}
	
	public String getRobotId() {
		return robotId;
	}
	
	public Date getlastedPlay() {
		return lastedPlay;
	}
	
	public void setlastedPlay(Date lasted) {
		lastedPlay = lasted;
	}
	
	// 增加打断，频繁打断true 否则false  （3s内两次打断为频繁打断）
	public boolean isFrequent(Date inter) {
		interrupt.add(inter);
		int size = interrupt.size();
		if (size > 1 && (interrupt.get(size -1).getTime() - interrupt.get(size -2).getTime() < 3 * 1000)) {
			// 最新一次打断时间与上一次打断没有超过3S 既频繁打断
			return true;
		}else {
			return false;
		}
	}
	
	public String toString() {
		String ret = "robotId:" + robotId;
		ret += " interrupt:[";
		for (int i = 0; i < interrupt.size(); i++) {
			ret = ret + sdFormat.format(interrupt.get(i)) + ",";
		}
		ret += "] lastedPlay:";
		ret += sdFormat.format(lastedPlay);
		ret += " outTime:";
		ret += outTime;
		ret += " answerTime:";
		ret += sdFormat.format(answerTime);
		return ret;
	}
}
