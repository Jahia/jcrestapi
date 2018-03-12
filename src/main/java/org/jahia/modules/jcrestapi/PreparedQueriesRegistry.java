package org.jahia.modules.jcrestapi;

import org.jahia.modules.jcrestapi.api.PreparedQuery;
import org.jahia.modules.jcrestapi.api.PreparedQueryService;
import org.jahia.services.templates.JahiaModulesBeanPostProcessor;
import org.springframework.beans.BeansException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class stores all prepared queries that will be usable by the query endpoint.
 * It automatically registers PreparedQuery that are declared in modules spring contexts.
 */
public class PreparedQueriesRegistry implements PreparedQueryService, JahiaModulesBeanPostProcessor {
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

    /**
     * Register a PreparedQuery object
     * @param preparedQuery
     */
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

    /**
     * Unregister a PreparedQuery object
     * @param preparedQuery
     */
    public void removeQuery(PreparedQuery preparedQuery) {
        queries.remove(preparedQuery.getName());
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * Get a query based on its name
     * @param name
     * @return
     */
    public PreparedQuery getQuery(String name) {
        return queries.get(name);
    }
}
