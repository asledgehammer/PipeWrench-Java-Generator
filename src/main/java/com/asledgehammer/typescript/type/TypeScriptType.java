package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

public class TypeScriptType extends TypeScriptElement {

  private final List<TypeScriptGeneric> genericParameters = new ArrayList<>();

  public TypeScriptType(TypeScriptNamespace namespace, Class<?> clazz, String name) {
    super(namespace, clazz);
    if (clazz == null) {
      this.name = name;
    }
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    if (clazz != null) {
      genericParameters.clear();
      TypeVariable<?>[] params = clazz.getTypeParameters();

      for (TypeVariable<?> param : params) {
        TypeScriptGeneric generic = new TypeScriptGeneric(param);
        genericParameters.add(generic);
      }

      for (TypeScriptGeneric param : genericParameters) {
        param.walk(graph);
      }
    }
    this.walked = true;
  }

  @Override
  public String compile(String prefix) {
    StringBuilder compiled = new StringBuilder(prefix);
    compiled.append(prefix).append("export type ").append(name);
    if (!genericParameters.isEmpty()) {
      compiled.append('<');
      for (TypeScriptGeneric param : genericParameters) {
        compiled.append(param.compile("")).append(", ");
      }
      compiled = new StringBuilder(compiled.substring(0, compiled.length() - 2) + '>');
    }
    compiled.append(" = any;");
    return compiled.toString();
  }
}
