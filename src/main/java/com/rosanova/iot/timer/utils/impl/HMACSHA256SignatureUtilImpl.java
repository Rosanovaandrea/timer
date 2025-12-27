package com.rosanova.iot.timer.utils.impl;

import jakarta.annotation.PostConstruct;

import java.util.Base64;

public class HMACSHA256SignatureUtilImpl {

    private final byte[] ipad = new byte[64];
    private final byte[] opad = new byte[64];

    private final String secretKey;

    public static final int[] K = {
            // Riga 1 (t=0 a t=7)
            0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
            0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,

            // Riga 2 (t=8 a t=15)
            0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,

            // Riga 3 (t=16 a t=23)
            0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,

            // Riga 4 (t=24 a t=31)
            0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
            0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,

            // Riga 5 (t=32 a t=39)
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,

            // Riga 6 (t=40 a t=47)
            0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
            0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,

            // Riga 7 (t=48 a t=55)
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,

            // Riga 8 (t=56 a t=63)
            0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
            0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
    };

    public HMACSHA256SignatureUtilImpl(String secret) {
        secretKey = secret;
        // add custom exception to not generate call tree
        if (secretKey.length() != 64) {
            System.err.println("Invalid secret key length: " + secretKey.length());
            throw new IllegalArgumentException();
        }
    }

    @PostConstruct
    public void computeSecretKey() {
        for (int i = 0; i < 64; i++) {
            ipad[i] = (byte) (secretKey.charAt(i) ^ 0x36);
            opad[i] = (byte) (secretKey.charAt(i) ^ 0x5c);
        }

    }

    public String computeHMACSHA256(String payload) {

            byte[] internalHashData = new byte[128];
            byte[] outerHashData = new byte[128];
            System.arraycopy(ipad, 0, internalHashData, 0, 64);

            for(int i = 0; i < payload.length(); i++){
                internalHashData[i+64] = (byte) payload.charAt(i);
            }

            internalHashData = computeSHA256(internalHashData,77);

            System.arraycopy(opad, 0, outerHashData, 0, 64);
            System.arraycopy(internalHashData, 0, outerHashData, 64, 32);

            outerHashData = computeSHA256(outerHashData,96);

            return Base64.getUrlEncoder().encodeToString(outerHashData);

    }

    public byte[] computeSHA256(byte[] message, int dataDimension) {
        int[] hash = {0x6a09e667,
                0xbb67ae85,
                0x3c6ef372,
                0xa54ff53a,
                0x510e527f,
                0x9b05688c,
                0x1f83d9ab,
                0x5be0cd19};

        int[] W = new int[64];

        preProcessingSHA256(message, dataDimension);

        for (int offset = 0; offset < message.length; offset += 64) {
            for (int i = 0; i < 16; i++) {
                int pos = offset + (i * 4);
                W[i] = ((message[pos] & 0xFF) << 24) |
                        ((message[pos + 1] & 0xFF) << 16) |
                        ((message[pos + 2] & 0xFF) << 8) |
                        (message[pos + 3] & 0xFF);


            }

            computeWordsSHA256(W);
            computeHashSHA256(W, hash);

        }

        byte[] result = new byte[32];

            for (int i = 0; i < 8; i++) {
                int pos = i * 4;
                result[pos] = (byte) ((hash[i] >>> 24)& 0xFF);
                result[pos+1] = (byte) ((hash[i] >>> 16)& 0xFF);
                result[pos+2] = (byte) ((hash[i] >>> 8)& 0xFF);
                result[pos+3] = (byte) (hash[i] & 0xFF);


        }

        return result;

    }

    public void preProcessingSHA256(byte[] message, int dataDimension) {



        /*

        // LOGICA INIZIALE PER UN COMPUTE STANDARD NOTA NEL METODO COMPUTE HMAC DOVRAI REINSERIRE IL SALVATAGGIO IN UN ARRAY DI BYTE

        int dimension = message.length;
        int module = (dimension & 63);
        int addToMultiple64 = (module < 56) ? 64 - module : 128 - module ;
        byte[] messageBytes = new byte[dimension+addToMultiple64];
        System.arraycopy(message, 0, messageBytes, 0, dimension);
        messageBytes[dimension] = (byte) 0x80;

         */
        message[dataDimension] = (byte) 0x80;
        long lengthInBits = (long) dataDimension * 8;

        // Scriviamo il long negli ultimi 5 byte (Big Endian) sufficienti per rappresentare una lunghezza di  2^31(caratteri) x 8 bit
        int lastIndex = message.length - 5;
        message[lastIndex] = (byte) ((lengthInBits >>> 32) & 0xFF);
        message[lastIndex + 1] = (byte) ((lengthInBits >>> 24) & 0xFF);
        message[lastIndex + 2] = (byte) ((lengthInBits >>> 16) & 0xFF);
        message[lastIndex + 3] = (byte) ((lengthInBits >>> 8) & 0xFF);
        message[lastIndex + 4] = (byte) (lengthInBits & 0xFF);

    }



    public void computeWordsSHA256(int[] W){

        for(int i = 16; i < 64; i++){
            W[i] = Rho_1(W[i-2])+W[i-7]+Rho_0(W[i-15])+W[i-16];
        }
    }

    public void computeHashSHA256(int[] W, int[] hash /*computed words*/) {
        int a,b,c,d,e,f,g,h,T1,T2 ;


        a=hash[0];
        b=hash[1];
        c=hash[2];
        d=hash[3];
        e=hash[4];
        f=hash[5];
        g=hash[6];
        h=hash[7];

        for(int i = 0; i < 64; i++) {

            T1 = h + Sigma_1(e) + Ch(e, f, g) + K[i] + W[i];
            T2 = Sigma_0(a) + Maj(a, b, c);
            h = g;
            g = f;
            f = e;
            e = d + T1;
            d = c;
            c = b;
            b = a;
            a = T1 + T2;
        }
        hash[0] = hash[0] + a;
        hash[1] = hash[1] + b;
        hash[2] = hash[2] + c;
        hash[3] = hash[3] + d;
        hash[4] = hash[4] + e;
        hash[5] = hash[5] + f;
        hash[6] = hash[6] + g;
        hash[7] = hash[7] + h;
    }

    public int Sigma_1(int x){
        return ((x << 26) | (x >>> 6)) ^
                ((x << 21) | (x >>> 11)) ^
                ((x << 7 ) | (x >>> 25));
    }

    public int Sigma_0(int x){
        return ((x << 30) | (x >>> 2)) ^
                ((x << 19) | (x >>> 13)) ^
                ((x << 10 ) | (x >>> 22));
    }

    public int Rho_0(int x){
        return ((x << 25) | (x >>> 7)) ^
                ((x << 14) | (x >>> 18)) ^
                (x >>> 3 );
    }

    public int Rho_1(int x){
        return ((x << 15) | (x >>> 17)) ^
                ((x << 13) | (x >>> 19)) ^
                (x >>> 10 );
    }

    public int Ch (int x ,int y,int z){
        return (x & y) ^ (~x & z);
    }

    public int Maj (int x,int y,int z){
        return (x & y) ^ (x & z) ^ (y & z);
    }
}
