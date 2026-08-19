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

import javax.jcr.nodetype.PropertyDefinition;

import org.jahia.modules.jcrestapi.SpringBeansAccess;

/**
 * Defines which JCR properties and mixins the accessors may write.
 *
 * <p>An accessor takes the name to write from the request itself: from a JSON key under {@code properties} or
 * {@code mixins}, or from the last segment of the URL. This API is for content, so the names that carry the
 * repository's own user, group and access-control model are out of its scope. Those are maintained through their
 * dedicated services, which apply the validation a generic write cannot.</p>
 *
 * <p>Which names are out of scope is configurable, with a restrictive default:
 * {@code jahia.api.jcr.restrictedProperties} and {@code jahia.api.jcr.restrictedMixins}. Reading is unaffected: a
 * restricted property is still returned by a {@code GET}.</p>
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
     * @param mixinName the mixin type name the request asks to add or remove
     * @return {@code true} if the mixin must not be added or removed
     */
    public static boolean isRestrictedMixin(String mixinName) {
        return mixinName != null
                && SpringBeansAccess.getInstance().getRestrictedMixins().contains(mixinName);
    }
}
