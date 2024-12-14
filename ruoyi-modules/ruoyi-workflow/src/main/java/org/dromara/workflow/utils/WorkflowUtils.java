package org.dromara.workflow.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dromara.common.core.domain.dto.UserDTO;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mail.utils.MailUtils;
import org.dromara.common.websocket.dto.WebSocketMessageDto;
import org.dromara.common.websocket.utils.WebSocketUtils;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.entity.User;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.workflow.common.enums.MessageTypeEnum;
import org.dromara.workflow.service.IFlwTaskService;
import org.dromara.workflow.service.IWfTaskAssigneeService;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 工作流工具
 *
 * @author may
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WorkflowUtils {
    public static final IWfTaskAssigneeService taskAssigneeService = SpringUtils.getBean(IWfTaskAssigneeService.class);
    public static final IFlwTaskService iFlwTaskService = SpringUtils.getBean(IFlwTaskService.class);

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
        // 获取所有用户的 UserDTO 列表
        return new ArrayList<>(userList.stream()
            .map(User::getProcessedBy)
            .filter(Objects::nonNull)
            .flatMap(processedBy -> taskAssigneeService.fetchUsersByStorageId(processedBy).stream())
            .collect(Collectors.toMap(UserDTO::getUserId, user -> user, (ex, rep) -> ex)).values());
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

    /**
     * 发送消息
     *
     * @param flowName    流程定义名称
     * @param messageType 消息类型
     * @param message     消息内容，为空则发送默认配置的消息内容
     */
    public static void sendMessage(String flowName, Long instId, List<String> messageType, String message) {
        List<UserDTO> userList = new ArrayList<>();
        List<FlowTask> list = iFlwTaskService.selectByInstId(instId);
        if (StringUtils.isBlank(message)) {
            message = "有新的【" + flowName + "】单据已经提交至您的待办，请您及时处理。";
        }
        for (Task task : list) {
            List<UserDTO> users = iFlwTaskService.currentTaskAllUser(task.getId());
            if (CollUtil.isNotEmpty(users)) {
                userList.addAll(users);
            }
        }
        if (CollUtil.isNotEmpty(userList)) {
            for (String code : messageType) {
                MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByCode(code);
                if (ObjectUtil.isNotEmpty(messageTypeEnum)) {
                    switch (messageTypeEnum) {
                        case SYSTEM_MESSAGE:
                            WebSocketMessageDto dto = new WebSocketMessageDto();
                            dto.setSessionKeys(new ArrayList<>(StreamUtils.toList(userList, UserDTO::getUserId)));
                            dto.setMessage(message);
                            WebSocketUtils.publishMessage(dto);
                            break;
                        case EMAIL_MESSAGE:
                            MailUtils.sendText(StreamUtils.join(userList, UserDTO::getEmail), "单据审批提醒", message);
                            break;
                        case SMS_MESSAGE:
                            //todo 短信发送
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + messageTypeEnum);
                    }
                }
            }
        }
    }

}
