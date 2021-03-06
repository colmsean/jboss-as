/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.logging;

import javax.xml.stream.XMLStreamException;

import org.jboss.staxmapper.XMLExtendedStreamWriter;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LoggerElement extends AbstractLoggerElement<LoggerElement> {

    private static final long serialVersionUID = -7380623095970294691L;

    private final String name;

    private boolean useParentHandlers = true;

    LoggerElement(final String name) {
        this.name = name;
    }

    @Override
    protected Class<LoggerElement> getElementClass() {
        return LoggerElement.class;
    }

    public String getName() {
        return name;
    }

    public boolean isUseParentHandlers() {
        return useParentHandlers;
    }

    void setUseParentHandlers(final boolean useParentHandlers) {
        this.useParentHandlers = useParentHandlers;
    }


    @Override
    protected void writeAttributes(final XMLExtendedStreamWriter streamWriter) throws XMLStreamException {
        streamWriter.writeAttribute(Attribute.CATEGORY.getLocalName(), name);
        if (!useParentHandlers) {
            streamWriter.writeAttribute(Attribute.USE_PARENT_HANDLERS.getLocalName(), Boolean.valueOf(useParentHandlers).toString());
        }
    }
}
