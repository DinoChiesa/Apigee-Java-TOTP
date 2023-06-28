# Apigee Callout: TOTP Generator

This is a simple Apigee callout that generates and verifies a Time-based
One-time Password (TOTP), as described in [IETF RFC
6238](https://tools.ietf.org/html/rfc6238).

This callout produces the TOTP. It relies on [a TOTP library from jchambers](https://github.com/jchambers/java-otp/).

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
  </Properties>
  <ClassName>com.google.apigee.callouts.TotpCallout</ClassName>
  <ResourceURL>java://apigee-google-authenticator-totp-20230628.jar</ResourceURL>
</JavaCallout>
```

Within the Properties, you can specify the various inputs for the TOTP.

| name           | required | meaning                                 |
| -------------- | -------- | ----------------------------------------|
| key            | required | a key to use for the TOTP. It can be encoded. You can use curly-braces to refer to a variable. |
| decode-key     | optional | The way to decode the key.  Valid values: base16, base32, base64.  Default: none, meaning the key is just a string. |
| code-digits    | optional | how many digits to produce.  Default: 6 |
| time-step      | optional | The time step in seconds. Default: 30   |
| hash-function  | optional | The HMAC hash to use. Valid values: sha1, sha256, sha512. Default: sha1 |
| expected-value | optional | a value, if present, the policy will check against the generated value. |

All of these properties should coincide with the properties you used to create the barcode.
But be careful: the code-digits, time-step, and hash-function are all ignored by the Google Authenticator app.
You should probably just leave those as defaults.


The output of the callout is context variable:

| name                  | meaning                                             |
| --------------------- | ----------------------------------------------------|
| totp\_code             | the One-time password computed from the inputs.     |

If the callout fails for some reason, such as misconfiguration, these variables get set:

| name                  | meaning |
| --------------------- | ---------------------------------------------------------------------- |
| totp\_error            | a human-readable error message.                                        |
| totp\_stacktrace       | a human-readable stacktrace.                                           |


## Notes on Using the Policy

It might be a good idea to use the consumer app "secret key" as the key for
the OTP. To do that, you'd need to precede this policy with a VerifyApiKey
or AccessEntity, so that you can obtain the secret key corresponding to the
consumer key.

## Examples

**Generate a code**

This configuration uses test vector values from RFC 6238, p 14.

```
<JavaCallout name='Java-TOTP-Test-sha1'>
  <Properties>
    <Property name='key'>12345678901234567890</Property>
    <Property name='code-digits'>8</Property>
    <Property name='hash-function>sha1</Property>
    <Property name='fake-time-seconds'>59</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.TotpCallout</ClassName>
  <ResourceURL>java://apigee-google-authenticator-totp-20230628.jar</ResourceURL>
</JavaCallout>
```

The result,  `94287082`, will be placed into the context variable `totp_code`.

**Verify a Code**


```
<JavaCallout name='Java-TOTP-Test-sha1'>
  <Properties>
    <Property name='key'>12345678901234567890</Property>
    <Property name='code-digits'>8</Property>
    <Property name='hash-function>sha1</Property>
    <Property name='fake-time-seconds'>59</Property>
    <Property name='expected-value'>{request.queryparam.totp}</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.TotpCallout</ClassName>
  <ResourceURL>java://apigee-google-authenticator-totp-20230628.jar</ResourceURL>
</JavaCallout>
```

 This callout policy will _generate_ a TOTP, and then check it against
 an expected value.

 Rather than having the policy check the value, you can alternatively omit the
 `expected-value` property, and then use your own external `Condition` element
 to check the generated code against something the user passed in.  Eg,

   ```
     <Step>
       <Name>Java-TOTP</Name>
     </Step>
     <Step>
       <Condition>totp_code != request.queryparam.totp</Condition>
       <Name>RaiseFault-TOTPInvalid</Name>
     </Step>
   ```



## A Working Proxy

See the attached [bundle](./bundle) for a working API Proxy.
To try out the following scenarios, deploy that proxy to any org and environment.

### Generating a code

Invoke the proxy to generate a TOTP like this:

```
ORG=myorg
ENV=myenv
curl -i https://$ORG-$ENV.apigee.net/totp/generate
```

You should see a code upon output:

```
$ curl -i https://$ORG-$ENV.apigee.net/totp/generate
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


### RFC6238 Test Vectors

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


### Verifying codes

Scan this barcode with an authenticator app on your mobile device:

![barcode](./images/TOTP-Proxy-Example-QR-Code.png "Barcode for Example")

This barcode was generated via [this link](https://www.google.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=otpauth%3A%2F%2Ftotp%2FTOTP-Proxy-example%3Fsecret%3DIFBEGRCFIZDUQMJSGM2DKNRXHA4TA%26issuer%3Dcommunity.apigee.com).  It uses as the secret, "ABCDEFGH1234567890".

After you scan this barcode, the authenticator app will generate codes under the
label "TOTP-Proxy-example", that you can verify with this proxy.

Send a verification request like this:

```
curl -i https://$ORG-$ENV.apigee.net/totp/verify?totp=XXXXX
```

Replace the xxxx with the generated code shown in your mobile device app.

Example success case:
```
$ curl -i https://$ORG-$ENV.apigee.net/totp/verify?totp=376411
HTTP/1.1 200 OK
Date: Wed, 14 Nov 2018 23:21:05 GMT
Content-Type: application/json
Content-Length: 21
Connection: keep-alive

{
  "status" : "ok"
}
```

Example rejection case:

```
$ curl -i https://$ORG-$ENV.apigee.net/totp/verify?totp=376432
HTTP/1.1 401 Unauthorized
Date: Wed, 14 Nov 2018 23:21:10 GMT
Content-Type: application/json
Content-Length: 100
Connection: keep-alive

{
  "error" : {
    "code" : 401.01,
    "message" : "unauthorized. The TOTP does not match."
  }
}

```


## Getting your own barcode

You can get your own barcode for use with this callout policy.
by asking google to prepare one for you, by creating a URL pointing to a link like this: https://www.google.com/chart?chs=...

You need to pass Google the secret to do this!
Only do this if trust Google not to store or use those secrets.

You can use [this form](https://dinochiesa.github.io/totp/link-builder.html) to generate a barcode easily.

If instead you would like to generate a barcode manually, without the assistance of a form, the following describes how.
For full details, see [here](https://github.com/google/google-authenticator/wiki/Key-Uri-Format).

The url structure is:

```
https://www.google.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=CHL_PARAMETER
```

The CHL_PARAMETER should be structured like this:

```
otpauth://totp/LABEL?secret=SECRET&issuer=ISSUER
```


* The LABEL should conform to this pattern: PREFIX:IDENTIFIER, and should be something
  meaningful to your users. Example:godino@google.com is a good label.

* The SECRET must be a byte stream that is base32-encoded (per [IETF RFC
  4648](https://tools.ietf.org/html/rfc4648)).  For example, for the secret
  consisting of these ASCII characters, "ABCDEFGH1234567890", the value to
  include in the URL would be IFBEGRCFIZDUQMJSGM2DKNRXHA4TA .  Base-32 encoding
  allows secrets that contain non-ASCII characters.

* The ISSUER is anything that identifies the service provider. Often, but not
  always, it is the same as the PREFIX used in the LABEL.


The CHL parameter needs to be url-encoded, and then embedded into the base URL.

Assembling the URL, you get something like this:
```
https://www.google.com/chart?chs=200x200&chld=M%7C0&cht=qr&chl=otpauth%3A%2F%2Ftotp%2Fapigee.com%3ATOTP-Proxy-example%3Fsecret%3DIFBEGRCFIZDUQMJSGM2DKNRXHA4TA%26issuer%3Dcommunity.apigee.com

```

If you open that URL in the browser, you'll see a barcode corresponding to:

| param    | value                         |
| -------- | ----------------------------- |
| label    | apigee.com:TOTP-Proxy-example |
| secret   | ABCDEFGH1234567890            |
| issuer   | community.apigee.com          |


If you use different parameters, your URL and your barcode will be
different. Scan the resulting barcode with the Google Authenticator app on your
mobile device, and it will begin generating TOTP codes for your parameters.


## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## License

This material is copyright 2018-2023, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.

## Building

You do not need to build this callout in order to use it.  Even so, you can build it.  To do so,
you need Apache Maven 3.5 or later, and JDK v8.

Follow these steps:

```
./buildsetup.sh
cd callout
mvn clean package
```

## Status

This is a community supported project. There is no warranty for this code.  If
you have problems or questions, as on
[commmunity.apigee.com](https://community.apigee.com).
