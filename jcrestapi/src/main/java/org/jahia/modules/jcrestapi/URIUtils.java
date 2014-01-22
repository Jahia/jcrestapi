/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jcrestapi;

import org.jahia.modules.jcrestapi.model.JSONLinkable;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Christophe Laprun
 */
public class URIUtils {
    private static final AtomicReference<String> baseURI = new AtomicReference<String>(UriBuilder.fromResource(API
            .class).build().toASCIIString());
    private static final AtomicReference<String> idURI = new AtomicReference<String>(baseURI + "/nodes/");
    private static final AtomicReference<String> nodetypesURI = new AtomicReference<String>(baseURI +
            "/byPath/jcr__system/jcr__nodeTypes/");

    public static String getTypeURI(String typeName) {
        return nodetypesURI.get() + typeName;
    }

    public static String getIdURI(String identifier) {
        return idURI.get() + identifier;
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
            throw new RuntimeException(e);
        }
    }

    public static String getURIFor(Property property)  {
        final String properties;
        try {
            properties = getURIForProperties(property.getParent());
            return getChildURI(properties, property.getName(), true);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getURIForChildren(JSONLinkable parentURI) {
        return getURIForChildren(parentURI.getURI());
    }

    public static String getURIForChildren(String parentURI) {
        return getChildURI(parentURI, API.CHILDREN, false);
    }

    public static String getURIForProperties(Node node) {
        return getChildURI(getURIFor(node), API.PROPERTIES, false);
    }

    public static String getURIForMixins(Node node) {
        return getChildURI(getURIFor(node), API.MIXINS, false);
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

    public static URI getChildURI(URI parent, String childName) {
        return getChildURI(parent, childName, false);
    }

    public static URI getChildURI(URI parent, String childName, boolean escapeChildName) {
        try {
            return new URI(getChildURI(parent.toASCIIString(), childName, escapeChildName));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
}
