package org.dromara.system.domain.vo;

import lombok.Data;
import org.dromara.common.sensitive.annotation.Sensitive;
import org.dromara.common.sensitive.core.SensitiveStrategy;
import org.dromara.common.translation.annotation.Translation;
import org.dromara.common.translation.constant.TransConstant;

/**
 * 用户简略信息
 *
 * @author AprilWind
 */
@Data
public class UserBriefInfoVo {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户账号
     */
    @Sensitive(strategy = SensitiveStrategy.FIRST_MASK, perms = "system:user:edit")
    private String userName;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名
     */
    private String deptName;

    /**
     * 祖级列表
     */
    private String ancestors;

    /**
     * 部门祖级列表名称
     */
    @Translation(type = TransConstant.DEPT_ID_TO_NAME, mapper = "ancestors")
    private String ancestorsName;

    /**
     * 负责人ID
     */
    private Long leader;

    /**
     * 负责人昵称
     */
    @Translation(type = TransConstant.USER_ID_TO_NICKNAME, mapper = "leader")
    private String leaderName;

    /**
     * 用户类型（sys_user系统用户）
     */
    private String userType;

    /**
     * 用户邮箱
     */
    @Sensitive(strategy = SensitiveStrategy.EMAIL, perms = "system:user:edit")
    private String email;

    /**
     * 手机号码
     */
    @Sensitive(strategy = SensitiveStrategy.PHONE, perms = "system:user:edit")
    private String phonenumber;

    /**
     * 用户性别（0男 1女 2未知）
     */
    private String sex;

    /**
     * 头像地址
     */
    @Translation(type = TransConstant.OSS_ID_TO_URL)
    private Long avatar;

}
