package com.example.ffmpegjava;

import android.util.Log;

public class ProgressManager {

	private int duration;
	private int time;
	
	public ProgressManager() {
		duration = 0;
		time = 0;
	}
	
	//Duration: 00:00:05.65, start: 0.000000, bitrate: 1401 kb/s
	public void setDurationFromShell(String str) {
		String tok[] = str.split("Duration: ");
		//00:00:05.65
		String durationStr = tok[1].substring(0, 11);
		duration = convertTimeToInt(durationStr);
	}

	//frame= 12 fps=0.0 q=24.0 size= 90kB time=00:00:00.50 bitrate=1467.7kbits/s
	public void setTimeFromShell(String str) {
		String tok[] = str.split("time=");
		//00:00:00.50
		String timeStr = tok[1].substring(0, 11);
		time = convertTimeToInt(timeStr);
	}
	
	//정수형태로 시간 계산
	private int convertTimeToInt(String time) {
		//00:00:00.50
		//hh:mm:ss:ms
		String tok1[] = time.split(":");
		int hour = Integer.parseInt(tok1[0]);
		int min = Integer.parseInt(tok1[1]);
		
		//00.50
		String tok2[] = tok1[2].split("\\.");
		int sec = Integer.parseInt(tok2[0]);
		int miliSec = Integer.parseInt(tok2[1]);
		
		return hour*60*60*100 + min*60*100 + sec*100 + miliSec;
	}
	
	public int getProgress() {
		if(duration == 0) 
			return 0;
		else 
			return (int)((double)time/(double)duration * 100);
	}
	
	public void setComplete() {
		time = duration;
	}
}
