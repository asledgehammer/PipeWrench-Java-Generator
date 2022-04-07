package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.settings.Recursion;

import java.util.*;

public class TypeScriptNamespace
    implements TypeScriptResolvable, TypeScriptWalkable, TypeScriptCompilable {

  private final Map<String, TypeScriptNamespace> namespaces = new HashMap<>();
  private final Map<String, TypeScriptElement> elements = new HashMap<>();
  private final TypeScriptNamespace parent;
  private final TypeScriptGraph graph;
  private final String path;
  private final String fullPath;
  private final String name;

  public TypeScriptNamespace(TypeScriptGraph graph, TypeScriptNamespace parent, String path) {
    this.graph = graph;
    this.parent = parent;
    this.path = path;
    String[] split = path.split("\\.");
    String name = split[split.length - 1];

    if (name.equals("function")) name = '_' + name;

    this.name = name;

    this.fullPath =
        (parent != null ? parent.fullPath + '.' : "") + (path.contains(".") ? split[0] : path);
  }

  @Override
  public String toString() {
    return "TypeScriptNamespace{" + "name='" + name + '\'' + '}';
  }

  @Override
  public TypeScriptElement resolve(String path) {

    if (path.isEmpty()) return null;

    if (path.contains(".")) {
      String[] info = TypeScriptNamespace.shift(path);

      String subNamespace = info[0];
      if (subNamespace.contains(".")) {
        subNamespace = subNamespace.split("\\.")[0];
        if (!subNamespace.contains("_function") && subNamespace.contains("function")) {
          subNamespace = subNamespace.replaceAll("function", "_function");
        }
      }

      String finalInfo = subNamespace;
      TypeScriptNamespace namespace =
          namespaces.computeIfAbsent(
              subNamespace,
              s -> new TypeScriptNamespace(graph, TypeScriptNamespace.this, finalInfo));

      return namespace.resolve(info[1]);
    }

    if (elements.containsKey(path)) return elements.get(path);

    Class<?> clazz = null;
    try {
      clazz = Class.forName(fullPath.replace("_function", "function") + "." + (path));
    } catch (Throwable ignored) {
    }

    Recursion recursion = graph.getCompiler().getSettings().recursion;
    if ((clazz != null && clazz.isEnum()) || !graph.isWalking() || recursion == Recursion.ALL) {
      if (clazz != null) {
        TypeScriptElement element = TypeScriptElement.resolve(this, clazz);
        elements.put(path, element);
        return element;
      }
    }

    if(clazz != null && clazz.getName().startsWith("zombie")) {
      TypeScriptElement element = TypeScriptElement.resolve(this, clazz);
      elements.put(path, element);
      return element;
    }

    TypeScriptType type = new TypeScriptType(this, clazz, path);
    elements.put(path, type);

    return type;
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    for (TypeScriptNamespace namespace : new ArrayList<>(namespaces.values())) {
      namespace.walk(graph);
    }
    for (TypeScriptElement element : new ArrayList<>(elements.values())) {
      element.walk(graph);
    }
  }

  @Override
  public String compile(String prefixOriginal) {

    boolean valid = false;
    for (TypeScriptElement element : elements.values()) {
      if (element.isValid()) {
        valid = true;
        break;
      }
    }

    if (!valid) return "";

    namespaces.remove("function");

    StringBuilder builder = new StringBuilder(prefixOriginal);

    String fullPath = this.fullPath;
    fullPath = fullPath.replaceAll("\\.function ", "._function_ ");
    fullPath = fullPath.replaceAll("\\.function\\.", "._function_.");

    builder.append("export namespace ").append(fullPath).append(" {\n");

    String prefix = prefixOriginal + "  ";

    List<String> names = new ArrayList<>(elements.keySet());
    names.sort(Comparator.naturalOrder());

    for (String key : names) {
      TypeScriptElement element = elements.get(key);
      if (element.isValid()) {
        builder.append(element.compile(prefix)).append('\n');
      }
    }

    builder.append(prefixOriginal).append("}");

    return builder.toString();
  }

  public TypeScriptElement get(String id) {
    return elements.get(id);
  }

  public void set(String id, TypeScriptElement element) {
    if (element.isValid()) {
      elements.put(id, element);
    }
  }

  public String getName() {
    return name;
  }

  public boolean isEmpty() {
    return elements.isEmpty();
  }

  public TypeScriptNamespace getParent() {
    return parent;
  }

  public TypeScriptGraph getGraph() {
    return graph;
  }

  public boolean hasParent() {
    return parent != null;
  }

  public static String[] shift(String path) {
    if (!path.contains(".")) return new String[] {path};
    String[] split = path.split("\\.");
    if (split.length == 2) {
      return new String[] {split[0], split[1]};
    } else {
      StringBuilder builder = new StringBuilder(split[1]);
      for (int index = 2; index < split.length; index++) {
        builder.append('.').append(split[index]);
      }
      return new String[] {split[0], builder.toString()};
    }
  }

  public boolean hasElements() {
    return !elements.isEmpty();
  }

  public Map<String, TypeScriptNamespace> getAllPopulatedNamespaces() {
    Map<String, TypeScriptNamespace> map = new HashMap<>();

    Set<String> keys = namespaces.keySet();
    for (String key : keys) {
      TypeScriptNamespace namespace = namespaces.get(key);
      if (namespace.hasElements()) {
        map.put(namespace.fullPath, namespace);
      }
      map.putAll(namespace.getAllPopulatedNamespaces());
    }

    return map;
  }

  public String getFullPath() {
    return this.fullPath;
  }
}
