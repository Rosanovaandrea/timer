package com.rosanova.iot.timer.utils.impl;

import lombok.Getter;

public class HashMapInt {

    private final int[] bucket;

    @Getter
    private int size = 0;

    public HashMapInt(){
        // array molto ampio per evitare collisioni e con potenza di 2 per usare il bitwise AND mask come modulo
        bucket = new int[64];
    }

    public int add(int value){

        if(size == 20) return -1;

        int index = ((value >>> 16) ^ value) & 63;

        while (bucket[index] != 0) {
            if (bucket[index] == value) return 1;
            index = (index + 1) & 63;
        }

        bucket[index] = value;
        size++;

        return 0;
    }

    public boolean search(int value){

        int index = ((value >>> 16) ^ value) & 63;

        int startIndex = index;
        while (bucket[index] != 0) {
            if (bucket[index] == value) return true;

            index = (index + 1) & 63;

            if (index == startIndex) break;
        }

        return false;

    }
}
