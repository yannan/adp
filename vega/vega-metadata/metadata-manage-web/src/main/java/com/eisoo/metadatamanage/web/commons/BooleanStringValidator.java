package com.eisoo.metadatamanage.web.commons;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/6/7 13:51
 * @Version:1.0
 */
public class BooleanStringValidator implements ConstraintValidator<BooleanEnumString, String> {
    private List<String> enumStringList;

    @Override
    public void initialize(BooleanEnumString constraintAnnotation) {
        enumStringList = Arrays.asList(constraintAnnotation.value());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        }
        return enumStringList.contains(value);
    }
}
