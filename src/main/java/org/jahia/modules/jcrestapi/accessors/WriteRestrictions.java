package org.jahia.modules.jcrestapi.accessors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.PropertyDefinition;

import org.jahia.modules.jcrestapi.SpringBeansAccess;

/**
 * Defines which JCR properties and mixins the accessors may write.
 *
 * <p>An accessor takes the name to write from the request itself: from a JSON key under {@code properties} or
 * {@code mixins}, from the node type of a child to create, or from the last segment of the URL. This API is for
 * content, so the names that carry the repository's own user, group and access-control model are out of its scope.
 * Those are maintained through their dedicated services, which apply the validation a generic write cannot.</p>
 *
 * <p>Which names are out of scope is configurable, with a restrictive default:
 * {@code jahia.api.jcr.restrictedProperties}, {@code jahia.api.jcr.restrictedMixins} and
 * {@code jahia.api.jcr.restrictedNodeTypes}. Reading is unaffected: a restricted property is still returned by a
 * {@code GET}. Modifying an existing node is unaffected by the node-type list, which governs creation.</p>
 *
 * <p>The answer depends on the name and the node type alone, never on the caller or on the session the write runs
 * under, so it is the same however the request reached the accessor.</p>
 */
public final class WriteRestrictions {

    private WriteRestrictions() {
        // utility class
    }

    /**
     * Whether the given property may not be written through this API: its name is configured as restricted, or its node
     * type declares the definition {@code protected}.
     *
     * <p>A {@code protected} definition is maintained by the repository itself, so a request is never a legitimate
     * source for it.</p>
     *
     * @param propertyName the unescaped property name the request asks to write
     * @param definition   the applicable property definition, may be {@code null}
     * @return {@code true} if the property must not be written
     */
    public static boolean isRestrictedProperty(String propertyName, PropertyDefinition definition) {
        return isRestrictedPropertyName(propertyName) || (definition != null && definition.isProtected());
    }

    /**
     * Whether the given property name is configured as restricted
     * ({@code jahia.api.jcr.restrictedProperties}).
     *
     * @param propertyName the unescaped property name the request asks to write
     * @return {@code true} if the name is restricted
     */
    public static boolean isRestrictedPropertyName(String propertyName) {
        return propertyName != null
                && SpringBeansAccess.getInstance().getRestrictedProperties().contains(propertyName);
    }

    /**
     * Whether the given mixin may not be added or removed through this API
     * ({@code jahia.api.jcr.restrictedMixins}).
     *
     * <p>The match is on the exact type name. This differs from {@link #isRestrictedNode(Node)}, which asks
     * {@link Node#isNodeType(String)} and so answers for a subtype as well as for the type itself. A mixin that a
     * custom module declares as a subtype of a restricted mixin is therefore not matched here. No mixin in the
     * definitions Jahia ships declares a restricted mixin as its supertype.</p>
     *
     * @param mixinName the mixin type name the request asks to add or remove
     * @return {@code true} if the mixin must not be added or removed
     */
    public static boolean isRestrictedMixin(String mixinName) {
        return mixinName != null
                && SpringBeansAccess.getInstance().getRestrictedMixins().contains(mixinName);
    }

    /**
     * Whether the given node may not be created through this API
     * ({@code jahia.api.jcr.restrictedNodeTypes}).
     *
     * <p>The question is asked of the node the repository built, and not of the type name the request sent, for two
     * reasons. {@link Node#isNodeType(String)} answers for a subtype as well as for the type itself. And a request
     * that sends no type at all still gets a type, which the parent's child-node definition supplies.</p>
     *
     * @param node the node the request has just created
     * @return {@code true} if the node must not be created through this API
     * @throws RepositoryException if the node's types cannot be read
     */
    public static boolean isRestrictedNode(Node node) throws RepositoryException {
        if (node == null) {
            return false;
        }

        for (String restricted : SpringBeansAccess.getInstance().getRestrictedNodeTypes()) {
            if (node.isNodeType(restricted)) {
                return true;
            }
        }
        return false;
    }
}
