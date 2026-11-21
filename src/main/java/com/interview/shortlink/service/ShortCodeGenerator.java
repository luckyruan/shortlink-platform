package com.interview.shortlink.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        char[] result = new char[length];
        for (int i = 0; i < length; i++) {
            result[i] = BASE62[random.nextInt(BASE62.length)];
        }
        return new String(result);
    }
}
