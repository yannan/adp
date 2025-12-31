package com.eisoo.metadatamanage.web.commons;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @Author: Lan Tian
 * @Date: 2024/6/7 13:49
 * @Version:1.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Repeatable(BooleanEnumString.List.class)
@Constraint(validatedBy = BooleanStringValidator.class)//标明由哪个类执行校验逻辑
public @interface BooleanEnumString {
    String message() default "value not in enum values.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] palyload() default {};

    String[] value();

    @Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
    @Retention(RUNTIME)
    @interface List {
        BooleanEnumString[] value();
    }
}
