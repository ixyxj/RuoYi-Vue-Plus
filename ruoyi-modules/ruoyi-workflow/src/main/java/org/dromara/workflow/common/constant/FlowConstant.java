package org.dromara.workflow.common.constant;


/**
 * 工作流常量
 *
 * @author may
 */
public interface FlowConstant {

    /**
     * 流程发起人
     */
    String INITIATOR = "initiator";

    /**
     * 流程实例id
     */
    String PROCESS_INSTANCE_ID = "processInstanceId";

    /**
     * 业务id
     */
    String BUSINESS_KEY = "businessKey";

    /**
     * 任务id
     */
    String TASK_ID = "taskId";

    /**
     * 委托
     */
    String DELEGATE_TASK = "delegateTask";

    /**
     * 转办
     */
    String TRANSFER_TASK = "transferTask";

    /**
     * 加签
     */
    String ADD_SIGNATURE = "addSignature";

    /**
     * 减签
     */
    String REDUCTION_SIGNATURE = "reductionSignature";

}
