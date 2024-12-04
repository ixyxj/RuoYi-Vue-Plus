package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.dromara.common.core.enums.TaskAssigneeEnum.USER;

/**
 * 任务操作业务对象，用于描述任务委派、转办、加签等操作的必要参数
 * 包含了用户ID、任务ID、任务相关的消息、以及加签/减签的用户ID
 *
 * @author AprilWind
 */
@Data
public class TaskOperationBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 委派/转办人的用户ID（必填，准对委派/转办人/修改办理人操作）
     */
    @NotNull(message = "委派/转办人id不能为空", groups = {AddGroup.class})
    private String userId;

    /**
     * 加签/减签人的用户ID列表（必填，针对加签/减签操作）
     */
    @NotNull(message = "加签/减签id不能为空", groups = {EditGroup.class})
    private List<String> userIds;

    /**
     * 任务ID（必填）
     */
    @NotNull(message = "任务id不能为空")
    private Long taskId;

    /**
     * 意见或备注信息（可选）
     */
    private String message;

    /**
     * 获取委派或转办的用户标识符列表
     * <p>
     * 该方法将用户的代码（由 `USER.getCode()` 获取）与当前用户的 ID（`userId`）进行拼接，返回一个包含拼接结果的单一元素列表
     *
     * @return 返回一个包含用户标识符的单一元素列表
     */
    public List<String> getUserIdentifiers() {
        return Optional.ofNullable(userId)
            .map(id -> Collections.singletonList(USER.getCode() + id))
            .orElse(Collections.emptyList());
    }

    /**
     * 获取加签或减签的用户标识符列表
     * <p>
     * 该方法将用户代码（由 `USER.getCode()` 获取）与 `userIds` 列表中的每个用户ID拼接
     * 返回一个新的列表，每个元素都是用户代码与用户ID的拼接结果
     *
     * @return 返回一个包含所有用户标识符的列表
     */
    public List<String> getAllUserIdentifiers() {
        return Optional.ofNullable(userIds)
            .filter(ids -> !ids.isEmpty())
            .map(ids -> StreamUtils.toList(ids, id -> USER.getCode() + id))
            .orElse(Collections.emptyList());
    }

}
