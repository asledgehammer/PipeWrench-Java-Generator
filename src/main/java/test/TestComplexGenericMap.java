package test;

import com.asledgehammer.typescript.ComplexGenericMap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class MyHashMap<V, K> extends HashMap<K, V> {}
class MyNestedHashMap extends MyHashMap<String, Integer> {}

public class TestComplexGenericMap {

  public static void main(String[] args) {

    ComplexGenericMap mapMyHashMap = new ComplexGenericMap(MyNestedHashMap.class);
    for (Method method : MyHashMap.class.getMethods()) {
      if (!method.getName().equals("put")) continue;

      int modifiers = method.getModifiers();
      if (!Modifier.isPublic(modifiers)
          || Modifier.isStatic(modifiers)
          || Modifier.isNative(modifiers)) {
        continue;
      }

      String compiled = method.getName() + "(";

      Parameter[] parameters = method.getParameters();
      if (parameters.length != 0) {
        for (Parameter parameter : method.getParameters()) {
          compiled += mapMyHashMap.resolveDeclaredParameter(parameter) + ", ";
        }
        compiled = compiled.substring(0, compiled.length() - 2);
      }

      compiled += "): ";
      compiled +=
          mapMyHashMap.resolveDeclaredType(
                  method.getDeclaringClass(), method.getGenericReturnType())
              + ";";

      System.out.println(compiled);
    }

    //    List<String> paramNames = ComplexGenericMap.extractTypeDeclarations(HashMap.class);
    //    List<String> paramNames2 = ComplexGenericMap.extractTypeDeclarations(MyHashMap.class);
    //
    //    System.out.println(paramNames);
    //    System.out.println(paramNames2);

    //            ComplexGenericMap v = new ComplexGenericMap(ItemVisuals.class);
    //            System.out.println(v);

    //    extractTypeDeclarations(HashMap.class);
  }

  public static List<String> extractTypeDeclarations(Class<?> clazz) {
    List<String> list = new ArrayList<>();
    String raw = clazz.getGenericSuperclass().getTypeName();

    if (!raw.contains("<")) {
      return list;
    }

    int indexOf = raw.indexOf("<");
    int inside = 1;

    int indexCurrent = indexOf + 1;
    StringBuilder builder = new StringBuilder();
    while (inside != 0) {
      char next = raw.charAt(indexCurrent++);
      switch (next) {
        case '<' -> {
          inside++;
          builder.append(next);
        }
        case '>' -> {
          inside--;
          if (inside == 0) {
            if (builder.length() != 0) {
              list.add(builder.toString());
            }
          } else if (inside == 1) {
            list.add(builder.toString());
            builder = new StringBuilder();
          } else {
            builder.append(next);
          }
        }
        default -> builder.append(next);
      }
    }

    return list;
  }
}
