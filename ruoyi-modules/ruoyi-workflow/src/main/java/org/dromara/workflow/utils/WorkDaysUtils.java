package org.dromara.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.constant.GlobalConstants;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.redis.utils.RedisUtils;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 工作日工具类
 *
 * @author AprilWind
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkDaysUtils {

    private static final String DAYS_IN_YEARS_KEY = GlobalConstants.GLOBAL_REDIS_KEY + "DAYS_IN_YEARS:";
    private static final LockTemplate LOCK_TEMPLATE = SpringUtils.getBean(LockTemplate.class);
    //工作日
    public static final int WORKDAY = 0;
    //休息日
    public static final int RESTDAY = 1;

    /**
     * 根据开始日期和工作日数计算截止日期
     *
     * @param startDate 开始日期
     * @param workDays  工作日数
     * @return 截止日期
     */
    public static Date calculateEndDate(Date startDate, int workDays) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        // 获取开始日期的年份
        int startYear = getYear(startDate);
        //开始日期年份天
        Integer[] daysArray = daysInYear(startYear);
        int startIndex = getDayOfYearIndex(startDate);
        int endIndex = startIndex;
        // 循环找到指定的工作日数
        while (workDays > 0) {
            // 检查当前日期是否是工作日
            if (daysArray[endIndex] == WORKDAY) {
                workDays--;
            }
            // 结束条件：工作日数为零
            if (workDays <= 0) {
                break;
            }
            // 移动到下一天
            endIndex++;
            // 如果移动到下一年
            if (endIndex >= daysArray.length) {
                startYear++;
                daysArray = daysInYear(startYear);
                endIndex = 0;
                // 更新年份
                calendar.set(Calendar.YEAR, startYear);
            }
        }
        // 设置截止日期
        calendar.set(Calendar.DAY_OF_YEAR, endIndex + 1);
        return calendar.getTime();
    }

    /**
     * 计算两个日期之间的工作日天数
     *
     * @param start 开始日期
     * @param end   结束日期
     * @return 实际工作日天数
     */
    public static int calculateWorkDays(Date start, Date end) {
        if (start.after(end)) {
            throw new IllegalArgumentException("开始日期不能晚于结束日期");
        }
        int workDays = 0;

        // 获取开始和结束日期的年份
        int startYear = getYear(start);
        int endYear = getYear(end);

        // 如果在同一年
        if (startYear == endYear) {
            //开始日期年份天
            Integer[] daysArray = daysInYear(startYear);
            for (int i = getDayOfYearIndex(start); i <= getDayOfYearIndex(end); i++) {
                if (daysArray[i] == WORKDAY) {
                    workDays++;
                }
            }
        } else {//跨年
            // 处理开始年份
            Integer[] startYearDaysArray = daysInYear(startYear);
            int startIndex = getDayOfYearIndex(start);
            for (int i = startIndex; i < startYearDaysArray.length; i++) {
                if (startYearDaysArray[i] == WORKDAY) {
                    workDays++;
                }
            }

            // 处理结束年份
            Integer[] endYearDaysArray = daysInYear(endYear);
            int endIndex = getDayOfYearIndex(end);
            for (int i = 0; i <= endIndex; i++) {
                if (endYearDaysArray[i] == WORKDAY) {
                    workDays++;
                }
            }

            // 处理中间的完整年份
            for (int year = startYear + 1; year < endYear; year++) {
                Integer[] fullYearDaysArray = daysInYear(year);
                for (Integer integer : fullYearDaysArray) {
                    if (integer == WORKDAY) {
                        workDays++;
                    }
                }
            }
        }
        return workDays;
    }

    /**
     * 删除指定年份的数据
     */
    public static void clearData(Integer year) {
        RedisUtils.deleteObject(DAYS_IN_YEARS_KEY + year);
    }

    /**
     * 计算指定年份的工作日数组，标记个人的加班和调休日期
     *
     * @param year        指定年份
     * @param overtime    个人加班日期列表
     * @param adjustments 个人调休日期列表
     * @return 表示每一天状态的数组，0 表示工作日，1 表示休息日
     */
    public static Integer[] calculateWorkDays(Integer year, List<Date> overtime, List<Date> adjustments) {
        //获取指定年份天数
        Integer[] daysArray = daysInYear(year);
        //设置为工作日（假设用于标记补班的工作日）
        updateDaysArray(daysArray, overtime, WORKDAY);
        //设置为休息日（假设用于标记节假日）
        updateDaysArray(daysArray, adjustments, RESTDAY);
        return daysArray;
    }

    /**
     * 获取指定年份的天数数组。如果指定年份的数据不存在，则生成并缓存数据
     *
     * @param year 指定年份
     * @return 表示一年中每一天的状态数组，0 表示工作日，1 表示周末
     */
    private static Integer[] daysInYear(Integer year) {
        String key = DAYS_IN_YEARS_KEY + year;
        Integer[] daysInYears = RedisUtils.getCacheObject(key);
        if (ArrayUtil.isNotEmpty(daysInYears)) {
            return daysInYears;
        }
        LockInfo lock = LOCK_TEMPLATE.lock(key);
        try {
            // 再次检查缓存，防止缓存穿透
            daysInYears = RedisUtils.getCacheObject(key);
            if (ArrayUtil.isNotEmpty(daysInYears)) {
                return daysInYears;
            }
            // 如果年份数据不存在，则生成指定年份的数据
            Integer[] daysArray = generateDaysArray(year);
            RedisUtils.setCacheObject(key, daysArray);
            return daysArray;
        } finally {
            LOCK_TEMPLATE.releaseLock(lock);
        }
    }

    /**
     * 根据指定年份生成天数数组，其中工作日用 0 表示，周末用 1 表示
     *
     * @param year 指定年份
     * @return 表示一年中每一天的状态数组
     */
    private static Integer[] generateDaysArray(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, Calendar.JANUARY, 1);

        int daysInYear = getDaysInYear(year);
        Integer[] daysArray = new Integer[daysInYear];

        for (int i = 0; i < daysInYear; i++) {
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            daysArray[i] = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) ? RESTDAY : WORKDAY;
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        //设置为工作日（假设用于标记补班的工作日）
        updateDaysArray(daysArray, Collections.emptyList(), WORKDAY);
        //设置为休息日（假设用于标记节假日）
        updateDaysArray(daysArray, Collections.emptyList(), RESTDAY);
        return daysArray;
    }

    /**
     * 根据日期下标更新 daysArray，将对应下标的值改为指定值
     *
     * @param daysArray  原始的天数数组
     * @param days       要修改的日期列表
     * @param valueToSet 休息日用 1，工作日用 0
     */
    private static void updateDaysArray(Integer[] daysArray, List<Date> days, int valueToSet) {
        if (CollUtil.isEmpty(days)) {
            return;
        }

        // 遍历日期下标数组，将对应的 daysArray 元素设置为1
        for (Date holiday : days) {
            int index = getDayOfYearIndex(holiday);
            // 确保下标在有效范围内
            if (index >= 0 && index < daysArray.length) {
                daysArray[index] = valueToSet;
            }
        }
    }

    /**
     * 获取日期在一年中的下标（天数）
     *
     * @param date 指定日期
     * @return 日期在该年份的下标（天数）
     */
    private static int getDayOfYearIndex(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        return dayOfYear - 1;
    }

    /**
     * 获取指定日期的年份
     *
     * @param date 指定日期
     * @return 年份
     */
    private static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    /**
     * 获取指定年份的天数
     *
     * @param year 指定年份
     * @return 年份中的天数
     */
    private static int getDaysInYear(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, Calendar.DECEMBER, 31);
        return calendar.get(Calendar.DAY_OF_YEAR);
    }

}
