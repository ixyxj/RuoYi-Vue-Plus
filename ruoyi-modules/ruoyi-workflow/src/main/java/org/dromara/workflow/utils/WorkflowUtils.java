package org.dromara.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.workflow.common.enums.TaskAssigneeEnum;
import org.dromara.workflow.service.IWfTaskAssigneeService;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 工作流工具
 *
 * @author may
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkflowUtils {

    /**
     * 当前用户所有权限
     *
     * @return 权限列表
     */
    public static List<String> permissionList() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        Long userId = loginUser.getUserId();
        Long deptId = loginUser.getDeptId();
        // 使用一个流来构建权限列表
        return Stream.of(
                // 角色权限前缀
                loginUser.getRoles().stream()
                    .map(role -> TaskAssigneeEnum.ROLE.getCode() + role.getRoleId()),

                // 岗位权限前缀
                Stream.ofNullable(loginUser.getPosts())
                    .flatMap(Collection::stream)
                    .map(post -> TaskAssigneeEnum.POST.getCode() + post.getPostId()),

                // 用户和部门权限
                Stream.of(
                    TaskAssigneeEnum.USER.getCode() + userId,
                    TaskAssigneeEnum.DEPT.getCode() + deptId
                )
            )
            .flatMap(stream -> stream)
            .collect(Collectors.toList());
    }

    /**
     * 获取办理人
     *
     * @param userList 办理用户
     * @return 用户
     */
    public static List<UserDTO> getHandlerUser(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return Collections.emptyList();
        }
        IWfTaskAssigneeService taskAssigneeService = SpringUtils.getBean(IWfTaskAssigneeService.class);
        // 获取所有用户的 UserDTO 列表
        return userList.stream()
            .map(User::getProcessedBy)
            .filter(Objects::nonNull)
            .flatMap(processedBy -> taskAssigneeService.fetchUsersByStorageId(processedBy).stream())
            .collect(Collectors.toList());
    }

    /**
     * 获取办理人
     *
     * @param userList 办理用户
     * @param taskId   任务ID
     * @return 用户
     */
    public static Set<User> getUser(List<User> userList, Long taskId) {
        if (CollUtil.isEmpty(userList)) {
            return Set.of();
        }
        Set<User> list = new HashSet<>();
        IWfTaskAssigneeService taskAssigneeService = SpringUtils.getBean(IWfTaskAssigneeService.class);
        for (User user : userList) {
            // 根据 processedBy 前缀判断处理人类型，分别获取用户列表
            List<UserDTO> users = taskAssigneeService.fetchUsersByStorageId(user.getProcessedBy());
            // 转换为 FlowUser 并添加到结果集合
            if (CollUtil.isNotEmpty(users)) {
                users.forEach(dto -> {
                    FlowUser flowUser = new FlowUser();
                    flowUser.setType(user.getType());
                    flowUser.setProcessedBy(String.valueOf(dto.getUserId()));
                    flowUser.setAssociated(taskId);
                    list.add(flowUser);
                });
            }
        }
        return list;
    }

}
