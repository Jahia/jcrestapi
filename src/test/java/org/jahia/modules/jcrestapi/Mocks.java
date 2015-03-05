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
package org.jahia.modules.jcrestapi;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.jackrabbit.value.ReferenceValue;
import org.apache.jackrabbit.value.StringValue;
import org.jahia.api.Constants;
import org.jahia.modules.jcrestapi.accessors.PropertyElementAccessor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author Christophe Laprun
 */
public class Mocks {

    public static final String BASE_URI = "http://api.example.org:9999/context/subContext";
    public static final String VERSION = "version";
    public static final String PATH_TO_NODE = "/path/to/node";
    public static final String PROPERTY = "property";
    public static final String REF_PROPERTY = "refProperty";
    public static final String CHILD = "child";
    public static final String NODE_ID = "nodeId";
    public static final String MIXIN = "mixin";
    public static final String VERSION_ID = "versionId";
    public static final String CHILD_ID = "childId";
    public static final String NODE_NAME = "node";

    public static final Map<String,Node> sessionNodes = new LinkedHashMap<String,Node>();

    public static Node createMockNode(String name, String id, String pathToNode) throws RepositoryException {
        return createMockNode(name, id, pathToNode, 1, 1, 1);
    }

    public static Node createMockNode(String name, String id, final String pathToNode, int numberOfChildren, int numberOfStringProperties, int numberOfMixins) throws RepositoryException {

        // mock node type
        final NodeType nodeType = createNodeType("nodeType");

        // mock parent
        Node parent = mock(Node.class);
        when(parent.getIdentifier()).thenReturn("parentId");

        // mock node
        final Node node = mock(Node.class);

        final Map<String,Property> properties = new LinkedHashMap<String,Property>();

        // mock string properties
        for (int i = 0; i < numberOfStringProperties; i++) {
            final String propertyName = PROPERTY + i;
            final Property property = createMockProperty(node, propertyName, nodeType, StringValue.TYPE, false, true);
            when(node.getProperty(propertyName)).thenReturn(property);
            properties.put(propertyName, property);
        }

        // mock children
        final List<Node> children;
        if (numberOfChildren > 0) {
            children = new ArrayList<Node>(numberOfChildren);
            for (int i = 0; i < numberOfChildren; i++) {
                final String childName = CHILD + i;
                final Node child = createMockNode(childName, CHILD_ID + i, pathToNode + "/" + childName, numberOfChildren - 1, numberOfStringProperties - 1, numberOfMixins - 1);
                children.add(child);
                when(node.getNode(childName)).thenReturn(child);
            }
        } else {
            children = Collections.emptyList();
        }

        // mock mixins
        NodeType[] mixins = null;
        if (numberOfMixins > 0) {
            mixins = new NodeType[numberOfMixins];
            for (int i = 0; i < numberOfMixins; i++) {
                final String mixinName = MIXIN + i;
                mixins[i] = createNodeType(mixinName);
            }
        }
        final NodeType[] finalMixins = mixins;

        when(node.getPrimaryNodeType()).thenReturn(nodeType);
        when(node.getIdentifier()).thenReturn(id);
        when(node.getUUID()).thenReturn(id);
        when(node.getParent()).thenReturn(parent);
        when(node.getPath()).thenReturn(pathToNode);
        // use an Answer to make sure that we always return a new iterator
        // (otherwise, it iterates through its elements and then won't work for subsequent calls).
        when(node.getNodes()).then(new Answer<NodeIterator>() {
            @Override
            public NodeIterator answer(InvocationOnMock invocation) throws Throwable {
                return new ListBackedNodeIterator(children);
            }
        });
        when(node.isNodeType(Constants.MIX_VERSIONABLE)).thenReturn(true);
        when(node.getName()).thenReturn(name);
        when(node.getMixinNodeTypes()).thenReturn(finalMixins);
        when(node.getProperties()).then(new Answer<PropertyIterator>() {
            @Override
            public PropertyIterator answer(InvocationOnMock invocation) throws Throwable {
                return new ListBackedPropertyIterator(properties.values());
            }
        });

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                NodeType[] newMixins;
                final NodeType mixin = createNodeType(invocation.getArguments()[0].toString());
                if (finalMixins != null) {
                    newMixins = Arrays.copyOf(finalMixins, finalMixins.length + 1);
                    newMixins[finalMixins.length] = mixin;
                } else {
                    newMixins = new NodeType[]{mixin};
                }

                when(node.getMixinNodeTypes()).thenReturn(newMixins);
                return null;
            }
        }).when(node).addMixin(anyString());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final PropertyDefinition[] definitions = nodeType.getPropertyDefinitions();
                final String propertyName = invocation.getArguments()[0].toString();
                final PropertyDefinition definition = PropertyElementAccessor.getPropertyDefinitionFrom(propertyName, definitions);
                if (definition != null && definition.getRequiredType() == ((Integer) invocation.getArguments()[2])) {
                    Property property = properties.get(propertyName);
                    if (property == null) {
                        property = createMockProperty(node, propertyName, nodeType, ((Integer) invocation.getArguments()[2]), false, false);
                    }
                    when(property.getValue()).thenReturn(new StringValue(invocation.getArguments()[1].toString()));
                    properties.put(propertyName, property);
                    return property;
                }
                return null;
            }
        }).when(node).setProperty(anyString(), anyString(), anyInt());

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final PropertyDefinition[] definitions = nodeType.getPropertyDefinitions();
                final String propertyName = invocation.getArguments()[0].toString();
                PropertyDefinition definition = PropertyElementAccessor.getPropertyDefinitionFrom(propertyName, definitions);
                if (definition == null) {
                    definition = createPropertyDefinition(propertyName, nodeType, ReferenceValue.TYPE, false, null);
                }
                if (definition != null && definition.getRequiredType() == ReferenceValue.TYPE) {
                    Property property = properties.get(propertyName);
                    if (property == null) {
                        property = createMockProperty(node, propertyName, nodeType, ReferenceValue.TYPE, false, false);
                    }
                    ReferenceValue referenceValue = new ReferenceValue(((Node) invocation.getArguments()[1]));
                    when(property.getValue()).thenReturn(referenceValue);
                    properties.put(propertyName, property);
                    return property;
                }
                return null;
            }
        }).when(node).setProperty(anyString(), any(Node.class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final PropertyDefinition[] definitions = nodeType.getPropertyDefinitions();
                final String propertyName = invocation.getArguments()[0].toString();
                final Value[] propertyValues = (Value[]) invocation.getArguments()[1];
                if (propertyValues == null && propertyValues.length == 0) {
                    return properties.remove(propertyName);
                }
                final int propertyType = propertyValues[0].getType();
                PropertyDefinition definition = PropertyElementAccessor.getPropertyDefinitionFrom(propertyName, definitions);
                if (definition == null) {
                    definition = createPropertyDefinition(propertyName, nodeType, propertyType, true, null);
                }
                if (definition != null && definition.getRequiredType() == propertyType) {
                    Property property = properties.get(propertyName);
                    if (property == null) {
                        property = createMockProperty(node, propertyName, nodeType, propertyType, true, false);
                    }
                    when(property.getValues()).thenReturn(propertyValues);
                    properties.put(propertyName, property);
                    return property;
                }
                return null;
            }
        }).when(node).setProperty(anyString(), any(Value[].class));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                final String name = invocation.getArguments()[0].toString();
                return createMockNode(name, name + "Id", pathToNode + "/" + name);
            }
        }).when(node).addNode(anyString());

        sessionNodes.put(id, node);

        return node;
    }

    protected static NodeType createNodeType(String typeName) {
        NodeType nodeType = mock(NodeType.class);
        when(nodeType.getName()).thenReturn(typeName);
        return nodeType;
    }

    protected static Property createMockProperty(Node parent, String propertyName, NodeType parentNodeType, Integer requiredType, boolean multiple, boolean createAssociatedDefinition) throws RepositoryException {
        PropertyDefinition propertyDefinition;
        if (createAssociatedDefinition) {
            propertyDefinition = createPropertyDefinition(propertyName, parentNodeType, requiredType, multiple, null);
        } else {
            propertyDefinition = PropertyElementAccessor.getPropertyDefinitionFrom(propertyName, parentNodeType.getPropertyDefinitions());
        }

        // mock property
        Property property = mock(Property.class);
        when(property.toString()).thenReturn("Property:" + propertyName);
        when(property.getName()).thenReturn(propertyName);
        when(property.getType()).thenReturn(requiredType);
        when(property.getDefinition()).thenReturn(propertyDefinition);
        when(property.getPath()).thenReturn(PATH_TO_NODE + "/" + propertyName);
        when(property.isMultiple()).thenReturn(multiple);

        // set node as property's parent
        when(property.getParent()).thenReturn(parent);
        return property;
    }

    public static PropertyDefinition createPropertyDefinition(String propertyName, NodeType parentNodeType, Integer requiredType, boolean multiple, final PropertyDefinition[] definitions) {
        // mock property definition
        PropertyDefinition propertyDefinition = mock(PropertyDefinition.class);
        when(propertyDefinition.getName()).thenReturn(propertyName);
        when(propertyDefinition.getRequiredType()).thenReturn(requiredType);

        // add property definition to parent node type
        PropertyDefinition[] newPropertyDefinitions;
        if (definitions != null) {
            final int length = definitions.length;
            newPropertyDefinitions = new PropertyDefinition[length + 1];
            System.arraycopy(definitions, 0, newPropertyDefinitions, 0, length);
            newPropertyDefinitions[length] = propertyDefinition;
        } else {
            newPropertyDefinitions = new PropertyDefinition[]{propertyDefinition};
        }
        when(parentNodeType.getDeclaredPropertyDefinitions()).thenReturn(newPropertyDefinitions);
        when(parentNodeType.getPropertyDefinitions()).thenReturn(newPropertyDefinitions);

        // set the property definition's node type
        when(propertyDefinition.getDeclaringNodeType()).thenReturn(parentNodeType);
        when(propertyDefinition.isMultiple()).thenReturn(multiple);
        return propertyDefinition;
    }

    public static UriInfo createMockUriInfo(boolean fullChildren) {
        final UriInfo info = mock(UriInfo.class);
        try {
            when(info.getBaseUri()).thenReturn(new URI(BASE_URI));
            MultivaluedMap<String, String> queryParams = new MultivaluedHashMap<String, String>();
            if (fullChildren) {
                queryParams.putSingle(API.INCLUDE_FULL_CHILDREN, "");
            }

            when(info.getQueryParameters()).thenReturn(queryParams);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return info;
    }

    public static Session createMockSession() throws RepositoryException {
        // mock version
        Version version = mock(Version.class);
        when(version.getName()).thenReturn(VERSION);
        when(version.getIdentifier()).thenReturn(VERSION_ID);
        when(version.getFrozenNode()).thenReturn(mock(Node.class));

        // mock version history
        VersionHistory versionHistory = mock(VersionHistory.class);
        when(versionHistory.getAllVersions()).thenReturn(new SingletonVersionIterator(version));
        when(versionHistory.getVersion(VERSION)).thenReturn(version);

        // mock version manager
        VersionManager versionManager = mock(VersionManager.class);
        when(versionManager.getVersionHistory(PATH_TO_NODE)).thenReturn(versionHistory);

        // mock workspace
        Workspace workspace = mock(Workspace.class);
        when(workspace.getVersionManager()).thenReturn(versionManager);


        Session session = mock(Session.class);
        when(session.getWorkspace()).thenReturn(workspace);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String identifier = (String) invocation.getArguments()[0];
                Node node = sessionNodes.get(identifier);
                return node;
            }
        }).when(session).getNodeByIdentifier(anyString());

        return session;
    }

    private static class EmptyNodeIterator implements NodeIterator {
        @Override
        public Node nextNode() {
            return null;
        }

        @Override
        public void skip(long skipNum) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            return 0;
        }

        @Override
        public long getPosition() {
            return 0;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ListBackedNodeIterator implements NodeIterator {
        private Iterator<Node> iterator;
        private int size;
        private int index;

        public ListBackedNodeIterator(Collection<Node> nodes) {
            this.iterator = nodes.iterator();
            this.size = nodes.size();
        }

        @Override
        public Node nextNode() {
            index++;
            return iterator.next();
        }

        @Override
        public void skip(long skipNum) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public long getPosition() {
            return index;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Object next() {
            return nextNode();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ListBackedPropertyIterator implements PropertyIterator {

        private Iterator<Property> iterator;
        private int size;
        private int index;

        public ListBackedPropertyIterator(Collection<Property> properties) {
            this.iterator = properties.iterator();
            this.size = properties.size();
        }

        @Override
        public Property nextProperty() {
            index++;
            return iterator.next();
        }

        @Override
        public void skip(long skipNum) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            return size;
        }

        @Override
        public long getPosition() {
            return index;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Object next() {
            return nextProperty();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class SingletonVersionIterator implements VersionIterator {

        private final Version version;
        private int position;

        private SingletonVersionIterator(Version version) {
            this.version = version;
        }

        @Override
        public Version nextVersion() {
            if (hasNext()) {
                position = 1;
                return version;
            }

            return null;
        }

        @Override
        public void skip(long skipNum) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            return 1;
        }

        @Override
        public long getPosition() {
            return position;
        }

        @Override
        public boolean hasNext() {
            return position == 0;
        }

        @Override
        public Object next() {
            return nextVersion();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
