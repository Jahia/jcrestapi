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

import org.mockito.Mockito;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Christophe Laprun
 */
public class Mocks {

    public static final String BASE_URI = "http://api.example.org:9999/context/subContext";

    public static Node createMockNode() throws RepositoryException {
        // mock node type
        NodeType nodeType = Mockito.mock(NodeType.class);
        Mockito.stub(nodeType.getName()).toReturn("nodeType");

        // mock parent
        Node parent = Mockito.mock(Node.class);
        Mockito.stub(parent.getIdentifier()).toReturn("parentId");

        // mock node
        Node node = Mockito.mock(Node.class);
        Mockito.stub(node.getPrimaryNodeType()).toReturn(nodeType);
        Mockito.stub(node.getIdentifier()).toReturn("nodeId");
        Mockito.stub(node.getParent()).toReturn(parent);
        Mockito.stub(node.getPath()).toReturn("/path/to/node");
        Mockito.stub(node.getMixinNodeTypes()).toReturn(new NodeType[]{});
        Mockito.stub(node.getNodes()).toReturn(new NodeIterator() {
            @Override
            public Node nextNode() {
                return null;
            }

            @Override
            public void skip(long skipNum) {

            }

            @Override
            public long getSize() {
                return 0;
            }

            @Override
            public long getPosition() {
                return 0;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Object next() {
                return null;
            }

            @Override
            public void remove() {

            }
        });

        return node;
    }

    public static UriInfo createMockUriInfo() {
        final UriInfo info = Mockito.mock(UriInfo.class);
        try {
            Mockito.stub(info.getBaseUri()).toReturn(new URI(BASE_URI));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return info;
    }
}
