package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.utils.StreamUtils;
import org.dromara.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import static org.dromara.common.core.enums.TaskAssigneeEnum.USER;

/**
 * 加签请求对象
 *
 * @author may
 */
@Data
public class AddSignatureBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 加签人id
     */
    @NotNull(message = "加签id不能为空", groups = {AddGroup.class})
    private List<String> userIds;

    /**
     * 任务id
     */
    @NotNull(message = "任务id不能为空", groups = {AddGroup.class})
    private Long taskId;

    /**
     * 意见
     */
    private String message;

    /**
     * 获取包含用户标识符的列表
     * <p>
     * 该方法将用户代码（由 `USER.getCode()` 获取）与 `userIds` 列表中的每个用户ID拼接，
     * 然后返回一个新的列表，其中每个元素都是用户代码与用户ID的拼接结果
     *
     * @return 返回一个列表，其中每个元素是由用户代码和用户ID拼接而成的字符串
     */
    public List<String> getUserIdentifierList() {
        return StreamUtils.toList(userIds, userId -> USER.getCode() + userId);
    }

}
