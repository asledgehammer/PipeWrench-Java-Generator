package com.asledgehammer.typescript;

import com.asledgehammer.typescript.type.TypeScriptElement;
import com.asledgehammer.typescript.type.TypeScriptNamespace;

import java.util.*;

public class TypeScriptGraph {
  final Map<String, TypeScriptNamespace> namespaces = new HashMap<>();
  final List<Class<?>> knownClasses = new ArrayList<>();
  private final TypeScriptCompiler compiler;
  private boolean readOnly = false;

  private boolean addedWhileWalking = false;
  private boolean walking = false;

  public List<Class<?>> getAllKnownClasses() {
    return knownClasses;
  }

  public TypeScriptGraph(TypeScriptCompiler compiler) {
    this.compiler = compiler;
  }

  public String compile() {

    Map<String, TypeScriptNamespace> namespaces = getAllPopulatedNamespaces();

    String prefix = "";
    StringBuilder builder = new StringBuilder();
    //
    List<String> names = new ArrayList<>(namespaces.keySet());
    names.sort(Comparator.naturalOrder());
    //

    for (String key : names) {
      // Internal Object[] arrays.
      if (key.startsWith("[L")) continue;

      // Any array.
      if (key.startsWith("[")) continue;

      if (key.startsWith("void")
          || key.startsWith("unknown")
          || key.startsWith("any")
          || key.startsWith("object")
          || key.startsWith("boolean")
          || key.startsWith("byte")
          || key.startsWith("short")
          || key.startsWith("int")
          || key.startsWith("float")
          || key.startsWith("double")
          || key.startsWith("long")
          || key.startsWith("char")
          || key.startsWith("string")) {
        continue;
      }

      if (key.equals("java.util.function")) continue;

      TypeScriptNamespace namespace = namespaces.get(key);
      if (namespace.getName().isEmpty()) continue;
      String compiled = namespace.compile(prefix);
      if (compiled.isEmpty()) continue;
      builder.append(compiled).append('\n');
    }

    return builder.toString();
  }

  public void walk() {
    if (readOnly) throw new RuntimeException("Cannot walk when in read-only mode.");

    int cycle = 1;
    int maxCycles = 1000;
    walking = true;

    do {
      addedWhileWalking = false;
      for (TypeScriptNamespace namespace : new ArrayList<>(namespaces.values())) {
        namespace.walk(this);
      }
    } while (addedWhileWalking && cycle < maxCycles);

    walking = false;
    readOnly = true;
  }

  public void add(Class<?>... clazzes) {
    if (readOnly) throw new RuntimeException("Cannot add classes when in read-only mode.");
    for (Class<?> clazz : clazzes) {
      if (clazz.equals(Object.class)
          || clazz.equals(Void.class)
          || clazz.equals(Character.class)
          || clazz.equals(String.class)
          || clazz.equals(Boolean.class)
          || clazz.equals(Byte.class)
          || clazz.equals(Short.class)
          || clazz.equals(Integer.class)
          || clazz.equals(Float.class)
          || clazz.equals(Double.class)
          || clazz.equals(Long.class)) {
        continue;
      }
      TypeScriptElement element = resolve(clazz.getName());
      if (walking && element != null && !element.hasWalked()) {
        addedWhileWalking = true;
      }

      if (clazz.isArray()) clazz = clazz.getComponentType();
      if (clazz.equals(Void.class)
          || clazz.equals(Object.class)
          || clazz.equals(Boolean.class)
          || clazz.equals(Byte.class)
          || clazz.equals(Short.class)
          || clazz.equals(Integer.class)
          || clazz.equals(Float.class)
          || clazz.equals(Double.class)
          || clazz.equals(Long.class)
          || clazz.equals(Character.class)
          || clazz.equals(String.class)) {
        return;
      }
      if (!knownClasses.contains(clazz)) knownClasses.add(clazz);
    }
  }

  public List<Class<?>> getAllDeclaredClasses() {
    List<Class<?>> list = new ArrayList<>();
    for (TypeScriptNamespace namespace : namespaces.values()) {
      list.addAll(namespace.getAllDeclaredClasses());
    }
    return list;
  }

  public List<TypeScriptElement> getAllGeneratedElements() {
    List<TypeScriptElement> list = new ArrayList<>();
    for (TypeScriptNamespace namespace : namespaces.values()) {
      list.addAll(namespace.getAllGeneratedElements());
    }
    return list;
  }

  public TypeScriptElement resolve(String path) {

    if (path.trim().isEmpty()) {
      System.out.println("RESOLVE PATH IS EMPTY.");
      return null;
    }

    if (path.equals("any")
        || path.equals("number")
        || path.equals("object")
        || path.equals("unknown")
        || path.equals("string")
        || path.equals("void")
        || path.equals("boolean")
        || path.equals("byte")
        || path.equals("short")
        || path.equals("int")
        || path.equals("float")
        || path.equals("double")
        || path.equals("long")) {
      return null;
    }

    while (path.endsWith("[]")) {
      path = path.substring(0, path.length() - 2);
    }

    if (path.contains(";")) {
      path = path.replaceAll(";", "");
    }

    String[] info = TypeScriptNamespace.shift(path);
    TypeScriptNamespace typeScriptNamespace = namespaces.get(info[0]);
    if (typeScriptNamespace == null) {
      typeScriptNamespace = new TypeScriptNamespace(this, null, info[0]);
      namespaces.put(info[0], typeScriptNamespace);
    }
    if (info.length == 1) {
      return typeScriptNamespace.resolve(info[0]);
    } else {
      return typeScriptNamespace.resolve(info[1]);
    }
  }

  public Map<String, TypeScriptNamespace> getAllPopulatedNamespaces() {

    Map<String, TypeScriptNamespace> map = new HashMap<>();

    Set<String> keys = namespaces.keySet();
    for (String key : keys) {
      TypeScriptNamespace namespace = namespaces.get(key);
      if (namespace.hasElements()) {
        map.put(namespace.getFullPath(), namespace);
      }
      map.putAll(namespace.getAllPopulatedNamespaces());
    }

    return map;
  }

  public void set(String path, TypeScriptNamespace namespace) {
    namespaces.put(path, namespace);
  }

  public TypeScriptCompiler getCompiler() {
    return compiler;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public boolean isWalking() {
    return walking;
  }
}
