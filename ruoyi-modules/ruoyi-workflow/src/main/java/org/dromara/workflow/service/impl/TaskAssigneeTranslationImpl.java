package org.dromara.workflow.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.translation.annotation.TranslationType;
import org.dromara.common.translation.core.TranslationInterface;
import org.dromara.workflow.constant.WfConstant;
import org.dromara.workflow.service.IFlwTaskService;
import org.springframework.stereotype.Service;

/**
 * 任务办理人翻译实现
 *
 * @author AprilWind
 */
@Slf4j
@RequiredArgsConstructor
@Service
@TranslationType(type = WfConstant.TASK_ID_TO_ASSIGNEE)
public class TaskAssigneeTranslationImpl implements TranslationInterface<String> {

    private final IFlwTaskService flwTaskService;

    @Override
    public String translation(Object key, String other) {
        if (key instanceof Long id) {
            return flwTaskService.selectAssigneeNamesByIds(id.toString());
        } else if (key instanceof String id) {
            return flwTaskService.selectAssigneeNamesByIds(id);
        }
        return null;
    }
}
