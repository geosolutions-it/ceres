package com.bc.ceres.binding.converters;

import com.bc.ceres.binding.ConversionException;

public class StringArrayConverterTest extends AbstractConverterTest {

    public StringArrayConverterTest() {
        super(new ArrayConverter(String[].class, new StringConverter()));
    }

    @Override
    public void testConverter() throws ConversionException {
        assertEquals("\u002C", ",");
        testValueType(String[].class);
        testParseSuccess(new String[]{"a", "b", "c"}, "a,b,c");
        testParseSuccess(new String[]{" a", "\tb", "c\n"}, " a,\tb,c\n"); // test space decoding (none!)
        testParseSuccess(new String[]{"a,b", "c,d", "e,f"}, "a\\u002Cb,c\\u002Cd,e\\u002Cf"); // test comma decoding
        testFormatSuccess("a,b,c", new String[]{"a", "b", "c"});
        testFormatSuccess(" a,\tb,c\n", new String[]{" a", "\tb", "c\n"});  // test space encoding (none!)
        testFormatSuccess("a\\u002Cb,c\\u002Cd,e\\u002Cf", new String[]{"a,b", "c,d", "e,f"});  // test comma encoding
        assertNullCorrectlyHandled();
    }
}