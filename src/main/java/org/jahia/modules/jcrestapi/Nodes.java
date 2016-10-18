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
package org.jahia.modules.jcrestapi;

import org.jahia.modules.jcrestapi.accessors.ElementAccessor;
import org.jahia.modules.json.JSONConstants;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONProperty;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author Christophe Laprun
 */
@Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
public class Nodes extends API {

    static final String MAPPING = "nodes";

    public Nodes(String workspace, String language, Repository repository, UriInfo context) {
        super(workspace, language, repository, context);
    }

    @OPTIONS
    public Object options() {
        return Response.ok("Allow: OPTIONS,GET,PUT,POST,DELETE").build();
    }

    @GET
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    /**
     * Needed to get URI without trailing / to work :(
     */
    public Object getRootNode(@Context UriInfo context) {
        return perform(workspace, language, "", "", "", context, READ, null);
    }

    @GET
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    @Path("/{id: [^/]*}{subElementType: (/(" + JSONConstants.CHILDREN +
            "|" + JSONConstants.MIXINS +
            "|" + JSONConstants.PROPERTIES +
            "|" + JSONConstants.VERSIONS +
            "))?}{subElement: .*}")
    public Object getNodeById(@PathParam("id") String id,
                              @PathParam("subElementType") String subElementType,
                              @PathParam("subElement") String subElement,
                              @Context UriInfo context) {
        return perform(workspace, language, id, subElementType, subElement, context, READ, null);
    }

    @PUT
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    @Path("/{id: [^/]*}{subElementType: (/(" + JSONConstants.CHILDREN +
            "|" + JSONConstants.MIXINS +
            "|" + JSONConstants.PROPERTIES +
            "|" + JSONConstants.VERSIONS +
            "))?}{subElement: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createOrUpdateChildNode(@PathParam("id") String id,
                                          @PathParam("subElementType") String subElementType,
                                          @PathParam("subElement") String subElement,
                                          JSONNode childData,
                                          @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, subElementType, subElement);
        return perform(workspace, language, context, CREATE_OR_UPDATE, childData, NodeAccessor.BY_ID, processor);
    }

    @POST
    @Path("/{id: [^/]*}/" + JSONConstants.CHILDREN)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    public Object createAutomaticallyNamedChildOrProperty(@PathParam("id") String id,
                                          JSONNode childData,
                                          @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, JSONConstants.CHILDREN, null);
        return perform(workspace, language, context, CREATE_OR_UPDATE, childData, NodeAccessor.BY_ID, processor);
    }

    @PUT
    @Path("/{id: [^/]*}/" + JSONConstants.PROPERTIES + "/{subElement}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    public Object createOrUpdateProperty(@PathParam("id") String id,
                                         @PathParam("subElement") String subElement,
                                         JSONProperty childData,
                                         @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, JSONConstants.PROPERTIES, subElement);
        return perform(workspace, language, context, CREATE_OR_UPDATE, childData, NodeAccessor.BY_ID, processor);
    }

    @GET
    @Path("/{id: [^/]*}/" + JSONConstants.PROPERTIES + "/{subElement}")
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    public Object getProperty(@PathParam("id") String id,
                              @PathParam("subElement") String subElement,
                              @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, JSONConstants.PROPERTIES, subElement);
        return perform(workspace, language, context, READ, null, NodeAccessor.BY_ID, processor);
    }

    @DELETE
    @Path("/{id: [^/]*}/" + JSONConstants.PROPERTIES + "/{subElement}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object deleteProperty(@PathParam("id") String id,
                                 @PathParam("subElement") String subElement,
                                 @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, JSONConstants.PROPERTIES, subElement);
        return perform(workspace, language, context, DELETE, null, NodeAccessor.BY_ID, processor);
    }

    @DELETE
    @Path("/{id: [^/]*}{subElementType: (/(" + JSONConstants.CHILDREN +
            "|" + JSONConstants.MIXINS +
            "|" + JSONConstants.PROPERTIES +
            "|" + JSONConstants.VERSIONS +
            "))?}{subElement: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object deleteNode(@PathParam("id") String id,
                             @PathParam("subElementType") String subElementType,
                             @PathParam("subElement") String subElement,
                             List<String> subElementsToDelete,
                             @Context UriInfo context) {
        if (subElementsToDelete != null) {
            return performBatchDelete(workspace, language, id, subElementType, subElementsToDelete, context, NodeAccessor.BY_ID);
        }
        return perform(workspace, language, id, subElementType, subElement, context, DELETE, null);
    }

    @POST
    @Path("/{id}/moveto/{newName}")
    public Object renameNode(@PathParam("id") String id,
                             @PathParam("newName") String newName,
                             @Context UriInfo context) {
        Session session = null;
        try {
            session = getSession(workspace, language);
            final Node node = session.getNodeByIdentifier(id);
            session.move(node.getPath(), node.getParent().getPath() + "/" + newName);
            session.save();
            return ElementAccessor.getSeeOtherResponse(URIUtils.getIdURI(id), context);
        } catch (Exception e) {
            throw new APIException(e);
        } finally {
            closeSession(session);
        }
    }
}
