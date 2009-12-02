/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.bc.ceres.binding.swing.internal;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.swing.BindingContext;

import java.io.File;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import junit.framework.TestCase;

public class FileEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        FileEditor fileEditor = new FileEditor();
        
        PropertyDescriptor fileDescriptor = new PropertyDescriptor("test", File.class);
        assertTrue(fileEditor.isValidFor(fileDescriptor));
        
        PropertyDescriptor doubleDescriptor = new PropertyDescriptor("test", Double.TYPE);
        assertFalse(fileEditor.isValidFor(doubleDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        FileEditor fileEditor = new FileEditor();
        
        PropertyContainer propertyContainer = PropertyContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(propertyContainer);
        PropertyDescriptor propertyDescriptor = propertyContainer.getDescriptor("file");
        assertSame(File.class, propertyDescriptor.getType());
        
        assertTrue(fileEditor.isValidFor(propertyDescriptor));
        JComponent editorComponent = fileEditor.createEditorComponent(propertyDescriptor, bindingContext);
        assertNotNull(editorComponent);
        assertSame(JPanel.class, editorComponent.getClass());
        assertEquals(2, editorComponent.getComponentCount());
        
        JComponent[] components = bindingContext.getBinding("file").getComponents();
        assertEquals(1, components.length);
        assertSame(JTextField.class, components[0].getClass());
    }
    
    private static class V {
        File file;
    }
}