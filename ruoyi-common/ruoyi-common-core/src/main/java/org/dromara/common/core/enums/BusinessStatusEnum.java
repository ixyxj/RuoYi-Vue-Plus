package org.dromara.common.core.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;

import java.util.Arrays;

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

    /**
     * 获取业务状态
     *
     * @param status 状态
     */
    public static String findByStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return StrUtil.EMPTY;
        }
        return Arrays.stream(BusinessStatusEnum.values())
            .filter(statusEnum -> statusEnum.getStatus().equals(status))
            .findFirst()
            .map(BusinessStatusEnum::getDesc)
            .orElse(StrUtil.EMPTY);
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
     * 启动流程校验
     *
     * @param status 状态
     */
    public static void checkStartStatus(String status) {
        if (WAITING.getStatus().equals(status)) {
            throw new ServiceException("该单据已提交过申请,正在审批中！");
        } else if (FINISH.getStatus().equals(status)) {
            throw new ServiceException("该单据已完成申请！");
        } else if (INVALID.getStatus().equals(status)) {
            throw new ServiceException("该单据已作废！");
        } else if (TERMINATION.getStatus().equals(status)) {
            throw new ServiceException("该单据已终止！");
        } else if (StringUtils.isBlank(status)) {
            throw new ServiceException("流程状态为空！");
        }
    }

    /**
     * 撤销流程校验
     *
     * @param status 状态
     */
    public static void checkCancelStatus(String status) {
        if (CANCEL.getStatus().equals(status)) {
            throw new ServiceException("该单据已撤销！");
        } else if (FINISH.getStatus().equals(status)) {
            throw new ServiceException("该单据已完成申请！");
        } else if (INVALID.getStatus().equals(status)) {
            throw new ServiceException("该单据已作废！");
        } else if (TERMINATION.getStatus().equals(status)) {
            throw new ServiceException("该单据已终止！");
        } else if (BACK.getStatus().equals(status)) {
            throw new ServiceException("该单据已退回！");
        } else if (StringUtils.isBlank(status)) {
            throw new ServiceException("流程状态为空！");
        }
    }

    /**
     * 驳回流程校验
     *
     * @param status 状态
     */
    public static void checkBackStatus(String status) {
        if (BACK.getStatus().equals(status)) {
            throw new ServiceException("该单据已退回！");
        } else if (FINISH.getStatus().equals(status)) {
            throw new ServiceException("该单据已完成申请！");
        } else if (INVALID.getStatus().equals(status)) {
            throw new ServiceException("该单据已作废！");
        } else if (TERMINATION.getStatus().equals(status)) {
            throw new ServiceException("该单据已终止！");
        } else if (CANCEL.getStatus().equals(status)) {
            throw new ServiceException("该单据已撤销！");
        } else if (StringUtils.isBlank(status)) {
            throw new ServiceException("流程状态为空！");
        }
    }

    /**
     * 作废,终止流程校验
     *
     * @param status 状态
     */
    public static void checkInvalidStatus(String status) {
        if (FINISH.getStatus().equals(status)) {
            throw new ServiceException("该单据已完成申请！");
        } else if (INVALID.getStatus().equals(status)) {
            throw new ServiceException("该单据已作废！");
        } else if (TERMINATION.getStatus().equals(status)) {
            throw new ServiceException("该单据已终止！");
        } else if (StringUtils.isBlank(status)) {
            throw new ServiceException("流程状态为空！");
        }
    }
}

