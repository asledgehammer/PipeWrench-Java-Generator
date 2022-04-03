package test;

import java.util.List;

public class ExampleClass<K, V extends String> {

    public static String exMutable = "I'm mutable!";
    public static boolean exBoolean = false;
    public static Boolean exBooleanWrapper = Boolean.FALSE;

    public K genericValue;

    public ExampleClass() {}

    public V get(K key) {
        return null;
    }

    public K getKey(V value) {
        return null;
    }

    public void basicMethod() {

    }

    public static void basicStaticMethod() {

    }

    public static <K, V extends List<String>> V getStatic(K key, Class<V> valueType) {
        return null;
    }
}
