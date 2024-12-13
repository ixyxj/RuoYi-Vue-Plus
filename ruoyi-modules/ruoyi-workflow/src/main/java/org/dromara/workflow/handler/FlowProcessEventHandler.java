package org.dromara.workflow.handler;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.event.ProcessEvent;
import org.dromara.common.core.domain.event.ProcessTaskEvent;
import org.dromara.common.core.utils.SpringUtils;
import org.springframework.stereotype.Component;

/**
 * 流程监听服务
 *
 * @author may
 * @date 2024-06-02
 */
@Slf4j
@Component
public class FlowProcessEventHandler {

    /**
     * 总体流程监听(例如: 提交 退回 撤销 终止 作废等)
     *
     * @param flowCode    流程定义编码
     * @param businessKey 业务id
     * @param status      状态
     * @param submit      当为true时为申请人节点办理
     */
    public void processHandler(String flowCode, String businessKey, String status, boolean submit) {
        log.info("发布流程事件，流程状态: {}, 流程编码: {}, 业务ID: {}", status, flowCode, businessKey);
        ProcessEvent processEvent = new ProcessEvent();
        processEvent.setFlowCode(flowCode);
        processEvent.setBusinessKey(businessKey);
        processEvent.setStatus(status);
        processEvent.setSubmit(submit);
        SpringUtils.context().publishEvent(processEvent);
    }

    /**
     * 执行办理任务监听
     *
     * @param flowCode    流程定义编码
     * @param nodeCode    审批节点编码
     * @param taskId      任务id
     * @param businessKey 业务id
     */
    public void processTaskHandler(String flowCode, String nodeCode, Long taskId, String businessKey) {
        ProcessTaskEvent processTaskEvent = new ProcessTaskEvent();
        processTaskEvent.setFlowCode(flowCode);
        processTaskEvent.setNodeCode(nodeCode);
        processTaskEvent.setTaskId(taskId);
        processTaskEvent.setBusinessKey(businessKey);
        SpringUtils.context().publishEvent(processTaskEvent);
    }
}
