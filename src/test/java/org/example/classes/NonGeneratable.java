package org.example.classes;

public class NonGeneratable {
    int i;

    public NonGeneratable(int i) {
        this.i = i;
    }

    @Override
    public String toString() {
        return "NonGeneratable(" + i + ")";
    }
}
