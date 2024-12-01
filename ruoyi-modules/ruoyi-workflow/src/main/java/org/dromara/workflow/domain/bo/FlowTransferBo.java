package org.dromara.workflow.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dromara.common.core.validate.AddGroup;

import java.io.Serial;
import java.io.Serializable;

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
}
