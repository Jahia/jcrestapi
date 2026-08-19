package org.jahia.modules.jcrestapi.accessors;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.apache.jackrabbit.value.StringValue;
import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.Mocks;
import org.jahia.modules.jcrestapi.SpringBeansAccess;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.json.JSONMixin;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONProperty;
import org.jahia.modules.json.jcr.SessionAccess;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks which properties and mixins the accessors accept to write.
 *
 * <p>Each restriction is asserted in both directions: the restricted name is refused or skipped, and an ordinary name
 * of the same shape still goes through. A guard that refused everything would not satisfy this class.</p>
 */
public class WriteRestrictionsTest {

    private static final String RESTRICTED_PROPERTY = "j:accountLocked";
    private static final String ORDINARY_PROPERTY = "j:firstName";
    private static final String PROTECTED_PROPERTY = "j:nodename";
    private static final String RESTRICTED_MIXIN = "jmix:accessControlled";
    private static final String ORDINARY_MIXIN = "jmix:comments";
    private static final String RESTRICTED_TYPE = "jnt:member";
    private static final String ORDINARY_TYPE = "jnt:contentFolder";

    private String previousProperties;
    private String previousMixins;
    private String previousNodeTypes;

    @Before
    public void configureRestrictions() throws RepositoryException {
        // the accessors build links from the current session's workspace and language
        final Session mockSession = Mocks.createMockSession();
        SessionAccess.setCurrentSession(mockSession, "default", "en");
        URIUtils.setBaseURI(Mocks.BASE_URI);

        previousProperties = join(SpringBeansAccess.getInstance().getRestrictedProperties());
        previousMixins = join(SpringBeansAccess.getInstance().getRestrictedMixins());
        previousNodeTypes = join(SpringBeansAccess.getInstance().getRestrictedNodeTypes());

        SpringBeansAccess.getInstance().setRestrictedProperties(RESTRICTED_PROPERTY + ",j:password");
        SpringBeansAccess.getInstance().setRestrictedMixins(RESTRICTED_MIXIN);
        SpringBeansAccess.getInstance().setRestrictedNodeTypes(RESTRICTED_TYPE);
    }

    @After
    public void restoreRestrictions() {
        SpringBeansAccess.getInstance().setRestrictedProperties(previousProperties);
        SpringBeansAccess.getInstance().setRestrictedMixins(previousMixins);
        SpringBeansAccess.getInstance().setRestrictedNodeTypes(previousNodeTypes);
    }

    private static String join(java.util.Set<String> values) {
        final StringBuilder joined = new StringBuilder();
        for (String value : values) {
            joined.append(joined.length() == 0 ? "" : ",").append(value);
        }
        return joined.toString();
    }

    // ---------------------------------------------------------------- the decision itself

    @Test
    public void aConfiguredPropertyNameIsRestricted() {
        assertThat(WriteRestrictions.isRestrictedPropertyName(RESTRICTED_PROPERTY)).isTrue();
        assertThat(WriteRestrictions.isRestrictedPropertyName("j:password")).isTrue();
    }

    @Test
    public void anOrdinaryPropertyNameIsNotRestricted() {
        assertThat(WriteRestrictions.isRestrictedPropertyName(ORDINARY_PROPERTY)).isFalse();
        assertThat(WriteRestrictions.isRestrictedPropertyName("jcr:title")).isFalse();
    }

    @Test
    public void aPropertyNameIsOnlyRestrictedWhileItIsConfigured() {
        SpringBeansAccess.getInstance().setRestrictedProperties(null);

        assertThat(WriteRestrictions.isRestrictedPropertyName(RESTRICTED_PROPERTY)).isFalse();
    }

    @Test
    public void aProtectedDefinitionIsRestrictedWhateverItsName() {
        assertThat(WriteRestrictions.isRestrictedProperty(PROTECTED_PROPERTY, protectedDefinition())).isTrue();
    }

    @Test
    public void anOrdinaryDefinitionIsNotRestricted() {
        assertThat(WriteRestrictions.isRestrictedProperty(ORDINARY_PROPERTY, ordinaryDefinition())).isFalse();
    }

    @Test
    public void aConfiguredNameIsRestrictedEvenWithoutADefinition() {
        assertThat(WriteRestrictions.isRestrictedProperty(RESTRICTED_PROPERTY, null)).isTrue();
        assertThat(WriteRestrictions.isRestrictedProperty(ORDINARY_PROPERTY, null)).isFalse();
        assertThat(WriteRestrictions.isRestrictedProperty(null, null)).isFalse();
    }

    @Test
    public void aConfiguredMixinIsRestricted() {
        assertThat(WriteRestrictions.isRestrictedMixin(RESTRICTED_MIXIN)).isTrue();
        assertThat(WriteRestrictions.isRestrictedMixin(ORDINARY_MIXIN)).isFalse();
        assertThat(WriteRestrictions.isRestrictedMixin(null)).isFalse();
    }

    @Test
    public void aMixinIsOnlyRestrictedWhileItIsConfigured() {
        SpringBeansAccess.getInstance().setRestrictedMixins(null);

        assertThat(WriteRestrictions.isRestrictedMixin(RESTRICTED_MIXIN)).isFalse();
    }

    // ------------------------------------------- a whole representation: the restricted name is skipped

    @Test
    public void applyingARepresentationSkipsARestrictedPropertyAndKeepsTheRest() throws RepositoryException, IOException {
        final Node node = nodeDeclaring(RESTRICTED_PROPERTY, ORDINARY_PROPERTY);

        NodeElementAccessor.initNodeFrom(node, representationSetting(RESTRICTED_PROPERTY, ORDINARY_PROPERTY));

        verify(node, never()).setProperty(eq(RESTRICTED_PROPERTY), anyString(), anyInt());
        verify(node).setProperty(eq(ORDINARY_PROPERTY), anyString(), anyInt());
    }

    @Test
    public void applyingARepresentationSkipsAProtectedProperty() throws RepositoryException, IOException {
        final Node node = nodeDeclaring(ORDINARY_PROPERTY);
        final NodeType nodeType = node.getPrimaryNodeType();
        final PropertyDefinition protectedDefinition =
                Mocks.createPropertyDefinition(PROTECTED_PROPERTY, nodeType, StringValue.TYPE, false, nodeType.getPropertyDefinitions());
        when(protectedDefinition.isProtected()).thenReturn(true);

        NodeElementAccessor.initNodeFrom(node, representationSetting(PROTECTED_PROPERTY, ORDINARY_PROPERTY));

        verify(node, never()).setProperty(eq(PROTECTED_PROPERTY), anyString(), anyInt());
        verify(node).setProperty(eq(ORDINARY_PROPERTY), anyString(), anyInt());
    }

    @Test
    public void applyingARepresentationSkipsARestrictedMixinAndKeepsTheRest() throws RepositoryException {
        final Node node = nodeDeclaring(ORDINARY_PROPERTY);

        NodeElementAccessor.initNodeFrom(node, representationWithMixins(RESTRICTED_MIXIN, ORDINARY_MIXIN));

        verify(node, never()).addMixin(RESTRICTED_MIXIN);
        verify(node).addMixin(ORDINARY_MIXIN);
    }

    // --------------------------------- a named sub-element: the restricted name fails the request

    @Test
    public void writingARestrictedPropertyByNameIsRefused() throws RepositoryException, IOException {
        final Node node = nodeDeclaring(RESTRICTED_PROPERTY);

        try {
            new PropertyElementAccessor().perform(node, RESTRICTED_PROPERTY, API.CREATE_OR_UPDATE, valueFor(RESTRICTED_PROPERTY), Mocks.createMockUriInfo(false, null));
            fail("writing " + RESTRICTED_PROPERTY + " through the property route should have been refused");
        } catch (AccessDeniedException expected) {
            verify(node, never()).setProperty(eq(RESTRICTED_PROPERTY), anyString(), anyInt());
        }
    }

    @Test
    public void writingAnOrdinaryPropertyByNameStillWorks() throws RepositoryException, IOException {
        final Node node = nodeDeclaring(ORDINARY_PROPERTY);

        new PropertyElementAccessor().perform(node, ORDINARY_PROPERTY, API.CREATE_OR_UPDATE, valueFor(ORDINARY_PROPERTY), Mocks.createMockUriInfo(false, null));

        verify(node).setProperty(eq(ORDINARY_PROPERTY), anyString(), anyInt());
    }

    @Test
    public void deletingARestrictedPropertyByNameIsRefused() throws RepositoryException {
        final Node node = nodeDeclaring(RESTRICTED_PROPERTY);

        try {
            new PropertyElementAccessor().perform(node, RESTRICTED_PROPERTY, API.DELETE, null, Mocks.createMockUriInfo(false, null));
            fail("deleting " + RESTRICTED_PROPERTY + " through the property route should have been refused");
        } catch (AccessDeniedException expected) {
            // the property is left alone
        }
    }

    @Test
    public void addingARestrictedMixinByNameIsRefused() throws RepositoryException, IOException {
        final Node node = nodeDeclaring(ORDINARY_PROPERTY);

        try {
            new MixinElementAccessor().perform(node, RESTRICTED_MIXIN, API.CREATE_OR_UPDATE, emptyRepresentation(), Mocks.createMockUriInfo(false, null));
            fail("adding " + RESTRICTED_MIXIN + " through the mixin route should have been refused");
        } catch (AccessDeniedException expected) {
            verify(node, never()).addMixin(RESTRICTED_MIXIN);
        }
    }

    @Test
    public void removingARestrictedMixinByNameIsRefused() throws RepositoryException {
        final Node node = nodeDeclaring(ORDINARY_PROPERTY);

        try {
            new MixinElementAccessor().perform(node, RESTRICTED_MIXIN, API.DELETE, null, Mocks.createMockUriInfo(false, null));
            fail("removing " + RESTRICTED_MIXIN + " through the mixin route should have been refused");
        } catch (AccessDeniedException expected) {
            verify(node, never()).removeMixin(RESTRICTED_MIXIN);
        }
    }

    @Test
    public void addingAnOrdinaryMixinByNameStillWorks() throws RepositoryException, IOException {
        final Node node = nodeDeclaring(ORDINARY_PROPERTY);

        new MixinElementAccessor().perform(node, ORDINARY_MIXIN, API.CREATE_OR_UPDATE, emptyRepresentation(), Mocks.createMockUriInfo(false, null));

        verify(node).addMixin(ORDINARY_MIXIN);
    }

    // ---------------------------------------------------- the node type of a node the request creates

    @Test
    public void creatingARestrictedNodeTypeThroughTheChildrenRouteIsRefused() throws RepositoryException, IOException {
        final Node parent = nodeDeclaring();
        final Node created = nodeOfType(RESTRICTED_TYPE);
        when(parent.hasNode("newChild")).thenReturn(false);
        when(parent.addNode(anyString(), anyString())).thenReturn(created);

        try {
            new ChildrenElementAccessor().perform(parent, "newChild", API.CREATE_OR_UPDATE,
                    ElementAccessor.mapper.readValue("{\"type\":\"" + RESTRICTED_TYPE + "\"}", JSONNode.class),
                    Mocks.createMockUriInfo(false, null));
            fail("creating a " + RESTRICTED_TYPE + " node should have been refused");
        } catch (AccessDeniedException expected) {
            // the request is answered before the session is saved
        }
    }

    @Test
    public void creatingAnOrdinaryNodeTypeThroughTheChildrenRouteStillWorks() throws RepositoryException, IOException {
        final Node parent = nodeDeclaring();
        final Node created = nodeOfType(ORDINARY_TYPE);
        when(parent.hasNode("newChild")).thenReturn(false);
        when(parent.addNode(anyString(), anyString())).thenReturn(created);

        new ChildrenElementAccessor().perform(parent, "newChild", API.CREATE_OR_UPDATE,
                ElementAccessor.mapper.readValue("{\"type\":\"" + ORDINARY_TYPE + "\"}", JSONNode.class),
                Mocks.createMockUriInfo(false, null));

        verify(parent).addNode("newChild", ORDINARY_TYPE);
    }

    @Test
    public void aRestrictedNodeTypeIsRefusedForASubtypeToo() throws RepositoryException {
        final Node subtype = nodeOfType("jnt:externalMember");
        when(subtype.isNodeType(RESTRICTED_TYPE)).thenReturn(true);

        assertThat(WriteRestrictions.isRestrictedNode(subtype)).isTrue();
        assertThat(WriteRestrictions.isRestrictedNode(nodeOfType(ORDINARY_TYPE))).isFalse();
        assertThat(WriteRestrictions.isRestrictedNode(null)).isFalse();
    }

    @Test
    public void applyingARepresentationRefusesARestrictedChildNodeType() throws RepositoryException, IOException {
        final Node node = nodeDeclaring(ORDINARY_PROPERTY);
        final Node created = nodeOfType(RESTRICTED_TYPE);
        when(node.addNode(anyString(), anyString())).thenReturn(created);

        try {
            NodeElementAccessor.initNodeFrom(node, ElementAccessor.mapper.readValue(
                    "{\"children\":{\"c\":{\"name\":\"c\",\"type\":\"" + RESTRICTED_TYPE + "\"}}}", JSONNode.class));
            fail("a child of type " + RESTRICTED_TYPE + " should have been refused");
        } catch (AccessDeniedException expected) {
            // the request is answered before the session is saved
        }
    }

    // ------------------------------------------- the recursion, and the production definition lookup

    @Test
    public void aChildInTheSameRequestGetsTheSameTreatment() throws RepositoryException, IOException {
        final Node node = nodeDeclaring(ORDINARY_PROPERTY);
        final Node child = nodeDeclaring(RESTRICTED_PROPERTY, ORDINARY_PROPERTY);
        when(child.isNodeType(RESTRICTED_TYPE)).thenReturn(false);
        when(node.addNode(anyString(), anyString())).thenReturn(child);

        final String childJson = "{\"name\":\"c\",\"type\":\"" + ORDINARY_TYPE + "\",\"properties\":{"
                + "\"" + escape(RESTRICTED_PROPERTY) + "\":{\"value\":\"true\"},"
                + "\"" + escape(ORDINARY_PROPERTY) + "\":{\"value\":\"true\"}}}";

        NodeElementAccessor.initNodeFrom(node,
                ElementAccessor.mapper.readValue("{\"children\":{\"c\":" + childJson + "}}", JSONNode.class));

        verify(child, never()).setProperty(eq(RESTRICTED_PROPERTY), anyString(), anyInt());
        verify(child).setProperty(eq(ORDINARY_PROPERTY), anyString(), anyInt());
    }

    @Test
    public void theDefinitionLookupUsesTheWrapperOnARealNode() throws RepositoryException {
        // a real Jahia node is a JCRNodeWrapper, so production resolves the definition through the wrapper
        final JCRNodeWrapper wrapper = mock(JCRNodeWrapper.class);
        final ExtendedPropertyDefinition wrapperDefinition = mock(ExtendedPropertyDefinition.class);
        when(wrapperDefinition.isProtected()).thenReturn(true);
        when(wrapper.getApplicablePropertyDefinition(PROTECTED_PROPERTY)).thenReturn(wrapperDefinition);

        final PropertyDefinition resolved = PropertyElementAccessor.getPropertyDefinitionOnNode(PROTECTED_PROPERTY, wrapper);

        assertThat(resolved).isSameAs(wrapperDefinition);
        assertThat(WriteRestrictions.isRestrictedProperty(PROTECTED_PROPERTY, resolved)).isTrue();
    }

    @Test
    public void aRestrictedNameIsRefusedOnANodeTypeThatDoesNotDeclareIt() throws RepositoryException, IOException {
        final Node node = nodeDeclaring(ORDINARY_PROPERTY);

        try {
            new PropertyElementAccessor().perform(node, RESTRICTED_PROPERTY, API.CREATE_OR_UPDATE,
                    valueFor(RESTRICTED_PROPERTY), Mocks.createMockUriInfo(false, null));
            fail("writing " + RESTRICTED_PROPERTY + " should have been refused, whatever the node type declares");
        } catch (AccessDeniedException expected) {
            verify(node, never()).setProperty(eq(RESTRICTED_PROPERTY), anyString(), anyInt());
        }
    }

    // ---------------------------------------------------------------- helpers

    private static String escape(String name) {
        return name.replace(":", "__");
    }

    private static PropertyDefinition protectedDefinition() {
        final PropertyDefinition definition = mock(PropertyDefinition.class);
        when(definition.isProtected()).thenReturn(true);
        return definition;
    }

    private static PropertyDefinition ordinaryDefinition() {
        return mock(PropertyDefinition.class);
    }

    /**
     * A node reporting the given primary type, for the node-type decision.
     */
    private static Node nodeOfType(String typeName) throws RepositoryException {
        final Node node = Mocks.createMockNode("n-" + typeName, "id-" + typeName, Mocks.PATH_TO_NODE + "/" + typeName, 0, 0, 0);
        when(node.getPrimaryNodeType().getName()).thenReturn(typeName);
        when(node.isNodeType(typeName)).thenReturn(true);
        return node;
    }

    /**
     * A node whose type declares each of the given property names, so that the schema check the accessors make first
     * passes and the restriction is what decides the outcome.
     */
    private static Node nodeDeclaring(String... propertyNames) throws RepositoryException {
        final Node node = Mocks.createMockNode(Mocks.NODE_NAME, Mocks.NODE_ID, Mocks.PATH_TO_NODE, 0, 0, 0);
        final NodeType nodeType = node.getPrimaryNodeType();
        for (String propertyName : propertyNames) {
            Mocks.createPropertyDefinition(propertyName, nodeType, StringValue.TYPE, false, nodeType.getPropertyDefinitions());
        }
        return node;
    }

    private static JSONNode<APIDecorator> representationSetting(String... propertyNames) throws IOException {
        final StringBuilder json = new StringBuilder("{\"properties\": {");
        for (int i = 0; i < propertyNames.length; i++) {
            json.append(i == 0 ? "" : ",").append('"').append(escape(propertyNames[i])).append("\": {\"value\": \"true\"}");
        }
        return ElementAccessor.mapper.readValue(json.append("}}").toString(), JSONNode.class);
    }

    /**
     * A representation carrying the given mixin names. It is built rather than parsed because
     * {@code JSONMixin} has no constructor Jackson can call, so a body carrying a non-empty {@code mixins} map does not
     * get as far as the accessor.
     */
    @SuppressWarnings("unchecked")
    private static JSONNode<APIDecorator> representationWithMixins(String... mixinNames) {
        final Map<String, JSONMixin<APIDecorator>> mixins = new LinkedHashMap<String, JSONMixin<APIDecorator>>();
        for (String mixinName : mixinNames) {
            mixins.put(escape(mixinName), null);
        }
        final JSONNode<APIDecorator> representation = mock(JSONNode.class);
        when(representation.getMixins()).thenReturn(mixins);
        return representation;
    }

    private static JSONNode<APIDecorator> emptyRepresentation() throws IOException {
        return ElementAccessor.mapper.readValue("{}", JSONNode.class);
    }

    private static JSONProperty valueFor(String propertyName) throws IOException {
        return ElementAccessor.mapper.readValue("{\"name\":\"" + escape(propertyName) + "\",\"value\": \"true\"}", JSONProperty.class);
    }
}
