package com.google.apigee.edgecallouts.test;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.edgecallouts.TotpCallout;
import com.google.common.io.BaseEncoding;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestTotpCallout {

    MessageContext msgCtxt;
    InputStream messageContentStream;
    Message message;
    ExecutionContext exeCtxt;

    @BeforeMethod()
    public void beforeMethod() {

        msgCtxt = new MockUp<MessageContext>() {
            private Map variables;
            public void $init() {
                variables = new HashMap();
            }

            @Mock()
            public <T> T getVariable(final String name){
                if (variables == null) {
                    variables = new HashMap();
                }
                return (T) variables.get(name);
            }

            @Mock()
            public boolean setVariable(final String name, final Object value) {
                if (variables == null) {
                    variables = new HashMap();
                }
                //System.out.printf("%s := %s\n", name, value);
                variables.put(name, value);
                return true;
            }

            @Mock()
            public boolean removeVariable(final String name) {
                if (variables == null) {
                    variables = new HashMap();
                }
                if (variables.containsKey(name)) {
                    variables.remove(name);
                }
                return true;
            }

            @Mock()
            public Message getMessage() {
                return message;
            }
        }.getMockInstance();

        exeCtxt = new MockUp<ExecutionContext>(){ }.getMockInstance();

        message = new MockUp<Message>(){
            @Mock()
            public InputStream getContentAsStream() {
                // new ByteArrayInputStream(messageContent.getBytes(StandardCharsets.UTF_8));
                return messageContentStream;
            }
        }.getMockInstance();
    }


    @Test
    public void test_EmptyKey() {
        String expectedError = "key resolves to an empty string";
        Map<String,String> props = new HashMap<String,String>();
        TotpCallout callout = new TotpCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("totp_error");
        Assert.assertNotNull(errorOutput, "errorOutput");
        Assert.assertEquals(errorOutput, expectedError, "error not as expected");
        System.out.println("expected error: " + expectedError);
        System.out.println("=========================================================");
    }

    @Test
    public void test_GoodResult() throws Exception {
        msgCtxt.setVariable("my-key", "ABCDEFGH1234567890");

        Map<String,String> props = new HashMap<String,String>();
        props.put("key","{my-key}");

        TotpCallout callout = new TotpCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("totp_error");
        Assert.assertNull(errorOutput, "errorOutput");
        Object stacktrace =  msgCtxt.getVariable("totp_stacktrace");
        Assert.assertNull(stacktrace, "GoodResult() stacktrace");
        Object code = msgCtxt.getVariable("totp_code");
        Assert.assertNotNull(code, "code");
        System.out.println("totp code: " + code);
        System.out.println("=========================================================");
    }

    private static byte[] hexStr2Bytes(String hex)
    {
        // Adding one byte to get the right conversion
        // Values starting with "0" can be converted
        byte[] bArray = new BigInteger("10" + hex, 16).toByteArray();

        // Copy all the REAL bytes, not the "first"
        byte[] ret = new byte[bArray.length - 1];
        System.arraycopy(bArray, 1, ret, 0, ret.length);

        return ret;
    }

    @Test
    public void test_GoodResultHexKey_Millis() throws Exception {
        String rfc6238TestKey = "3132333435363738393031323334353637383930";

        // these values are taken from RFC 6238, p. 14
        String expectedCodeValue = "89005924";
        String fakeTime = "1234567890000"; // milliseconds since epoch
        byte[] keyBytes = BaseEncoding .base16().decode(rfc6238TestKey);
        msgCtxt.setVariable("my-key", BaseEncoding .base64().encode(keyBytes));
        Map<String,String> props = new HashMap<String,String>();
        props.put("key", "{my-key}");
        props.put("decode-key", "base64");
        props.put("fake-time-millis", fakeTime);
        props.put("code-digits", "8");

        TotpCallout callout = new TotpCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("totp_error");
        Assert.assertNull(errorOutput, "errorOutput");
        Object stacktrace =  msgCtxt.getVariable("totp_stacktrace");
        Assert.assertNull(stacktrace, "GoodResultHexKey() stacktrace");
        Object actualCodeValue = msgCtxt.getVariable("totp_code");
        Assert.assertNotNull(actualCodeValue, "actualCodeValue");
        System.out.println("actualCodeValue: " + actualCodeValue);
        Assert.assertEquals(actualCodeValue, expectedCodeValue);
        System.out.println("=========================================================");
    }

    @Test
    public void test_GoodResultHexKey_Seconds() throws Exception {
        String rfc6238TestKey = "3132333435363738393031323334353637383930";

        // these values are taken from RFC 6238, p. 14
        String expectedCodeValue = "89005924";
        String fakeTime = "1234567890"; // milliseconds since epoch
        byte[] keyBytes = BaseEncoding .base16().decode(rfc6238TestKey);
        msgCtxt.setVariable("my-key", BaseEncoding .base64().encode(keyBytes));
        Map<String,String> props = new HashMap<String,String>();
        props.put("key", "{my-key}");
        props.put("decode-key", "base64");
        props.put("fake-time-seconds", fakeTime);
        props.put("code-digits", "8");

        TotpCallout callout = new TotpCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("totp_error");
        Assert.assertNull(errorOutput, "errorOutput");
        Object stacktrace =  msgCtxt.getVariable("totp_stacktrace");
        Assert.assertNull(stacktrace, "GoodResultHexKey() stacktrace");
        Object actualCodeValue = msgCtxt.getVariable("totp_code");
        Assert.assertNotNull(actualCodeValue, "actualCodeValue");
        System.out.println("actualCodeValue: " + actualCodeValue);
        Assert.assertEquals(actualCodeValue, expectedCodeValue);
        System.out.println("=========================================================");
    }

    @Test
    public void test_GoodResultHexKey_Seconds_Ref() throws Exception {
        String rfc6238TestKey = "3132333435363738393031323334353637383930";

        // these values are taken from RFC 6238, p. 14
        String expectedCodeValue = "89005924";
        String fakeTime = "1234567890"; // milliseconds since epoch
        byte[] keyBytes = BaseEncoding .base16().decode(rfc6238TestKey);
        msgCtxt.setVariable("my-key", BaseEncoding .base64().encode(keyBytes));
        msgCtxt.setVariable("faketime", fakeTime);
        Map<String,String> props = new HashMap<String,String>();
        props.put("key", "{my-key}");
        props.put("decode-key", "base64");
        props.put("fake-time-seconds", "{faketime}");
        props.put("code-digits", "8");

        TotpCallout callout = new TotpCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("totp_error");
        Assert.assertNull(errorOutput, "errorOutput");
        Object stacktrace =  msgCtxt.getVariable("totp_stacktrace");
        Assert.assertNull(stacktrace, "GoodResultHexKey() stacktrace");
        Object actualCodeValue = msgCtxt.getVariable("totp_code");
        Assert.assertNotNull(actualCodeValue, "actualCodeValue");
        System.out.println("actualCodeValue: " + actualCodeValue);
        Assert.assertEquals(actualCodeValue, expectedCodeValue);
        System.out.println("=========================================================");
    }

    @Test
    public void test_GoodResultHexKey_Seconds_Ref_SHA256() throws Exception {
        String rfc6238TestKey = "3132333435363738393031323334353637383930313233343536373839303132";

        // these values are taken from RFC 6238, p. 14
        String expectedCodeValue = "91819424";
        String fakeTime = "1234567890"; // milliseconds since epoch
        byte[] keyBytes = BaseEncoding .base16().decode(rfc6238TestKey);
        msgCtxt.setVariable("my-key", BaseEncoding .base64().encode(keyBytes));
        msgCtxt.setVariable("faketime", fakeTime);
        Map<String,String> props = new HashMap<String,String>();
        props.put("key", "{my-key}");
        props.put("decode-key", "base64");
        props.put("fake-time-seconds", "{faketime}");
        props.put("code-digits", "8");
        props.put("hash-function", "sha256");

        TotpCallout callout = new TotpCallout(props);

        // execute and retrieve output
        ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
        Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
        Object errorOutput = msgCtxt.getVariable("totp_error");
        Assert.assertNull(errorOutput, "errorOutput");
        Object stacktrace =  msgCtxt.getVariable("totp_stacktrace");
        Assert.assertNull(stacktrace, "GoodResultHexKey() stacktrace");
        Object actualCodeValue = msgCtxt.getVariable("totp_code");
        Assert.assertNotNull(actualCodeValue, "actualCodeValue");
        System.out.println("actualCodeValue: " + actualCodeValue);
        Assert.assertEquals(actualCodeValue, expectedCodeValue);
        System.out.println("=========================================================");
    }

}
