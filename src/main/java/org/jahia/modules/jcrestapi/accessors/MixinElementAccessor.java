/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.core.UriInfo;

import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.json.JSONMixin;
import org.jahia.modules.json.JSONMixins;
import org.jahia.modules.json.JSONNode;

/**
 * @author Christophe Laprun
 */
public class MixinElementAccessor extends ElementAccessor<JSONMixins<APIDecorator>, JSONMixin<APIDecorator>, JSONNode> {
    @Override
    protected JSONMixins<APIDecorator> getSubElementContainer(Node node, UriInfo context) throws RepositoryException {
        return getFactory().createMixins(getParentFrom(node), node);
    }

    @Override
    protected JSONMixin<APIDecorator> getSubElement(Node node, String subElement, UriInfo context) throws RepositoryException {
        final NodeType mixin = getMixin(node, subElement);
        if (mixin == null) {
            return null;
        }

        return getFactory().createMixin(node, mixin);
    }

    @Override
    protected void delete(Node node, String subElement) throws RepositoryException {
        node.removeMixin(subElement);
    }

    @Override
    protected CreateOrUpdateResult<JSONMixin<APIDecorator>> createOrUpdate(Node node, String subElement, JSONNode childData) throws RepositoryException {
        if(subElement == null || subElement.isEmpty()) {
            throw new UnsupportedOperationException("Cannot create an automatically named mixin");
        }

        // if the node doesn't already have the mixin, add it
        final boolean isCreation = !node.isNodeType(subElement);
        if (isCreation) {
            node.addMixin(subElement);
        }

        // retrieve node type associated with mixin
        NodeType mixin = getMixin(node, subElement);

        // we now need to use the rest of the given child data to add / update the parent node content
        NodeElementAccessor.initNodeFrom(node, childData);

        return new CreateOrUpdateResult<JSONMixin<APIDecorator>>(!isCreation, getFactory().createMixin(node, mixin));
    }

    @Override
    protected String getSeeOtherURIAsString(Node node) {
        return URIUtils.getURIForMixins(node);
    }

    private NodeType getMixin(Node node, String subElement) throws RepositoryException {
        NodeType mixin = null;
        final NodeType[] mixinNodeTypes = node.getMixinNodeTypes();
        for (NodeType mixinNodeType : mixinNodeTypes) {
            if (mixinNodeType.getName().equals(subElement)) {
                mixin = mixinNodeType;
                break;
            }
        }
        return mixin;
    }
}
