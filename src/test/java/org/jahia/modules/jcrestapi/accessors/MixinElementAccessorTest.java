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
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;

import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.Mocks;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.jcrestapi.links.JSONLink;
import org.jahia.modules.json.JSONConstants;
import org.jahia.modules.json.JSONMixin;
import org.jahia.modules.json.JSONMixins;
import org.jahia.modules.json.JSONNode;

/**
 * @author Christophe Laprun
 */
public class MixinElementAccessorTest extends ElementAccessorTest<JSONMixins<APIDecorator>, JSONMixin<APIDecorator>, JSONNode> {
    private final MixinElementAccessor accessor = new MixinElementAccessor();

    @Override
    protected JSONNode getDataForNewChild(String name) throws IOException {
        return ElementAccessor.mapper.readValue(
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
        return JSONConstants.MIXINS;
    }

    @Override
    protected String getSubElementName() {
        return Mocks.MIXIN + 0;
    }

    @Override
    protected JSONMixin<APIDecorator> getSubElementFrom(Response response) {
        return (JSONMixin<APIDecorator>) response.getEntity();
    }

    @Override
    protected JSONMixins<APIDecorator> getContainerFrom(Response response) {
        return (JSONMixins<APIDecorator>) response.getEntity();
    }

    @Override
    public ElementAccessor<JSONMixins<APIDecorator>, JSONMixin<APIDecorator>, JSONNode> getAccessor() {
        return accessor;
    }

    @Override
    protected void checkLinksIfNeeded(Node node, JSONMixin<APIDecorator> subElement, Map<String, JSONLink> links) throws RepositoryException {
        // nothing to do for now
    }

    @Override
    protected String[] getMandatoryLinkRels() {
        return new String[]{API.TYPE};
    }
}
