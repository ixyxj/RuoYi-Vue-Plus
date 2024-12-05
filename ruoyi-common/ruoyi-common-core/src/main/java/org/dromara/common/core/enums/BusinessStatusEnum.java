package org.dromara.common.core.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 业务状态枚举
 *
 * @author may
 */
@Getter
@AllArgsConstructor
public enum BusinessStatusEnum {

    /**
     * 已撤销
     */
    CANCEL("cancel", "已撤销"),

    /**
     * 草稿
     */
    DRAFT("draft", "草稿"),

    /**
     * 待审核
     */
    WAITING("waiting", "待审核"),

    /**
     * 已完成
     */
    FINISH("finish", "已完成"),
    /**
     * 已作废
     */
    INVALID("invalid", "已作废"),

    /**
     * 已退回
     */
    BACK("back", "已退回"),

    /**
     * 已终止
     */
    TERMINATION("termination", "已终止");

    /**
     * 状态
     */
    private final String status;

    /**
     * 描述
     */
    private final String desc;

    private static final Map<String, BusinessStatusEnum> STATUS_MAP = Arrays.stream(BusinessStatusEnum.values())
        .collect(Collectors.toConcurrentMap(BusinessStatusEnum::getStatus, Function.identity()));

    /**
     * 根据状态获取对应的 BusinessStatusEnum 枚举
     *
     * @param status 业务状态码
     * @return 对应的 BusinessStatusEnum 枚举，如果找不到则返回 null
     */
    public static BusinessStatusEnum getByStatus(String status) {
        // 使用 STATUS_MAP 获取对应的枚举，若找不到则返回 null
        return STATUS_MAP.get(status);
    }

    /**
     * 根据状态获取对应的业务状态描述信息
     *
     * @param status 业务状态码
     * @return 返回业务状态描述，若状态码为空或未找到对应的枚举，返回空字符串
     */
    public static String findByStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return StrUtil.EMPTY;
        }
        BusinessStatusEnum statusEnum = STATUS_MAP.get(status);
        return (statusEnum != null) ? statusEnum.getDesc() : StrUtil.EMPTY;
    }

    /**
     * 判断是否为指定的状态之一：草稿、已撤销或已退回
     *
     * @param status 要检查的状态
     * @return 如果状态为草稿、已撤销或已退回之一，则返回 true；否则返回 false
     */
    public static boolean isDraftOrCancelOrBack(String status) {
        return DRAFT.status.equals(status) || CANCEL.status.equals(status) || BACK.status.equals(status);
    }

    /**
     * 运行中的实例状态
     *
     * @return 运行中的实例状态
     */
    public static List<String> runningStatus() {
        List<String> list = new ArrayList<>();
        list.add(BusinessStatusEnum.DRAFT.getStatus());
        list.add(BusinessStatusEnum.WAITING.getStatus());
        list.add(BusinessStatusEnum.BACK.getStatus());
        list.add(BusinessStatusEnum.CANCEL.getStatus());
        return list;
    }

    /**
     * 结束实例状态
     *
     * @return 结束实例状态
     */
    public static List<String> finishStatus() {
        List<String> list = new ArrayList<>();
        list.add(BusinessStatusEnum.FINISH.getStatus());
        list.add(BusinessStatusEnum.INVALID.getStatus());
        list.add(BusinessStatusEnum.TERMINATION.getStatus());
        return list;
    }

}
