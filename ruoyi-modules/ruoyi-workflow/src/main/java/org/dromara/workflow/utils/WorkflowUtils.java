package org.dromara.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.workflow.service.IWfTaskAssigneeService;

import java.util.*;
import java.util.stream.Collectors;

import static org.dromara.common.core.enums.TaskAssigneeEnum.USER;

/**
 * 工作流工具
 *
 * @author may
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkflowUtils {

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
                    flowUser.setProcessedBy(USER.getCode()+dto.getUserId());
                    flowUser.setAssociated(taskId);
                    list.add(flowUser);
                });
            }
        }
        return list;
    }

}
