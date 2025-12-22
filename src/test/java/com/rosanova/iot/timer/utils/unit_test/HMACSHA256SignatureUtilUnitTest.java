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
    void _AsimmetricSecretKey_256SignatureTest() {
        String sha256_doubleSecretKey = "/VudJ6UAM7WzpBtTQvd4fpqKiRLV7Pcz93pEI3RgHRE=";
        HMACSHA256SignatureUtil test = new HMACSHA256SignatureUtil(testSecretKey);

        byte[] secretkeyByte= new byte[128];
        System.arraycopy((testSecretKey+"xfch255h").getBytes(),0,secretkeyByte,0,72);


        String result = Base64.getEncoder().encodeToString(test.computeSHA256(secretkeyByte,72));

        Assertions.assertEquals(sha256_doubleSecretKey, result);

    }

    @Test
    void testPerformance() {
        HMACSHA256SignatureUtil util = new HMACSHA256SignatureUtil(testSecretKey);
        util.computeSecretKey(); // Simuliamo il post-construct
        String payload = "1734538070000";

        // 1. WARM-UP (importante per GraalVM/JIT)
        for (int i = 0; i < 10000; i++) {
            util.computeHMACSHA256(payload);
        }

        // 2. MISURAZIONE EFFETTIVA
        long start = System.nanoTime();
        int iterations = 100000;
        for (int i = 0; i < iterations; i++) {
            util.computeHMACSHA256(payload);
        }
        long end = System.nanoTime();

        long avgTime = (end - start) / iterations;
        System.out.println("Tempo medio per HMAC: " + avgTime + " ns");
    }
}