package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.util.DocBuilder;

import java.lang.reflect.*;
import java.util.*;

public class TypeScriptClass extends TypeScriptElement {

  private final List<TypeScriptGeneric> genericParameters = new ArrayList<>();
  private final Map<String, TypeScriptField> fields = new HashMap<>();
  private final Map<String, TypeScriptMethodCluster> methods = new HashMap<>();
  private final Map<String, TypeScriptMethodCluster> staticMethods = new HashMap<>();
  private TypeScriptConstructor constructor;

  protected TypeScriptClass(TypeScriptNamespace namespace, Class<?> clazz) {
    super(namespace, clazz);
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    if (this.walked) {
      return;
    }
    System.out.println("Walking " + getName());
    walkConstructors(graph);
    walkGenericParameters(graph);
    walkFields(graph);
    walkMethods(graph);
    walkSub(graph);
    checkDuplicateFieldMethods(graph);
    this.walked = true;
  }

  private void walkConstructors(TypeScriptGraph graph) {
    if (clazz == null) {
      return;
    }
    constructor = new TypeScriptConstructor(this);
    constructor.walk(graph);
  }

  private void checkDuplicateFieldMethods(TypeScriptGraph ignored) {

    TypeScriptSettings settings = getNamespace().getGraph().getCompiler().getSettings();
    if (!settings.renderStaticFields && !settings.renderNonStaticFields) {
      return;
    }

    List<String> names = new ArrayList<>(fields.keySet());
    for (String fieldName : names) {
      if (methods.containsKey(fieldName)) {
        System.out.println(
            "[" + this.name + "]: Removing duplicate field as function: " + fieldName);
        fields.remove(fieldName);
      }
    }
  }

  private void walkSub(TypeScriptGraph graph) {
    if (this.clazz == null) {
      return;
    }
    for (Class<?> clazz : this.clazz.getNestMembers()) {
      if (!Modifier.isPublic(clazz.getModifiers())) {
        continue;
      }
      graph.add(clazz);
    }
  }

  private void walkGenericParameters(TypeScriptGraph graph) {
    genericParameters.clear();
    if (clazz == null) {
      return;
    }
    TypeVariable<?>[] params = clazz.getTypeParameters();

    for (TypeVariable<?> param : params) {
      TypeScriptGeneric generic = new TypeScriptGeneric(param);
      genericParameters.add(generic);
    }

    for (TypeScriptGeneric param : genericParameters) {
      param.walk(graph);
    }
  }

  private void walkFields(TypeScriptGraph graph) {
    if (clazz == null) {
      return;
    }
    TypeScriptSettings settings = getNamespace().getGraph().getCompiler().getSettings();
    if (!settings.renderStaticFields && !settings.renderNonStaticFields) {
      return;
    }
    fields.clear();
    for (Field field : clazz.getDeclaredFields()) {
      if (Modifier.isPublic(field.getModifiers())) {

        boolean isStatic = Modifier.isStatic(field.getModifiers());
        if ((isStatic && !settings.renderStaticFields)
            || (!isStatic && !settings.renderNonStaticFields)) {
          continue;
        }

        fields.put(field.getName(), new TypeScriptField(this, field));
      }
    }
    for (TypeScriptField field : fields.values()) {
      field.walk(graph);
    }
  }

  private void walkMethods(TypeScriptGraph graph) {
    if (clazz == null) {
      return;
    }
    methods.clear();

    TypeScriptSettings settings = namespace.getGraph().getCompiler().getSettings();
    for (Method method : clazz.getMethods()) {
      if (settings.isBlackListed(method)) {
        continue;
      }
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      if (Modifier.isStatic(method.getModifiers())) {
        if (!staticMethods.containsKey(method.getName())) {
          staticMethods.put(method.getName(), new TypeScriptMethodCluster(this, method));
        }
      } else {
        if (!methods.containsKey(method.getName())) {
          methods.put(method.getName(), new TypeScriptMethodCluster(this, method));
        }
      }
    }

    for (TypeScriptMethodCluster method : this.methods.values()) {
      method.walk(graph);
    }
    for (TypeScriptMethodCluster method : this.staticMethods.values()) {
      method.walk(graph);
    }
  }

  @Override
  public String compile(String prefixOriginal) {

    if (clazz == null) {
      return "";
    }

    TypeScriptSettings settings = getNamespace().getGraph().getCompiler().getSettings();

    DocBuilder docBuilder = new DocBuilder();
    docBuilder.appendLine("@customConstructor " + clazz.getSimpleName() + ".new");
    docBuilder.appendLine("@");

    String prefix = prefixOriginal + "  ";
    StringBuilder stringBuilder = new StringBuilder();

    if (clazz.isInterface()) {
      stringBuilder.append("[INTERFACE] ");
    } else {
      stringBuilder.append("[");
      if (Modifier.isAbstract(clazz.getModifiers())) {
        stringBuilder.append("ABSTRACT ");
      }
      stringBuilder.append("CLASS] ");
    }

    stringBuilder.append(clazz.getName());
    Class<?> superClazz = clazz.getSuperclass();

    Type genericSuperclazz = clazz.getGenericSuperclass();
    if (genericSuperclazz != null && !genericSuperclazz.equals(Object.class)) {
      stringBuilder.append(" extends ").append(clazz.getGenericSuperclass().getTypeName());
    } else {
      if (superClazz != null && !superClazz.equals(Object.class)) {
        stringBuilder.append(" extends ").append(superClazz.getName());
      }
    }

    docBuilder.appendLine(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append(docBuilder.build(prefixOriginal)).append("\n").append(prefixOriginal);
    stringBuilder.append("export class ").append(name);

    StringBuilder compiledParams = new StringBuilder();
    if (!genericParameters.isEmpty()) {
      compiledParams.append('<');
      for (TypeScriptGeneric param : genericParameters) {
        compiledParams.append(param.compile("")).append(", ");
      }
      compiledParams = new StringBuilder(
          compiledParams.substring(0, compiledParams.length() - 2) + '>');
    }
    stringBuilder.append(compiledParams);
    stringBuilder.append(" {\n");

    if (settings.renderStaticFields || settings.renderNonStaticFields) {
      if (!fields.isEmpty()) {
        List<String> names = new ArrayList<>(fields.keySet());
        names.sort(Comparator.naturalOrder());
        // Static Field(s)
        for (String _name : names) {
          TypeScriptField field = fields.get(_name);

          if (field.isStatic() && settings.renderStaticFields) {
            stringBuilder.append(field.compile(prefix)).append('\n');
          }
        }
        // Non-Static Field(s)
        for (String name : names) {
          TypeScriptField field = fields.get(name);
          if (!field.isStatic() && settings.renderNonStaticFields) {
            stringBuilder.append(field.compile(prefix)).append('\n');
          }
        }
        stringBuilder.append('\n');
      }
    }

    if (clazz.isInterface()) {
      stringBuilder.append(prefix).append("protected constructor();\n");
    } else {
      stringBuilder.append(constructor.compileCustomConstructor(prefix)).append('\n');
    }

    if (!methods.isEmpty()) {
      List<String> names = new ArrayList<>(methods.keySet());
      names.sort(Comparator.naturalOrder());
      // Non-Static Method(s)
      for (String name : names) {
        TypeScriptMethodCluster method = this.methods.get(name);
        stringBuilder.append(method.compile(prefix)).append('\n');
      }
    }

    if (!staticMethods.isEmpty()) {
      List<String> names = new ArrayList<>(staticMethods.keySet());
      names.sort(Comparator.naturalOrder());
      // Static Method(s)
      for (String name : names) {
        TypeScriptMethodCluster method = this.staticMethods.get(name);
        stringBuilder.append(method.compile(prefix)).append('\n');
      }
    }

    if (!stringBuilder.toString().endsWith("\n")) {
      stringBuilder.append('\n');
    }

    stringBuilder.append(prefixOriginal).append("}");
    return stringBuilder.toString();
  }

  @Override
  public String compileLua(String table) {
    StringBuilder stringBuilder = new StringBuilder();
    if (!staticMethods.isEmpty()) {
      List<String> names = new ArrayList<>(staticMethods.keySet());
      names.sort(Comparator.naturalOrder());
      for (String name : names) {
        TypeScriptMethodCluster method = this.staticMethods.get(name);
        stringBuilder.append(method.compileLua(table)).append('\n');
      }
    }
    return stringBuilder.toString();
  }

  public Map<String, TypeScriptMethodCluster> getStaticMethods() {
    return this.staticMethods;
  }
}
