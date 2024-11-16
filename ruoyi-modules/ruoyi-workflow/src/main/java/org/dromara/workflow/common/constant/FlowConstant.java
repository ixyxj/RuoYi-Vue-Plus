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
     * 模型标识key命名规范正则表达式
     */
    String MODEL_KEY_PATTERN = "^[a-zA-Z][a-zA-Z0-9_]{0,254}$";
}
