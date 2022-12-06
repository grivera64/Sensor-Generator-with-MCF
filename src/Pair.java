public class Pair<K, V> {
    private K key;
    private V value;
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return this.key;
    }

    public V getValue() {
        return this.value;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Pair<?, ?> p)) {
            return false;
        }

        return this.getKey().equals(p.getKey()) && this.getValue().equals(p.getValue());
    }
}
