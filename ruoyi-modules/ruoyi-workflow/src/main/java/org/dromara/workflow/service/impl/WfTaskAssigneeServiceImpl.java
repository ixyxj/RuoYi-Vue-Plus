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
        TaskAssigneeEnum type = TaskAssigneeEnum.fromDesc(query.getHandlerType());
        TaskAssigneeBody taskQuery = BeanUtil.toBean(query, TaskAssigneeBody.class);
        List<DeptDTO> depts = new ArrayList<>();
        TaskAssigneeDTO dto = new TaskAssigneeDTO();
        if (TaskAssigneeEnum.USER == type) {
            // 处理用户相关的业务逻辑
            dto = userService.selectUsersByUserList(taskQuery);
            depts = userService.selectUsersByDeptList();
        } else if (TaskAssigneeEnum.ROLE == type) {
            // 处理角色相关的业务逻辑
            dto = userService.selectUsersByRoleList(taskQuery);
        } else if (TaskAssigneeEnum.DEPT == type) {
            // 处理部门相关的业务逻辑
            dto = userService.selectUsersByDeptList(taskQuery);
        } else if (TaskAssigneeEnum.POST == type) {
            // 处理岗位相关的业务逻辑
            dto = userService.selectUsersByPostList(taskQuery);
            depts = userService.selectUsersByDeptList();
        }

        // 业务系统机构，转成组件内部左侧树列表能够显示的数据
        TreeFunDto<DeptDTO> treeFunDto = new TreeFunDto<>(depts)
            .setId(dept -> String.valueOf(dept.getDeptId()))
            .setName(DeptDTO::getDeptName)
            .setParentId(dept -> String.valueOf(dept.getParentId()));

        // 业务系统数据，转成组件内部能够显示的数据, total是业务数据总数，用于分页显示
        HandlerFunDto<TaskAssigneeDTO.TaskHandler> handlerFunDto = new HandlerFunDto<>(dto.getList(), dto.getTotal())
            .setStorageId(assignee -> type.getCode() + assignee.getStorageId())
            .setHandlerCode(TaskAssigneeDTO.TaskHandler::getHandlerCode)
            .setHandlerName(TaskAssigneeDTO.TaskHandler::getHandlerName)
            .setGroupName(TaskAssigneeDTO.TaskHandler::getGroupName)
            .setCreateTime(assignee -> DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, assignee.getCreateTime()));

        return getHandlerSelectVo(handlerFunDto, treeFunDto);
    }

}
