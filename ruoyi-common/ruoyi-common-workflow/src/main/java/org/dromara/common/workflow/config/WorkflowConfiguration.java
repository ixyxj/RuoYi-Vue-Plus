package org.dromara.common.workflow.config;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.factory.YmlPropertySourceFactory;
import org.dromara.common.workflow.handler.WorkflowExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

/**
 * workflow 配置属性
 *
 * @author AprilWind
 */
@Slf4j
@AutoConfiguration
@PropertySource(value = "classpath:common-workflow.yml", factory = YmlPropertySourceFactory.class)
public class WorkflowConfiguration {

    /**
     * workflow 异常处理器
     */
    @Bean
    public WorkflowExceptionHandler workflowExceptionHandler() {
        return new WorkflowExceptionHandler();
    }

}
