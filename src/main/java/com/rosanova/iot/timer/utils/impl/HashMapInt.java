package com.rosanova.iot.timer.utils.impl;

import lombok.Getter;

import java.util.Arrays;

public class HashMapInt {

    private final int[] bucket;

    @Getter
    private int size = 0;

    public HashMapInt(){
        // array molto ampio per evitare collisioni e con potenza di 2 per usare il bitwise AND mask come modulo
        bucket = new int[128];

        Arrays.fill(bucket, -1);
    }

    public int add(int value){

        if(size == 40) return -1;

        int index = ((value >>> 16) ^ value) & 127;

        while (bucket[index] != -1) {
            if (bucket[index] == value) return 1;
            index = (index + 1) & 127;
        }

        bucket[index] = value;
        size++;

        return 0;
    }

    public boolean search(int value){

        int index = ((value >>> 16) ^ value) & 127;

        int startIndex = index;
        while (bucket[index] != -1) {
            if (bucket[index] == value) return true;

            index = (index + 1) & 127;

            if (index == startIndex) break;
        }

        return false;

    }
}
