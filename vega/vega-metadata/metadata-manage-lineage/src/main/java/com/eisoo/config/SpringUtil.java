package com.eisoo.config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
/**
 * @Author: Lan Tian
 * @Date: 2024/12/12 10:11
 * @Version:1.0
 */
@Slf4j
@Component
public class SpringUtil implements ApplicationContextAware{
    @Getter
    private static ApplicationContext applicationContext = null;
    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        if(SpringUtil.applicationContext == null){
            SpringUtil.applicationContext  = applicationContext;
        }
        log.info("ApplicationContext配置成功,在普通类可以通过调用SpringUtils.getAppContext()获取applicationContext对象,applicationContext={}",SpringUtil.applicationContext);
    }
    public static Object getBean(String name){
        return getApplicationContext().getBean(name);
    }
    public static <T> T getBean(Class<T> clazz){
        return getApplicationContext().getBean(clazz);
    }
    public static <T> T getBean(String name,Class<T> clazz){
        return getApplicationContext().getBean(name, clazz);
    }
}
