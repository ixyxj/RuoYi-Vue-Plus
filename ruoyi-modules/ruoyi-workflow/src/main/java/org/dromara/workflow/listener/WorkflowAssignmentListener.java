package org.dromara.workflow.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.warm.flow.core.constant.FlowCons;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.listener.Listener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.dromara.workflow.service.impl.WfTaskAssigneeServiceImpl;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 分派办理人监听器，动态修改代办任务信息
 *
 * @author AprilWind
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class WorkflowAssignmentListener implements Listener {

    private final WfTaskAssigneeServiceImpl assigneeService;

    @Override
    public void notify(ListenerVariable listenerVariable) {
        log.info("分派办理人监听器开始执行......");
        List<Task> tasks = listenerVariable.getNextTasks();
        Instance instance = listenerVariable.getInstance();
        for (Task task : tasks) {
            List<String> permissionList = task.getPermissionList();
            // 记录待添加的权限项
            List<String> toAddPermissions = new ArrayList<>();
            // 使用迭代器来避免直接删除元素
            Iterator<String> iterator = permissionList.iterator();
            while (iterator.hasNext()) {
                String permission = iterator.next();
                // 替换发起人审批权限
                if (StringUtils.isNotEmpty(permission) && permission.contains(FlowCons.WARMFLOWINITIATOR)) {
                    iterator.remove();
                    permissionList.add(permission.replace(FlowCons.WARMFLOWINITIATOR, instance.getCreateBy()));
                } else {
                    // 获取办理人
                    List<UserDTO> users = assigneeService.fetchUsersByStorageId(permission);
                    if (!users.isEmpty()) {
                        // 移除当前权限并替换成多个用户的权限
                        iterator.remove();
                        for (UserDTO user : users) {
                            toAddPermissions.add(String.valueOf(user.getUserId()));
                        }
                    }
                }
            }
            // 添加新权限项
            permissionList.addAll(toAddPermissions);
        }
        log.info("分派办理人监听器执行结束......");
    }

}
