package com.grivera.util;

/**
 * Represents an immutable list of three elements.
 *
 * @param <T> first element
 * @param <U> second element
 * @param <V> third element
 */
public record Tuple<T, U, V>(T first, U second, V third) {

    public static <T, U, V> Tuple<T, U, V> of(T t, U u, V v) {
        return new Tuple<>(t, u, v);
    }

    public boolean equals(Object o) {
        if (!(o instanceof Tuple t)) {
            return false;
        }

        return t.first().equals(this.first()) &&
                t.second().equals(this.second()) &&
                t.third().equals(this.third());
    }
}
