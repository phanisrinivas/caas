package org.kisst.cordys.caas.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Set;

public class DateUtil {
	
	private static final String STANDARD_DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss'Z'";
	
	// Gets current UTC date in this format: 2004-12-05T09:21:59Z"
	public static String getUTCDate(){
		DateFormat df = new SimpleDateFormat(STANDARD_DATE_FORMAT_STRING);
		String date = df.format(new Date());
		return date;
	}
	
	//Gets the difference between 2 dates in Seconds or Minutes or Hours or Days based on the choice
	public static long getDifference(String dateStr1, String dateStr2, char choice){
		
		if(dateStr1==null || dateStr2 ==null) throw new RuntimeException("Invalid input");
        DateFormat df = new SimpleDateFormat(STANDARD_DATE_FORMAT_STRING);
        Date date1=null, date2=null;
        try {
			date1 = df.parse(dateStr1);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("Invalid Date Format "+dateStr1);
		}
		try {
			date2 = df.parse(dateStr2);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new RuntimeException("Invalid Date Format "+dateStr2);
		}
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal1.setTime(date2);
        long milis1 = cal1.getTimeInMillis();
        long milis2 = cal2.getTimeInMillis();
        long diffMillis = milis2 - milis1;
        long diffSeconds = diffMillis / 1000;
        long diffMinutes = diffMillis / (60 * 1000);        
        long diffHours = diffMillis / (60 * 60 * 1000);
        long diffDays = diffMillis / (24 * 60 * 60 * 1000);           
        switch (choice) {
            case 'S':  return diffSeconds;
            case 's':  return diffSeconds;
            case 'M':  return diffMinutes;
            case 'm':  return diffMinutes;
            case 'H':  return diffHours;
            case 'h':  return diffHours;
            case 'D':  return diffDays;
            case 'd':  return diffDays;
            default: return diffMillis;
        }
	}	
}
