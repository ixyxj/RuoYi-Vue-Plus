package org.dromara.workflow.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.model.LoginUser;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.listener.Listener;
import org.dromara.warm.flow.core.listener.ListenerVariable;
import org.dromara.common.core.enums.TaskAssigneeEnum;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 流程启动监听器，用于处理流程开始时的用户信息和权限设置
 * <p>
 * 该监听器在流程启动阶段执行，主要任务是获取当前登录用户的信息，并为流程设置办理人ID及其相关权限
 * 它通过监听器变量从流程中获取参数，并设置当前用户的权限信息，如角色、岗位、用户ID和部门ID等
 * 该监听器可以避免重复编写相同的权限设置逻辑，简化流程的启动配置
 * </p>
 *
 * @author AprilWind
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class WorkflowStartListener implements Listener {

    /**
     * 全局开始监听器，用于在流程开始时，设置当前办理人的信息和权限
     * <p>
     * 此方法通过监听器变量获取流程参数，获取当前登录用户，并设置当前办理人的ID和其权限信息
     * 该监听器可以用于流程的开始阶段，避免重复设置办理人信息和权限
     * </p>
     *
     * @param listenerVariable 监听器变量，包含流程参数等信息
     */
    @Override
    public void notify(ListenerVariable listenerVariable) {
        log.info("流程启动监听器");
        FlowParams flowParams = listenerVariable.getFlowParams();
        LoginUser loginUser = LoginHelper.getLoginUser();
        // 设置当前办理人id
        flowParams.handler(loginUser.getUserId().toString());
        // 设置办理人所拥有的权限，比如角色、部门、用户等
        flowParams.permissionFlag(buildUserPermissions(loginUser));
        log.info("流程启动监听器结束;{}", "开启流程完成");
    }

    /**
     * 构建当前用户的所有权限列表，包括角色、岗位、用户和部门权限
     * <p>
     * 通过合并用户的角色、岗位、用户ID和部门ID来构建用户的权限列表，格式化为字符串列表
     * </p>
     *
     * @param loginUser 当前登录的用户对象，包含用户的角色、岗位、ID等信息
     * @return 权限列表，包含角色、岗位、用户和部门的权限，格式化为字符串列表
     */
    private List<String> buildUserPermissions(LoginUser loginUser) {
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
                    TaskAssigneeEnum.USER.getCode() + loginUser.getUserId(),
                    TaskAssigneeEnum.DEPT.getCode() + loginUser.getDeptId()
                )
            )
            .flatMap(stream -> stream)
            .collect(Collectors.toList());
    }

}
