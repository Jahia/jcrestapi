package org.jahia.modules.jcrestapi;

import org.jahia.modules.jcrestapi.api.PreparedQuery;
import org.jahia.services.templates.JahiaModulesBeanPostProcessor;
import org.springframework.beans.BeansException;

import java.util.LinkedHashMap;
import java.util.Map;

public class PreparedQueriesRegistry implements JahiaModulesBeanPostProcessor {
    private final static PreparedQueriesRegistry INSTANCE = new PreparedQueriesRegistry();

    public static PreparedQueriesRegistry getInstance() {
        return INSTANCE;
    }

    private Map<String, PreparedQuery> queries = new LinkedHashMap<String, PreparedQuery>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof PreparedQuery) {
            PreparedQuery preparedQuery = (PreparedQuery) bean;
            addQuery(preparedQuery);
        }
        return bean;
    }

    public void addQuery(PreparedQuery preparedQuery) {
        queries.put(preparedQuery.getName(), preparedQuery);
    }

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (bean instanceof PreparedQuery) {
            PreparedQuery preparedQuery = (PreparedQuery) bean;
            removeQuery(preparedQuery);
        }
    }

    public void removeQuery(PreparedQuery preparedQuery) {
        queries.remove(preparedQuery.getName());
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }


    public PreparedQuery getQuery(String name) {
        return queries.get(name);
    }
}
