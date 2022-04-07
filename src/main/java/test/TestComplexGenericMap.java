package test;

import com.asledgehammer.typescript.util.ClazzUtils;

import java.util.HashMap;
import java.util.List;

public class TestComplexGenericMap {

  public static void main(String[] args) {
    ClazzUtils.evaluate(MyNestedHashMap.class, "put");
    System.out.println();
    ClazzUtils.evaluate(MyNestedList.class, "add", "remove");
  }
}

class MyHashMap<V, K> extends HashMap<K, V> {}

class MyNestedHashMap extends MyHashMap<String, Integer> {}

abstract class MyList<E> implements List<E> {}

abstract class MyNestedList extends MyList<Byte> {}
