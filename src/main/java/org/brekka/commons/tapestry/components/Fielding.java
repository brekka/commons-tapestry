/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brekka.commons.tapestry.components;

import java.util.List;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.Block;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Field;
import org.apache.tapestry5.MarkupWriter;
import org.apache.tapestry5.ValidationDecorator;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.HeartbeatDeferred;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.dom.Element;
import org.apache.tapestry5.internal.structure.BlockImpl;
import org.apache.tapestry5.internal.structure.ComponentPageElementImpl;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.runtime.Component;
import org.apache.tapestry5.runtime.RenderCommand;

/**
 * Fielding
 * 
 * @author Andrew Taylor (andrew@brekka.org)
 */
public class Fielding {

    @Parameter(required = false, defaultPrefix = BindingConstants.MESSAGE)
    private String label;
    
    @Inject
    private ComponentResources resources;
    
    @Environmental
    private ValidationDecorator decorator;
    
    private Block toRender;
    private Element labelElement;
    private Field field;

    void beginRender(MarkupWriter writer) throws Exception {
        toRender = resources.getBody();
        
        writer.element("div", "class", "bkc-fielding");
        
        writer.element("div", "class", "bkc-label");
        field = extractField();
        
        decorator.beforeField(field);
        
        if (field == null 
                && label == null) {
            // No label
        } else {
            labelElement = writer.element("label");
            
            if (field != null) {
                 updateLabelAttributes();
            }
            
            if (label != null) {
                writer.write(label);
            } else {
                writer.write(field.getLabel());
            }
            writer.end();
        }
        
        writer.end();
        
        writer.element("div", "class", "bkc-value");
    }
    
    /**
     * @return
     */
    @SuppressWarnings("unchecked")
    private Field extractField() {
        Field field = null;
        BlockImpl block = (BlockImpl) toRender;
        List<RenderCommand> elements;
        try {
            java.lang.reflect.Field elementsField = block.getClass().getDeclaredField("elements");
            if (!elementsField.isAccessible()) {
                elementsField.setAccessible(true);
            }
            elements = (List<RenderCommand>) elementsField.get(block);
        } catch (Exception e) {
            // Should not happen unless security manager is present or Tapestry changes.
            throw new IllegalStateException("Unable to extract elements from the block");
        }
        for (RenderCommand renderCommand : elements) {
            if (renderCommand instanceof ComponentPageElementImpl) {
                ComponentPageElementImpl cpei = (ComponentPageElementImpl) renderCommand;
                Component component = cpei.getComponent();
                if (component instanceof Field) {
                    if (field != null) {
                        // Too many fields, can use fielding.
                        throw new IllegalStateException("More than one field found in this fielding");
                    }
                    field = (Field) component;
                }
            }
        }
        return field;
    }

    @HeartbeatDeferred
    private void updateLabelAttributes() {
        String fieldId = field.getClientId();
        labelElement.forceAttributes("for", fieldId);
        decorator.insideLabel(field, labelElement);
    }

    void afterRenderBody(MarkupWriter writer) {
        writer.end();
        
        writer.element("div", "class", "clear");
        writer.write("");
        writer.end();
        writer.end();
    }
}
