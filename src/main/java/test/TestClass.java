package test;

import com.asledgehammer.typescript.TypeScriptCompiler;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.type.TypeScriptClass;
import com.asledgehammer.typescript.type.TypeScriptElement;
import zombie.util.list.PZArrayList;

import java.util.List;

public class TestClass {

  public static void main(String[] args) {
    TypeScriptCompiler compiler = new TypeScriptCompiler(new TypeScriptSettings());
    compiler.add(ExampleClass.class);
    compiler.add(PZArrayList.class);
    compiler.walk();

    String compiled = compiler.compile();
    compiled += '\n';

    List<TypeScriptElement> elements = compiler.getAllGeneratedElements();
    for (TypeScriptElement element : elements) {
      if (!(element instanceof TypeScriptClass tsClass)) {
        continue;
      }
      compiled += tsClass.compileStaticOnly("") + "\n\n";
    }

    System.out.println("\n\n\nRESULT: ");
    System.out.println(compiled);
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
