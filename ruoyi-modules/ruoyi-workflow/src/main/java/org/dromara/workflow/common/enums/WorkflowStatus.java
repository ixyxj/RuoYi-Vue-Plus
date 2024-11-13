package org.dromara.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.dromara.warm.flow.core.utils.ObjectUtil;

/**
 * 工作流状态
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum WorkflowStatus {

    /**
     * 待提交状态
     * 流程还未提交，处于待提交状态
     */
    TOBESUBMIT("0", "待提交", "submitted"),

    /**
     * 审批中状态
     * 流程正在审批过程中，处于审批中状态
     */
    APPROVAL("1", "审批中", "approving"),

    /**
     * 审批通过状态
     * 流程已通过审批，处于审批通过状态
     */
    PASS("2", "审批通过", "passed"),

    /**
     * 自动完成状态
     * 流程已自动完成，通常不需要人工干预
     */
    AUTO_PASS("3", "自动完成", "auto"),

    /**
     * 终止状态
     * 流程已终止，不能继续执行
     */
    TERMINATE("4", "终止", "terminated"),

    /**
     * 作废状态
     * 流程被作废，已不再有效
     */
    NULLIFY("5", "作废", "nullified"),

    /**
     * 撤销状态
     * 流程已被撤销，撤销后流程无法继续
     */
    CANCEL("6", "撤销", "cancelled"),

    /**
     * 取回状态
     * 流程被取回，通常是审批被暂停，重新操作后可继续
     */
    RETRIEVE("7", "取回", "retrieved"),

    /**
     * 已完成状态
     * 流程已全部完成，且所有操作已结束
     */
    FINISHED("8", "已完成", "done"),

    /**
     * 已退回状态
     * 流程被退回，通常是审批不通过或者需要重新处理
     */
    REJECT("9", "已退回", "rejected"),

    /**
     * 失效状态
     * 流程已失效，不再有效，不能继续执行
     */
    INVALID("10", "失效", "invalid");

    /**
     * 唯一标识符
     */
    private final String key;

    /**
     * 中文描述
     */
    private final String value;

    /**
     * 状态
     */
    private final String status;

    public static WorkflowStatus getByKey(String key) {
        for (WorkflowStatus workflowStatus : WorkflowStatus.values()) {
            if (workflowStatus.getKey().equals(key)) {
                return workflowStatus;
            }
        }
        return null;
    }

    public static WorkflowStatus getByValue(String value) {
        for (WorkflowStatus workflowStatus : WorkflowStatus.values()) {
            if (workflowStatus.getValue().equals(value)) {
                return workflowStatus;
            }
        }
        return null;
    }

    public static WorkflowStatus getByStatus(String status) {
        for (WorkflowStatus workflowStatus : WorkflowStatus.values()) {
            if (workflowStatus.getStatus().equals(status)) {
                return workflowStatus;
            }
        }
        return null;
    }

    public static Boolean isFinished(String key) {
        return ObjectUtil.isNotNull(key) && (WorkflowStatus.FINISHED.getKey().equals(key));
    }

    public static Boolean isFinished(WorkflowStatus workflowStatus) {
        return ObjectUtil.isNotNull(workflowStatus) && (WorkflowStatus.FINISHED == workflowStatus);
    }
}
