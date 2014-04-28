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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.Mocks;
import org.jahia.modules.jcrestapi.model.JSONMixin;
import org.jahia.modules.jcrestapi.model.JSONMixins;
import org.jahia.modules.jcrestapi.model.JSONNode;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christophe Laprun
 */
public class MixinElementAccessorTest extends ElementAccessorTest<JSONMixins, JSONMixin, JSONNode> {
    private static final ObjectMapper mapper = new JacksonJaxbJsonProvider().locateMapper(JSONNode.class, MediaType.APPLICATION_JSON_TYPE);
    private final MixinElementAccessor accessor = new MixinElementAccessor();

    protected JSONNode getDataForNewChild(String name) throws IOException {
        return mapper.readValue(
                "{" +
                        "   \"properties\" : {" +
                        "       \"j__lastVote\": {\"value\": \"-1\"}," +
                        "       \"j__nbOfVotes\": {\"value\": \"100\"}," +
                        "       \"j__sumOfVotes\": {\"value\": \"1000\"}" +
                        "   }" +
                        "}", JSONNode.class
        );
    }

    @Override
    protected String getSubElementType() {
        return API.MIXINS;
    }

    @Override
    protected String getSubElementName() {
        return Mocks.MIXIN + 0;
    }

    @Override
    protected JSONMixin getSubElementFrom(Response response) {
        return (JSONMixin) response.getEntity();
    }

    @Override
    protected JSONMixins getContainerFrom(Response response) {
        return (JSONMixins) response.getEntity();
    }

    @Override
    public ElementAccessor<JSONMixins, JSONMixin, JSONNode> getAccessor() {
        return accessor;
    }

    @Test
    public void simpleCreateShouldWork() throws RepositoryException, URISyntaxException, IOException, RepositoryException {
        final Node node = createBasicNode();
        final String newChildName = "newChild";
        final JSONNode dataForNewChild = getDataForNewChild(newChildName);

        // rather crappy way of doing things but at least we check that we give in is what we get out
        final String location = context.getBaseUri().toASCIIString() + "/" + getContainerURIFor(node) + "/" + newChildName;
        Mockito.when(context.getAbsolutePath()).thenReturn(new URI(location));

        final Response response = getAccessor().perform(node, newChildName, API.CREATE_OR_UPDATE, dataForNewChild, context);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);

        assertThat(response.getLocation()).isEqualTo(new URI(location));

        JSONMixin subElement = getSubElementFrom(response);
        assertThat(subElement).isNotNull();

    }
}
