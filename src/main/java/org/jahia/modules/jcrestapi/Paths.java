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

import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jahia.api.Constants;
import org.jahia.modules.jcrestapi.accessors.ElementAccessor;
import org.jahia.modules.jcrestapi.json.APINode;
import org.jahia.modules.json.Filter;
import org.jahia.modules.json.JSONItem;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Christophe Laprun
 */
@Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
public class Paths extends API {

    static final String MAPPING = "paths";
    private static final String[] ALLOWED_METHODS = new String[]{"OPTIONS", "GET", "PUT", "POST", "DELETE"};

    /**
     * Records how many segments in API.API_PATH/{workspace}/{language}/Paths.MAPPING must be ignored to get actual path
     */
    private static final int IGNORE_SEGMENTS = getSegmentsNbFrom(API.API_PATH) + 2 + getSegmentsNbFrom(MAPPING);

    public Paths(String workspace, String language, Repository repository, UriInfo context) {
        super(workspace, language, repository, context);
    }

    private Object performByPath(UriInfo context, String operation, Object data) {

        // only consider useful segments
        final List<PathSegment> usefulSegments = getUsefulSegments(context);
        int index = 0;
        for (PathSegment segment : usefulSegments) {
            // check if segment is a sub-element marker
            String subElementType = segment.getPath();
            ElementAccessor accessor = ACCESSORS.get(subElementType);
            if (accessor != null) {
                String nodePath = computePathUpTo(usefulSegments, index);
                String subElement = getSubElement(usefulSegments, index);
                JSONItem converted;
                if (data != null) {
                    if (data instanceof String) {
                        String dataAsString = (String) data;
                        try {
                            converted = accessor.convertFrom(dataAsString);
                        } catch (Exception e) {
                            throw new APIException(e.getCause(), operation, NodeAccessor.BY_PATH.getType(), nodePath, subElementType, Collections.singletonList(subElement), data);
                        }
                    } else if (data instanceof List) {
                        List<String> dataAsList = (List<String>) data;
                        return performBatchDelete(workspace, language, nodePath, subElementType, dataAsList, context,
                                NodeAccessor.BY_PATH);
                    }
                    else {
                        throw new APIException(new IllegalArgumentException("Unknown payload type"), operation,
                                NodeAccessor.BY_PATH.getType(), nodePath,
                                subElementType, Collections.singletonList(subElement), data);
                    }
                } else {
                    converted = null;
                }
                return perform(workspace, language, nodePath, subElementType, subElement, context, operation, converted, NodeAccessor.BY_PATH);
            }
            index++;
        }

        // todo: check
        return perform(workspace, language, computePathUpTo(usefulSegments, usefulSegments.size()), "", "", context, operation, null, NodeAccessor.BY_PATH);
    }

    protected String[] allowedMethods() {
        return ALLOWED_METHODS;
    }

    @GET
    @Path("/{path: .*}")
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    public Object get(@PathParam("path") String path,
                      @Context UriInfo context) {
        return performByPath(context, READ, null);
    }

    @PUT
    @Path("/{path: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    public Object createOrUpdate(String childDataAsJSON,
                                 @Context UriInfo context) {
        return performByPath(context, CREATE_OR_UPDATE, childDataAsJSON);
    }

    @POST
    @Path("/{path: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    public Object createOrUpdateChildNode(String childData,
                                          @Context UriInfo context) {
        return performByPath(context, CREATE_OR_UPDATE, childData);
    }

    @DELETE
    @Path("/{path: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object delete(List<String> subElementsToDelete, @Context UriInfo context) {
        return performByPath(context, DELETE, subElementsToDelete);
    }

    private List<PathSegment> getUsefulSegments(UriInfo context) {

        final List<PathSegment> pathSegments = context.getPathSegments();
        final List<PathSegment> usefulSegments = new ArrayList<PathSegment>(pathSegments.size());

        // skip all initial empty segments
        int nbOfEmptySegments = 0;
        while (pathSegments.get(nbOfEmptySegments).getPath().isEmpty()) {
            nbOfEmptySegments++;
        }

        for (int fromIndex = IGNORE_SEGMENTS + nbOfEmptySegments; fromIndex < pathSegments.size(); fromIndex++) {
            final PathSegment pathSegment = pathSegments.get(fromIndex);
            final String path = pathSegment.getPath();
            if (!path.isEmpty() && !MAPPING.equals(path)) {
                usefulSegments.add(pathSegment);
            }
        }

        return usefulSegments;
    }

    private static int getSegmentsNbFrom(String path) {
        final String[] split = path.split("/");
        int segments = 0;
        for (String s : split) {
            // only include non-empty segments
            if (!s.isEmpty()) {
                segments++;
            }
        }
        return segments;
    }

    @POST
    @Path("/{path: .*}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
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
            final Node node = NodeAccessor.BY_PATH.getNode(idOrPath, session);

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

                final APINode apiNode = getFactory().createAPINode(childNode, Filter.OUTPUT_ALL, false, false, true);
                // since IE attempts to download the return JSON, we state that we produce plain/text and return a String representation of the JSONNode instead
                final String jsonString = apiNode.asJSONString();
                final Response.ResponseBuilder builder = isUpdate ? Response.ok(jsonString) : Response.created(context.getAbsolutePath()).entity(jsonString);
                return builder.build();
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new APIException(e, UPLOAD, NodeAccessor.BY_PATH.getType(), idOrPath, null, Collections.singletonList(fileName), null);
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
