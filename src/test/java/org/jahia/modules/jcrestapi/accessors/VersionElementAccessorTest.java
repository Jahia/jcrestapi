/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;

import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.Mocks;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.jcrestapi.links.JSONLink;
import org.jahia.modules.json.JSONConstants;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONVersion;
import org.jahia.modules.json.JSONVersions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * @author Christophe Laprun
 */
public class VersionElementAccessorTest extends ElementAccessorTest<JSONVersions<APIDecorator>, JSONVersion<APIDecorator>, JSONNode> {
    private final VersionElementAccessor accessor = new VersionElementAccessor();

    @Override
    protected String getSubElementType() {
        return JSONConstants.VERSIONS;
    }

    @Override
    protected String getSubElementName() {
        return Mocks.VERSION;
    }

    @Override
    protected JSONVersion<APIDecorator> getSubElementFrom(Response response) {
        return (JSONVersion<APIDecorator>) response.getEntity();
    }

    @Override
    protected JSONVersions<APIDecorator> getContainerFrom(Response response) {
        return (JSONVersions<APIDecorator>) response.getEntity();
    }

    @Override
    public ElementAccessor<JSONVersions<APIDecorator>, JSONVersion<APIDecorator>, JSONNode> getAccessor() {
        return accessor;
    }

    @Override
    protected JSONNode getDataForNewChild(String name) throws IOException {
        throw new UnsupportedOperationException();
    }


    @Override
    protected void checkLinksIfNeeded(Node node, JSONVersion<APIDecorator> subElement, Map<String, JSONLink> links) throws RepositoryException {
        // nothing to do here
    }

    @Override
    protected String[] getMandatoryLinkRels() {
        return new String[]{API.NODE_AT_VERSION};
    }

    @Override
    @Test
    public void simpleCreateShouldWork() throws RepositoryException, URISyntaxException, IOException {
        final Node node = createBasicNode();

        // try to inject some data to create a new version... already not possible to create a node with a versions object :)
        final JSONNode version;
        try {
            version = (JSONNode) accessor.convertFrom("{\"properties\" : {}}");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            getAccessor().perform(node, "foo", API.CREATE_OR_UPDATE, version, context);
            failBecauseExceptionWasNotThrown(UnsupportedOperationException.class);
        } catch (UnsupportedOperationException e) {
            assertThat(e.getLocalizedMessage()).contains("create", "update");
        }
    }

    /**
     * Versions are nodes so they can be directly accessed by their id.
     *
     * @param node
     * @return
     * @throws javax.jcr.RepositoryException
     */
    @Override
    protected JSONLink getSelfLinkForChild(Node node) throws RepositoryException {
        return JSONLink.createLink(API.SELF, URIUtils.getIdURI(Mocks.VERSION_ID));
    }
}
