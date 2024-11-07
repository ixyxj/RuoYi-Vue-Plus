package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.warm.flow.ui.dto.HandlerFunDto;
import com.warm.flow.ui.dto.HandlerQuery;
import com.warm.flow.ui.dto.TreeFunDto;
import com.warm.flow.ui.service.HandlerSelectService;
import com.warm.flow.ui.vo.HandlerSelectVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.DeptDTO;
import org.dromara.common.core.domain.dto.TaskAssigneeDTO;
import org.dromara.common.core.domain.model.TaskAssigneeBody;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.service.DeptService;
import org.dromara.common.core.service.PostService;
import org.dromara.common.core.service.RoleService;
import org.dromara.common.core.service.UserService;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.workflow.common.enums.TaskAssigneeEnum;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 流程设计器-获取办理人权限设置列表
 *
 * @author AprilWind
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WfTaskAssigneeServiceImpl implements HandlerSelectService {
    private final UserService userService;
    private final DeptService deptService;
    private final RoleService roleService;
    private final PostService postService;

    /**
     * 获取办理人权限设置列表tabs页签
     *
     * @return tabs页签
     */
    @Override
    public List<String> getHandlerType() {
        return TaskAssigneeEnum.getAssigneeTypeList();
    }

    /**
     * 获取办理列表, 同时构建左侧部门树状结构
     *
     * @param query 查询条件
     * @return HandlerSelectVo
     */
    @Override
    public HandlerSelectVo getHandlerSelect(HandlerQuery query) {
        // 获取任务办理类型
        TaskAssigneeEnum type = TaskAssigneeEnum.fromDesc(query.getHandlerType());
        // 转换查询条件为 TaskAssigneeBody
        TaskAssigneeBody taskQuery = BeanUtil.toBean(query, TaskAssigneeBody.class);

        // 统一查询并构建业务数据
        TaskAssigneeDTO dto = fetchTaskAssigneeData(type, taskQuery);
        List<DeptDTO> depts = fetchDeptData(type);

        return getHandlerSelectVo(buildHandlerData(dto, type), buildDeptTree(depts));
    }

    /**
     * 根据任务办理类型查询对应的数据
     */
    private TaskAssigneeDTO fetchTaskAssigneeData(TaskAssigneeEnum type, TaskAssigneeBody taskQuery) {
        return switch (type) {
            case USER -> userService.selectUsersByTaskAssigneeList(taskQuery);
            case ROLE -> roleService.selectRolesByTaskAssigneeList(taskQuery);
            case DEPT -> deptService.selectDeptsByTaskAssigneeList(taskQuery);
            case POST -> postService.selectPostsByTaskAssigneeList(taskQuery);
            default -> throw new ServiceException("Unsupported handler type");
        };
    }

    /**
     * 根据任务办理类型获取部门数据
     */
    private List<DeptDTO> fetchDeptData(TaskAssigneeEnum type) {
        if (type == TaskAssigneeEnum.USER || type == TaskAssigneeEnum.POST) {
            return deptService.selectDeptsByList();
        }
        return new ArrayList<>();
    }

    /**
     * 构建部门树状结构
     */
    private TreeFunDto<DeptDTO> buildDeptTree(List<DeptDTO> depts) {
        return new TreeFunDto<>(depts)
            .setId(dept -> String.valueOf(dept.getDeptId()))
            .setName(DeptDTO::getDeptName)
            .setParentId(dept -> String.valueOf(dept.getParentId()));
    }

    /**
     * 构建任务办理人数据
     */
    private HandlerFunDto<TaskAssigneeDTO.TaskHandler> buildHandlerData(TaskAssigneeDTO dto, TaskAssigneeEnum type) {
        return new HandlerFunDto<>(dto.getList(), dto.getTotal())
            .setStorageId(assignee -> type.getCode() + assignee.getStorageId())
            .setHandlerCode(TaskAssigneeDTO.TaskHandler::getHandlerCode)
            .setHandlerName(TaskAssigneeDTO.TaskHandler::getHandlerName)
            .setGroupName(TaskAssigneeDTO.TaskHandler::getGroupName)
            .setCreateTime(assignee -> DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, assignee.getCreateTime()));
    }

}
