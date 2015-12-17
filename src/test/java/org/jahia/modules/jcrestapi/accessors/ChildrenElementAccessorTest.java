/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software; you can redistribute it and/or
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
package org.jahia.modules.jcrestapi.accessors;

import java.io.IOException;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;

import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.Mocks;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.jcrestapi.links.JSONLink;
import org.jahia.modules.json.JSONChildren;
import org.jahia.modules.json.JSONConstants;
import org.jahia.modules.json.JSONNode;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christophe Laprun
 */
public class ChildrenElementAccessorTest extends ElementAccessorTest<JSONChildren<APIDecorator>, JSONNode<APIDecorator>, JSONNode> {
    private final ChildrenElementAccessor accessor = new ChildrenElementAccessor();

    @Override
    protected String getSubElementType() {
        return JSONConstants.CHILDREN;
    }

    @Override
    protected String getSubElementName() {
        return Mocks.CHILD + 0;
    }

    @Override
    protected JSONNode<APIDecorator> getSubElementFrom(Response response) {
        return (JSONNode<APIDecorator>) response.getEntity();
    }

    @Override
    protected JSONChildren<APIDecorator> getContainerFrom(Response response) {
        return (JSONChildren<APIDecorator>) response.getEntity();
    }

    @Override
    public ElementAccessor<JSONChildren<APIDecorator>, JSONNode<APIDecorator>, JSONNode> getAccessor() {
        return accessor;
    }

    @Override
    protected JSONNode getDataForNewChild(String name) throws IOException {
        return ElementAccessor.mapper.readValue("{\"name\":\"" + name + "\"}", JSONNode.class
        );
    }

    /**
     * Children are nodes so they can be directly accessed by their id.
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    @Override
    protected JSONLink getSelfLinkForChild(Node node) throws RepositoryException {
        return JSONLink.createLink(API.SELF, URIUtils.getIdURI(Mocks.CHILD_ID + 0));
    }

    @Test
    public void usingFullChildrenShouldIncludeCompleteChildren() throws RepositoryException {
        context = Mocks.createMockUriInfo(true, null);

        final Node node = Mocks.createMockNode(Mocks.NODE_NAME, Mocks.NODE_ID, Mocks.PATH_TO_NODE, 2, 2, 2);
        final Response response = accessor.perform(node, (String) null, API.READ, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

        final JSONChildren children = getContainerFrom(response);
        assertThat(children).isNotNull();

        final JSONNode child = (JSONNode) children.getChildren().get(Mocks.CHILD + '0');
        final Map greatChildren = child.getChildren();
        assertThat(greatChildren).isNotNull();
        assertThat(greatChildren.size()).isEqualTo(1);
    }
}
