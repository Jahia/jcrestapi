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

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.api.Constants;
import org.jahia.modules.jcrestapi.accessors.ElementAccessor;
import org.jahia.modules.jcrestapi.model.JSONNode;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Christophe Laprun
 */
public class Paths extends API {

    static final String MAPPING = "paths";

    /**
     * Records how many segments in API.API_PATH/{workspace}/{language}/Paths.MAPPING must be ignored to get actual path
     */
    private static final AtomicInteger IGNORE_SEGMENTS = new AtomicInteger(-1);

    public Paths(String workspace, String language) {
        super(workspace, language);
    }

    @GET
    @Path("/{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getByPath(@PathParam("path") String path,
                            @Context UriInfo context) {

        // only consider useful segments
        final List<PathSegment> usefulSegments = getUsefulSegments(context);
        int index = 0;
        for (PathSegment segment : usefulSegments) {
            // check if segment is a sub-element marker
            String subElementType = segment.getPath();
            ElementAccessor accessor = accessors.get(subElementType);
            if (accessor != null) {
                String nodePath = computePathUpTo(usefulSegments, index);
                String subElement = getSubElement(usefulSegments, index);
                return perform(workspace, language, nodePath, subElementType, subElement, context, READ, null, NodeAccessor.byPath);
            }
            index++;
        }

        return perform(workspace, language, computePathUpTo(usefulSegments, usefulSegments.size()), "", "", context, READ, null, NodeAccessor.byPath);
    }

    private List<PathSegment> getUsefulSegments(UriInfo context) {
        final List<PathSegment> pathSegments = context.getPathSegments();
        return pathSegments.subList(getNumberOfIgnoredSegments(), pathSegments.size());
    }

    private static int getNumberOfIgnoredSegments() {
        if(IGNORE_SEGMENTS.get() == -1) {
            IGNORE_SEGMENTS.set(getSegmentsNbFrom(API.API_PATH) + 2 + getSegmentsNbFrom(MAPPING));
        }

        return IGNORE_SEGMENTS.get();
    }

    private static int getSegmentsNbFrom(String path) {
        final String[] split = path.split("/");
        int segments = 0;
        for (String s : split) {
            // only include non-empty segments
            if(!s.isEmpty()) {
                segments++;
            }
        }
        return segments;
    }

    @POST
    @Path("/{path: .*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Object upload(@PathParam("path") String path,
                         @FormDataParam("file") FormDataBodyPart part,
                         @Context UriInfo context) {
        final List<PathSegment> usefulSegments = getUsefulSegments(context);
        final ElementsProcessor processor = new ElementsProcessor(computePathUpTo(usefulSegments, usefulSegments.size()), "", "");
        Session session = null;

        final String idOrPath = processor.getIdOrPath();
        String fileName = null;
        try {
            session = getSession(workspace, language);
            final Node node = NodeAccessor.byPath.getNode(idOrPath, session);

            // check that the node is a folder
            if (node.isNodeType(Constants.NT_FOLDER)) {

                // get the file name
                fileName = part.getContentDisposition().getFileName();
                boolean isUpdate = false;
                if (fileName == null) {
                    // if we didn't get a file name for some reason, create one
                    fileName = node.getName() + System.currentTimeMillis();
                } else {
                    // check if we've already have a child with the same name, in which case we want to update
                    // todo: support same name siblings?
                    isUpdate = node.hasNode(fileName);
                }

                Node childNode;
                Node contentNode;
                if (isUpdate) {
                    // retrieve the existing node
                    childNode = node.getNode(fileName);

                    // check that we're dealing with a jnt:file node
                    if (!childNode.isNodeType(Constants.NT_FILE)) {
                        throw new IllegalArgumentException(fileName + " already exists and is not a " + Constants.NT_FILE + " node!");
                    } else {
                        contentNode = childNode.getNode(Constants.JCR_CONTENT);
                    }
                } else {
                    // create the node
                    childNode = node.addNode(fileName, Constants.JAHIANT_FILE);

                    // actual content is in a jcr:content child node
                    contentNode = childNode.addNode(Constants.JCR_CONTENT, Constants.JAHIANT_RESOURCE);
                }

                InputStream stream = new BufferedInputStream(part.getEntityAs(InputStream.class));
                Binary binary = session.getValueFactory().createBinary(stream);
                contentNode.setProperty(Constants.JCR_DATA, binary);
                contentNode.setProperty(Constants.JCR_MIMETYPE, part.getMediaType().toString());

                session.save();

                final JSONNode jsonNode = new JSONNode(childNode, 0);
                final Response.ResponseBuilder builder = isUpdate ? Response.ok(jsonNode) : Response.created(context.getAbsolutePath()).entity(jsonNode);
                return builder.build();
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new APIException(e, "upload", NodeAccessor.BY_PATH, idOrPath, null, Collections.singletonList(fileName), null);
        } finally {
            closeSession(session);
        }
    }

    private static String computePathUpTo(List<PathSegment> segments, int index) {
        StringBuilder path = new StringBuilder(30 * index);
        for (int i = 0; i < index; i++) {
            final String segment = segments.get(i).getPath();
            if (!segment.isEmpty()) {
                path.append("/").append(segment);
            }
        }

        final String result = path.toString();
        return !result.isEmpty() ? result : "/";
    }

    private static String getSubElement(List<PathSegment> segments, int index) {
        final int next = index + 1;
        if (next < segments.size()) {
            return segments.get(next).getPath();
        } else {
            return "";
        }
    }
}
