package org.tabooproject.fluxon.runtime.function;

import org.tabooproject.fluxon.runtime.FluxonRuntime;
import org.tabooproject.fluxon.runtime.stdlib.Coerce;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class FunctionTime {

    public static void init(FluxonRuntime runtime) {
        // 获取当前时间戳（毫秒）
        runtime.registerFunction("now", 0, args -> System.currentTimeMillis());

        // 获取当前时间戳（秒）
        runtime.registerFunction("nowSeconds", 0, args -> System.currentTimeMillis() / 1000);

        // 获取当前日期时间字符串 formatDateTime(pattern?)
        runtime.registerFunction("formatDateTime", Arrays.asList(0, 1), args -> {
            String pattern = args.length == 1 ? Coerce.asString(args[0]).orElse("yyyy-MM-dd HH:mm:ss") : "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(new Date());
        });

        // 格式化时间戳 formatTimestamp(timestamp, pattern?)
        runtime.registerFunction("formatTimestamp", Arrays.asList(1, 2), args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            String pattern = args.length == 2 ? Coerce.asString(args[1]).orElse("yyyy-MM-dd HH:mm:ss") : "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.format(new Date(timestamp));
        });

        // 解析日期字符串 parseDateTime(dateString, pattern)
        runtime.registerFunction("parseDateTime", 2, args -> {
            String dateString = Coerce.asString(args[0]).orElse("");
            String pattern = Coerce.asString(args[1]).orElse("yyyy-MM-dd HH:mm:ss");
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                Date date = sdf.parse(dateString);
                return date.getTime();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse date: " + dateString + " with pattern: " + pattern);
            }
        });

        // 获取当前年份
        runtime.registerFunction("year", 0, args -> {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.YEAR);
        });

        // 获取当前月份（1-12）
        runtime.registerFunction("month", 0, args -> {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.MONTH) + 1;
        });

        // 获取当前日期（1-31）
        runtime.registerFunction("day", 0, args -> {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.DAY_OF_MONTH);
        });

        // 获取当前小时（0-23）
        runtime.registerFunction("hour", 0, args -> {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.HOUR_OF_DAY);
        });

        // 获取当前分钟（0-59）
        runtime.registerFunction("minute", 0, args -> {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.MINUTE);
        });

        // 获取当前秒数（0-59）
        runtime.registerFunction("second", 0, args -> {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.SECOND);
        });

        // 获取当前星期几（1-7，1=星期日）
        runtime.registerFunction("weekday", 0, args -> {
            Calendar cal = Calendar.getInstance();
            return cal.get(Calendar.DAY_OF_WEEK);
        });

        // 从时间戳获取年份 yearFromTimestamp(timestamp)
        runtime.registerFunction("yearFromTimestamp", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.YEAR);
        });

        // 从时间戳获取月份 monthFromTimestamp(timestamp)
        runtime.registerFunction("monthFromTimestamp", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.MONTH) + 1;
        });

        // 从时间戳获取日期 dayFromTimestamp(timestamp)
        runtime.registerFunction("dayFromTimestamp", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.DAY_OF_MONTH);
        });

        // 从时间戳获取小时 hourFromTimestamp(timestamp)
        runtime.registerFunction("hourFromTimestamp", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.HOUR_OF_DAY);
        });

        // 从时间戳获取分钟 minuteFromTimestamp(timestamp)
        runtime.registerFunction("minuteFromTimestamp", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.MINUTE);
        });

        // 从时间戳获取秒数 secondFromTimestamp(timestamp)
        runtime.registerFunction("secondFromTimestamp", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            return cal.get(Calendar.SECOND);
        });

        // 时间计算函数
        
        // 添加天数 addDays(timestamp, days)
        runtime.registerFunction("addDays", 2, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            int days = Coerce.asInteger(args[1]).orElse(0);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.add(Calendar.DAY_OF_MONTH, days);
            return cal.getTimeInMillis();
        });

        // 添加小时 addHours(timestamp, hours)
        runtime.registerFunction("addHours", 2, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            int hours = Coerce.asInteger(args[1]).orElse(0);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.add(Calendar.HOUR_OF_DAY, hours);
            return cal.getTimeInMillis();
        });

        // 添加分钟 addMinutes(timestamp, minutes)
        runtime.registerFunction("addMinutes", 2, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            int minutes = Coerce.asInteger(args[1]).orElse(0);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.add(Calendar.MINUTE, minutes);
            return cal.getTimeInMillis();
        });

        // 添加秒数 addSeconds(timestamp, seconds)
        runtime.registerFunction("addSeconds", 2, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            int seconds = Coerce.asInteger(args[1]).orElse(0);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.add(Calendar.SECOND, seconds);
            return cal.getTimeInMillis();
        });

        // 计算两个时间戳之间的天数差 daysBetween(timestamp1, timestamp2)
        runtime.registerFunction("daysBetween", 2, args -> {
            long timestamp1 = Coerce.asLong(args[0]).orElse(0L);
            long timestamp2 = Coerce.asLong(args[1]).orElse(0L);
            long diffInMillis = Math.abs(timestamp2 - timestamp1);
            return diffInMillis / (24 * 60 * 60 * 1000);
        });

        // 计算两个时间戳之间的小时差 hoursBetween(timestamp1, timestamp2)
        runtime.registerFunction("hoursBetween", 2, args -> {
            long timestamp1 = Coerce.asLong(args[0]).orElse(0L);
            long timestamp2 = Coerce.asLong(args[1]).orElse(0L);
            long diffInMillis = Math.abs(timestamp2 - timestamp1);
            return diffInMillis / (60 * 60 * 1000);
        });

        // 计算两个时间戳之间的分钟差 minutesBetween(timestamp1, timestamp2)
        runtime.registerFunction("minutesBetween", 2, args -> {
            long timestamp1 = Coerce.asLong(args[0]).orElse(0L);
            long timestamp2 = Coerce.asLong(args[1]).orElse(0L);
            long diffInMillis = Math.abs(timestamp2 - timestamp1);
            return diffInMillis / (60 * 1000);
        });

        // 计算两个时间戳之间的秒数差 secondsBetween(timestamp1, timestamp2)
        runtime.registerFunction("secondsBetween", 2, args -> {
            long timestamp1 = Coerce.asLong(args[0]).orElse(0L);
            long timestamp2 = Coerce.asLong(args[1]).orElse(0L);
            long diffInMillis = Math.abs(timestamp2 - timestamp1);
            return diffInMillis / 1000;
        });

        // 时间比较函数
        
        // 检查时间戳是否在今天 isToday(timestamp)
        runtime.registerFunction("isToday", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(0L);
            Calendar cal1 = Calendar.getInstance();
            cal1.setTimeInMillis(timestamp);
            Calendar cal2 = Calendar.getInstance();
            
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        });

        // 检查时间戳是否在昨天 isYesterday(timestamp)
        runtime.registerFunction("isYesterday", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(0L);
            Calendar cal1 = Calendar.getInstance();
            cal1.setTimeInMillis(timestamp);
            Calendar cal2 = Calendar.getInstance();
            cal2.add(Calendar.DAY_OF_YEAR, -1);
            
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        });

        // 检查时间戳是否在明天 isTomorrow(timestamp)
        runtime.registerFunction("isTomorrow", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(0L);
            Calendar cal1 = Calendar.getInstance();
            cal1.setTimeInMillis(timestamp);
            Calendar cal2 = Calendar.getInstance();
            cal2.add(Calendar.DAY_OF_YEAR, 1);
            
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                   cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        });

        // 检查时间戳是否在指定日期范围内 isBetween(timestamp, startTimestamp, endTimestamp)
        runtime.registerFunction("isBetween", 3, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(0L);
            long startTimestamp = Coerce.asLong(args[1]).orElse(0L);
            long endTimestamp = Coerce.asLong(args[2]).orElse(0L);
            return timestamp >= startTimestamp && timestamp <= endTimestamp;
        });

        // 获取时间戳的开始时间（00:00:00） startOfDay(timestamp)
        runtime.registerFunction("startOfDay", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        });

        // 获取时间戳的结束时间（23:59:59） endOfDay(timestamp)
        runtime.registerFunction("endOfDay", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTimeInMillis();
        });

        // 获取时间戳所在月份的第一天 startOfMonth(timestamp)
        runtime.registerFunction("startOfMonth", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        });

        // 获取时间戳所在月份的最后一天 endOfMonth(timestamp)
        runtime.registerFunction("endOfMonth", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTimeInMillis();
        });

        // 获取时间戳所在年份的第一天 startOfYear(timestamp)
        runtime.registerFunction("startOfYear", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        });

        // 获取时间戳所在年份的最后一天 endOfYear(timestamp)
        runtime.registerFunction("endOfYear", 1, args -> {
            long timestamp = Coerce.asLong(args[0]).orElse(System.currentTimeMillis());
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(timestamp);
            cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR));
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTimeInMillis();
        });
    }
} 