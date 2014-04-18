/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Christophe Laprun
 */
public final class URIUtils {
    private static final AtomicReference<String> BASE_URI = new AtomicReference<String>();

    private URIUtils() {
    }

    private static String getURIWithWorkspaceAndLanguage() {
        final API.SessionInfo currentSession = API.getCurrentSession();
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
        if (item instanceof Node) {
            Node node = (Node) item;
            return getURIFor(node);
        } else {
            return getURIFor((Property) item);
        }
    }

    public static String getURIFor(Node node) {
        try {
            return getIdURI(node.getIdentifier());
        } catch (RepositoryException e) {
            throw new APIException(e);
        }
    }

    public static String getURIFor(Property property) {
        final String properties;
        try {
            properties = getURIForProperties(property.getParent());
            return getChildURI(properties, property.getName(), true);
        } catch (RepositoryException e) {
            throw new APIException(e);
        }
    }

    public static String getURIForChildren(Node node) {
        return getChildURI(getURIFor(node), API.CHILDREN, false);
    }
    public static String getURIForProperties(Node node) {
        return getChildURI(getURIFor(node), API.PROPERTIES, false);
    }
    public static String getURIForMixins(Node node) {
        return getChildURI(getURIFor(node), API.MIXINS, false);
    }
    public static String getURIForVersions(Node node) {
        return getChildURI(getURIFor(node), API.VERSIONS, false);
    }

    public static String escape(String value) {
        return escape(value, 1);
    }

    public static String escape(String value, int index) {
        if (index > 1) {
            value += "--" + index;
        }
        return value.replace(":", "__");
    }

    public static String unescape(String value) {
        String replace = value.replace("__", ":");

        final int indexMarker = replace.lastIndexOf("--");
        if (indexMarker > 0) {
            // we have an index marker that we need to replace
            String index = replace.substring(indexMarker + 2);
            replace = replace.substring(0, indexMarker) + "[" + index + "]";
        }

        return replace;
    }

    public static String getChildURI(String parent, String childName, boolean escapeChildName) {
        if (childName.startsWith("/")) {
            childName = childName.substring(1);
        }

        if (escapeChildName) {
            childName = escape(childName);
        }

        if (parent.endsWith("/")) {
            return parent + childName;
        } else {
            return parent + "/" + childName;
        }
    }

    public static String addModulesContextTo(String uriAsString, UriInfo context) {
            return context.getBaseUri().toASCIIString() + uriAsString;
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
