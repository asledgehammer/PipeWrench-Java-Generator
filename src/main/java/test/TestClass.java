package test;

import com.asledgehammer.typescript.TypeScriptCompiler;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.type.TypeScriptClass;
import com.asledgehammer.typescript.type.TypeScriptMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TestClass {

  public static void main(String[] args) {
    //    TypeScriptCompiler compiler = new TypeScriptCompiler(new TypeScriptSettings());
    //    compiler.add(ExampleClass.class);
    //    compiler.add(BodyPartType.class);
    //    compiler.walk();
    //
    //    String compiled = compiler.compile();
    //    compiled += '\n';
    //
    //    List<TypeScriptElement> elements = compiler.getAllGeneratedElements();
    //    for (TypeScriptElement element : elements) {
    //      if (element instanceof TypeScriptClass tsClass) {
    //        compiled += tsClass.compileStaticOnly("") + "\n\n";
    //      } else if (element instanceof TypeScriptEnum tsEnum) {
    //        compiled += tsEnum.compileStaticOnly("") + "\n\n";
    //      }
    //    }
    //
    //    System.out.println("\n\n\nRESULT: ");
    //    System.out.println(compiled);

    TypeScriptCompiler compiler = new TypeScriptCompiler(new TypeScriptSettings());
    compiler.add(ExampleClass.class);
    compiler.walk();

    TypeScriptClass globalObject = (TypeScriptClass) compiler.resolve(ExampleClass.class);
    Map<String, List<TypeScriptMethod>> methods = globalObject.getMethods();
    List<String> methodNames = new ArrayList<>(methods.keySet());
    methodNames.sort(Comparator.naturalOrder());

    StringBuilder builder = new StringBuilder();
    for (String methodName : methodNames) {
      List<TypeScriptMethod> methodsList = methods.get(methodName);
      for (TypeScriptMethod method : methodsList) {
        if (method.isStatic()) {
          builder.append(method.compileTypeScriptFunction("")).append('\n');
        }
      }
    }

    System.out.println(builder);
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
