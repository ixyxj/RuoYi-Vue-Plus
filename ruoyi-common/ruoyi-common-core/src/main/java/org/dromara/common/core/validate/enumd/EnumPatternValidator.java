package org.dromara.common.core.validate.enumd;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.reflect.ReflectUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

/**
 * 自定义枚举校验注解实现
 *
 * @author 秋辞未寒
 * @date 2024-12-09
 */
public class EnumPatternValidator implements ConstraintValidator<EnumPattern, String> {

    private EnumPattern annotation;;

    @Override
    public void initialize(EnumPattern annotation) {
        ConstraintValidator.super.initialize(annotation);
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        try {
            if (StringUtils.isNotBlank(value)) {
                Class<?> type = annotation.type();
                Object[] enumConstants = type.getEnumConstants();
                Method method = ReflectUtils.getMethod(type, annotation.method());
                for (Object e : enumConstants) {
                    if (value.equals(method.invoke(e))) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw new ValidationException(e);
        }
    }

}