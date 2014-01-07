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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.Map;

/**
 * A JSON representation of a JCR node.
 * <p/>
 * <pre>
 * "name" : <the node's unescaped name>,
 * "type" : <the node's node type name>,
 * "properties" : <properties representation>,
 * "mixins" : <mixins representation>,
 * "children" : <children representation>,
 * "versions" : <versions representation>,
 * "links" : {
 * "self" : "<URI identifying the resource associated with this node>",
 * "type" : "<URI identifying the resource associated with this node's type>",
 * "properties" : "<URI identifying the resource associated with this node's properties>",
 * "mixins" : "<URI identifying the resource associated with this node's mixins>",
 * "children" : "<URI identifying the resource associated with this node's children>",
 * "versions" : "<URI identifying the resource associated with this node's versions>"
 * }
 * </pre>
 *
 * @author Christophe Laprun
 */
@XmlRootElement
public class JSONNode extends JSONItem<Node> {
    @XmlElement
    private final JSONProperties properties;
    @XmlElement
    private final JSONMixins mixins;
    @XmlElement
    private final JSONChildren children;
    @XmlElement
    private final JSONVersions versions;

    public JSONNode(Node node, URI absoluteURI, int depth) throws RepositoryException {
        super(node, absoluteURI);

        // add links
        final JSONLink propertiesLink = getChildLink(absoluteURI, API.PROPERTIES);
        addLink(propertiesLink);
        final JSONLink childrenLink = getChildLink(absoluteURI, API.CHILDREN);
        addLink(childrenLink);
        final JSONLink mixinsLink = getChildLink(absoluteURI, API.MIXINS);
        addLink(mixinsLink);
        final JSONLink versionsLink = getChildLink(absoluteURI, API.VERSIONS);
        addLink(versionsLink);

        if (depth > 0) {
            properties = new JSONProperties(this, node, propertiesLink.getURI());

            mixins = new JSONMixins(this, node, mixinsLink.getURI());

            children = new JSONChildren(this, node, childrenLink.getURI());

            versions = new JSONVersions(this, node, versionsLink.getURI());
        } else {
            properties = null;
            mixins = null;
            children = null;
            versions = null;
        }
    }

    @Override
    protected String getUnescapedTypeName(Node item) throws RepositoryException {
        return item.getPrimaryNodeType().getName();
    }

    public JSONProperty getProperty(String property) {
        property = URIUtils.escape(property);
        return getProperties().get(property);
    }

    public Map<String, JSONProperty> getProperties() {
        return properties.getProperties();
    }

    public JSONChildren getJSONChildren() {
        return children;
    }

    Map<String, JSONNode> getChildren() {
        return children.getChildren();
    }

    public JSONMixins getJSONMixins() {
        return mixins;
    }

    public JSONVersions getJSONVersions() {
        return versions;
    }

    public JSONProperties getJSONProperties() {
        return properties;
    }
}
