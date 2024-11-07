package org.dromara.common.core.service;

import org.dromara.common.core.domain.dto.TaskAssigneeDTO;
import org.dromara.common.core.domain.model.TaskAssigneeBody;

/**
 * 通用 角色服务
 *
 * @author AprilWind
 */
public interface RoleService {

    /**
     * 查询角色并返回任务指派的列表，支持分页
     *
     * @param taskQuery 查询条件
     * @return 办理人
     */
    TaskAssigneeDTO selectRolesByTaskAssigneeList(TaskAssigneeBody taskQuery);

}
