package org.dromara.common.workflow.handler;

import cn.hutool.http.HttpStatus;
import com.warm.flow.core.exception.FlowException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Workflow 异常处理器
 *
 * @author AprilWind
 */
@Slf4j
@RestControllerAdvice
public class WorkflowExceptionHandler {

    /**
     * 流程异常
     */
    @ExceptionHandler(FlowException.class)
    public R<Void> handleForestException(FlowException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',流程异常.{}", requestURI, e.getMessage());
        return R.fail(HttpStatus.HTTP_UNAVAILABLE, e.getMessage());
    }

}
