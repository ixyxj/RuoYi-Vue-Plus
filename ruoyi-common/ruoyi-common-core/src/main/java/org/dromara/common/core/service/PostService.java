package org.dromara.common.core.service;

import org.dromara.common.core.domain.dto.TaskAssigneeDTO;
import org.dromara.common.core.domain.model.TaskAssigneeBody;

import java.util.List;

/**
 * 通用 岗位服务
 *
 * @author AprilWind
 */
public interface PostService {

    /**
     * 根据用户 ID 查询其所属的岗位 ID 列表
     *
     * @param userId 用户 ID，用于确定用户所属的岗位
     * @return 与该用户关联的岗位 ID 列表，如果未找到则返回空列表
     */
    List<Long> selectPostIdByUserIdList(Long userId);

    /**
     * 查询岗位并返回任务指派的列表，支持分页
     *
     * @param taskQuery 查询条件
     * @return 办理人
     */
    TaskAssigneeDTO selectPostsByTaskAssigneeList(TaskAssigneeBody taskQuery);

}
