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
