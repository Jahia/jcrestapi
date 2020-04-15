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
package org.jahia.modules.jcrestapi;

import java.util.List;
import javax.ws.rs.WebApplicationException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Christophe Laprun
 */
public class APIException extends WebApplicationException {
    private final JSONError error;

    public APIException(Throwable e) {
        this(e, null);
    }

    public APIException(Throwable e, String operation, String nodeAccess, String idOrPath, String subElementType, List<String> subElements, Object data) {
        this(e, new JSONError(e, operation, nodeAccess, idOrPath, subElementType, subElements, data));
    }

    public APIException(Throwable cause, JSONError error) {
        super(cause);
        this.error = error;
    }

    public JSONError getError() {
        return error;
    }

    @XmlRootElement
    public static class JSONError {
        @XmlElement
        private final String exception;
        @XmlElement
        private final String message;
        @XmlElement
        private final String operation;
        @XmlElement
        private final String nodeAccess;
        @XmlElement
        private final String idOrPath;
        @XmlElement
        private final String subElementType;
        @XmlElement
        private final List<String> subElements;
        @XmlElement
        private final Object data;

        public JSONError(Throwable throwable, String operation, String nodeAccess, String idOrPath, String subElementType, List<String> subElements, Object data) {
            this.exception = throwable.getClass().getName();
            final String localizedMessage = throwable.getLocalizedMessage();
            this.message = localizedMessage != null ? localizedMessage : this.exception;
            this.operation = operation;
            this.nodeAccess = nodeAccess;
            this.idOrPath = idOrPath;
            this.subElementType = subElementType;
            this.subElements = subElements;
            this.data = data;
        }

        public JSONError(Throwable throwable) {
            this(throwable, null, null, null, null, null, null);
        }
    }
}
