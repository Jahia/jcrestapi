/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
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
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
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
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
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
import org.jahia.modules.jcrestapi.model.JSONNode;
import org.jahia.modules.jcrestapi.model.JSONProperty;

import javax.jcr.Node;
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
public class Nodes extends API {
    static final String MAPPING = "nodes";

    public Nodes(String workspace, String language) {
        super(workspace, language);
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Needed to get URI without trailing / to work :(
     */
    public Object getRootNode(@Context UriInfo context) {
        return perform(workspace, language, "", "", "", context, READ, null);
    }

    @GET
    @Path("/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getNodeById(@PathParam("id") String id,
                              @PathParam("subElementType") String subElementType,
                              @PathParam("subElement") String subElement,
                              @Context UriInfo context) {
        return perform(workspace, language, id, subElementType, subElement, context, READ, null);
    }

    @PUT
    @Path("/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createOrUpdateChildNode(@PathParam("id") String id,
                                          @PathParam("subElementType") String subElementType,
                                          @PathParam("subElement") String subElement,
                                          JSONNode childData,
                                          @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, subElementType, subElement);
        if (childData == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing body").build();
        }
        return perform(workspace, language, context, CREATE_OR_UPDATE, childData, NodeAccessor.byId, processor);
    }

    @PUT
    @Path("/{id: [^/]*}/properties/{subElement}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createOrUpdateProperty(@PathParam("id") String id,
                                         @PathParam("subElement") String subElement,
                                         JSONProperty childData,
                                         @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, PROPERTIES, subElement);
        return perform(workspace, language, context, CREATE_OR_UPDATE, childData, NodeAccessor.byId, processor);
    }

    @GET
    @Path("/{id: [^/]*}/properties/{subElement}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getProperty(@PathParam("id") String id,
                              @PathParam("subElement") String subElement,
                              @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, PROPERTIES, subElement);
        return perform(workspace, language, context, READ, null, NodeAccessor.byId, processor);
    }

    @DELETE
    @Path("/{id: [^/]*}/properties/{subElement}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object deleteProperty(@PathParam("id") String id,
                                 @PathParam("subElement") String subElement,
                                 @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, PROPERTIES, subElement);
        return perform(workspace, language, context, DELETE, null, NodeAccessor.byId, processor);
    }

    @DELETE
    @Path("/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    public Object deleteNode(@PathParam("id") String id,
                             @PathParam("subElementType") String subElementType,
                             @PathParam("subElement") String subElement,
                             List<String> subElementsToDelete,
                             @Context UriInfo context) {
        if (subElementsToDelete != null) {
            return performBatchDelete(workspace, language, id, subElementType, subElementsToDelete, context);
        }
        return perform(workspace, language, id, subElementType, subElement, context, DELETE, null);
    }

    @POST
    @Path("/{id}/moveto/{newName}")
    @Produces(MediaType.APPLICATION_JSON)
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
