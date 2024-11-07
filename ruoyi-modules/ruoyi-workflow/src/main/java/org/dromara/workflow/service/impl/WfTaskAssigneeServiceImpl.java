package org.dromara.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.warm.flow.ui.dto.HandlerFunDto;
import com.warm.flow.ui.dto.HandlerQuery;
import com.warm.flow.ui.service.HandlerSelectService;
import com.warm.flow.ui.vo.HandlerSelectVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.TaskAssigneeDTO;
import org.dromara.common.core.domain.model.TaskAssigneeBody;
import org.dromara.common.core.service.UserService;
import org.dromara.common.core.utils.DateUtils;
import org.dromara.workflow.common.enums.TaskAssigneeEnum;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.dromara.workflow.common.enums.TaskAssigneeEnum.ROLE;

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
     * 获取用户列表, 同时构建左侧部门树状结构
     *
     * @param query 查询条件
     * @return HandlerSelectVo
     */
    @Override
    public HandlerSelectVo getHandlerSelect(HandlerQuery query) {
        TaskAssigneeEnum type = TaskAssigneeEnum.fromDesc(query.getHandlerType());
        TaskAssigneeBody taskQuery = BeanUtil.toBean(query, TaskAssigneeBody.class);
        TaskAssigneeDTO dto = new TaskAssigneeDTO();
        if (TaskAssigneeEnum.USER == type) {
            // 处理用户相关的业务逻辑
        } else if (ROLE == type) {
            // 处理角色相关的业务逻辑
            dto = userService.selectUsersByRoleList(taskQuery);
        } else if (TaskAssigneeEnum.DEPT == type) {
            // 处理部门相关的业务逻辑
        } else if (TaskAssigneeEnum.POST == type) {
            // 处理岗位相关的业务逻辑
        }
        // 业务系统数据，转成组件内部能够显示的数据, total是业务数据总数，用于分页显示
        HandlerFunDto<TaskAssigneeDTO.TaskHandler> handlerFunDto = new HandlerFunDto<>(dto.getList(), dto.getTotal())
            .setStorageId(assignee -> ROLE.getCode() + assignee.getStorageId())
            .setHandlerCode(TaskAssigneeDTO.TaskHandler::getHandlerCode)
            .setHandlerName(TaskAssigneeDTO.TaskHandler::getHandlerName)
            .setGroupName(TaskAssigneeDTO.TaskHandler::getGroupName)
            .setCreateTime(assignee -> DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, assignee.getCreateTime()));
        return getHandlerSelectVo(handlerFunDto);
    }

//    /**
//     * 获取用户列表
//     *
//     * @param query 查询条件
//     * @return HandlerSelectVo
//     */
//    private HandlerSelectVo getDept(HandlerQuery query) {
//        // 查询部门列表
//        List<SysDept> deptList = userService.selectDeptList(sysDept);
//        long total = new PageInfo<>(deptList).getTotal();
//
//        // 业务系统数据，转成组件内部能够显示的数据, total是业务数据总数，用于分页显示
//        HandlerFunDto<SysDept> handlerFunDto = new HandlerFunDto<>(deptList, total)
//            .setStorageId(dept -> "dept:" + dept.getDeptId()) // 前面拼接dept:  是为了防止用户、部门的主键重复
//            .setHandlerName(SysDept::getDeptName) // 权限名称
//            .setCreateTime(dept -> DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, dept.getCreateTime()));
//
//        return getHandlerSelectVo(handlerFunDto);
//
//    }
//
//    /**
//     * 获取用户列表, 同时构建左侧部门树状结构
//     *
//     * @param query 查询条件
//     * @return HandlerSelectVo
//     */
//    private HandlerSelectVo getUser(HandlerQuery query) {
//        ......
//        // 查询用户列表
//        List<SysUser> userList = userService.selectUserList(sysUser);
//        long total = new PageInfo<>(userList).getTotal();
//        // 查询部门列表，构建树状结构
//        List<SysDept> deptList = deptMapper.selectDeptList(new SysDept());
//
//        // 业务系统数据，转成组件内部能够显示的数据, total是业务数据总数，用于分页显示
//        HandlerFunDto<SysUser> handlerFunDto = new HandlerFunDto<>(userList, total)
//            .setStorageId(user -> user.getUserId().toString())
//            .setHandlerCode(SysUser::getUserName) // 权限编码
//            .setHandlerName(SysUser::getNickName) // 权限名称
//            .setCreateTime(user -> DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, user.getCreateTime()))
//            .setGroupName(user -> user.getDept() != null ? user.getDept().getDeptName() : "");
//
//        // 业务系统机构，转成组件内部左侧树列表能够显示的数据
//        TreeFunDto<SysDept> treeFunDto = new TreeFunDto<>(deptList)
//            .setId(dept -> dept.getDeptId().toString()) // 左侧树ID
//            .setName(SysDept::getDeptName) // 左侧树名称
//            .setParentId(dept -> dept.getParentId().toString()); // 左侧树父级ID
//
//        return getHandlerSelectVo(handlerFunDto, treeFunDto);
//    }

}
