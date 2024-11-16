package org.dromara.common.translation.core.impl;

import lombok.AllArgsConstructor;
import org.dromara.common.core.service.UserService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.dromara.common.core.enums.TaskAssigneeEnum.USER;

/**
 * 用户名称翻译实现
 *
 * @author may
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NICKNAME)
public class NicknameTranslationImpl implements TranslationInterface<String> {

    private final UserService userService;

    @Override
    public String translation(Object key, String other) {
        if (key instanceof Long id) {
            return userService.selectNicknameByIds(id.toString());
        } else if (key instanceof String ids) {
            if (StringUtils.isNotBlank(ids)) {
                ids = Stream.of(ids.split(StringUtils.SEPARATOR))
                    .map(userId -> userId.contains(USER.getCode()) ? userId.replaceAll(USER.getCode(), StringUtils.EMPTY) : userId)
                    .collect(Collectors.joining(StringUtils.SEPARATOR));
            }
            return userService.selectNicknameByIds(ids);
        }
        return null;
    }
}
