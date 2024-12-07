package org.dromara.common.core.service;

import java.util.List;
import java.util.Map;

/**
 * 通用 工作流服务
 *
 * @author may
 */
public interface WorkflowService {

    /**
     * 运行中的实例 删除程实例，删除历史记录，删除业务与流程关联信息
     *
     * @param businessKeys 业务id
     * @return 结果
     */
    boolean deleteInstance(List<Long> businessKeys);

    /**
     * 获取当前流程状态
     *
     * @param taskId 任务id
     */
    String getBusinessStatusByTaskId(Long taskId);

    /**
     * 获取当前流程状态
     *
     * @param businessKey 业务id
     */
    String getBusinessStatus(String businessKey);

    /**
     * 设置流程变量
     *
     * @param instanceId 流程实例id
     * @param variable   流程变量
     */
    void setVariable(Long instanceId, Map<String, Object> variable);

    /**
     * 按照业务id查询流程实例id
     *
     * @param businessKey 业务id
     * @return 结果
     */
    Long getInstanceIdByBusinessKey(String businessKey);
}
