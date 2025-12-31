package com.eisoo.metadatamanage.web.config;

import com.baomidou.mybatisplus.core.MybatisPlusVersion;
import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.ClassUtils;
import com.baomidou.mybatisplus.extension.injector.methods.InsertBatchSomeColumn;
import com.github.yulichang.mapper.MPJTableMapperHelper;
import com.github.yulichang.method.*;
import com.github.yulichang.method.mp.SelectOne;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.core.GenericTypeResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/11 15:01
 * @Version:1.0
 */
public class EasySqlInjector extends DefaultSqlInjector {
//    @Override
//    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
//        List<AbstractMethod> methodList = super.getMethodList(mapperClass,tableInfo);
//        methodList.add(new InsertBatchSomeColumn()); // 添加InsertBatchSomeColumn方法
//        return methodList;
//    }

    @Override
    public List<AbstractMethod> getMethodList(Class<?> mapperClass, TableInfo tableInfo) {
        List<String> methodList = Arrays.asList("SelectOne", "SelectCount",
                "SelectMaps", "SelectMapsPage", "SelectObjs", "SelectList", "SelectPage");
        List<AbstractMethod> list = super.getMethodList(mapperClass, tableInfo);
        list.removeIf(i -> methodList.contains(i.getClass().getSimpleName()));
        list.addAll(getSelectMethod());
        list.addAll(getJoinMethod());
        list.add(new InsertBatchSomeColumn()); // 添加InsertBatchSomeColumn方法
        return list;
    }

    private List<AbstractMethod> getJoinMethod() {
        List<AbstractMethod> list = new ArrayList<>();

        int v1, v2;
        try {
            String version = MybatisPlusVersion.getVersion();
            String[] split = version.split("\\.");
            v1 = Integer.parseInt(split[0]);
            v2 = Integer.parseInt(split[1]);
        } catch (Exception e) {
            v1 = 3;
            v2 = 4;
        }
        if ((v1 == 3 && v2 >= 5) || v1 > 3) {
            list.add(new SelectJoinCount(SqlMethod.SELECT_JOIN_COUNT.getMethod()));
            list.add(new SelectJoinOne(SqlMethod.SELECT_JOIN_ONE.getMethod()));
            list.add(new SelectJoinList(SqlMethod.SELECT_JOIN_LIST.getMethod()));
            list.add(new SelectJoinPage(SqlMethod.SELECT_JOIN_PAGE.getMethod()));
            list.add(new SelectJoinMap(SqlMethod.SELECT_JOIN_MAP.getMethod()));
            list.add(new SelectJoinMaps(SqlMethod.SELECT_JOIN_MAPS.getMethod()));
            list.add(new SelectJoinMapsPage(SqlMethod.SELECT_JOIN_MAPS_PAGE.getMethod()));
        } else {
            list.add(new SelectJoinCount());
            list.add(new SelectJoinOne());
            list.add(new SelectJoinList());
            list.add(new SelectJoinPage());
            list.add(new SelectJoinMap());
            list.add(new SelectJoinMaps());
            list.add(new SelectJoinMapsPage());
        }
        return list;
    }

    private List<AbstractMethod> getSelectMethod() {
        List<AbstractMethod> list = new ArrayList<>();
        list.add(new SelectOne());
        list.add(new com.github.yulichang.method.mp.SelectCount());
        list.add(new com.github.yulichang.method.mp.SelectMaps());
        list.add(new com.github.yulichang.method.mp.SelectMapsPage());
        list.add(new com.github.yulichang.method.mp.SelectObjs());
        list.add(new com.github.yulichang.method.mp.SelectList());
        list.add(new com.github.yulichang.method.mp.SelectPage());
        return list;
    }

    @Override
    public void inspectInject(MapperBuilderAssistant builderAssistant, Class<?> mapperClass) {
        Class<?> modelClass = getSuperClassGenericType(mapperClass, Mapper.class, 0);
        super.inspectInject(builderAssistant, mapperClass);
        MPJTableMapperHelper.init(modelClass, mapperClass);
    }

    public static Class<?> getSuperClassGenericType(final Class<?> clazz, final Class<?> genericIfc, final int index) {
        Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(ClassUtils.getUserClass(clazz), genericIfc);
        return null == typeArguments ? null : typeArguments[index];
    }
}
