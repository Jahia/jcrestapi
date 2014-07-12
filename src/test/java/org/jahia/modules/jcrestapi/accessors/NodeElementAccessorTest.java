/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
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
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
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
        final Node node = Mocks.createMockNode(Mocks.NODE_NAME, Mocks.NODE_ID, Mocks.PATH_TO_NODE);
        final Response response = getAccessor().perform(node, (String) null, API.READ, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        final Object entity = response.getEntity();
        assertThat(entity instanceof JSONNode);

        JSONNode jsonNode = (JSONNode) entity;
        // check that the return JSONNode is the same as the one we called perform on
        assertThat(jsonNode.getId()).isEqualTo(node.getIdentifier());

        final Map<String, JSONLink> links = jsonNode.getLinks();

        assertThat(links).containsKeys(API.ABSOLUTE, API.SELF, API.PARENT);
        assertThat(links.get(API.PARENT)).isEqualTo(JSONLink.createLink(API.PARENT, URIUtils.getIdURI(node.getParent().getIdentifier())));
        assertThat(links.get(API.ABSOLUTE).getURIAsString()).startsWith(Mocks.BASE_URI);
        assertThat(links.get(API.SELF)).isEqualTo(getSelfLinkForChild(node));
    }

    @Override
    protected JSONNode getDataForNewChild(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * We're not creating new nodes from NodeElementAccessor, just updating so this test is checking that we properly have a null operation if we don't pass any new information.
     *
     * @throws RepositoryException
     * @throws URISyntaxException
     * @throws IOException
     */
    @Test
    public void simpleCreateShouldWork() throws RepositoryException, URISyntaxException, IOException {
        final Node node = createBasicNode();

        final Response response = getAccessor().perform(node, (String) null, API.CREATE_OR_UPDATE, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

        final JSONNode jsonNode = getSubElementFrom(response);
        assertThat(jsonNode.getName()).isEqualTo(node.getName());
        assertThat(jsonNode.getId()).isEqualTo(node.getIdentifier());
        final NodeIterator nodes = node.getNodes();
        final String[] childNames = new String[(int)nodes.getSize()];
        for(int i = 0; nodes.hasNext(); i++) {
            final Node child = nodes.nextNode();
            childNames[i] = child.getName();
        }
        assertThat(jsonNode.getChildren()).containsKeys(childNames);
    }

    @Override
    protected String getSubElementType() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getSubElementName() {
        return Mocks.NODE_NAME; // subElement name is the node's name since that's what perform should return
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
