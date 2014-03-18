/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.jcrestapi.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.URIUtils;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Christophe Laprun
 */
@XmlRootElement
@JsonDeserialize(using = JSONProperties.PropertiesDeserializer.class)
public class JSONProperties extends JSONSubElementContainer {
    private Map<String, JSONProperty> properties;

    public JSONProperties() {
    }

    public JSONProperties(JSONNode parent, Node node) throws RepositoryException {
        initWith(parent, node);
    }

    public void initWith(JSONNode parent, Node node) throws RepositoryException {
        super.initWith(parent, API.PROPERTIES);

        final PropertyIterator props = node.getProperties();

        // properties URI builder
        properties = new HashMap<String, JSONProperty>((int) props.getSize());
        while (props.hasNext()) {
            Property property = props.nextProperty();
            final String propertyName = property.getName();

            // add property
            this.properties.put(URIUtils.escape(propertyName), new JSONProperty(property));
        }
    }

    @XmlElement
    public Map<String, JSONProperty> getProperties() {
        return properties;
    }

    @PUT
    @Path("{name}")
    public void addProperty(@PathParam("name") String name, JSONProperty property) {
        if (properties == null) {
            properties = new HashMap<String, JSONProperty>(7);
        }
        properties.put(name, property);
    }

    public static class PropertiesDeserializer extends JsonDeserializer<JSONProperties> {
        @Override
        public JSONProperties deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
            ObjectCodec codec = parser.getCodec();
            ObjectNode root = codec.readTree(parser);

            final int size = root.size();
            if (size > 0) {
                final JSONProperties properties = new JSONProperties();
                final Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    final Map.Entry<String, JsonNode> field = fields.next();
                    properties.addProperty(field.getKey(), codec.treeToValue(field.getValue(), JSONProperty.class));
                }

                return properties;
            } else {
                return null;
            }
        }
    }
}
