/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.json;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.xml.bind.annotation.XmlElement;
import java.io.IOException;

/**
 * @author Christophe Laprun
 */
public abstract class JSONBase<T extends JSONDecorator<T>> {
    private T decorator;
    private final JSONDecorator<T> nullOpDecorator = new NullDecorator();

    protected JSONBase(T decorator) {
        this.decorator = decorator;
    }

    @JsonUnwrapped
    @JsonDeserialize(using = DecoratorDeserializer.class)
    @XmlElement
    public T getDecorator() {
        return decorator;
    }

    protected JSONDecorator<T> getDecoratorOrNullOpIfNull() {
        return decorator != null ? decorator : nullOpDecorator;
    }

    protected T getNewDecoratorOrNull() {
        return decorator == null ? null : decorator.newInstance();
    }

    private class NullDecorator implements JSONDecorator<T> {

        @Override
        public void initFrom(JSONSubElementContainer<T> subElementContainer) {

        }

        @Override
        public <I extends Item> void initFrom(JSONItem<I, T> jsonItem, I item) throws RepositoryException {

        }

        @Override
        public void initFrom(JSONNode<T> jsonNode) {

        }

        @Override
        public void initFrom(JSONProperty<T> jsonProperty) throws RepositoryException {

        }

        @Override
        public T newInstance() {
            return null;
        }

        @Override
        public void initFrom(JSONVersion<T> jsonVersion, Version version) throws RepositoryException {

        }

        @Override
        public void initFrom(JSONMixin<T> jsonMixin) {

        }
    }

    public static class DecoratorDeserializer extends JsonDeserializer<Object> {

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            // for now assume that decorator data is never passed to the API and therefore doesn't need to be deserialized
            // if we ever need to deserialize, we should probably register decorators with this deserializer and use the tactics as described
            // in http://programmerbruce.blogspot.fr/2011/05/deserialize-json-with-jackson-into.html section #6 where the decorator class
            // would be selected based on the unique name of its root element
            return null;
        }
    }
}
