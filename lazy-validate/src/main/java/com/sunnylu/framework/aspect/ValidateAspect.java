package com.sunnylu.framework.aspect;

import com.sunnylu.framework.annotation.Valid;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;

import javax.validation.*;
import java.util.Iterator;
import java.util.Set;

/**
 * 全局校验器切面 基于JSR-303
 * 使用方须要自行添加校验注解
 */
@Aspect
//@Order
public class ValidateAspect {

    @Pointcut(value = "@annotation(valid)",argNames = "valid")
    public void pointcut(Valid valid){}

    @Before(value = "pointcut(valid)")
    public void validate(JoinPoint joinPoint,Valid valid){
        Object[] args = joinPoint.getArgs();
        for (Object arg : args){
            if(arg == null)//null值不做校验
                continue;
            validate(arg);
        }
    }

    private void validate(Object object){
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<Object>> set = validator.validate(object);
        Iterator<ConstraintViolation<Object>> iter = set.iterator();
        if (iter.hasNext()) {
            ConstraintViolation<Object> violation = iter.next();
            throw new ValidationException(violation.getPropertyPath()+":"+violation.getMessage());
        }
    }
}
