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

import javax.jcr.*;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
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
    private final Map<String, JSONProperty> properties;
    @XmlElement
    private final List<Object> mixins;
    @XmlElement
    private final Map<String, JSONItem> children;
    @XmlElement
    private final List<Object> versions;

    public JSONNode(Node node, URI absoluteURI, int depth) throws RepositoryException {
        super(node, absoluteURI);

        // add links
        final JSONLink propertiesLink = getChildLink(absoluteURI, "properties");
        addLink(propertiesLink);
        addLink(getChildLink(absoluteURI, "children"));
        addLink(getChildLink(absoluteURI, "mixins"));
        addLink(getChildLink(absoluteURI, "versions"));

        if (depth > 0) {
            final PropertyIterator props = node.getProperties();

            // properties URI builder
            properties = new HashMap<String, JSONProperty>((int) props.getSize());
            while (props.hasNext()) {
                Property property = props.nextProperty();
                final String propertyName = property.getName();
                final String escapedPropertyName = escape(propertyName);

                // add property
                this.properties.put(escapedPropertyName, new JSONProperty(property, getChildURI(propertiesLink.getURI(), escapedPropertyName)));
            }

            mixins = null;

            final NodeIterator nodes = node.getNodes();
            children = new HashMap<String, JSONItem>((int) nodes.getSize());

            while (nodes.hasNext()) {
                Node child = nodes.nextNode();

                // build child resource URI
                final String childName = child.getName();
                final String escapedChildName = escape(childName);

                children.put(escapedChildName, new JSONNode(child, getChildURI(absoluteURI, escapedChildName), depth - 1));
            }

            versions = null;
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
        return properties.get(property);
    }
}
