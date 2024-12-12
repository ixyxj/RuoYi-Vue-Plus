package org.dromara.workflow.listener;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.event.ProcessEvent;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.workflow.service.IFlwInstanceService;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 全局任务办理监听
 *
 * @author may
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WorkflowGlobalListener implements GlobalListener {

    private final IFlwTaskService taskService;
    private final IFlwInstanceService instanceService;

    /**
     * 创建监听器，任务创建时执行
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void create(ListenerVariable listenerVariable) {
    }

    /**
     * 开始监听器，任务开始办理时执行
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void start(ListenerVariable listenerVariable) {
    }

    /**
     * 分派监听器，动态修改代办任务信息
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void assignment(ListenerVariable listenerVariable) {
    }

    /**
     * 完成监听器，当前任务完成后执行
     *
     * @param listenerVariable 监听器变量
     */
    @Override
    public void finish(ListenerVariable listenerVariable) {
        Instance instance = listenerVariable.getInstance();
        Definition definition = listenerVariable.getDefinition();
        String businessId = instance.getBusinessId();
        String instanceFlowStatus = instance.getFlowStatus();

        // 判断流程状态
        String status = determineFlowStatus(instance, instanceFlowStatus);
        if (StringUtils.isNotBlank(status)) {
            // 如果流程状态有效，发布事件
            publishProcessEvent(status, definition.getFlowCode(), businessId);
        }
    }

    /**
     * 根据流程实例和当前流程状态确定最终状态
     *
     * @param instance           流程实例
     * @param instanceFlowStatus 流程实例当前状态
     * @return 流程最终状态
     */
    private String determineFlowStatus(Instance instance, String instanceFlowStatus) {
        if (StringUtils.isNotBlank(instanceFlowStatus) && BusinessStatusEnum.initialState(instanceFlowStatus)) {
            log.info("流程实例当前状态: {}", instanceFlowStatus);
            return instanceFlowStatus;
        } else {
            Long instanceId = instance.getId();
            List<FlowTask> flowTasks = taskService.selectByInstId(instanceId);
            if (CollUtil.isEmpty(flowTasks)) {
                String status = BusinessStatusEnum.FINISH.getStatus();
                // 更新流程状态为已完成
                instanceService.updateStatus(instanceId, status);
                log.info("流程已结束，状态更新为: {}", status);
                return status;
            }
            log.warn("流程未结束，实例ID: {}", instanceId);
            return null;
        }
    }

    /**
     * 发布事件
     *
     * @param status     状态
     * @param flowCode   流程编码
     * @param businessId 业务id
     */
    private void publishProcessEvent(String status, String flowCode, String businessId) {
        log.info("发布流程事件，流程状态: {}, 流程编码: {}, 业务ID: {}", status, flowCode, businessId);
        ProcessEvent processEvent = new ProcessEvent();
        processEvent.setStatus(status);
        processEvent.setSubmit(false);
        processEvent.setFlowCode(flowCode);
        processEvent.setBusinessKey(businessId);
        SpringUtils.context().publishEvent(processEvent);
    }

}
