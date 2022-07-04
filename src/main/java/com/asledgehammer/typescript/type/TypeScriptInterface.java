package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeScriptInterface extends TypeScriptElement {

  private final List<TypeScriptGeneric> genericParameters = new ArrayList<>();
  private final Map<String, TypeScriptField> fields = new HashMap<>();
  private final Map<String, TypeScriptMethod> methods = new HashMap<>();

  protected TypeScriptInterface(TypeScriptNamespace namespace, Class<?> clazz) {
    super(namespace, clazz);
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    walkGenericParameters(graph);
    walkFields(graph);
    walkMethods(graph);
    walked = true;
  }

  private void walkGenericParameters(TypeScriptGraph graph) {
    if (clazz == null) return;

    TypeVariable[] parameters = clazz.getTypeParameters();
    for (TypeVariable parameter : parameters) {
      TypeScriptGeneric generic = new TypeScriptGeneric(parameter);
      genericParameters.add(generic);
    }

    for (TypeScriptGeneric generic : genericParameters) {
      generic.walk(graph);
    }
  }

  private void walkFields(TypeScriptGraph graph) {
    if (clazz == null) return;
    for (Field field : clazz.getDeclaredFields()) {
      fields.put(field.getName(), new TypeScriptField(this, field));
    }
    for (TypeScriptField field : fields.values()) field.walk(graph);
  }

  private void walkMethods(TypeScriptGraph graph) {
    if (clazz == null) return;
    for (Method method : clazz.getDeclaredMethods()) {
      methods.put(method.getName(), new TypeScriptMethod(this, method));
    }
    for (TypeScriptMethod method : methods.values()) method.walk(graph);
  }

  @Override
  public String compile(String prefix) {
    return prefix + "export interface " + name + " {}";
  }
}
