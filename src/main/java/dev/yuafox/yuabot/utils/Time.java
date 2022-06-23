package dev.yuafox.yuabot.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Time {
    public static long getTime(String dateString){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(dateString);
            return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String getLastMonth(String dateString){
        try {
            if(dateString == null){
                Date date = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
                dateString = calendar.get(Calendar.YEAR) + "-" + (calendar.get(Calendar.MONTH)+1) + "-" + calendar.get(Calendar.DAY_OF_MONTH);
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date date = sdf.parse(dateString);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int monthId = calendar.get(Calendar.MONTH) - 1;
            if(monthId == -1){
                monthId = 11;
                calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR)-1);
            }
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.MONTH, monthId);
            return calendar.get(Calendar.YEAR) + "-" + String.format("%02d", (calendar.get(Calendar.MONTH)+1)) + "-" + String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
