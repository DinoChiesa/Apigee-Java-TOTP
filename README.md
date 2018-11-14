# Edge Callout: TOTP Generator

This is a simple Apigee Edge callout that generates and verifies a Time-based
One-time Password (TOTP), as described in [IETF RFC
6238](https://tools.ietf.org/html/rfc6238).

This callout produces the TOTP. It relies on [a TOTP library from warren
strange](https://github.com/wstrange/GoogleAuth/), implemented based on code
from the Google Authenticator app.

API Proxies can require users to generate and send a TOTP with a request. This
callout can generate the TOTP. Then the proxy logic should verify that the
generated TOTP matches the passed-in value.

This code is open source. You don't need to compile it in order to use it.


## Configuration

Configure the policy like this:

```
<JavaCallout name='Java-TOTP'>
  <Properties>
    <Property name='key'>{my_key_here}</Property>
    <Property name='code-digits'>6</Property>
  </Properties>
  <ClassName>com.google.apigee.edgecallouts.TotpCallout</ClassName>
  <ResourceURL>java://edge-google-authenticator-totp-1.0.1.jar</ResourceURL>
</JavaCallout>
```

Within the Properties, you can specify the various inputs for the TOTP.

| name         | required | meaning                                 |
| ------------ | -------- | ----------------------------------------|
| key          | required | a key to use for the TOTP. It can be encoded. You can use curly-braces to refer to a variable. |
| code-digits  | optional | how many digits to produce.  Default: 6 |
| time-step    | optional | The time step in seconds. Default: 30   |
| decode-key   | optional | The way to decode the key.  Valid values: base16, base32, base64.  Default: none  |
| hash-function| optional | The HMAC hash to use. Valid values: sha1, sha256, sha512. Default: sha1 |

The output of the callout is context variable:

| name                  | meaning                                             |
| --------------------- | ----------------------------------------------------|
| totp_code             | the One-time password computed from the inputs.     |

If the callout fails for some reason, such as misconfiguration, these variables get set:

| name                  | meaning |
| --------------------- | ---------------------------------------------------------------------- |
| totp_error            | a human-readable error message.                                        |
| totp_stacktrace       | a human-readable stacktrace.                                           |


## Using the Policy

1. It might be a good idea to use the consumer app "secret key" as the key for
   the OTP.  To do that , you'd need to precede this policy with a VerifyApiKey
   or similar, so that you can obtain the secret key corresponding to the
   consumer key.

2. This callout policy merely _generates_ a TOTP.  You need to couple it with a
   Condition to _check_ the code against something the user passed in.  Eg,

   ```
     <Step>
       <Name>Java-TOTP</Name>
     </Step>
     <Step>
       <Condition>totp_code != request.queryparam.totp</Condition>
       <Name>RaiseFault-TOTPInvalid</Name>
     </Step>
   ```


## Example

See the attached [bundle](./bundle) for a working API Proxy.
To use it, deploy it to any org and environment, then invoke it like this:

```
ORG=myorg
ENV=myenv
curl -i https://$ORG-$ENV.apigee.net/totp/t1
```

You should see a code upon output:

```
$ curl -i https://$ORG-$ENV.apigee.net/totp/t1
HTTP/1.1 200 OK
Date: Wed, 14 Nov 2018 21:48:24 GMT
Content-Type: application/json
Content-Length: 42
Connection: keep-alive

{
  "status" : "ok",
  "code" : "717859"
}
```

Obviously your code will vary.  The key used here is a contrived secret key.

To test the values from the RFC6238 spec, you can `GET
/rfc6238test/sha{1,256,512}` .  Pass one of the well known time values.  The
values and their expected codes are:

| time value   | expected (sha1) | expected (sha256) | expected (sha512) |
|--------------|-----------------|-------------------|-------------------|
| 59           | 94287082        | 46119246          | 90693936 |
| 1111111109   | 7081804         | 68084774          | 25091201 |
| 1111111111   | 14050471        | 67062674          | 99943326 |
| 1234567890   | 89005924        | 91819424          | 93441116 |
| 2000000000   | 69279037        | 90698825          | 38618901 |
| 20000000000  | 65353130        | 77737706          | 47863826 |

Example:

```
$ curl -i https://$ORG-$ENV.apigee.net/totp/rfc6238test/sha256?faketime=1111111109
HTTP/1.1 200 OK
Date: Wed, 14 Nov 2018 21:55:33 GMT
Content-Type: application/json
Content-Length: 43
Connection: keep-alive

{
  "status" : "ok",
  "code" : "68084774"
}
```

NB: This test works by passing a "fake time" to the policy to use in place of
"now," and by using a well-known secret key. Don't use the `fake-time-seconds`
property in production, and don't reuse the well-known secret key. Those things
are expected to be used only for testing.


## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## License

This material is copyright 2018, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.

## Building

```
cd callout
mvn clean package
```

## Status

This is a community supported project. There is no warranty for this code.
If you have problems or questions, as on [commmunity.apigee.com](https://community.apigee.com).
