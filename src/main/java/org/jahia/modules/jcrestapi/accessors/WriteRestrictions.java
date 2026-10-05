package org.jahia.modules.jcrestapi.accessors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

import org.jahia.api.Constants;
import org.jahia.modules.jcrestapi.SpringBeansAccess;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.sites.SitesSettings;

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
 * <p>A property whose definition is {@code protected} is out of scope too, unless it is one of
 * {@link #SITE_LANGUAGE_PROPERTIES} on a site, or {@code jahia.api.jcr.additionalWritableProtectedProperties} names
 * it. That key adds to the built-in names, where the three keys above replace their default.</p>
 *
 * <p>Deleting a property follows the same rule as writing it, because a deletion writes a {@code null} value. A site
 * language property is the exception: it is never deleted through this API, because the repository rules that keep
 * the languages of the sites consistent react to a value being set, and not to a property being removed.</p>
 *
 * <p>The answer depends on the name and the node type alone, so it is the same however the request reached the
 * accessor. The one exception is a site language property, which also asks whether the caller holds
 * {@link #SITE_LANGUAGES_PERMISSION} on the site.</p>
 */
public final class WriteRestrictions {

    private WriteRestrictions() {
        // utility class
    }

    /**
     * The {@code protected} properties of {@code jnt:virtualsite} that the Languages screen of the site settings writes
     * through this API. They stay writable on a site, for a caller who holds {@link #SITE_LANGUAGES_PERMISSION} there,
     * unless {@code jahia.api.jcr.restrictedProperties} names them.
     */
    public static final Set<String> SITE_LANGUAGE_PROPERTIES = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList(SitesSettings.DEFAULT_LANGUAGE, SitesSettings.LANGUAGES, SitesSettings.MANDATORY_LANGUAGES,
                    SitesSettings.INACTIVE_LANGUAGES, SitesSettings.INACTIVE_LIVE_LANGUAGES,
                    SitesSettings.MIX_LANGUAGES_ACTIVE, SitesSettings.ALLOWS_UNLISTED_LANGUAGES)));

    /**
     * The permission that the Languages screen of the site settings requires. The site settings module defines it, so
     * on a Jahia without that module no caller holds it, and a site language property is refused like any other
     * {@code protected} property.
     */
    public static final String SITE_LANGUAGES_PERMISSION = "siteAdminLanguages";

    /**
     * Whether the given property may not be written through this API: its name is configured as restricted, or its node
     * type declares the definition {@code protected} and the name is not writable
     * ({@link #isRestrictedProtectedProperty(Node, String, PropertyDefinition)}).
     *
     * @param node         the node the request writes to, may be {@code null}
     * @param propertyName the unescaped property name the request asks to write
     * @param definition   the applicable property definition, may be {@code null}
     * @return {@code true} if the property must not be written
     * @throws RepositoryException if the node's types cannot be read
     */
    public static boolean isRestrictedProperty(Node node, String propertyName, PropertyDefinition definition)
            throws RepositoryException {
        return isRestrictedPropertyName(propertyName) || isRestrictedProtectedProperty(node, propertyName, definition);
    }

    /**
     * Whether the given property may not be deleted through this API: it may not be written
     * ({@link #isRestrictedProperty(Node, String, PropertyDefinition)}), or it is a site language property
     * ({@link #isSiteLanguageProperty(Node, String, PropertyDefinition)}).
     *
     * @param node         the node the request deletes from, may be {@code null}
     * @param propertyName the unescaped property name the request asks to delete
     * @param definition   the applicable property definition, may be {@code null}
     * @return {@code true} if the property must not be deleted
     * @throws RepositoryException if the node's types cannot be read
     */
    public static boolean isRestrictedPropertyRemoval(Node node, String propertyName, PropertyDefinition definition)
            throws RepositoryException {
        return isRestrictedProperty(node, propertyName, definition)
                || isSiteLanguageProperty(node, propertyName, definition);
    }

    /**
     * Whether the given definition is {@code protected} and its name is not writable.
     *
     * <p>A {@code protected} definition is out of scope by default. A name stays writable in two cases:</p>
     * <ul>
     *     <li>it is a site language property ({@link #isSiteLanguageProperty(Node, String, PropertyDefinition)}), and
     *     the caller holds {@link #SITE_LANGUAGES_PERMISSION} on the node;</li>
     *     <li>{@code jahia.api.jcr.additionalWritableProtectedProperties} names it, on any node type and for any
     *     caller. That key only adds names, so an empty value leaves the site language properties writable. Listing
     *     one of {@link #SITE_LANGUAGE_PROPERTIES} there has no effect: only the first case makes those writable.</li>
     * </ul>
     *
     * <p>Neither case overrides {@link #isRestrictedPropertyName(String)}, which
     * {@link #isRestrictedProperty(Node, String, PropertyDefinition)} asks first.</p>
     *
     * @param node         the node the request writes to, may be {@code null}
     * @param propertyName the unescaped property name the request asks to write
     * @param definition   the applicable property definition, may be {@code null}
     * @return {@code true} if the definition is {@code protected} and the name is not writable
     * @throws RepositoryException if the node's types cannot be read
     */
    public static boolean isRestrictedProtectedProperty(Node node, String propertyName, PropertyDefinition definition)
            throws RepositoryException {
        if (definition == null || !definition.isProtected()) {
            return false;
        }
        if (SITE_LANGUAGE_PROPERTIES.contains(propertyName)) {
            return !isSiteLanguageProperty(node, propertyName, definition) || !holdsSiteLanguagesPermission(node);
        }
        return !SpringBeansAccess.getInstance().getAdditionalWritableProtectedProperties().contains(propertyName);
    }

    /**
     * Whether the given property is one of {@link #SITE_LANGUAGE_PROPERTIES} on a site: the node is a
     * {@code jnt:virtualsite}, and that type declares the definition. A translation node of a site resolves its
     * definitions from the site, so the node's own type is asked as well.
     *
     * @param node         the node the request writes to, may be {@code null}
     * @param propertyName the unescaped property name the request asks to write
     * @param definition   the applicable property definition, may be {@code null}
     * @return {@code true} if the property is a language property of a site
     * @throws RepositoryException if the node's types cannot be read
     */
    public static boolean isSiteLanguageProperty(Node node, String propertyName, PropertyDefinition definition)
            throws RepositoryException {
        if (node == null || definition == null || !SITE_LANGUAGE_PROPERTIES.contains(propertyName)) {
            return false;
        }
        final NodeType declaringType = definition.getDeclaringNodeType();
        return declaringType != null && declaringType.isNodeType(Constants.JAHIANT_VIRTUALSITE)
                && node.isNodeType(Constants.JAHIANT_VIRTUALSITE);
    }

    /**
     * Whether the caller holds {@link #SITE_LANGUAGES_PERMISSION} on the given node. Only a Jahia node answers a Jahia
     * permission, so any other node answers {@code false}.
     */
    private static boolean holdsSiteLanguagesPermission(Node node) {
        return node instanceof JCRNodeWrapper && ((JCRNodeWrapper) node).hasPermission(SITE_LANGUAGES_PERMISSION);
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
