package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.java.Export;
import org.tabooproject.fluxon.runtime.java.ExportRegistry;
import org.tabooproject.fluxon.runtime.java.Optional;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class FunctionTime {

    public static class TimeObject {

        public static final TimeObject INSTANCE = new TimeObject();

        // 基础时间函数
        @Export
        public long getNow() {
            return System.currentTimeMillis();
        }

        @Export
        public long getNowSeconds() {
            return System.currentTimeMillis() / 1000;
        }

        @Export
        public long getNano() {
            return System.nanoTime();
        }

        // 格式化函数
        @Export
        public String formatDateTime(@Optional String pattern) {
            String datePattern = pattern != null ? pattern : "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
            return sdf.format(new Date());
        }

        @Export
        public String formatTimestamp(long timestamp, @Optional String pattern) {
            String datePattern = pattern != null ? pattern : "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
            return sdf.format(new Date(timestamp));
        }

        @Export
        public long parseDateTime(String dateString, String pattern) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                Date date = sdf.parse(dateString);
                return date.getTime();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse date: " + dateString + " with pattern: " + pattern);
            }
        }

        // 当前时间组件获取
        @Export
        public int getYear() {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.YEAR);
        }

        @Export
        public int getMonth() {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.MONTH) + 1;
        }

        @Export
        public int getDay() {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.DAY_OF_MONTH);
        }

        @Export
        public int getHour() {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.HOUR_OF_DAY);
        }

        @Export
        public int getMinute() {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.MINUTE);
        }

        @Export
        public int getSecond() {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.SECOND);
        }

        @Export
        public int getWeekday() {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.DAY_OF_WEEK);
        }

        // 从时间戳获取时间组件
        @Export
        public int yearFromTimestamp(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.YEAR);
        }

        @Export
        public int monthFromTimestamp(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.MONTH) + 1;
        }

        @Export
        public int dayFromTimestamp(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.DAY_OF_MONTH);
        }

        @Export
        public int hourFromTimestamp(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.HOUR_OF_DAY);
        }

        @Export
        public int minuteFromTimestamp(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.MINUTE);
        }

        @Export
        public int secondFromTimestamp(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.SECOND);
        }

        // 时间计算函数
        @Export
        public long addDays(long timestamp, int days) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.add(Calendar.DAY_OF_MONTH, days);
            return cal.getTimeInMillis();
        }

        @Export
        public long addHours(long timestamp, int hours) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.add(Calendar.HOUR_OF_DAY, hours);
            return cal.getTimeInMillis();
        }

        @Export
        public long addMinutes(long timestamp, int minutes) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.add(Calendar.MINUTE, minutes);
            return cal.getTimeInMillis();
        }

        @Export
        public long addSeconds(long timestamp, int seconds) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.add(Calendar.SECOND, seconds);
            return cal.getTimeInMillis();
        }

        // 时间差计算
        @Export
        public long daysBetween(long timestamp1, long timestamp2) {
            long diffInMillis = Math.abs(timestamp2 - timestamp1);
            return diffInMillis / (24 * 60 * 60 * 1000);
        }

        @Export
        public long hoursBetween(long timestamp1, long timestamp2) {
            long diffInMillis = Math.abs(timestamp2 - timestamp1);
            return diffInMillis / (60 * 60 * 1000);
        }

        @Export
        public long minutesBetween(long timestamp1, long timestamp2) {
            long diffInMillis = Math.abs(timestamp2 - timestamp1);
            return diffInMillis / (60 * 1000);
        }

        @Export
        public long secondsBetween(long timestamp1, long timestamp2) {
            long diffInMillis = Math.abs(timestamp2 - timestamp1);
            return diffInMillis / 1000;
        }

        // 时间比较函数
        @Export
        public boolean isToday(long timestamp) {
            Calendar cal1 = Calendar.getInstance();
            cal1.setTimeInMillis(timestamp);
            Calendar cal2 = Calendar.getInstance();
            
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }

        @Export
        public boolean isYesterday(long timestamp) {
            Calendar cal1 = Calendar.getInstance();
            cal1.setTimeInMillis(timestamp);
            Calendar cal2 = Calendar.getInstance();
            cal2.add(Calendar.DAY_OF_YEAR, -1);
            
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }

        @Export
        public boolean isTomorrow(long timestamp) {
            Calendar cal1 = Calendar.getInstance();
            cal1.setTimeInMillis(timestamp);
            Calendar cal2 = Calendar.getInstance();
            cal2.add(Calendar.DAY_OF_YEAR, 1);
            
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }

        @Export
        public boolean isBetween(long timestamp, long startTimestamp, long endTimestamp) {
            return timestamp >= startTimestamp && timestamp <= endTimestamp;
        }

        // 时间边界函数
        @Export
        public long startOfDay(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }

        @Export
        public long endOfDay(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTimeInMillis();
        }

        @Export
        public long startOfMonth(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }

        @Export
        public long endOfMonth(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTimeInMillis();
        }

        @Export
        public long startOfYear(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        }

        @Export
        public long endOfYear(long timestamp) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTimeInMillis();
        }
    }

    public static void init(FluxonRuntime runtime) {
        // 获取时间对象
        runtime.registerFunction("time", 0, (context) -> TimeObject.INSTANCE);
        // 获取当前时间戳（毫秒）
        runtime.registerFunction("now", 0, (context) -> System.currentTimeMillis());
        // 注册时间相关的对象实例
        runtime.getExportRegistry().registerClass(TimeObject.class);
    }
} 