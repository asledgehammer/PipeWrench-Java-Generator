package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.util.ComplexGenericMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public abstract class TypeScriptElement
    implements TypeScriptResolvable, TypeScriptCompilable, TypeScriptWalkable {

  protected final Map<String, TypeScriptElement> elements = new HashMap<>();
  protected final TypeScriptNamespace namespace;
  protected final Class<?> clazz;
  final ComplexGenericMap genericMap;
  public String name;
  protected boolean walked = false;

  protected TypeScriptElement(TypeScriptNamespace namespace, Class<?> clazz) {
    this.namespace = namespace;
    this.clazz = clazz;
    this.genericMap = new ComplexGenericMap(clazz);

    if (this.clazz != null) {
      Class<?> enclosingClass = clazz.getEnclosingClass();
      String fullName = this.clazz.getSimpleName();
      while (enclosingClass != null) {
        fullName = enclosingClass.getSimpleName() + '$' + fullName;
        enclosingClass = enclosingClass.getEnclosingClass();
      }
      this.name = fullName;
    }
  }

  protected TypeScriptElement(TypeScriptNamespace namespace, String name) {
    this.namespace = namespace;
    this.clazz = null;
    this.genericMap = null;
    this.name = name;
  }

  @Override
  public TypeScriptElement resolve(String path) {
    if (this.clazz == null) return null;

    if (path.contains("$")) {
      String[] split = path.split("\\$");
      TypeScriptElement element = elements.get(split[0]);
      if (element == null) {
        element = getSubElement(namespace, clazz, split[0]);
        elements.put(split[0], element);
      }

      StringBuilder rebuiltPath = new StringBuilder(split[1]);
      if (split.length > 2) {
        for (int index = 2; index < split.length; index++) {
          rebuiltPath.append(split[index]);
        }
      }

      return element.resolve(rebuiltPath.toString());
    }

    if (elements.containsKey(path)) return elements.get(path);

    TypeScriptElement element = getSubElement(namespace, clazz, path);
    elements.put(path, element);

    return element;
  }

  public Class<?> getClazz() {
    return clazz;
  }

  public TypeScriptNamespace getNamespace() {
    return namespace;
  }

  public String getName() {
    return name;
  }

  public boolean hasWalked() {
    return walked;
  }

  public static TypeScriptElement getSubElement(
      TypeScriptNamespace namespace, Class<?> enclosingClazz, String name) {
    for (Class<?> next : enclosingClazz.getClasses()) {
      if (next.getSimpleName().equals(name)) {
        return resolve(namespace, next);
      }
    }
    for (Class<?> next : enclosingClazz.getInterfaces()) {
      if (next.getSimpleName().equals(name)) {
        return resolve(namespace, next);
      }
    }

    throw new RuntimeException();
  }

  public static TypeScriptElement resolve(TypeScriptNamespace namespace, Class<?> clazz) {
    if (clazz.isEnum()) {
      return new TypeScriptEnum(namespace, clazz);
    }
    return new TypeScriptClass(namespace, clazz);
  }

  public static String inspect(TypeScriptGraph graph, String string) {

    String original = "" + string;
    string =
        string.replaceAll("\\? super ", "").replaceAll("\\? extends ", "").replaceAll("\\?", "any");

    string = TypeScriptElement.adaptType(string);

    int arrayDimCount = 0;
    while (string.endsWith("[]")) {
      arrayDimCount++;
      string = string.substring(0, string.length() - 2);
    }

    if (string.contains("<")) {
      Stack<String> args = new Stack<>();
      args.add("");
      int insideCount = 1;
      String s = string;
      int indexOfStart = s.indexOf("<");
      label:
      for (int offset = indexOfStart + 1; offset < s.length(); offset++) {
        char nextChar = s.charAt(offset);
        switch (nextChar) {
          case '<' -> {
            insideCount++;
            args.push("");
          }
          case '>' -> {
            if (--insideCount == 0) {
              String arg = args.pop();
              if (!arg.isEmpty()) {
                if (arg.equals("?")) arg = "any";
                String reformedArg = inspect(graph, arg.trim());
                string = string.replaceAll(arg.replaceAll("\\[", "\\\\["), reformedArg);
              }
              break label;
            }
            String arg = args.pop();
            if (arg.equals("?")) arg = "any";
            String reformedArg = inspect(graph, arg.trim());
            string = string.replaceAll(arg, reformedArg);
          }
          case ' ' -> {}
          case ',' -> {
            String arg = args.pop();
            if (arg.equals("?")) arg = "any";
            String reformedArg = inspect(graph, arg.trim());
            string = string.replaceAll(arg, reformedArg);
            args.push("");
          }
          default -> args.push(args.pop() + nextChar);
        }
      }
      if (!args.isEmpty()) {
        throw new RuntimeException("Invalid string: " + original + " (" + args + ")");
      }
    }

    string = TypeScriptElement.adaptType(string);

    int index = string.indexOf("<");
    if (index != -1) {
      String forName = string.substring(0, index).replace("._function", ".function");
      try {
        Class<?> cl = Class.forName(forName);
        graph.add(cl);
      } catch (Exception ignored) {
      }
    } else {
      try {
        String forName = string.replace("._function", ".function");
        Class<?> cl = Class.forName(forName);
        graph.add(cl);
      } catch (Exception ignored) {
      }
    }

    while (arrayDimCount > 0) {
      string += "[]";
      arrayDimCount--;
    }

    return string;
  }

  public static String adaptType(String type) {
    if (type.startsWith("[")) {
      type = type.substring(2);
    }
    if (type.contains(".function.")) {
      type = type.replaceAll(".function.", "._function.");
    }

    return switch (type) {
      case "java.lang.Boolean" -> "boolean";
      case "byte",
          "short",
          "int",
          "float",
          "double",
          "long",
          "java.lang.Byte",
          "java.lang.Short",
          "java.lang.Integer",
          "java.lang.Float",
          "java.lang.Double",
          "java.lang.Long" -> "number";
      case "char", "java.lang.Character", "java.lang.String" -> "string";
      case "java.lang.Object" -> "any";
      case "java.lang.Void" -> "void";
      default -> type;
    };
  }

  public boolean isValid() {
    return true;
  }

  public String compileLua(String table) {
    return table;
  }
}
