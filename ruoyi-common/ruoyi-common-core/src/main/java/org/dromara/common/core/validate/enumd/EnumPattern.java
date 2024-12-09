package org.dromara.common.core.validate.enumd;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 自定义枚举校验注解
 *
 * @author 秋辞未寒
 * @date 2024-12-09
 */
@Documented
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Repeatable(EnumPattern.List.class) // 允许在同一元素上多次使用该注解
@Constraint(validatedBy = {EnumPatternValidator.class})
public @interface EnumPattern {

    Class<? extends ValidateEnum> type() default ValidateEnum.class;

    String message() default "输入值不在枚举范围内";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @Documented
    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @interface List {
        EnumPattern[] value();
    }

}
