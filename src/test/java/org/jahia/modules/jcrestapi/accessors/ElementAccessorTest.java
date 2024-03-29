/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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

import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.Mocks;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.Utils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.jcrestapi.links.JSONLink;
import org.jahia.modules.json.*;
import org.jahia.modules.json.jcr.SessionAccess;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christophe Laprun
 */
public abstract class ElementAccessorTest<C extends JSONSubElementContainer<APIDecorator>, T extends JSONNamed<APIDecorator>, U extends JSONItem> {

    static final String WORKSPACE = "default";
    static final String LANGUAGE = "en";

    protected UriInfo context;

    @Before
    public void setUp() throws RepositoryException {
        // fake session, at least to get access to a workspace name and language code for URIUtils
        final Session mockSession = Mocks.createMockSession();
        SessionAccess.setCurrentSession(mockSession, WORKSPACE, LANGUAGE);

        // set base URI for absolute links
        context = Mocks.createMockUriInfo(false, null);
        URIUtils.setBaseURI(context.getBaseUri().toASCIIString());
    }

    @Test
    public void readWithoutSubElementShouldReturnContainer() throws RepositoryException {
        final Node node = createBasicNode();
        final Response response = getAccessor().perform(node, (String) null, API.READ, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

        C container = getContainerFrom(response);
        final Map<String, JSONLink> links = container.getDecorator().getLinks();
        assertThat(links).containsKeys(API.ABSOLUTE, API.SELF, API.PARENT);
        assertThat(links.get(API.PARENT)).isEqualTo(JSONLink.createLink(API.PARENT, URIUtils.getIdURI(node.getIdentifier())));
        assertThat(links.get(API.ABSOLUTE).getURIAsString()).startsWith(Mocks.BASE_URI);
        assertThat(links.get(API.SELF)).isEqualTo(JSONLink.createLink(API.SELF, getContainerURIFor(node)));
    }

    protected Node createBasicNode() throws RepositoryException {
        return Mocks.createMockNode(Mocks.NODE_NAME, Mocks.NODE_ID, Mocks.PATH_TO_NODE);
    }

    @Test
    public void readWithSubElementShouldReturnSubElementWithThatName() throws RepositoryException {
        final Node node = createBasicNode();
        final Response response = getAccessor().perform(node, getSubElementName(), API.READ, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

        T subElement = getSubElementFrom(response);
        assertThat(subElement).isNotNull();

        final Map<String, JSONLink> links = subElement.getDecorator().getLinks();
        assertThat(links).containsKeys(getMandatoryLinkRels());

        checkLinksIfNeeded(node, subElement, links);
    }

    protected void checkLinksIfNeeded(Node node, T subElement, Map<String, JSONLink> links) throws RepositoryException {
        assertThat(links.get(API.SELF)).isEqualTo(getSelfLinkForChild(node));
        assertThat(subElement.getName()).isEqualTo(getSubElementName());
    }

    protected String[] getMandatoryLinkRels() {
        List<String> links = new LinkedList<>();
        Collections.addAll(links, API.ABSOLUTE, API.SELF, API.PATH, API.TYPE, API.PARENT);

        Filter filter = Utils.getFilter(context);
        if (filter.outputProperties()) {
            links.add(JSONConstants.PROPERTIES);
        }
        if (filter.outputVersions()) {
            links.add(JSONConstants.VERSIONS);
        }
        if (filter.outputChildren()) {
            links.add(JSONConstants.CHILDREN);
        }
        if (filter.outputMixins()) {
            links.add(JSONConstants.MIXINS);
        }

        return links.toArray(new String[links.size()]);
    }

    @Test
    public void simpleCreateShouldWork() throws RepositoryException, URISyntaxException, IOException {
        final Node node = createBasicNode();
        final String newChildName = "newChild";
        prepareNodeIfNeeded(node, newChildName);
        final U dataForNewChild = getDataForNewChild(newChildName);

        // rather crappy way of doing things but at least we check that we give in is what we get out
        final String location = context.getBaseUri().toASCIIString() + "/" + getContainerURIFor(node) + "/" + newChildName;
        Mockito.when(context.getAbsolutePath()).thenReturn(new URI(location));

        final Response response = getAccessor().perform(node, newChildName, API.CREATE_OR_UPDATE, dataForNewChild, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);

        assertThat(response.getLocation()).isEqualTo(new URI(location));

        T subElement = getSubElementFrom(response);
        assertThat(subElement).isNotNull();
        assertThat(subElement.getName()).isEqualTo(newChildName);

    }

    @Test
    public void attemptingCreationOrUpdateWithoutPassingDataShouldFail() throws RepositoryException, URISyntaxException, IOException {
        final Node node = createBasicNode();
        final Response response = getAccessor().perform(node, "foo", API.CREATE_OR_UPDATE, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.BAD_REQUEST);
    }

    protected void prepareNodeIfNeeded(Node node, String newChildName) throws RepositoryException {
        // do nothing by default
    }

    protected U getDataForNewChild(String name) throws RepositoryException, IOException {
        return null;
    }

    protected JSONLink getSelfLinkForChild(Node node) throws RepositoryException {
        final String containerURI = getContainerURIFor(node);
        return JSONLink.createLink(API.SELF, URIUtils.getChildURI(containerURI, getSubElementName(), true));
    }

    protected String getContainerURIFor(Node node) throws RepositoryException {
        return URIUtils.getChildURI(URIUtils.getIdURI(node.getIdentifier()), getSubElementType(), true);
    }

    protected abstract String getSubElementType();

    protected abstract String getSubElementName();

    protected abstract T getSubElementFrom(Response response);

    protected abstract C getContainerFrom(Response response);

    public abstract ElementAccessor<C, T, U> getAccessor();
}
