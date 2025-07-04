print "=== FunctionTime 标准库测试 ==="
print ""

startTime = now()
print "1. 基础时间获取测试:"
currentTime = now()
currentTimeSeconds = nowSeconds()
print "当前时间戳(毫秒): " + &currentTime
print "当前时间戳(秒): " + &currentTimeSeconds
print ""

print "2. 时间格式化测试:"
formattedNow = formatDateTime()
formattedCustom = formatDateTime("yyyy年MM月dd日 HH:mm:ss")
print "默认格式: " + &formattedNow
print "自定义格式: " + &formattedCustom
print ""

print "3. 当前时间组成部分测试:"
currentYear = year()
currentMonth = month()
currentDay = day()
currentHour = hour()
currentMinute = minute()
currentSecond = second()
currentWeekday = weekday()
print "年份: " + &currentYear
print "月份: " + &currentMonth
print "日期: " + &currentDay
print "小时: " + &currentHour
print "分钟: " + &currentMinute
print "秒数: " + &currentSecond
print "星期: " + &currentWeekday
print ""

print "4. 时间戳解析测试:"
testTimestamp = &currentTime
yearFromTs = yearFromTimestamp(&testTimestamp)
monthFromTs = monthFromTimestamp(&testTimestamp)
dayFromTs = dayFromTimestamp(&testTimestamp)
hourFromTs = hourFromTimestamp(&testTimestamp)
minuteFromTs = minuteFromTimestamp(&testTimestamp)
secondFromTs = secondFromTimestamp(&testTimestamp)
print "从时间戳获取年份: " + &yearFromTs
print "从时间戳获取月份: " + &monthFromTs
print "从时间戳获取日期: " + &dayFromTs
print "从时间戳获取小时: " + &hourFromTs
print "从时间戳获取分钟: " + &minuteFromTs
print "从时间戳获取秒数: " + &secondFromTs
print ""

print "5. 时间计算测试:"
tomorrow = addDays(&currentTime, 1)
yesterday = addDays(&currentTime, -1)
nextHour = addHours(&currentTime, 1)
nextMinute = addMinutes(&currentTime, 1)
nextSecond = addSeconds(&currentTime, 1)
print "明天: " + formatTimestamp(&tomorrow)
print "昨天: " + formatTimestamp(&yesterday)
print "下一小时: " + formatTimestamp(&nextHour)
print "下一分钟: " + formatTimestamp(&nextMinute)
print "下一秒: " + formatTimestamp(&nextSecond)
print ""

print "6. 时间差计算测试:"
daysDiff = daysBetween(&yesterday, &tomorrow)
hoursDiff = hoursBetween(&currentTime, &nextHour)
minutesDiff = minutesBetween(&currentTime, &nextMinute)
secondsDiff = secondsBetween(&currentTime, &nextSecond)
print "昨天到明天的天数差: " + &daysDiff
print "当前到下一小时的小时差: " + &hoursDiff
print "当前到下一分钟的分钟差: " + &minutesDiff
print "当前到下一秒的秒数差: " + &secondsDiff
print ""

print "7. 时间判断测试:"
isTodayNow = isToday(&currentTime)
isTodayTomorrow = isToday(&tomorrow)
isYesterdayYesterday = isYesterday(&yesterday)
isTomorrowTomorrow = isTomorrow(&tomorrow)
print "当前时间是否今天: " + &isTodayNow
print "明天是否今天: " + &isTodayTomorrow
print "昨天是否昨天: " + &isYesterdayYesterday
print "明天是否明天: " + &isTomorrowTomorrow
print ""

print "8. 时间范围判断测试:"
isInRange = isBetween(&currentTime, &yesterday, &tomorrow)
isInRange2 = isBetween(&currentTime, &tomorrow, addDays(&currentTime, 3))
print "当前时间是否在昨天到明天之间: " + &isInRange
print "当前时间是否在明天到后天之间: " + &isInRange2
print ""

print "9. 时间边界测试:"
startOfToday = startOfDay(&currentTime)
endOfToday = endOfDay(&currentTime)
startOfThisMonth = startOfMonth(&currentTime)
endOfThisMonth = endOfMonth(&currentTime)
startOfThisYear = startOfYear(&currentTime)
endOfThisYear = endOfYear(&currentTime)
print "今天的开始时间: " + formatTimestamp(&startOfToday)
print "今天的结束时间: " + formatTimestamp(&endOfToday)
print "本月的开始时间: " + formatTimestamp(&startOfThisMonth)
print "本月的结束时间: " + formatTimestamp(&endOfThisMonth)
print "本年的开始时间: " + formatTimestamp(&startOfThisYear)
print "本年的结束时间: " + formatTimestamp(&endOfThisYear)
print ""

print "10. 日期字符串解析测试:"
dateString = "2024-01-15 10:30:00"
parsedTimestamp = parseDateTime(&dateString, "yyyy-MM-dd HH:mm:ss")
parsedFormatted = formatTimestamp(&parsedTimestamp)
print "解析日期字符串: " + &dateString
print "解析后的时间戳: " + &parsedTimestamp
print "重新格式化: " + &parsedFormatted
print ""

print "11. 复杂时间计算测试:"
oneWeekLater = addDays(&currentTime, 7)
oneMonthLater = addDays(&currentTime, 30)
oneYearLater = addDays(&currentTime, 365)
print "一周后: " + formatTimestamp(&oneWeekLater)
print "一个月后: " + formatTimestamp(&oneMonthLater)
print "一年后: " + formatTimestamp(&oneYearLater)
print ""

print "12. 时间比较和条件测试"
when &currentHour {
    in 0..6 -> print "现在是凌晨时间"
    in 6..12 -> print "现在是上午时间"
    in 12..18 -> print "现在是下午时间"
    in 18..24 -> print "现在是晚上时间"
}

when &currentWeekday {
    1 -> print "今天是星期日"
    2 -> print "今天是星期一"
    3 -> print "今天是星期二"
    4 -> print "今天是星期三"
    5 -> print "今天是星期四"
    6 -> print "今天是星期五"
    7 -> print "今天是星期六"
}
print ""

print "13. 时间统计测试:"
daysFromYearStart = daysBetween(&startOfThisYear, &currentTime)
daysFromMonthStart = daysBetween(&startOfThisMonth, &currentTime)
print "从年初到现在过了 " + &daysFromYearStart + " 天"
print "从月初到现在过了 " + &daysFromMonthStart + " 天"
print ""

print "14. 时间格式化变体测试:"
formats = [
    "yyyy-MM-dd",
    "yyyy/MM/dd",
    "MM/dd/yyyy",
    "yyyy年MM月dd日",
    "HH:mm:ss",
    "yyyy-MM-dd HH:mm:ss",
    "yyyy-MM-dd HH:mm:ss.SSS"
]

for format in &formats then {
    formatted = formatDateTime(&format)
    print "格式 '" + &format + "': " + &formatted
}
print ""

print "15. 时间验证测试:"
testCases = [&currentTime, &yesterday, &tomorrow, &startOfToday, &endOfToday]

for i in 0..4 then {
    testTime = 0
    when &i {
        0 -> testTime = &currentTime
        1 -> testTime = &yesterday
        2 -> testTime = &tomorrow
        3 -> testTime = &startOfToday
        4 -> testTime = &endOfToday
    }
    isTodayResult = isToday(&testTime)
    isYesterdayResult = isYesterday(&testTime)
    isTomorrowResult = isTomorrow(&testTime)
    print "时间 " + &i + ": " + formatTimestamp(&testTime)
    print "  是否今天: " + &isTodayResult
    print "  是否昨天: " + &isYesterdayResult
    print "  是否明天: " + &isTomorrowResult
}
print ""

print "=== FunctionTime 测试完成 ==="
print "总共测试了 15 个主要功能模块"
print "当前时间: " + formatDateTime()
print "耗时: " + (now() - &startTime) + " ms"