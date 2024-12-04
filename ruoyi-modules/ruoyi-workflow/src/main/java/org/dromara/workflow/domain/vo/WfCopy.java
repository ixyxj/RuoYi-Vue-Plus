package org.dromara.workflow.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import static org.dromara.common.core.enums.TaskAssigneeEnum.USER;

/**
 * 抄送
 *
 * @author may
 */
@Data
public class WfCopy implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    public String getUserId() {
        return USER.getCode() + userId;
    }

}
