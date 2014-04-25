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
package org.jahia.modules.jcrestapi.accessors;

import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.Mocks;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.model.JSONLink;
import org.jahia.modules.jcrestapi.model.JSONNode;
import org.jahia.modules.jcrestapi.model.JSONSubElementContainer;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christophe Laprun
 */
public class NodeElementAccessorTest extends ElementAccessorTest<JSONSubElementContainer, JSONNode, JSONNode> {
    private final NodeElementAccessor accessor = new NodeElementAccessor();

    @Override
    protected JSONSubElementContainer getContainerFrom(Response response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ElementAccessor<JSONSubElementContainer, JSONNode, JSONNode> getAccessor() {
        return accessor;
    }

    @Override
    @Test
    /**
     * Overriden because NodeElementAccessor only returns data about the node that's given, not any other sub-elements.
     */
    public void readWithoutSubElementShouldReturnContainer() throws RepositoryException {
        final Node node = Mocks.createMockNode(Mocks.NODE_ID, Mocks.PATH_TO_NODE);
        final Response response = getAccessor().perform(node, (String) null, API.READ, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        final Object entity = response.getEntity();
        assertThat(entity instanceof JSONNode);

        JSONNode jsonNode = (JSONNode) entity;
        final Map<String, JSONLink> links = jsonNode.getLinks();

        assertThat(links).containsKeys(API.ABSOLUTE, API.SELF, API.PARENT);
        assertThat(links.get(API.PARENT)).isEqualTo(JSONLink.createLink(API.PARENT, URIUtils.getIdURI(node.getParent().getIdentifier())));
        assertThat(links.get(API.ABSOLUTE).getURIAsString()).startsWith(Mocks.BASE_URI);
    }

    @Override
    protected String getSubElementType() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getSubElementName() {
        return null;
    }

    @Override
    protected JSONNode getSubElementFrom(Response response) {
        return (JSONNode) response.getEntity();
    }

    /**
     * NodeElementAccessor should only return info about the node it operates on, nothing else.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    @Override
    protected JSONLink getSelfLinkForChild(Node node) throws RepositoryException {
        return JSONLink.createLink(API.SELF, URIUtils.getIdURI(node.getIdentifier()));
    }
}
