package org.example.classes;

public enum TestEnum {
    ONE("one"),
    TWO("two"),
    ;

    final String name;

    TestEnum(String name) {
        this.name = name;
    }
}
