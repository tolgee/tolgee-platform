package com.polygloat.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.UUID;

public interface ITest {

    default String generateUniqueString() {
        return UUID.randomUUID().toString();
    }
}
