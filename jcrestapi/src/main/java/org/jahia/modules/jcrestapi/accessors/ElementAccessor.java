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
import org.jahia.modules.jcrestapi.model.JSONItem;
import org.jahia.modules.jcrestapi.model.JSONLinkable;
import org.jahia.modules.jcrestapi.model.JSONNode;
import org.jahia.modules.jcrestapi.model.JSONSubElementContainer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;


/**
 * @author Christophe Laprun
 */
public abstract class ElementAccessor<C extends JSONSubElementContainer, T extends JSONLinkable, U extends JSONItem> {

    protected Object getElement(Node node, String subElement) throws RepositoryException {
        if (subElement.isEmpty()) {
            return getSubElementContainer(node);
        } else {
            return getSubElement(node, subElement);
        }
    }

    protected JSONNode getParentFrom(Node node) throws RepositoryException {
        return new JSONNode(node, 0);
    }

    protected abstract C getSubElementContainer(Node node) throws RepositoryException;
    protected abstract T getSubElement(Node node, String subElement) throws RepositoryException;
    protected abstract T delete(Node node, String subElement) throws RepositoryException;
    protected abstract T create(Node node, String subElement, U childData) throws RepositoryException;

    public Response perform(Node node, String subElement, String operation, U childData, UriInfo context) throws RepositoryException {
        if (API.DELETE.equals(operation)) {
            delete(node, subElement);
            return Response.noContent().build();
        } else if (API.CREATE_OR_UPDATE.equals(operation)) {
            // todo: deal with update scenario
            final T entity = create(node, subElement, childData);
            return Response.created(context.getAbsolutePath()).entity(entity).build();
        } else if (API.READ.equals(operation)) {
            final Object element = getElement(node, subElement);
            return element == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(element).build();
        }

        throw new IllegalArgumentException("Unknown operation: '" + operation + "'");
    }
}
