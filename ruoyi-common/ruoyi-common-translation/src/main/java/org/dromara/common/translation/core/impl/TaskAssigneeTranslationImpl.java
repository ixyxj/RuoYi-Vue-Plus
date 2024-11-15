package org.dromara.common.translation.core.impl;

import lombok.AllArgsConstructor;
import org.dromara.common.core.service.AssigneeService;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;

/**
 * 任务办理人翻译实现
 *
 * @author AprilWind
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.TASK_ID_TO_ASSIGNEE)
public class TaskAssigneeTranslationImpl implements TranslationInterface<String> {

    private final AssigneeService assigneeService;

    @Override
    public String translation(Object key, String other) {
        if (key instanceof Long id) {
            return assigneeService.selectAssigneeByIds(id.toString());
        } else if (key instanceof String id) {
            return assigneeService.selectAssigneeByIds(id);
        }
        return null;
    }
}
