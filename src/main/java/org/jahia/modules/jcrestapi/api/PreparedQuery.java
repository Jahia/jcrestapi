package org.jahia.modules.jcrestapi.api;

import org.jahia.services.content.JCRContentUtils;

import java.util.List;

public class PreparedQuery {
    private String name;
    private String source;

    public PreparedQuery() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

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
}
