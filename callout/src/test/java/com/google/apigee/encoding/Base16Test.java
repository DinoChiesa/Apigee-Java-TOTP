package com.google.apigee.encoding;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.testng.Assert;
import org.testng.annotations.Test;

public class Base16Test {

    static String[][] testcases = new String[][] {
        new String[] { "", ""},
        new String[] { "f", "66"},
        new String[] { "fo", "666F"},
        new String[] { "foo", "666F6F"},
        new String[] { "foob", "666F6F62"},
        new String[] { "fooba", "666F6F6261"},
        new String[] { "foobar", "666F6F626172"}
    };

    @Test
    public void rfc4648TestVectors_Encode() {
        Arrays.stream(testcases).forEach( tc -> {
                String actualResult = Base16.encode(tc[0].getBytes(StandardCharsets.UTF_8));
                Assert.assertEquals(actualResult.toLowerCase(), tc[1].toLowerCase(), "result in case '" + tc[0] + "' not as expected");
            });
    }

    @Test
    public void rfc4648TestVectors_Decode() {
        Arrays.stream(testcases).forEach( tc -> {
                byte[] bytes = Base16.decode(tc[1]);
                String actualResult = new String(bytes, StandardCharsets.UTF_8);
                Assert.assertEquals(actualResult, tc[0], "result in case '" + tc[0] + "' not as expected");
            });
    }

}
