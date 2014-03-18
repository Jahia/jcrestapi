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
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.Path;
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
@JsonDeserialize(using = JSONMixins.MixinsDeserializer.class)
public class JSONMixins extends JSONSubElementContainer {
    @XmlElement
    private Map<String, JSONMixin> mixins;

    public JSONMixins() {
    }

    public JSONMixins(JSONNode parent, Node node) throws RepositoryException {
        super(parent, API.MIXINS);

        final NodeType[] mixinNodeTypes = node.getMixinNodeTypes();
        mixins = new HashMap<String, JSONMixin>(mixinNodeTypes.length);
        for (NodeType mixinNodeType : mixinNodeTypes) {
            final String name = mixinNodeType.getName();
            mixins.put(URIUtils.escape(name), new JSONMixin(node, mixinNodeType));
        }
    }

    @Path(API.MIXINS)
    public Map<String, JSONMixin> getMixins() {
        return mixins;
    }

    public static class MixinsDeserializer extends JsonDeserializer<JSONMixins> {
        @Override
        public JSONMixins deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
            ObjectCodec codec = parser.getCodec();
            ObjectNode root = codec.readTree(parser);

            final int size = root.size();
            if (size > 0) {
                final JSONMixins mixins = new JSONMixins();
                final Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
                while (fields.hasNext()) {
                    final Map.Entry<String, JsonNode> field = fields.next();
                    mixins.addChild(field.getKey(), codec.treeToValue(field.getValue(), JSONMixin.class));
                }

                return mixins;
            } else {
                return null;
            }
        }
    }

    private void addChild(String name, JSONMixin mixin) {
        if (mixins == null) {
            mixins = new HashMap<String, JSONMixin>(7);
        }

        mixins.put(name, mixin);
    }
}
