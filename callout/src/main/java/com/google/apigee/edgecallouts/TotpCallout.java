package com.google.apigee.edgecallouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.encoding.Base16;
import com.google.apigee.encoding.Base32;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder;
import com.warrenstrange.googleauth.HmacHashFunction;
import com.warrenstrange.googleauth.KeyRepresentation;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TotpCallout extends CalloutBase implements Execution {
    private final static int DEFAULT_TIME_STEP_SECONDS = 30;
    private final static int DEFAULT_CODE_DIGITS = 6;
    private final HmacHashFunction DEFAULT_HASH_FUNCTION = HmacHashFunction.HmacSHA1;

    public TotpCallout (Map properties) {
        super(properties);
    }

    private int getTimeStep(MessageContext msgCtxt) throws Exception {
        return getIntegerWithDefault(msgCtxt, "time-step", DEFAULT_TIME_STEP_SECONDS);
    }

    private int getCodeDigits(MessageContext msgCtxt) throws Exception {
        return getIntegerWithDefault(msgCtxt, "code-digits", DEFAULT_CODE_DIGITS);
    }

    private int getIntegerWithDefault(MessageContext msgCtxt, String name, int defaultValue) throws Exception {
        String v = getSimpleOptionalProperty(name, msgCtxt);
        if (v==null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v);
        }
        catch (Exception ignoredException) {
            return defaultValue;
        }
    }

    private String encodeKey(String initialKey) {
        String value = (String) this.properties.get("decode-key");
        Function<byte[], String> encodeB64 = (byte[] x) -> Base64.getEncoder().encodeToString(x);
        if ("hex".equals(value) || "base16".equals(value)){
            return encodeB64.apply(Base16.decode(initialKey));
        }
        if ("base32".equals(value)){
            return encodeB64.apply(Base32.decode(initialKey));
        }
        if ("base64".equals(value)){
            return initialKey;
        }
        // not encoded... it's just a string
        return encodeB64.apply(initialKey.getBytes(StandardCharsets.UTF_8));
    }

    private String getEncodedKey(MessageContext msgCtxt) throws Exception {
        String keyString = getSimpleRequiredProperty("key", msgCtxt);
        return encodeKey(keyString);
    }

    private long getTime(MessageContext msgCtxt) {
        String value = (String) this.properties.get("fake-time-millis");
        if (value != null) {
            try {
                value = value.trim();
                value = resolvePropertyValue(value, msgCtxt);
                return Long.parseLong(value);
            }
            catch (Exception ignoredException) {
                return java.time.Instant.now().getEpochSecond()*1000L;
            }
        }
        value = (String) this.properties.get("fake-time-seconds");
        if (value != null) {
            try {
                value = value.trim();
                value = resolvePropertyValue(value, msgCtxt);
                return Long.parseLong(value) * 1000L;
            }
            catch (Exception ignoredException) {
                return java.time.Instant.now().getEpochSecond()*1000L;
            }
        }
        return java.time.Instant.now().getEpochSecond()*1000L;
    }


    private HmacHashFunction getHashFunction(MessageContext msgCtxt) throws Exception {
        String value = getSimpleOptionalProperty("hash-function", msgCtxt);
        if (value==null)
            return DEFAULT_HASH_FUNCTION;
        value = value.toLowerCase();
        if ("sha1".equals(value) || "hmacsha1".equals(value))
            return HmacHashFunction.HmacSHA1;
        if ("sha256".equals(value) || "hmacsha256".equals(value))
            return HmacHashFunction.HmacSHA256;
        if ("sha512".equals(value) || "hmacsha512".equals(value))
            return HmacHashFunction.HmacSHA512;
        return DEFAULT_HASH_FUNCTION;
    }

    public ExecutionResult execute (final MessageContext msgCtxt,
                                    final ExecutionContext execContext) {
        try {
            final int timeStepSizeInSeconds = getTimeStep(msgCtxt);
            final int codeDigits = getCodeDigits(msgCtxt);
            final String base64EncodedKey = getEncodedKey(msgCtxt);
            GoogleAuthenticatorConfigBuilder cb =
                new GoogleAuthenticatorConfigBuilder()
                 .setCodeDigits(codeDigits)
                 .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(timeStepSizeInSeconds))
                 .setKeyRepresentation(KeyRepresentation.BASE64);

            final HmacHashFunction hmacHashFunction = getHashFunction(msgCtxt);
            msgCtxt.setVariable(varName("hashfunction"), hmacHashFunction.toString());
            cb.setHmacHashFunction(hmacHashFunction);

            GoogleAuthenticator ga = new GoogleAuthenticator(cb.build());

            long epochMillisecond = getTime(msgCtxt);
            msgCtxt.setVariable(varName("time"), Long.toString(epochMillisecond/1000));
            String code =  Integer.toString(ga.getTotpPassword(base64EncodedKey, epochMillisecond));
            msgCtxt.setVariable(varName("code"), code);
            return ExecutionResult.SUCCESS;
        }
        catch (Exception e) {
            setExceptionVariables(e, msgCtxt);
            return ExecutionResult.ABORT;
        }
    }

}
