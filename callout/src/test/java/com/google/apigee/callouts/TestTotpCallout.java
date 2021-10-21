package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.google.common.io.BaseEncoding;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestTotpCallout {

  MessageContext msgCtxt;
  InputStream messageContentStream;
  Message message;
  ExecutionContext exeCtxt;

  @BeforeMethod()
  public void beforeMethod() {

    msgCtxt =
        new MockUp<MessageContext>() {
          private Map variables;

          public void $init() {
            variables = new HashMap();
          }

          @Mock()
          public <T> T getVariable(final String name) {
            if (variables == null) {
              variables = new HashMap();
            }
            T value = (T) variables.get(name);
            System.out.printf("%s = %s\n", name, (value == null) ? "(null)" : value.toString());
            return (T) value;
          }

          @Mock()
          public boolean setVariable(final String name, final Object value) {
            if (variables == null) {
              variables = new HashMap();
            }
            System.out.printf("%s := %s\n", name, value);
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

    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();

    message =
        new MockUp<Message>() {
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
    Map<String, String> props = new HashMap<String, String>();
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

    Map<String, String> props = new HashMap<String, String>();
    props.put("key", "{my-key}");

    TotpCallout callout = new TotpCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("totp_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object stacktrace = msgCtxt.getVariable("totp_stacktrace");
    Assert.assertNull(stacktrace, "GoodResult() stacktrace");
    Object code = msgCtxt.getVariable("totp_code");
    Assert.assertNotNull(code, "code");
    System.out.println("totp code: " + code);
    System.out.println("=========================================================");
  }

  private static byte[] hexStr2Bytes(String hex) {
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
    byte[] keyBytes = BaseEncoding.base16().decode(rfc6238TestKey);
    msgCtxt.setVariable("my-key", BaseEncoding.base64().encode(keyBytes));
    Map<String, String> props = new HashMap<String, String>();
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
    Object stacktrace = msgCtxt.getVariable("totp_stacktrace");
    Assert.assertNull(stacktrace, "GoodResultHexKey() stacktrace");
    Object actualCodeValue = msgCtxt.getVariable("totp_code");
    Assert.assertNotNull(actualCodeValue, "actualCodeValue");
    System.out.println("actualCodeValue: " + actualCodeValue);
    Assert.assertEquals(actualCodeValue, expectedCodeValue);
    System.out.println("=========================================================");
  }

  @DataProvider(name = "rfc6238-test-vectors")
  public Object[][] rfc6238_Data() {
    // these values are taken from RFC 6238, p. 14
    final String SHA1_KEY = "12345678901234567890";
    final String SHA256_KEY = "12345678901234567890123456789012";
    final String SHA512_KEY = "1234567890123456789012345678901234567890123456789012345678901234";

    return new Object[][] {
      {"SHA1", SHA1_KEY, new Long(59L), new Long(94287082)},
      {"SHA1", SHA1_KEY, new Long(1111111109L), new Long(7081804)},
      {"SHA1", SHA1_KEY, new Long(1111111111L), new Long(14050471)},
      {"SHA1", SHA1_KEY, new Long(1234567890L), new Long(89005924)},
      {"SHA1", SHA1_KEY, new Long(2000000000L), new Long(69279037)},
      {"SHA1", SHA1_KEY, new Long(20000000000L), new Long(65353130)},
      {"SHA256", SHA256_KEY, new Long(59L), new Long(46119246)},
      {"SHA256", SHA256_KEY, new Long(1111111109L), new Long(68084774)},
      {"SHA256", SHA256_KEY, new Long(1111111111L), new Long(67062674)},
      {"SHA256", SHA256_KEY, new Long(1234567890L), new Long(91819424)},
      {"SHA256", SHA256_KEY, new Long(2000000000L), new Long(90698825)},
      {"SHA256", SHA256_KEY, new Long(20000000000L), new Long(77737706)},
      {"SHA512", SHA512_KEY, new Long(59L), new Long(90693936)},
      {"SHA512", SHA512_KEY, new Long(1111111109L), new Long(25091201)},
      {"SHA512", SHA512_KEY, new Long(1111111111L), new Long(99943326)},
      {"SHA512", SHA512_KEY, new Long(1234567890L), new Long(93441116)},
      {"SHA512", SHA512_KEY, new Long(2000000000L), new Long(38618901)},
      {"SHA512", SHA512_KEY, new Long(20000000000L), new Long(47863826)}
    };
  }

  @Test(dataProvider = "rfc6238-test-vectors")
  public void rfc6238_Test(
      final String alg, final String key, Long epochSeconds, Long expectedValue) throws Exception {

    msgCtxt.setVariable("my-key", key);
    Map<String, String> props = new HashMap<String, String>();
    props.put("key", "{my-key}");
    props.put("hash-function", alg);
    props.put("fake-time-seconds", epochSeconds.toString());
    props.put("code-digits", "8");

    TotpCallout callout = new TotpCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("totp_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object stacktrace = msgCtxt.getVariable("totp_stacktrace");
    Assert.assertNull(stacktrace, "rfc6238_Test() stacktrace");
    Object actualCodeValue = msgCtxt.getVariable("totp_code");
    Assert.assertNotNull(actualCodeValue, "actualCodeValue");
    System.out.println("actualCodeValue: " + actualCodeValue);
    Assert.assertEquals(actualCodeValue, expectedValue.toString());
    System.out.println("=========================================================");
  }

  @Test
  public void test_GoodResultHexKey_Seconds() throws Exception {
    String rfc6238TestKey = "3132333435363738393031323334353637383930";

    // these values are taken from RFC 6238, p. 14
    String expectedCodeValue = "89005924";
    String fakeTime = "1234567890"; // milliseconds since epoch
    byte[] keyBytes = BaseEncoding.base16().decode(rfc6238TestKey);
    msgCtxt.setVariable("my-key", BaseEncoding.base64().encode(keyBytes));
    Map<String, String> props = new HashMap<String, String>();
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
    Object stacktrace = msgCtxt.getVariable("totp_stacktrace");
    Assert.assertNull(stacktrace, "GoodResultHexKey() stacktrace");
    Object actualCodeValue = msgCtxt.getVariable("totp_code");
    Assert.assertNotNull(actualCodeValue, "actualCodeValue");
    System.out.println("actualCodeValue: " + actualCodeValue);
    Assert.assertEquals(actualCodeValue, expectedCodeValue);
    System.out.println("=========================================================");
  }

  @Test
  public void test_GoodResultHexKey_Seconds_WithVerification_Success() throws Exception {
    String rfc6238TestKey = "3132333435363738393031323334353637383930";

    // these values are taken from RFC 6238, p. 14
    String expectedCodeValue = "89005924";
    String fakeTime = "1234567890"; // milliseconds since epoch
    byte[] keyBytes = BaseEncoding.base16().decode(rfc6238TestKey);
    msgCtxt.setVariable("my-key", BaseEncoding.base64().encode(keyBytes));
    Map<String, String> props = new HashMap<String, String>();
    props.put("key", "{my-key}");
    props.put("decode-key", "base64");
    props.put("fake-time-seconds", fakeTime);
    props.put("expected-value", expectedCodeValue);
    props.put("code-digits", "8");

    TotpCallout callout = new TotpCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("totp_error");
    Assert.assertNull(errorOutput, "errorOutput");
    Object stacktrace = msgCtxt.getVariable("totp_stacktrace");
    Assert.assertNull(stacktrace, "GoodResultHexKey() stacktrace");
    Object actualCodeValue = msgCtxt.getVariable("totp_code");
    Assert.assertNotNull(actualCodeValue, "actualCodeValue");
    System.out.println("actualCodeValue: " + actualCodeValue);
    Assert.assertEquals(actualCodeValue, expectedCodeValue);
    System.out.println("=========================================================");
  }

  @Test
  public void test_GoodResultHexKey_Seconds_WithVerification_Failure() throws Exception {
    String rfc6238TestKey = "3132333435363738393031323334353637383930";

    // these values are taken from RFC 6238, p. 14
    String expectedCodeValue = "89005924";
    String fakeTime = "1234567890"; // milliseconds since epoch
    byte[] keyBytes = BaseEncoding.base16().decode(rfc6238TestKey);
    msgCtxt.setVariable("my-key", BaseEncoding.base64().encode(keyBytes));
    Map<String, String> props = new HashMap<String, String>();
    props.put("key", "{my-key}");
    props.put("decode-key", "base64");
    props.put("fake-time-seconds", fakeTime);
    props.put("expected-value", "not-correct-value");
    props.put("code-digits", "8");

    TotpCallout callout = new TotpCallout(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("totp_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    Assert.assertEquals(errorOutput, "TOTP mismatch");
    Object stacktrace = msgCtxt.getVariable("totp_stacktrace");
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
    byte[] keyBytes = BaseEncoding.base16().decode(rfc6238TestKey);
    msgCtxt.setVariable("my-key", BaseEncoding.base64().encode(keyBytes));
    msgCtxt.setVariable("faketime", fakeTime);
    Map<String, String> props = new HashMap<String, String>();
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
    Object stacktrace = msgCtxt.getVariable("totp_stacktrace");
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
    byte[] keyBytes = BaseEncoding.base16().decode(rfc6238TestKey);
    msgCtxt.setVariable("my-key", BaseEncoding.base64().encode(keyBytes));
    msgCtxt.setVariable("faketime", fakeTime);
    Map<String, String> props = new HashMap<String, String>();
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
    Object stacktrace = msgCtxt.getVariable("totp_stacktrace");
    Assert.assertNull(stacktrace, "GoodResultHexKey() stacktrace");
    Object actualCodeValue = msgCtxt.getVariable("totp_code");
    Assert.assertNotNull(actualCodeValue, "actualCodeValue");
    System.out.println("actualCodeValue: " + actualCodeValue);
    Assert.assertEquals(actualCodeValue, expectedCodeValue);
    System.out.println("=========================================================");
  }

  // // This test can be uncommented, modified, and run.
  // // Replace:
  // //  1. the testKeyString with the key used to generate the TOTP barcode
  // //  2. the expectedCodeValue with the code generated "right now" from the mobile app.
  // //
  // @Test
  // public void test_MyCustomTotp_Ref_SHA1() throws Exception {
  //     String testKeyString = "1766176376398109-20-30";
  //     String expectedCodeValue = "704145"; // replace this with code from the app
  //     byte[] keyBytes = testKeyString.getBytes(StandardCharsets.UTF_8);
  //     msgCtxt.setVariable("my-key", BaseEncoding.base64().encode(keyBytes));
  //     //msgCtxt.setVariable("faketime", fakeTime);
  //     Map<String,String> props = new HashMap<String,String>();
  //     props.put("key", "{my-key}");
  //     props.put("decode-key", "base64");
  //
  //     TotpCallout callout = new TotpCallout(props);
  //
  //     // execute and retrieve output
  //     ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
  //     Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
  //     Object errorOutput = msgCtxt.getVariable("totp_error");
  //     Assert.assertNull(errorOutput, "errorOutput");
  //     Object stacktrace =  msgCtxt.getVariable("totp_stacktrace");
  //     Assert.assertNull(stacktrace, "GoodResultHexKey() stacktrace");
  //     Object actualCodeValue = msgCtxt.getVariable("totp_code");
  //     Assert.assertNotNull(actualCodeValue, "actualCodeValue");
  //     System.out.println("actualCodeValue: " + actualCodeValue);
  //     Assert.assertEquals(actualCodeValue, expectedCodeValue);
  //     System.out.println("=========================================================");
  // }

}
