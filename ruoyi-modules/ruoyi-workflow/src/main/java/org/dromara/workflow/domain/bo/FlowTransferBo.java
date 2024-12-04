package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static org.dromara.common.core.enums.TaskAssigneeEnum.USER;

/**
 * 终转办务请求对象
 *
 * @author may
 */
@Data
public class FlowTransferBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 转办人id
     */
    @NotNull(message = "转办人id不能为空", groups = {AddGroup.class})
    private String userId;

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
     * 该方法将用户的代码（由 `USER.getCode()` 获取）与当前用户的 ID（`userId`）进行拼接，
     * 然后返回一个包含该拼接结果的单一元素列表
     *
     * @return 返回一个列表，列表中包含一个字符串元素，该元素是由用户代码和用户ID拼接而成
     */
    public List<String> getUserIdentifierList() {
        return Collections.singletonList(USER.getCode() + userId);
    }

}
