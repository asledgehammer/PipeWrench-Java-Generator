package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.util.ClazzUtils;
import com.asledgehammer.typescript.util.ComplexGenericMap;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;

public class TypeScriptField implements TypeScriptCompilable, TypeScriptWalkable {

  private final TypeScriptElement container;
  private final Field field;
  private boolean walked = false;
  private String adaptedReturn;

  public TypeScriptField(TypeScriptElement container, Field field) {
    this.container = container;
    this.field = field;
  }

  @Override
  public void walk(TypeScriptGraph graph) {

    ComplexGenericMap genericMap = this.container.genericMap;
    if (genericMap != null) {
      Class<?> declClazz = field.getDeclaringClass();
      String before = field.getGenericType().getTypeName();
      this.adaptedReturn = ClazzUtils.walkTypesRecursively(genericMap, declClazz, before);
    } else {
      this.adaptedReturn = field.getGenericType().getTypeName();
    }

    String preAdaptedReturn = field.getGenericType().getTypeName();
    this.adaptedReturn = TypeScriptElement.adaptType(this.adaptedReturn);
    if (preAdaptedReturn.equals(adaptedReturn)) {
      graph.add(field.getType());
    }

    this.adaptedReturn = TypeScriptElement.inspect(graph, this.adaptedReturn);

    if (!adaptedReturn.contains("<")) {
      TypeVariable[] params = field.getType().getTypeParameters();
      if (params.length != 0) {
        adaptedReturn += "<";
        for (int i = 0; i < params.length; i++) {
          adaptedReturn += "any, ";
        }
        adaptedReturn = adaptedReturn.substring(0, adaptedReturn.length() - 2) + ">";
      }
    }

    this.walked = true;
  }

  @Override
  public String compile(String prefix) {
    int modifiers = field.getModifiers();
    boolean isStatic = Modifier.isStatic(modifiers);
    boolean isFinal = Modifier.isFinal(modifiers);
    boolean isPrimitive = field.getType().isPrimitive();
    String compiled =
        prefix
            + (isStatic ? "static " : "")
            + (isFinal ? "readonly " : "")
            + field.getName()
            + (!isPrimitive ? "?" : "")
            + ": ";
    compiled += adaptedReturn;
    return compiled + ";";
  }

  public TypeScriptElement getContainer() {
    return container;
  }

  public Field getField() {
    return field;
  }

  public boolean hasWalked() {
    return walked;
  }
}
