package lambda;

/**
 * @author pmilne
 */
@SuppressWarnings("WeakerAccess")
public class List<T> {
    private final List<T> parent;
    private final T value;

    protected List(List<T> parent, T value) {
        this.parent = parent;
        this.value = value;
    }

    public static <T> List<T> create(List<T> parent, T value) {
        return new List<>(parent, value);
    }

    public T get(int n) {
        List<T> that = this;
        for (int i = 0; i < n; i++) {
            that = that.parent;
        }
        return that.value;
    }

    public int indexOf(T o) {
        return value.equals(o) ? 0 : 1 + parent.indexOf(o);
    }

    public String toString() {
        return "{" + parent + " " + value + "}";
    }
}
