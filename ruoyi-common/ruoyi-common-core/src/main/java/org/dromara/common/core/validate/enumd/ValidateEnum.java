package org.dromara.common.core.validate.enumd;

import org.dromara.common.core.utils.StringUtils;

/**
 * 枚举类型校验接口
 *
 * @author 秋辞未寒
 * @date 2024-12-09
 */
public interface ValidateEnum {

    /**
     * 获取枚举code
     * <pre>该code用于匹配</pre>
     * @return 枚举code
     */
    String getCode();

    /**
     * 校验枚举code
     * @param code 枚举code
     * @return 校验结果
     */
    default boolean validate(String code) {
        return StringUtils.equals(code, getCode());
    }

}
