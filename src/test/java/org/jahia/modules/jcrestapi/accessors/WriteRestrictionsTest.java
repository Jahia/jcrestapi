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

    @Before
    public void configureRestrictions() throws RepositoryException {
        // the accessors build links from the current session's workspace and language
        final Session mockSession = Mocks.createMockSession();
        SessionAccess.setCurrentSession(mockSession, "default", "en");
        URIUtils.setBaseURI(Mocks.BASE_URI);

        SpringBeansAccess.getInstance().setRestrictedProperties(RESTRICTED_PROPERTY + ",j:password");
        SpringBeansAccess.getInstance().setRestrictedMixins(RESTRICTED_MIXIN);
    }

    @After
    public void clearRestrictions() {
        SpringBeansAccess.getInstance().setRestrictedProperties(null);
        SpringBeansAccess.getInstance().setRestrictedMixins(null);
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
