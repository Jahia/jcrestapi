/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.jcrestapi.api;

import org.jahia.services.content.JCRContentUtils;

import java.util.List;
import java.util.Map;

/**
 * Predefined queries which can be used by the query endpoint.
 */
public class PreparedQuery {
    private String name;
    private String source;

    public PreparedQuery() {
    }

    /**
     * Unique query name
     *
     * @return
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get original query source
     *
     * @return
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Get parsed query with position parameters replaced
     *
     * @param params
     * @return
     */
    public String getQuery(List<Object> params) {
        String res = source;
        for (Object param : params) {
            if (param instanceof Number) {
                res = res.replaceFirst("\\?", param.toString());
            } else {
                res = res.replaceFirst("\\?", "'" + JCRContentUtils.sqlEncode(param.toString()) + "'");
            }

        }
        return res;
    }

    /**
     * Get parsed query with named parameters replaced
     *
     * @param params
     * @return
     */
    public String getQuery(Map<String, Object> params) {
        String res = source;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            final String key = entry.getKey();
            if (key.matches("[a-zA-Z0-9_]+")) {
                final Object value = entry.getValue();
                if (value instanceof Number) {
                    res = res.replaceFirst("(\\s):" + key, "$1" + value.toString());
                } else {
                    res = res.replaceFirst("(\\s):" + key, "$1'" + JCRContentUtils.sqlEncode(value.toString()) + "'");
                }
            } else {
                throw new IllegalArgumentException("Invalid parameter name '" + key + "'");
            }
        }
        return res;
    }
}
