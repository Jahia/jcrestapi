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
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
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
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.ws.rs.core.Response;

import org.apache.jackrabbit.value.ReferenceValue;
import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.Mocks;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.jcrestapi.links.JSONLink;
import org.jahia.modules.json.JSONItem;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONProperty;
import org.jahia.modules.json.JSONSubElementContainer;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christophe Laprun
 */
public class NodeElementAccessorTest extends ElementAccessorTest<JSONSubElementContainer<APIDecorator>, JSONNode<APIDecorator>, JSONNode> {
    private final NodeElementAccessor accessor = new NodeElementAccessor();

    @Override
    protected JSONSubElementContainer getContainerFrom(Response response) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ElementAccessor<JSONSubElementContainer<APIDecorator>, JSONNode<APIDecorator>, JSONNode> getAccessor() {
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

        assertThat(jsonNode.getDecorator() instanceof APIDecorator);
        APIDecorator apiDecorator = (APIDecorator) jsonNode.getDecorator();
        final Map<String, JSONLink> links = apiDecorator.getLinks();
        assertThat(links).containsKeys(API.ABSOLUTE, API.SELF, API.PARENT);
        assertThat(links.get(API.PARENT)).isEqualTo(JSONLink.createLink(API.PARENT, URIUtils.getIdURI(node.getParent().getIdentifier())));
        assertThat(links.get(API.ABSOLUTE).getURIAsString()).startsWith(Mocks.BASE_URI);
        assertThat(links.get(API.SELF)).isEqualTo(getSelfLinkForChild(node));

    }

    @Test
    public void testResolveReferences() throws RepositoryException {
        testReferenceResolving(false);
    }


    private void testReferenceResolving(boolean withFullChildren) throws RepositoryException {
        // we save the old value to make sure we don't mess it up for other tests.
        boolean oldResolveReferencesValue = API.setResolveReferences(true);
        boolean oldIncludeChildren = API.setIncludeFullChildren(withFullChildren);

        try {
            context = Mocks.createMockUriInfo(withFullChildren, null);

            final Node node = Mocks.createMockNode(Mocks.NODE_NAME, Mocks.NODE_ID, Mocks.PATH_TO_NODE, 2, 2, 2);
            final Node secondNode = Mocks.createMockNode(Mocks.NODE_NAME + "2", Mocks.NODE_ID + "2", Mocks.PATH_TO_NODE + "2", 2, 2, 2);
            final Node thirdNode = Mocks.createMockNode(Mocks.NODE_NAME + "3", Mocks.NODE_ID + "3", Mocks.PATH_TO_NODE + "3", 2, 2, 2);

            Value[] referenceValues = new Value[]{
                    new ReferenceValue(secondNode),
                    new ReferenceValue(thirdNode)
            };

            node.setProperty(Mocks.REF_PROPERTY + "1", secondNode);
            node.setProperty(Mocks.REF_PROPERTY + "2", referenceValues);

            final Response response = accessor.perform(node, (String) null, API.READ, null, context);

            assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
            final Object entity = response.getEntity();
            assertThat(entity instanceof JSONNode);

            JSONNode jsonNode = (JSONNode) entity;
            // check that the return JSONNode is the same as the one we called perform on
            assertThat(jsonNode.getId()).isEqualTo(node.getIdentifier());

            assertThat(jsonNode.getDecorator() instanceof APIDecorator);

            JSONProperty jsonProperty = jsonNode.getProperty(Mocks.REF_PROPERTY + "1");
            assertThat(jsonProperty).isNotNull();
            assertThat(jsonProperty.isMultiValued()).isFalse();
            assertThat(jsonProperty.getValue()).isEqualTo(Mocks.NODE_ID + "2");
            assertThat(jsonProperty.getDecorator()).isNotNull();
            assertThat(jsonProperty.getDecorator() instanceof APIDecorator);
            APIDecorator propertyAPIDecorator = (APIDecorator) jsonProperty.getDecorator();
            assertThat(propertyAPIDecorator.getReferences()).isNotNull();
            Map<String, JSONItem<? extends Item, APIDecorator>> references = propertyAPIDecorator.getReferences();
            assertThat(references).hasSize(1);
            assertThat(references).containsKey(Mocks.NODE_ID + "2");
            assertThat(references.get(Mocks.NODE_ID + "2") instanceof JSONNode);
            JSONNode referencedNode = (JSONNode) references.get(Mocks.NODE_ID + "2");
            assertThat(referencedNode.getId()).isEqualTo(Mocks.NODE_ID + "2");
            assertThat(referencedNode.getName()).isEqualTo(Mocks.NODE_NAME + "2");

            final Map<String, JSONNode> children = referencedNode.getChildren();
            if (withFullChildren) {
                assertThat(children).isNotNull();
                assertThat(children.size()).isEqualTo(2);
                JSONNode child = children.get(Mocks.CHILD + '0');
                assertThat(child).isNotNull();
                assertThat(child.getId()).isEqualTo(Mocks.CHILD_ID + '0');

                child = children.get(Mocks.CHILD + '1');
                assertThat(child).isNotNull();
                assertThat(child.getId()).isEqualTo(Mocks.CHILD_ID + '1');

                // check that we only resolve at one-level depth
                final Map greatChildren = child.getChildren();
                assertThat(greatChildren).isNull();
            } else {
                assertThat(children).isNull();
            }

            // now let's validate the multi-valued reference case
            jsonProperty = jsonNode.getProperty(Mocks.REF_PROPERTY + "2");
            assertThat(jsonProperty).isNotNull();
            assertThat(jsonProperty.isMultiValued()).isTrue();
            assertThat(jsonProperty.getValue() instanceof String[]);
            assertThat(jsonProperty.getDecorator()).isNotNull();
            assertThat(jsonProperty.getDecorator() instanceof APIDecorator);
            propertyAPIDecorator = (APIDecorator) jsonProperty.getDecorator();
            assertThat(propertyAPIDecorator.getReferences()).isNotNull();
            references = propertyAPIDecorator.getReferences();
            assertThat(references).hasSize(2);
            assertThat(references).containsKey(Mocks.NODE_ID + "2");
            assertThat(references.get(Mocks.NODE_ID + "2") instanceof JSONNode);
            assertThat(references).containsKey(Mocks.NODE_ID + "3");
            assertThat(references.get(Mocks.NODE_ID + "3") instanceof JSONNode);
            referencedNode = (JSONNode) references.get(Mocks.NODE_ID + "2");
            assertThat(referencedNode.getId()).isEqualTo(Mocks.NODE_ID + "2");
            assertThat(referencedNode.getName()).isEqualTo(Mocks.NODE_NAME + "2");
            referencedNode = (JSONNode) references.get(Mocks.NODE_ID + "3");
            assertThat(referencedNode.getId()).isEqualTo(Mocks.NODE_ID + "3");
            assertThat(referencedNode.getName()).isEqualTo(Mocks.NODE_NAME + "3");

        } finally {
            // we restore the saved value to make sure we don't mess it up for other tests.
            API.setResolveReferences(oldResolveReferencesValue);
            API.setIncludeFullChildren(oldIncludeChildren);
        }
    }

    @Test
    public void includeFullChildrenShouldWorkWithReferenceResolving() throws RepositoryException {
        testReferenceResolving(true);
    }

    @Test
    public void testNoLinks() throws RepositoryException {

        // we save the old value to make sure we don't mess it up for other tests.
        Boolean oldOutputLinks = API.setOutputLinks(false);

        try {
            context = Mocks.createMockUriInfo(true, null);

            final Node node = Mocks.createMockNode(Mocks.NODE_NAME, Mocks.NODE_ID, Mocks.PATH_TO_NODE, 2, 2, 2);
            final Response response = accessor.perform(node, (String) null, API.READ, null, context);

            assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
            final Object entity = response.getEntity();
            assertThat(entity instanceof JSONNode);
            JSONNode jsonNode = (JSONNode) entity;

            assertThat(jsonNode.getDecorator()).isNull();

        } finally {
            // we restore the saved value to make sure we don't mess it up for other tests.
            API.setOutputLinks(oldOutputLinks);
        }

    }

    @Test
    public void testNodeTypeFilter() throws RepositoryException {

        Set<String> nodeTypes = new HashSet<String>();
        nodeTypes.add("nt:base");

        context = Mocks.createMockUriInfo(true, nodeTypes);

        final Node node = Mocks.createMockNode(Mocks.NODE_NAME, Mocks.NODE_ID, Mocks.PATH_TO_NODE, 2, 2, 2);
        Response response = accessor.perform(node, (String) null, API.READ, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        Object entity = response.getEntity();
        assertThat(entity instanceof JSONNode);
        JSONNode jsonNode = (JSONNode) entity;

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.getChildren()).hasSize(2);

        nodeTypes.clear();
        nodeTypes.add("jnt:nonExistingType");

        context = Mocks.createMockUriInfo(true, nodeTypes);

        response = accessor.perform(node, (String) null, API.READ, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);
        entity = response.getEntity();
        assertThat(entity instanceof JSONNode);
        jsonNode = (JSONNode) entity;

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.getChildren()).hasSize(0);

    }

    @Override
    protected JSONNode getDataForNewChild(String name) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    @Test(expected = UnsupportedOperationException.class)
    public void simpleCreateShouldWork() throws RepositoryException, URISyntaxException, IOException {
        super.simpleCreateShouldWork();
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
