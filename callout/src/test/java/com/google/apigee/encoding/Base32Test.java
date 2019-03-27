package com.google.apigee.encoding;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Base32Test {

    static String[][] testcases = new String[][] {

        new String[] { "", ""},
        new String[] { "f", "MY======"},
        new String[] { "fo", "MZXQ===="},
        new String[] { "foo", "MZXW6==="},
        new String[] { "foob", "MZXW6YQ="},
        new String[] { "fooba", "MZXW6YTB"},
        new String[] { "foobar", "MZXW6YTBOI======"}
    };

    private static String chopPadding(String s) {
        if (!s.endsWith("=")) return s;
        return chopPadding(s.substring(0,s.length() -1));
    }
    
    @Test
    public void rfc4648TestVectors_Encode() {
        Arrays.stream(testcases).forEach( tc -> {
                String actualResult = Base32.encode(tc[0].getBytes(StandardCharsets.UTF_8));
                Assert.assertEquals(actualResult, chopPadding(tc[1]), "result in case '" + tc[0] + "' not as expected");
            });
    }

    @Test
    public void rfc4648TestVectors_Decode() {
        Arrays.stream(testcases).forEach( tc -> {
                byte[] bytes = Base32.decode(tc[1]);
                String actualResult = new String(bytes, StandardCharsets.UTF_8);
                Assert.assertEquals(actualResult, tc[0], "result in case '" + tc[0] + "' not as expected");
            });
    }

}
