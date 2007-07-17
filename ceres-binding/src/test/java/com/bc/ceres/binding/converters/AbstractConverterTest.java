package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;
import junit.framework.TestCase;

public abstract class AbstractConverterTest extends TestCase {

    private com.bc.ceres.binding.Converter converter;

    protected AbstractConverterTest(com.bc.ceres.binding.Converter converter) {
        this.converter = converter;
    }

    public abstract void testConverter() throws ConversionException;

    protected void testValueType(Class<?> expectedType) {
        assertEquals(expectedType, converter.getValueType());
    }

    protected void testParseSuccess(Object expectedValue, String text) throws ConversionException {
        assertEquals(expectedValue, converter.parse(text));
        if (expectedValue != null) {
            assertTrue(converter.getValueType().isAssignableFrom(expectedValue.getClass()));
        }
    }

    protected void testParseFailed(String text) {
        try {
            converter.parse(text);
            fail("ConversionException expected: " + text + " should not be convertible");
        } catch (ConversionException e) {
            // OK!
        }
    }


    protected void testFormatSuccess(String expectedText, Object value) throws ConversionException {
        if (value != null) {
            assertTrue(converter.getValueType().isAssignableFrom(value.getClass()));
        }
        assertEquals(expectedText, converter.format(value));
    }

    public void assertNullCorrectlyHandled() {
        try {
            converter.parse(null);
            fail("NullPointerException expected");
        } catch (ConversionException e) {
            fail("ConversionException not expected");
        } catch (NullPointerException e) {
            // expected
        }
        try {
            String text = converter.format(null);
            // if null is supported, returned text shall not be null
            assertNotNull(text);
        } catch (ConversionException e) {
            // may be ok, if null is not supported
        }
    }
}