package org.dromara.workflow.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.event.ProcessEvent;
import org.dromara.common.core.enums.BusinessStatusEnum;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.dromara.warm.flow.core.listener.Listener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.dromara.workflow.service.IFlwInstanceService;
import org.springframework.stereotype.Component;

/**
 * 流程完成监听器，用于处理流程结束时的业务逻辑
 * <p>
 * 该监听器通常用于处理流程完成后的相关操作，如更新业务表、记录日志、通知等
 * 可以将业务逻辑放在此监听器中，也可以在业务代码中处理，或者使用局部监听器进行监听
 * </p>
 * <p>
 * 例如，当一个流程结束时，可能需要根据流程的状态更新业务表，执行后续的处理操作
 * 或者发送通知等。此监听器可以提供统一的处理入口
 * </p>
 *
 * @author AprilWind
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class WorkflowFinishListener implements Listener {

    private final IFlwInstanceService iFlwInstanceService;

    /**
     * 流程结束监听器，用于在流程结束后执行相关的业务逻辑
     * <p>
     * 该方法会在流程完成后被触发，通常用于执行业务表的新增或更新操作
     * 或者其他与业务相关的操作（如通知发送、记录日志等）
     * </p>
     *
     * @param listenerVariable 监听器变量，包含与当前流程相关的信息
     */
    @Override
    public void notify(ListenerVariable listenerVariable) {
        log.info("流程结束监听器");
        Instance instance = listenerVariable.getInstance();
        Definition definition = listenerVariable.getDefinition();
        ProcessEvent processEvent = new ProcessEvent();
        //检查流程是否已结束
        if (FlowStatus.isFinished(instance.getFlowStatus())) {
            // 若流程已结束，更新状态为已完成
            iFlwInstanceService.updateStatus(instance.getId(), BusinessStatusEnum.FINISH.getStatus());
            // 流程结束监听，处理结束后的业务逻辑
            processEvent.setStatus(BusinessStatusEnum.FINISH.getStatus());
            processEvent.setSubmit(false);
            processEvent.setFlowCode(definition.getFlowCode());
            processEvent.setBusinessKey(instance.getBusinessId());
            SpringUtils.context().publishEvent(processEvent);
        }
        log.info("流程结束监听器结束;{}", "流程完成处理");
    }

}
