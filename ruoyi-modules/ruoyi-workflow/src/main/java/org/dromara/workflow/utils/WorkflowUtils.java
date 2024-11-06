package org.dromara.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.entity.User;
import com.warm.flow.orm.entity.FlowUser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.domain.dto.RoleDTO;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.service.UserService;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;

import java.util.*;

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
        List<RoleDTO> roles = LoginHelper.getLoginUser().getRoles();
        Long deptId = LoginHelper.getDeptId();
        List<String> permissionList = StreamUtils.toList(roles, role -> "role:" + role.getRoleId());
        permissionList.add(LoginHelper.getUserIdStr());
        permissionList.add("dept:" + deptId);
        return permissionList;
    }

    /**
     * 获取办理人
     *
     * @param userList 办理用户
     * @return 用户
     */
    public static List<UserDTO> getHandlerUser(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return List.of();
        }
        UserService userService = SpringUtils.getBean(UserService.class);
        List<Long> userIds = new ArrayList<>();
        List<Long> roleIds = new ArrayList<>();
        List<Long> deptIds = new ArrayList<>();
        for (User user : userList) {
            String processedBy = user.getProcessedBy();
            Long id = Long.valueOf(StringUtils.substringAfter(processedBy, StrUtil.C_COLON));
            if (processedBy.startsWith("user:")) {
                userIds.add(id);
            } else if (processedBy.startsWith("role:")) {
                roleIds.add(id);
            } else if (processedBy.startsWith("dept:")) {
                deptIds.add(id);
            } else {
                userIds.add(Long.valueOf(processedBy));
            }
        }
        // 合并不同类型用户
        List<UserDTO> userDTOList = new ArrayList<>(userService.selectListByIds(userIds));
        userDTOList.addAll(userService.selectUsersByRoleIds(roleIds));
        userDTOList.addAll(userService.selectUsersByDeptIds(deptIds));
        return userDTOList;
    }

    /**
     * 获取办理人
     *
     * @param userList 办理用户
     * @return 用户
     */
    public static Set<User> getUser(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return Set.of();
        }
        Set<User> list = new HashSet<>();
        UserService userService = SpringUtils.getBean(UserService.class);
        for (User user : userList) {
            // 根据 processedBy 前缀判断处理人类型，分别获取用户列表
            List<UserDTO> users = getAssociatedUsers(userService, user);
            // 转换为 FlowUser 并添加到结果集合
            if (CollUtil.isNotEmpty(users)) {
                users.forEach(dto -> {
                    FlowUser flowUser = new FlowUser();
                    flowUser.setType(user.getType());
                    flowUser.setProcessedBy(String.valueOf(dto.getUserId()));
                    list.add(flowUser);
                });
            }
        }
        return list;
    }

    /**
     * 根据用户的 `processedBy` 前缀（user、role、dept）获取关联的用户列表
     *
     * @param userService 用户服务，用于从数据库中查询用户信息
     * @param user        办理用户实例，通过 `processedBy` 字段识别处理人类型
     * @return 返回符合条件的用户DTO列表，如果未找到匹配的前缀则返回空列表
     */
    private static List<UserDTO> getAssociatedUsers(UserService userService, User user) {
        String processedBy = user.getProcessedBy();
        // 提取 processedBy 字段中 ":" 后的部分作为ID
        Long id = Long.valueOf(StringUtils.substringAfter(processedBy, StrUtil.C_COLON));

        if (processedBy.startsWith("user:")) {
            // 如果前缀为 "user:"，根据用户ID查询
            return userService.selectListByIds(List.of(id));
        } else if (processedBy.startsWith("role:")) {
            // 如果前缀为 "role:"，根据角色ID查询用户
            return userService.selectUsersByRoleIds(List.of(id));
        } else if (processedBy.startsWith("dept:")) {
            // 如果前缀为 "dept:"，根据部门ID查询用户
            return userService.selectUsersByDeptIds(List.of(id));
        }
        // 未匹配任何前缀，返回空列表
        return Collections.emptyList();
    }

}
