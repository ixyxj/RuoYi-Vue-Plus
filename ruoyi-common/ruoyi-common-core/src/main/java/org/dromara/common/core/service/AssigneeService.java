package org.dromara.common.core.service;

import org.dromara.common.core.domain.dto.UserDTO;

import java.util.List;

/**
 * 通用 办理人服务
 *
 * @author AprilWind
 */
public interface AssigneeService {

    /**
     * 通过taskId查询对应的任务办理人
     *
     * @param taskIds taskId串逗号分隔
     * @return 任务办理人名称串逗号分隔
     */
    String selectAssigneeByIds(String taskIds);

    /**
     * 通过taskId查询对应的任务办理人列表
     *
     * @param taskIdList 任务id
     * @return 列表
     */
    List<UserDTO> selectByIds(List<Long> taskIdList);

}
