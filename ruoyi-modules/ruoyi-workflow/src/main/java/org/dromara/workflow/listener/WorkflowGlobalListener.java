package org.dromara.workflow.listener;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.event.ProcessEvent;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.listener.GlobalListener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.workflow.service.IFlwInstanceService;
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

    private final FlowTaskMapper flowTaskMapper;
    private final IFlwInstanceService flwInstanceService;

    @Override
    public void finish(ListenerVariable listenerVariable) {
        Instance instance = listenerVariable.getInstance();
        Definition definition = listenerVariable.getDefinition();
        FlowParams flowParams = listenerVariable.getFlowParams();
        //撤销，退回，作废，终止发送事件
        if (BusinessStatusEnum.initialState(flowParams.getFlowStatus())) {
            publishProcessEvent(flowParams.getFlowStatus(), definition.getFlowCode(), instance.getBusinessId());
            log.info("流程监听器流程状态:{}", flowParams.getFlowStatus());
        }
        List<FlowTask> flowTasks = flowTaskMapper.selectList(new LambdaQueryWrapper<>(FlowTask.class)
            .eq(FlowTask::getInstanceId, instance.getId()));
        if (CollUtil.isEmpty(flowTasks)) {
            // 若流程已结束，更新状态为已完成并发送完成事件
            flwInstanceService.updateStatus(instance.getId(), BusinessStatusEnum.FINISH.getStatus());
            publishProcessEvent(BusinessStatusEnum.FINISH.getStatus(), definition.getFlowCode(), instance.getBusinessId());
            log.info("流程结束，流程状态:{}", BusinessStatusEnum.FINISH.getStatus());
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
        ProcessEvent processEvent = new ProcessEvent();
        processEvent.setStatus(status);
        processEvent.setSubmit(false);
        processEvent.setFlowCode(flowCode);
        processEvent.setBusinessKey(businessId);
        SpringUtils.context().publishEvent(processEvent);
    }
}
