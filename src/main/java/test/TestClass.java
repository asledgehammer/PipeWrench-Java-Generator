package test;

import com.asledgehammer.typescript.TypeScriptCompiler;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.type.TypeScriptClass;
import com.asledgehammer.typescript.type.TypeScriptElement;
import com.asledgehammer.typescript.type.TypeScriptEnum;

import java.util.Arrays;
import java.util.List;

public class TestClass {

  public static void main(String[] args) {

    TypeScriptSettings settings = new TypeScriptSettings();
    settings.methodsBlackListByPath.add("java.lang.Object#equals");
    settings.methodsBlackListByPath.add("java.lang.Object#getClass");
    settings.methodsBlackListByPath.add("java.lang.Object#hashCode");
    settings.methodsBlackListByPath.add("java.lang.Object#notify");
    settings.methodsBlackListByPath.add("java.lang.Object#notifyAll");
    settings.methodsBlackListByPath.add("java.lang.Object#toString");
    settings.methodsBlackListByPath.add("java.lang.Object#wait");
    TypeScriptCompiler compiler = new TypeScriptCompiler(settings);
    compiler.add(ExampleClass.class);
    compiler.add(List.class);
    compiler.walk();

    String compiled = compiler.compile();
    compiled += '\n';

//    List<TypeScriptElement> elements = compiler.getAllGeneratedElements();
//    for (TypeScriptElement element : elements) {
//      if (element instanceof TypeScriptClass tsClass) {
//        compiled += tsClass.compileStaticOnly("") + "\n\n";
//      } else if (element instanceof TypeScriptEnum tsEnum) {
//        compiled += tsEnum.compileStaticOnly("") + "\n\n";
//      }
//    }

    System.out.println("\n\n\nRESULT: ");
    System.out.println(compiled);

    //    TypeScriptCompiler compiler = new TypeScriptCompiler(new TypeScriptSettings());
    //    compiler.add(ExampleClass.class);
    //    compiler.walk();
    //
    //    TypeScriptClass globalObject = (TypeScriptClass) compiler.resolve(ExampleClass.class);
    //    Map<String, List<TypeScriptMethod>> methods = globalObject.getMethods();
    //    List<String> methodNames = new ArrayList<>(methods.keySet());
    //    methodNames.sort(Comparator.naturalOrder());
    //
    //    StringBuilder builder = new StringBuilder();
    //    for (String methodName : methodNames) {
    //      List<TypeScriptMethod> methodsList = methods.get(methodName);
    //      for (TypeScriptMethod method : methodsList) {
    //        if (method.isStatic()) {
    //          builder.append(method.compileTypeScriptFunction("")).append('\n');
    //        }
    //      }
    //    }
    //
    //    System.out.println(builder);
  }
}

class ExampleClass<K, V> {

  public int[][][] MyInt;
  public String myString;
  public Integer myWrappedInt;
  public K key;
  public V value;

  public ExampleClass() {}

  public ExampleClass(String arg0, Integer... arg1) {}

  public ExampleClass(Integer arg0, Byte arg1, byte arg2) {}

  public void method() {}

  public void method(String arg0, Integer... arg1) {}

  public static String method(Integer arg0, Byte arg1, ExampleClass arg2) {
    return "";
  }
}
