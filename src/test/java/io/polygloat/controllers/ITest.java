package io.polygloat.controllers;

import java.util.UUID;

public interface ITest {
    default String generateUniqueString() {
        return UUID.randomUUID().toString();
    }
}
