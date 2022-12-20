public class Tuple<T, U, V> {
    private final T first;
    private final U second;
    private final V third;

    public Tuple(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public static <T, U, V> Tuple<T, U, V> of(T t, U u, V v) {
        return new Tuple<>(t, u, v);
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }

    public V getThird() {
        return third;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Tuple t)) {
            return false;
        }

        return t.getFirst().equals(this.getFirst()) &&
                t.getSecond().equals(this.getSecond()) &&
                t.getThird().equals(this.getThird());
    }
}
