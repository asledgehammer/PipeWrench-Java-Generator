package test;

import com.asledgehammer.typescript.TypeScriptCompiler;
import com.asledgehammer.typescript.settings.TypeScriptSettings;

public class TestClass {

  public static void main(String[] args) {
    TypeScriptCompiler compiler = new TypeScriptCompiler(new TypeScriptSettings());
    compiler.add(ExampleClass.class);
    compiler.walk();
    System.out.println("\n\n\nRESULT: ");
    System.out.println(compiler.compile());
    System.out.println();
    System.out.println(compiler.getAllKnownClasses());
  }
}

class ExampleClass<K, V> {

  public int[][][] MyInt;
  public String myString;
  public Integer myWrappedInt;
  public K key;
  public V value;

  public ExampleClass() {}

  public ExampleClass(String arg0, Integer arg1) {}

  public ExampleClass(Integer arg0, Byte arg1, Byte arg2) {}

  public void method() {}

  public void method(String arg0, Integer arg1) {}

  public static String method(Integer arg0, Byte arg1, Byte arg2) {
    return "";
  }
}
