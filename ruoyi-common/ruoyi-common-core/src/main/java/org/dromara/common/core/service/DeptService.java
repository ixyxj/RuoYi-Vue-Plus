package org.dromara.common.core.service;

import org.dromara.common.core.domain.dto.DeptDTO;
import org.dromara.common.core.domain.dto.TaskAssigneeDTO;
import org.dromara.common.core.domain.model.TaskAssigneeBody;

import java.util.List;

/**
 * 通用 部门服务
 *
 * @author Lion Li
 */
public interface DeptService {

    /**
     * 通过部门ID查询部门名称
     *
     * @param deptIds 部门ID串逗号分隔
     * @return 部门名称串逗号分隔
     */
    String selectDeptNameByIds(String deptIds);

    /**
     * 查询部门
     *
     * @return 部门列表
     */
    List<DeptDTO> selectDeptsByList();

    /**
     * 查询部门并返回任务指派的列表，支持分页
     *
     * @param taskQuery 查询条件
     * @return 办理人
     */
    TaskAssigneeDTO selectDeptsByTaskAssigneeList(TaskAssigneeBody taskQuery);

}
