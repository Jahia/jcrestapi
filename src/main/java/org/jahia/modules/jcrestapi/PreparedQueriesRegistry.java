/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jcrestapi;

import org.jahia.modules.jcrestapi.api.PreparedQuery;
import org.jahia.modules.jcrestapi.api.PreparedQueryService;
import org.jahia.services.templates.JahiaModulesBeanPostProcessor;
import org.springframework.beans.BeansException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class stores all prepared queries that will be usable by the query endpoint.
 * Two ways of register prepared queries:
 * - It automatically registers PreparedQuery that are declared in modules spring contexts.
 *   (old way, buggy since 7.2.0.0 because spring contexts can start independently, PreparedQuery could not be registered depending on module start up order)
 * - Use PreparedQueriesRegistry.addQuery to register a prepared query (new way, and recommended way to register prepared query since 7.2.0.0)
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

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
        if (bean instanceof PreparedQuery) {
            PreparedQuery preparedQuery = (PreparedQuery) bean;
            removeQuery(preparedQuery);
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    /**
     * Unregister a PreparedQuery object
     * @param preparedQuery
     */
    public void removeQuery(PreparedQuery preparedQuery) {
        queries.remove(preparedQuery.getName());
    }

    /**
     * Register a PreparedQuery object
     * @param preparedQuery
     */
    public void addQuery(PreparedQuery preparedQuery) {
        queries.put(preparedQuery.getName(), preparedQuery);
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
