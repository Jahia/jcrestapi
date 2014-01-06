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
package org.jahia.modules.jcrestapi.json;

import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.URIUtils;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Christophe Laprun
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public abstract class JSONItem<T extends Item> {
    private static final AtomicReference<URI> nodetypesURI = new AtomicReference<URI>(null);
    @XmlElement
    private final String name;
    @XmlElement
    private final String type;
    @XmlElement(name = "_links")
    private final Map<String, JSONLink> links;

    public JSONItem(T item, URI absoluteURI) throws RepositoryException {
        this.name = item.getName();
        this.type = getUnescapedTypeName(item);
        links = new HashMap<String, JSONLink>(7);
        addLink(new JSONLink("self", absoluteURI));


        if (nodetypesURI.get() == null) {
            // extract the complete URI containing the full URL path and API URI
            String api = UriBuilder.fromResource(API.class).build().toASCIIString();
            final String absoluteURIAsString = absoluteURI.toASCIIString();
            final String apiFullPath = absoluteURIAsString.substring(0, absoluteURIAsString.indexOf(api) + api.length());

            // complete path to node types URI
            String nodetypesURIAsString = apiFullPath + "/jcr__system/jcr__nodeTypes";
            try {
                nodetypesURI.set(new URI(nodetypesURIAsString));
            } catch (URISyntaxException e) {
                // shouldn't happen
                throw new RuntimeException(e);
            }
        }

        addLink(new JSONLink("type", URIUtils.getChildURI(nodetypesURI.get(), getTypeChildPath(item))));
    }

    protected void addLink(JSONLink link) {
        links.put(link.getRel(), link);
    }

    protected JSONLink getChildLink(URI parent, String childName) {
        return new JSONLink(childName, URIUtils.getChildURI(parent, childName));
    }

    protected String getTypeChildPath(T item) throws RepositoryException {
        return URIUtils.escape(type);
    }

    protected abstract String getUnescapedTypeName(T item) throws RepositoryException;
}
