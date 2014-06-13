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
import org.jahia.modules.jcrestapi.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christophe Laprun
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(API.class)
public abstract class ElementAccessorTest<C extends JSONSubElementContainer, T extends JSONLinkable, U extends JSONItem> {

    static final String WORKSPACE = "default";
    static final String LANGUAGE = "en";

    protected UriInfo context;

    @Before
    public void setUp() throws RepositoryException {
        // fake session, at least to get access to a workspace name and language code for URIUtils
        final Session mockSession = Mocks.createMockSession();

        // DANGER: must be careful with PowerMockito as it appears to replace ALL the static methods
        // so you might get default return values for methods you don't expect
        PowerMockito.mockStatic(API.class);
        PowerMockito.when(API.getCurrentSession()).thenReturn(new API.SessionInfo(mockSession, WORKSPACE, LANGUAGE));

        // set base URI for absolute links
        context = Mocks.createMockUriInfo();
        URIUtils.setBaseURI(context.getBaseUri().toASCIIString());
    }

    @Test
    public void readWithoutSubElementShouldReturnContainer() throws RepositoryException {
        final Node node = createBasicNode();
        final Response response = getAccessor().perform(node, (String) null, API.READ, null, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

        C container = getContainerFrom(response);
        final Map<String, JSONLink> links = container.getLinks();
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

        final Map<String, JSONLink> links = subElement.getLinks();
        assertThat(links).containsKeys(API.ABSOLUTE, API.SELF);

        assertThat(links.get(API.SELF)).isEqualTo(getSelfLinkForChild(node));
        assertThat(subElement.getURI()).isEqualTo(links.get(API.SELF).getURIAsString());

        if (subElement instanceof JSONNamed) {
            JSONNamed named = (JSONNamed) subElement;
            assertThat(named.getName()).isEqualTo(getSubElementName());
        }
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
