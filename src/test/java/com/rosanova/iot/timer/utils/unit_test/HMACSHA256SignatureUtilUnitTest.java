package com.rosanova.iot.timer.utils.unit_test;

import com.rosanova.iot.timer.utils.HMACSHA256SignatureUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class HMACSHA256SignatureUtilUnitTest {

    final String testSecretKey = "4vE9tZ2mP7qL5xR1nB8yW3kS6jD0hF4gA7sC2vN9mQ1pL8zX5rT3bY6nK0jH2wM4";

    @Test
    void _1734538070000_HMACSHA256SignatureTest() {
        String _1734538070000 = "1734538070000";
        String HMACsha256_1734538070000 = "0ffPZJNO98zXi7kXI8POhIRStpv1C7L4PuIxWlkKZ0E=";
        HMACSHA256SignatureUtil test = new HMACSHA256SignatureUtil(testSecretKey);

        //call postConstruct
        test.computeSecretKey();

        String result = test.computeHMACSHA256(_1734538070000);

        Assertions.assertEquals(HMACsha256_1734538070000, result);

    }
    @Test
    void _SecretKey_256SignatureTest() {
        String sha256_doubleSecretKey = "hQzhRLy/CBdhYNOLg1IrAMqfnV2X/at9uGkMC3ksay4=";
        HMACSHA256SignatureUtil test = new HMACSHA256SignatureUtil(testSecretKey);

        byte[] secretkeyByte= (testSecretKey+testSecretKey).getBytes(StandardCharsets.UTF_8);

        String result = Base64.getEncoder().encodeToString(test.computeSHA256(secretkeyByte));

        Assertions.assertEquals(sha256_doubleSecretKey, result);

    }

    @Test
    void _AsimmetricSecretKey_256SignatureTest() {
        String sha256_doubleSecretKey = "/VudJ6UAM7WzpBtTQvd4fpqKiRLV7Pcz93pEI3RgHRE=";
        HMACSHA256SignatureUtil test = new HMACSHA256SignatureUtil(testSecretKey);

        byte[] secretkeyByte= (testSecretKey+"xfch255h").getBytes(StandardCharsets.UTF_8);

        String result = Base64.getEncoder().encodeToString(test.computeSHA256(secretkeyByte));

        Assertions.assertEquals(sha256_doubleSecretKey, result);

    }
}