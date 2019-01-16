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

import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.jahia.modules.json.JSONConstants;
import org.jahia.modules.json.Names;
import org.jahia.modules.json.jcr.SessionAccess;

/**
 * @author Christophe Laprun
 */
public final class URIUtils {
    private static final AtomicReference<String> BASE_URI = new AtomicReference<String>();

    private URIUtils() {
    }

    private static String getURIWithWorkspaceAndLanguage() {
        final SessionAccess.SessionInfo currentSession = SessionAccess.getCurrentSession();
        return API.API_PATH + "/" + currentSession.workspace + "/" + currentSession.language;
    }

    public static String getByPathURI(String path) {
        return getByPathURI(path, false);
    }

    public static String getByPathURI(String path, boolean removeFirstSlash) {
        return getURIWithWorkspaceAndLanguage() + (removeFirstSlash ? "/" + Paths.MAPPING : "/" + Paths.MAPPING + "/") + path;
    }

    public static String getTypeURI(String typeName) {
        return getByPathURI("jcr__system/jcr__nodeTypes/") + typeName;
    }

    public static String getIdURI(String identifier) {
        return getURIWithWorkspaceAndLanguage() + "/" + Nodes.MAPPING + "/" + identifier;
    }

    public static String getURIFor(Item item) {
        return getURIFor(item, false);
    }

    public static String getURIFor(Item item, boolean byPath) {
        if (item instanceof Node) {
            return getURIFor((Node) item, byPath);
        } else {
            return getURIFor((Property) item, byPath);
        }
    }

    public static String getURIFor(Node node) {
        return getURIFor(node, false);
    }

    public static String getURIFor(Node node, boolean byPath) {
        try {
            return byPath ? getByPathURI(node.getPath(), true) : getIdURI(node.getIdentifier());
        } catch (RepositoryException e) {
            throw new APIException(e);
        }
    }

    public static String getURIFor(Property property) {
        return getURIFor(property, false);
    }

    public static String getURIFor(Property property, boolean byPath) {
        final String properties;
        try {
            properties = getChildURI(getURIFor(property.getParent(), byPath), JSONConstants.PROPERTIES, false);
            return getChildURI(properties, property.getName(), true);
        } catch (RepositoryException e) {
            throw new APIException(e);
        }
    }

    public static String getURIForChildren(Node node) {
        return getChildURI(getURIFor(node), JSONConstants.CHILDREN, false);
    }

    public static String getURIForProperties(Node node) {
        return getChildURI(getURIFor(node), JSONConstants.PROPERTIES, false);
    }

    public static String getURIForMixins(Node node) {
        return getChildURI(getURIFor(node), JSONConstants.MIXINS, false);
    }

    public static String getURIForVersions(Node node) {
        return getChildURI(getURIFor(node), JSONConstants.VERSIONS, false);
    }

    public static String getChildURI(String parent, String childName, boolean escapeChildName) {
        if (childName.startsWith("/")) {
            childName = childName.substring(1);
        }

        if (escapeChildName) {
            childName = Names.escape(childName);
        }

        if (parent.endsWith("/")) {
            return parent + childName;
        } else {
            return parent + "/" + childName;
        }
    }

    public static String addModulesContextTo(String uriAsString, UriInfo context) {
        String baseUri = context.getBaseUri().toASCIIString();
        if (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length() - 1);
        }
        return baseUri + uriAsString;
    }

    public static void setBaseURI(String baseURI) {
        if (URIUtils.BASE_URI.get() == null) {
            // remove final '/' if needed
            if (baseURI.endsWith("/")) {
                baseURI = baseURI.substring(0, baseURI.length() - 1);
            }
            URIUtils.BASE_URI.set(baseURI);
        }
    }

    public static String getAbsoluteURI(String relativeURI) {
        return BASE_URI.get() + relativeURI;
    }
}
