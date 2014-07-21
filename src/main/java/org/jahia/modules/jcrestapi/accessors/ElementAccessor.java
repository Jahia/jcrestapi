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
import org.jahia.modules.jcrestapi.APIObjectFactory;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.Utils;
import org.jahia.modules.jcrestapi.links.JSONLinkable;
import org.jahia.modules.json.*;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Christophe Laprun
 */
public abstract class ElementAccessor<C extends JSONSubElementContainer, T extends JSONNamed, U extends JSONItem> {
    public static JSONObjectFactory<JSONLinkable> getFactory() {
        return APIObjectFactory.getInstance();
    }

    protected Object getElement(Node node, String subElement) throws RepositoryException {
        if (!Utils.exists(subElement)) {
            return getSubElementContainer(node);
        } else {
            return getSubElement(node, subElement);
        }
    }

    protected JSONNode getParentFrom(Node node) throws RepositoryException {
        return getFactory().createNode(node, 0);
    }

    protected abstract C getSubElementContainer(Node node) throws RepositoryException;

    protected abstract T getSubElement(Node node, String subElement) throws RepositoryException;

    protected abstract void delete(Node node, String subElement) throws RepositoryException;

    protected abstract CreateOrUpdateResult<T> createOrUpdate(Node node, String subElement, U childData) throws RepositoryException;

    public Response perform(Node node, String subElement, String operation, U childData, UriInfo context) throws RepositoryException {
        if (API.DELETE.equals(operation)) {
            delete(node, subElement);
            return Response.noContent().build();
        } else if (API.CREATE_OR_UPDATE.equals(operation)) {
            final CreateOrUpdateResult<T> result = createOrUpdate(node, subElement, childData);
            final T entity = result.item;
            if (result.isUpdate) {
                return Response.ok(entity).build();
            } else {
                return Response.created(context.getAbsolutePath()).entity(entity).build();
            }
        } else if (API.READ.equals(operation)) {
            final Object element = getElement(node, subElement);
            return element == null ? Response.status(Response.Status.NOT_FOUND).build() : Response.ok(element).build();
        }

        throw new UnsupportedOperationException("Unsupported operation: " + operation);
    }

    public Response perform(Node node, List<String> subElements, String operation, List<U> childData, UriInfo context) throws RepositoryException {
        if (API.DELETE.equals(operation)) {
            for (String subElement : subElements) {
                delete(node, subElement);
            }
            return getSeeOtherResponse(node, context);
        } else if (API.CREATE_OR_UPDATE.equals(operation)) {
            for (U child : childData) {
                createOrUpdate(node, null, child);
            }
            return getSeeOtherResponse(node, context);
        } else if (API.READ.equals(operation)) {
            List<T> result = new ArrayList<T>(subElements.size());
            for (String subElement : subElements) {
                final T element = getSubElement(node, subElement);
                if (element != null) {
                    result.add(element);
                }
            }
            return Response.ok(result).build();
        }

        throw new IllegalArgumentException("Unknown operation: '" + operation + "'");
    }

    private Response getSeeOtherResponse(Node node, UriInfo context) throws RepositoryException {
        return Response.seeOther(context.getAbsolutePath()).build(); // request should be made on path we need to redirect to
    }

    public static Response getSeeOtherResponse(String seeOtherURIAsString, UriInfo context) throws RepositoryException {
        try {
            final URI seeOtherURI = new URI(URIUtils.addModulesContextTo(seeOtherURIAsString, context));
            return Response.seeOther(seeOtherURI).build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Couldn't get a proper See Other URI from " + seeOtherURIAsString, e);
        }
    }

    protected abstract String getSeeOtherURIAsString(Node node);

    protected static class CreateOrUpdateResult<T extends JSONNamed> {
        final boolean isUpdate;
        final T item;

        protected CreateOrUpdateResult(boolean isUpdate, T item) {
            this.isUpdate = isUpdate;
            this.item = item;
        }
    }
}
