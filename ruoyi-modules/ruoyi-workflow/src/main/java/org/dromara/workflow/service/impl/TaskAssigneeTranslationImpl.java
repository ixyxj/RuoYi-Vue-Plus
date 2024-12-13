package org.dromara.workflow.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.constant.TransConstant;
import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.workflow.service.AssigneeService;
import org.springframework.stereotype.Service;

/**
 * 任务办理人翻译实现
 *
 * @author AprilWind
 */
@Slf4j
@RequiredArgsConstructor
@Service
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
