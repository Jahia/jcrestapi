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
package org.jahia.modules.jcrestapi;

import org.jahia.modules.jcrestapi.accessors.ElementAccessor;
import org.jahia.modules.json.JSONConstants;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONProperty;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
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

    /**
     * Renames the node the request resolves to, and answers it the way the rest of the API answers a node: the node's
     * primary type must be one the API exposes, and the caller's scope must allow the move on it.
     *
     * <p>The new name resolves as a relative path, so a name such as {@code ../sibling/name} lands the node under
     * another parent. A rename that changes the node's parent therefore answers for that parent too.
     *
     * @param id      the identifier of the node to rename
     * @param newName the name the node takes
     * @param context a UriInfo instance, automatically injected, providing context about the request URI
     * @return a Response ready to be sent to the client
     */
    @POST
    @Path("/{id}/moveto/{newName}")
    public Object renameNode(@PathParam("id") String id,
                             @PathParam("newName") String newName,
                             @Context UriInfo context) {
        Session session = null;
        try {
            session = getSession(workspace, language);
            final Node node = session.getNodeByIdentifier(id);
            checkNodeIsInScope(node, MOVE);
            checkDestinationIsInScope(node, newName);
            session.move(node.getPath(), getDestinationPath(node, newName));
            session.save();
            return ElementAccessor.getSeeOtherResponse(URIUtils.getIdURI(id), context);
        } catch (Exception e) {
            throw new APIException(e);
        } finally {
            closeSession(session);
        }
    }

    /**
     * Checks that the node a rename lands the node under is exposed by the API, when that is not the node's own
     * parent.
     *
     * <p>The new name resolves the way a JCR path resolves, as a <em>relative path</em> rather than a name, so
     * {@code ../sibling/name} lands the node under another parent and {@code .} lands it beside its own parent. The
     * destination is therefore resolved here through the same path {@link Session#move} builds, with {@code /..}
     * appended so that JCR answers with the node that gains the child. The node checked is the node the move
     * reaches, whatever shape the name takes, and containment is deliberately not enforced: moving a node under
     * another parent is what such a name is for.
     *
     * <p>A name that keeps the node under its own parent reaches no node the request has not already answered for,
     * so it is left alone. A name that resolves to no node, or to no node at all above the root, is left to JCR,
     * which reports it the same way as before.
     *
     * @param node    the node the request resolved to
     * @param newName the name the node takes, resolved relative to that node's parent
     * @throws PathNotFoundException if the node the rename lands the node under is not exposed by the API
     * @throws RepositoryException   if that node cannot be read
     */
    private void checkDestinationIsInScope(Node node, String newName) throws RepositoryException {
        final Node sourceParent = node.getParent();
        final Node destinationParent = node.getSession().getNode(getDestinationPath(node, newName) + "/..");
        if (!destinationParent.isSame(sourceParent)) {
            checkNodeIsInScope(destinationParent, MOVE);
        }
    }

    /**
     * Builds the path a rename moves the node to. The new name resolves relative to the node's parent, so it is joined
     * to that parent's path. The root node's path is the separator on its own, so joining a name to it directly leaves
     * an empty segment in the path, and JCR refuses such a path.
     *
     * @param node    the node the request resolved to
     * @param newName the name the node takes, resolved relative to that node's parent
     * @return the absolute path the node moves to
     * @throws RepositoryException if the node's parent cannot be read
     */
    private static String getDestinationPath(Node node, String newName) throws RepositoryException {
        final String parentPath = node.getParent().getPath();
        return ("/".equals(parentPath) ? "" : parentPath) + "/" + newName;
    }
}
