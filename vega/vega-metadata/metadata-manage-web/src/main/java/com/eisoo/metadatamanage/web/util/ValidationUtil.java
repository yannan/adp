package com.eisoo.metadatamanage.web.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.hibernate.validator.HibernateValidator;

public class ValidationUtil {
    private final static Validator validator = Validation.byProvider(HibernateValidator.class)
        .configure().failFast(false).buildValidatorFactory().getValidator();
    
    public static <T> List<String> validateBean(T t, Class<?>... groups){
        List<String> result = new ArrayList<>();
        Set<ConstraintViolation<T>> violationSet = validator.validate(t, groups);
        if (violationSet != null && violationSet.size() > 0) {
            for (ConstraintViolation<T> violation : violationSet) {
                result.add(violation.getMessage());
            }
        }
        return result;
    }

    public static <T> List<String> validateProperty(T obj, String propertyName){
        List<String> result = new ArrayList<>();
        Set<ConstraintViolation<T>> violationSet = validator.validateProperty(obj, propertyName);
        if (violationSet != null && violationSet.size() > 0) {
            for (ConstraintViolation<T> violation : violationSet) {
                result.add(violation.getMessage());
            }
        }
        return result;
    }
}
