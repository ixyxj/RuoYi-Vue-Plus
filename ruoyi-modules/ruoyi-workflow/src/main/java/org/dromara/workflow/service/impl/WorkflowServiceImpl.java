package org.dromara.workflow.service.impl;

import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.service.WorkflowService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.workflow.service.IFlwInstanceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务实现
 *
 * @author may
 */
@RequiredArgsConstructor
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final IFlwInstanceService flwInstanceService;

    /**
     * 删除流程实例
     *
     * @param businessKeys 业务id
     * @return 结果
     */
    @Override
    public boolean deleteInstance(List<Long> businessKeys) {
        return flwInstanceService.deleteByBusinessIds(businessKeys);
    }

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    @Override
    public String getBusinessStatusByTaskId(Long taskId) {
        FlowInstance flowInstance = flwInstanceService.selectByTaskId(taskId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 获取当前流程状态
     *
     * @param businessId 业务id
     */
    @Override
    public String getBusinessStatus(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getFlowStatus() : StringUtils.EMPTY;
    }

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例id
     * @param variables  流程变量
     */
    @Override
    public void setVariable(Long instanceId, Map<String, Object> variables) {
        flwInstanceService.setVariable(instanceId, variables);
    }

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessId 业务id
     * @return 结果
     */
    @Override
    public Long getInstanceIdByBusinessKey(String businessId) {
        FlowInstance flowInstance = flwInstanceService.selectInstByBusinessId(businessId);
        return ObjectUtil.isNotNull(flowInstance) ? flowInstance.getId() : null;
    }
}
