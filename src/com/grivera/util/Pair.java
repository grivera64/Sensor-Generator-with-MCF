package com.grivera.util;

/**
 * Represents an immutable list of two elements.
 *
 * @param <T> first element
 * @param <U> second element
 */
public record Pair<T, U>(T first, U second) {

    public static <T, U> Pair<T, U> of(T t, U u) {
        return new Pair<>(t, u);
    }

    public boolean equals(Object o) {
        if (!(o instanceof Pair t)) {
            return false;
        }

        return t.first().equals(this.first()) &&
                t.second().equals(this.second());
    }
}
