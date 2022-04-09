package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;

import java.lang.reflect.*;
import java.util.*;

public class TypeScriptClass extends TypeScriptElement {

  private final List<TypeScriptGeneric> genericParameters = new ArrayList<>();
  private final Map<String, TypeScriptField> fields = new HashMap<>();
  private final Map<String, List<TypeScriptMethod>> methods = new HashMap<>();
  private TypeScriptConstructor constructor;

  protected TypeScriptClass(TypeScriptNamespace namespace, Class<?> clazz) {
    super(namespace, clazz);
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    if (this.walked) return;
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
    if (clazz == null) return;
    constructor = new TypeScriptConstructor(this);
    constructor.walk(graph);
  }

  private void checkDuplicateFieldMethods(TypeScriptGraph graph) {
    List<String> names = new ArrayList<>(fields.keySet());
    for (String fieldName : names) {
      if (methods.containsKey(fieldName)) {
        System.out.println(
            "(Class " + this.name + ") -> Removing duplicate field as function: " + fieldName);
        fields.remove(fieldName);
      }
    }
  }

  private void walkSub(TypeScriptGraph graph) {
    if (this.clazz == null) return;
    for (Class<?> clazz : this.clazz.getNestMembers()) {
      if (!Modifier.isPublic(clazz.getModifiers())) continue;
      graph.add(clazz);
    }
  }

  private void walkGenericParameters(TypeScriptGraph graph) {
    genericParameters.clear();
    TypeVariable[] params = clazz.getTypeParameters();

    for (TypeVariable param : params) {
      TypeScriptGeneric generic = new TypeScriptGeneric(param);
      genericParameters.add(generic);
    }

    for (TypeScriptGeneric param : genericParameters) {
      param.walk(graph);
    }
  }

  private void walkFields(TypeScriptGraph graph) {
    if (clazz == null) return;
    fields.clear();
    for (Field field : clazz.getFields()) {
      if (Modifier.isPublic(field.getModifiers())) {
        fields.put(field.getName(), new TypeScriptField(this, field));
      }
    }
    for (TypeScriptField field : fields.values()) field.walk(graph);
  }

  private void walkMethods(TypeScriptGraph graph) {
    if (clazz == null) return;
    methods.clear();
    for (Method method : clazz.getMethods()) {
      if (Modifier.isPublic(method.getModifiers())) {
        List<TypeScriptMethod> ms =
            methods.computeIfAbsent(method.getName(), s -> new ArrayList<>());
        ms.add(new TypeScriptMethod(this, method));
      }
    }
    for (List<TypeScriptMethod> methods : this.methods.values()) {
      for (TypeScriptMethod method : methods) method.walk(graph);
    }
  }

  @Override
  public String compile(String prefixOriginal) {
    String prefix = prefixOriginal + "  ";
    StringBuilder stringBuilder = new StringBuilder(prefixOriginal);
    stringBuilder.append("// ").append((clazz != null ? clazz.getName() : "(Unknown)"));
    if (clazz != null) {
      Type genericSuperclass = clazz.getGenericSuperclass();
      if (genericSuperclass != null) {
        stringBuilder.append(" extends ").append(clazz.getGenericSuperclass().getTypeName());
      } else {
        Class<?> superClazz = clazz.getSuperclass();
        if (superClazz != null) {
          stringBuilder.append(" extends ").append(superClazz.getName());
        }
      }
    }

    stringBuilder.append("\n").append(prefixOriginal);
    stringBuilder.append("export class ").append(name);

    String compiledParams = "";
    if (!genericParameters.isEmpty()) {
      compiledParams += '<';

      for (TypeScriptGeneric param : genericParameters) {
        compiledParams += param.compile("") + ", ";
      }
      compiledParams = compiledParams.substring(0, compiledParams.length() - 2) + '>';
    }
    stringBuilder.append(compiledParams);

    stringBuilder.append(" {\n");

    if (!fields.isEmpty()) {
      List<String> names = new ArrayList<>(fields.keySet());
      names.sort(Comparator.naturalOrder());
      for (String name : names) {
        TypeScriptField field = fields.get(name);
        stringBuilder.append(field.compile(prefix)).append('\n');
      }
      stringBuilder.append('\n');
    }

    if (constructor.exists) {
      stringBuilder.append(constructor.compile(prefix)).append("\n\n");
    } else {
      if (namespace.getGraph().getCompiler().getSettings().readOnly) {
        stringBuilder.append(prefix).append("private constructor();\n\n");
      }
    }

    if (!methods.isEmpty()) {
      List<String> names = new ArrayList<>(methods.keySet());
      names.sort(Comparator.naturalOrder());
      for (String name : names) {
        List<TypeScriptMethod> methods = this.methods.get(name);
        for (TypeScriptMethod method : methods) {
          stringBuilder.append(method.compile(prefix)).append('\n');
        }
      }
    }

    stringBuilder.append(prefixOriginal).append("}");
    return stringBuilder.toString();
  }

  @Override
  public String compileLua(String table) {
    StringBuilder stringBuilder = new StringBuilder();
    if (!methods.isEmpty()) {
      List<String> names = new ArrayList<>(methods.keySet());
      names.sort(Comparator.naturalOrder());
      for (String name : names) {
        List<TypeScriptMethod> methods = this.methods.get(name);
        for (TypeScriptMethod method : methods) {
          stringBuilder.append(method.compileLua(table)).append('\n');
        }
      }
    }
    return stringBuilder.toString();
  }

  public Map<String, List<TypeScriptMethod>> getMethods() {
    return this.methods;
  }
}
