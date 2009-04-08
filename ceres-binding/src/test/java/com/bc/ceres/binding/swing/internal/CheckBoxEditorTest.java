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

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.swing.BindingContext;

import javax.swing.JCheckBox;
import javax.swing.JComponent;

import junit.framework.TestCase;

public class CheckBoxEditorTest extends TestCase {

    public void testIsApplicable() throws Exception {
        CheckBoxEditor checkBoxEditor = new CheckBoxEditor();
        
        ValueDescriptor booleanDescriptor = new ValueDescriptor("test", Boolean.TYPE);
        assertTrue(checkBoxEditor.isValidFor(booleanDescriptor));
        
        ValueDescriptor doubleDescriptor = new ValueDescriptor("test", Double.TYPE);
        assertFalse(checkBoxEditor.isValidFor(doubleDescriptor));
    }
    
    public void testCreateEditorComponent() throws Exception {
        CheckBoxEditor checkBoxEditor = new CheckBoxEditor();
        
        ValueContainer valueContainer = ValueContainer.createValueBacked(V.class);
        BindingContext bindingContext = new BindingContext(valueContainer);
        ValueDescriptor valueDescriptor = valueContainer.getDescriptor("b");
        assertSame(Boolean.TYPE, valueDescriptor.getType());
        
        assertTrue(checkBoxEditor.isValidFor(valueDescriptor));
        JComponent editorComponent = checkBoxEditor.createEditorComponent(valueDescriptor, bindingContext);
        assertNotNull(editorComponent);
        assertSame(JCheckBox.class, editorComponent.getClass());
        JComponent[] components = bindingContext.getBinding("b").getComponents();
        assertEquals(1, components.length);
        assertSame(JCheckBox.class, components[0].getClass());
    }
    
    private static class V {
        boolean b;
    }
}
