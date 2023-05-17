package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.util.ClazzUtils;
import com.asledgehammer.typescript.util.ComplexGenericMap;
import com.asledgehammer.typescript.util.DocBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;

@SuppressWarnings("unused")
public class TypeScriptField implements TypeScriptCompilable, TypeScriptWalkable {

  private final TypeScriptElement container;
  private final Field field;
  private final boolean bStatic;
  private final boolean bFinal;
  private final boolean bPrimitive;
  private final boolean bPublic;

  private boolean walked = false;
  private String adaptedReturn;

  public boolean isStatic() {
    return bStatic;
  }

  public boolean isFinal() {
    return bFinal;
  }

  public boolean isPrimitive() {
    return bPrimitive;
  }

  public boolean isbPublic() {
    return bPublic;
  }

  public TypeScriptField(TypeScriptElement container, Field field) {
    this.container = container;
    this.field = field;
    int modifiers = field.getModifiers();
    this.bStatic = Modifier.isStatic(modifiers);
    this.bFinal = Modifier.isFinal(modifiers);
    this.bPrimitive = field.getType().isPrimitive();
    this.bPublic = Modifier.isPublic(modifiers);
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

    this.adaptedReturn = TypeScriptElement.adaptType(this.adaptedReturn);
    this.adaptedReturn = TypeScriptElement.inspect(graph, this.adaptedReturn);

    try {
      ClassLoader cls = ClassLoader.getSystemClassLoader();
      Class<?> cl = Class.forName(this.adaptedReturn, false, cls);
      graph.add(cl);
    } catch (Exception ignored) {
    }

    if (!adaptedReturn.contains("<")) {
      TypeVariable<?>[] params = field.getType().getTypeParameters();
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

    DocBuilder doc = new DocBuilder();
    doc.appendLine(field.getGenericType().getTypeName());

    String compiled =
        doc.build(prefix)
            + '\n'
            + prefix
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

  public String getType() {
    String s = field.getType().getTypeName();
    if (s.indexOf('.') != -1) {
      String[] ss = s.split("\\.");
      s = ss[ss.length - 1];
    }

//    if(s.equals("String")) {
//      s = s.toLowerCase();
//    }
//
//    if(s.equals("int") || s.equals("double") || s.equals("float") || s.equals("long") || s.equals("short") || s.equals("byte")) {
//      s = "number";
//    }

    return s;
  }
}
