package org.dromara.workflow.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务状态枚举
 *
 * @author may
 */
@Getter
@AllArgsConstructor
public enum FormTypeEnum {

    /**
     * 自定义表单
     */
    STATIC("static", "自定义表单"),

    /**
     * 动态表单
     */
    DYNAMIC("dynamic", "动态表单");

    /**
     * 类型
     */
    private final String type;

    /**
     * 描述
     */
    private final String desc;

    private static final Map<String, String> TYPE_DESC_MAP = Arrays.stream(values())
        .collect(Collectors.toConcurrentMap(FormTypeEnum::getType, FormTypeEnum::getDesc));

    /**
     * 表单类型
     *
     * @param formType 表单类型
     */
    public static String findByType(String formType) {
        // 从缓存中直接获取描述
        return TYPE_DESC_MAP.getOrDefault(formType, StrUtil.EMPTY);
    }

}

