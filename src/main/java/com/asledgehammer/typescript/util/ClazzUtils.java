package com.asledgehammer.typescript.util;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

public class ClazzUtils {

  private ClazzUtils() {
    throw new RuntimeException("Cannot instantiate ClazzUtils.");
  }

  public String removeDuplicateNamespaces(String input) {
    if (input == null || !input.contains(".")) {
      return input;
    }
    String[] split = input.split("\\.");
    StringBuilder built = new StringBuilder(split[0]);
    if (split.length != 1) {
      for (int index = 1; index < split.length; index++) {
        if (split[index - 1].equals(split[index])) {
          continue;
        }
        built.append('.').append(split[index]);
      }
    }
    return built.toString();
  }

  public static List<String> extractTypeDeclarations(Class<?> clazz) {
    List<String> list = new ArrayList<>();
    Type superClazz = clazz.getGenericSuperclass();
    if (superClazz == null) {
      return list;
    }
    return extractNestedArgs(superClazz.getTypeName());
  }

  public static List<String> extractNestedArgs(String raw) {
    List<String> list = new ArrayList<>();
    int indexOf = raw.indexOf("<");
    if (indexOf == -1) {
      return list;
    }
    int indexCurrent = indexOf + 1;
    int inside = 1;
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
              list.add(builder.toString().trim());
            }
          } else if (inside == 1) {
            builder.append('>');
            list.add(builder.toString().trim());
            builder = new StringBuilder();
          } else {
            builder.append(next);
          }
        }
        case ',' -> {
          if (inside == 1) {
            list.add(builder.toString().trim());
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

  public static String print(Class<?> clazz) {
    int mods = clazz.getModifiers();
    String visibility = "";
    if (Modifier.isPublic(mods)) {
      visibility = "public ";
    } else if (Modifier.isProtected(mods)) {
      visibility = "protected ";
    } else if (Modifier.isPrivate(mods)) {
      visibility = "private ";
    }
    String isStatic = Modifier.isStatic(mods) ? "static " : "";
    String isFinal = Modifier.isFinal(mods) ? "final " : "";
    String isAbstract = Modifier.isAbstract(mods) ? "abstract " : "";
    String type;
    if (clazz.isInterface()) {
      type = "interface ";
    } else if (clazz.isEnum()) {
      type = "enum ";
    } else {
      type = "class ";
    }

    StringBuilder isParamTypes = new StringBuilder();
    TypeVariable<?>[] vars = clazz.getTypeParameters();
    if (vars.length != 0) {
      isParamTypes.append("<");
      for (TypeVariable<?> var : vars) {
        isParamTypes.append(var.getTypeName()).append(", ");
      }
      isParamTypes = new StringBuilder(isParamTypes.substring(0, isParamTypes.length() - 2) + ">");
    }

    String isExtends = "";
    Type gSuperClazz = clazz.getGenericSuperclass();
    if (gSuperClazz != null) {
      isExtends = " extends " + gSuperClazz.getTypeName();
    } else {
      Class<?> superClazz = clazz.getSuperclass();
      if (superClazz != null) {
        isExtends = " extends " + superClazz.getSimpleName();
      }
    }

    StringBuilder isImplements = new StringBuilder();
    Type[] interfaces = clazz.getGenericInterfaces();
    if (interfaces.length != 0) {
      isImplements = new StringBuilder(" implements ");
      for (Type i : interfaces) {
        isImplements.append(i.getTypeName()).append(", ");
      }
      isImplements = new StringBuilder(isImplements.substring(0, isImplements.length() - 2) + " ");
    }

    return visibility
        + isStatic
        + isFinal
        + isAbstract
        + type
        + clazz.getSimpleName()
        + isParamTypes
        + isExtends
        + isImplements;
  }

  public static void evaluate(Class<?> clazz, String... includesMethods) {
    List<String> includes = new ArrayList<>();
    for (String i : includesMethods) {
      if (!includes.contains(i)) {
        includes.add(i);
      }
    }

    StringBuilder builder = new StringBuilder();
    builder.append(clazz.getName()).append(" {\n");
    ComplexGenericMap genericMap = new ComplexGenericMap(clazz);
    for (Method method : clazz.getMethods()) {
      if (!includes.isEmpty() && !includes.contains(method.getName())) {
        continue;
      }

      int modifiers = method.getModifiers();
      if (!Modifier.isPublic(modifiers)
          || Modifier.isStatic(modifiers)
          || Modifier.isNative(modifiers)) {
        continue;
      }

      StringBuilder compiled = new StringBuilder("  " + method.getName() + "(");

      Parameter[] parameters = method.getParameters();
      if (parameters.length != 0) {
        for (Parameter parameter : method.getParameters()) {
          compiled.append(genericMap.resolveDeclaredParameter(parameter)).append(", ");
        }
        compiled = new StringBuilder(compiled.substring(0, compiled.length() - 2));
      }

      compiled.append("): ");
      compiled
          .append(
              genericMap.resolveDeclaredType(
                  method.getDeclaringClass(), method.getGenericReturnType()))
          .append(";\n");

      builder.append(compiled);
    }

    builder.append("}");

    System.out.println(builder);
  }

  public static String walkTypesRecursively(
      ComplexGenericMap genericMap, Class<?> declClazz, String s) {

    int indexOf = s.indexOf('<');
    String rootString = indexOf != -1 ? s.substring(0, indexOf) : s;

    s = s.replaceAll("\\? extends ", "").replaceAll("\\? super ", "").replaceAll("capture of ", "");

    boolean hasNestedArgs = indexOf != -1;
    StringBuilder nestedArgsString = new StringBuilder();

    if (hasNestedArgs) {
      List<String> nestedArgs = extractNestedArgs(s);
      if (!nestedArgs.isEmpty()) {
        nestedArgsString.append("<");
        for (String inner : nestedArgs) {
          if (inner == null || inner.isEmpty()) {
            continue;
          }
          if (inner.indexOf('<') != -1) {
            nestedArgsString.append(walkTypesRecursively(genericMap, declClazz, inner))
                .append(", ");
          } else {
            if (genericMap != null) {
              nestedArgsString.append(genericMap.resolveDeclaredType(declClazz, inner))
                  .append(", ");
            } else {
              nestedArgsString.append(inner).append(", ");
            }
          }
        }
        nestedArgsString = new StringBuilder(
            nestedArgsString.substring(0, nestedArgsString.length() - 2) + ">");
      }
    }

    if (genericMap != null) {
      rootString = genericMap.resolveDeclaredType(declClazz, rootString);
    }

    String result = rootString + nestedArgsString;

    result = result.replaceAll("null", "java.lang.Object");

    return result;
  }
}
