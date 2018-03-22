/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.Utils;
import org.jahia.modules.jcrestapi.json.APIObjectFactory;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.json.JSONItem;
import org.jahia.modules.json.JSONNamed;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONSubElementContainer;


/**
 * @author Christophe Laprun
 */
public abstract class ElementAccessor<C extends JSONSubElementContainer<APIDecorator>, T extends JSONNamed<APIDecorator>, U extends JSONItem> {
    static final ObjectMapper mapper = new JacksonJaxbJsonProvider().locateMapper(JSONNode.class, MediaType.APPLICATION_JSON_TYPE);

    public static APIObjectFactory getFactory() {
        return APIObjectFactory.getInstance();
    }

    protected Object getElement(Node node, String subElement, UriInfo context) throws RepositoryException {
        if (!Utils.exists(subElement)) {
            return getSubElementContainer(node, context);
        } else {
            return getSubElement(node, subElement, context);
        }
    }

    protected JSONNode<APIDecorator> getParentFrom(Node node) throws RepositoryException {
        return getFactory().createNode(node, 0);
    }

    protected abstract C getSubElementContainer(Node node, UriInfo context) throws RepositoryException;

    protected abstract T getSubElement(Node node, String subElement, UriInfo context) throws RepositoryException;

    protected abstract void delete(Node node, String subElement) throws RepositoryException;

    protected abstract CreateOrUpdateResult<T> createOrUpdate(Node node, String subElement, U childData) throws RepositoryException;

    public JSONItem convertFrom(String rawJSONData) throws Exception {
        return mapper.readValue(rawJSONData, JSONNode.class);
    }

    public Response perform(Node node, String subElement, String operation, U childData, UriInfo context) throws RepositoryException {
        if (API.DELETE.equals(operation)) {
            delete(node, subElement);
            return Response.noContent().build();
        } else if (API.CREATE_OR_UPDATE.equals(operation)) {
            if (childData == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Missing body").build();
            }
            final CreateOrUpdateResult<T> result = createOrUpdate(node, subElement, childData);
            final T entity = result.item;
            if (result.isUpdate) {
                return Response.ok(entity).build();
            } else {
                return Response.created(context.getAbsolutePath()).entity(entity).build();
            }
        } else if (API.READ.equals(operation)) {
            final Object element = getElement(node, subElement, context);
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
                final T element = getSubElement(node, subElement, context);
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
