package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.util.ClazzUtils;
import com.asledgehammer.typescript.util.ComplexGenericMap;
import com.asledgehammer.typescript.util.DocBuilder;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class TypeScriptMethod implements TypeScriptCompilable, TypeScriptWalkable {

  private final TypeScriptElement container;
  private final Method method;
  private final List<TypeScriptMethodParameter> parameters = new ArrayList<>();
  private final boolean bStatic;
  private boolean walked = false;
  private String returnType;

  public boolean isStatic() {
    return bStatic;
  }

  public TypeScriptMethod(TypeScriptElement container, Method method) {
    this.container = container;
    this.method = method;
    this.bStatic = Modifier.isStatic(this.method.getModifiers());
  }

  public static List<String> extractGenericDefinitions(String raw) {
    List<String> list = new ArrayList<>();
    int indexOf = raw.indexOf('<');
    if (indexOf == -1) {
      return list;
    }
    String sub = raw.substring(indexOf + 1, raw.length() - 1);
    for (String entry : sub.split(",")) {
      list.add(entry.trim());
    }
    return list;
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    Parameter[] parameters = method.getParameters();
    Type[] genericParameterTypes = method.getGenericParameterTypes();
    for (int i = 0; i < genericParameterTypes.length; i++) {
      Type parameter = genericParameterTypes[i];
      TypeScriptMethodParameter typeScriptMethodParameter =
          new TypeScriptMethodParameter(this, parameters[i], parameter);
      this.parameters.add(typeScriptMethodParameter);
    }

    for (Parameter parameter : parameters) {
      graph.add(parameter.getType());
    }

    for (TypeScriptMethodParameter parameter : this.parameters) {
      parameter.walk(graph);
    }

    ComplexGenericMap genericMap = this.container.genericMap;
    if (genericMap != null) {
      Class<?> declClazz = method.getDeclaringClass();
      String before = method.getGenericReturnType().getTypeName();
      this.returnType = ClazzUtils.walkTypesRecursively(genericMap, declClazz, before);
    } else {
      this.returnType = method.getGenericReturnType().getTypeName();
    }

    this.returnType = TypeScriptElement.adaptType(this.returnType);
    this.returnType = TypeScriptElement.inspect(graph, this.returnType);

    Class<?> returnClazz = method.getReturnType();

    if (!returnType.contains("<")) {
      TypeVariable<?>[] params = returnClazz.getTypeParameters();
      if (params.length != 0) {
        returnType += "<";
        for (int i = 0; i < params.length; i++) {
          returnType += "any, ";
        }
        returnType = returnType.substring(0, returnType.length() - 2) + ">";
      }
    }

    graph.add(returnClazz);
    walked = true;
  }

  @Override
  public String compile(String prefix) {
    return compileTypeScriptDeclaration(prefix);
  }

  private String compileTypeScriptDeclaration(String prefix) {
    StringBuilder builder = new StringBuilder();

    Parameter[] parameters = method.getParameters();
    Type[] genericTypes = method.getTypeParameters();

    ComplexGenericMap genericMap = container.genericMap;

    DocBuilder doc = new DocBuilder();
    if (bStatic) doc.appendLine("@noSelf");
    if (parameters.length != 0) {
      if (!doc.isEmpty()) {
        doc.appendLine();
      }
      StringBuilder compiled = new StringBuilder("(");
      for (Parameter parameter : parameters) {
        String tName =
            (parameter.isVarArgs()
                    ? parameter.getType().getComponentType().getSimpleName() + "..."
                    : parameter.getType().getSimpleName())
                + " "
                + parameter.getName();
        if (genericMap != null) {
          tName = ClazzUtils.walkTypesRecursively(genericMap, container.clazz, tName);
        }
        tName = TypeScriptElement.adaptType(tName);
        tName = TypeScriptElement.inspect(container.namespace.getGraph(), tName);
        compiled.append(tName).append(", ");
      }
      compiled =
          new StringBuilder(
              compiled.substring(0, compiled.length() - 2)
                  + "): "
                  + method.getReturnType().getSimpleName());

      doc.appendLine(compiled.toString());
    }

    if (!doc.isEmpty()) {
      builder.append(doc.build(prefix)).append('\n');
    }

    StringBuilder compiled =
        new StringBuilder(prefix + (bStatic ? "static " : "") + method.getName());

    if (genericTypes.length != 0) {
      compiled.append("<");
      for (Type genericType : genericTypes) {
        compiled.append(genericType.getTypeName()).append(", ");
      }
      compiled = new StringBuilder(compiled.substring(0, compiled.length() - 2));
      compiled.append(">");
    }

    compiled.append("(");

    if (!this.parameters.isEmpty()) {
      for (TypeScriptMethodParameter parameter : this.parameters) {
        compiled.append(parameter.compile("")).append(", ");
      }
      compiled = new StringBuilder(compiled.substring(0, compiled.length() - 2));
    }
    compiled.append("): ").append(returnType).append(';');

    builder.append(compiled);
    return builder.toString();
  }

  public String compileTypeScriptFunction(String prefix) {
    StringBuilder builder = new StringBuilder();

    DocBuilder doc = new DocBuilder();
    if (bStatic) doc.appendLine("@noSelf");
    if (!parameters.isEmpty()) {
      if (!doc.isEmpty()) doc.appendLine();
      StringBuilder compiled = new StringBuilder("(");

      ComplexGenericMap genericMap = container.genericMap;
      Parameter[] parameters = method.getParameters();
      for (Parameter parameter : parameters) {
        String tName =
            (parameter.isVarArgs()
                    ? parameter.getType().getComponentType().getSimpleName() + "..."
                    : parameter.getType().getSimpleName())
                + " "
                + parameter.getName();
        if (genericMap != null) {
          tName = ClazzUtils.walkTypesRecursively(genericMap, container.clazz, tName);
        }
        tName = TypeScriptElement.adaptType(tName);
        tName = TypeScriptElement.inspect(container.namespace.getGraph(), tName);
        compiled.append(tName).append(", ");
      }
      compiled =
          new StringBuilder(
              compiled.substring(0, compiled.length() - 2)
                  + "): "
                  + method.getReturnType().getSimpleName());

      doc.appendLine(compiled.toString());
    }

    if (!doc.isEmpty()) {
      builder.append(doc.build(prefix)).append('\n');
    }
    StringBuilder compiled = new StringBuilder("export function " + method.getName() + "(");

    if (!parameters.isEmpty()) {
      compiled.append("this: void, ");
      for (TypeScriptMethodParameter parameter : parameters) {
        compiled.append(parameter.compile("")).append(", ");
      }
      compiled = new StringBuilder(compiled.substring(0, compiled.length() - 2));
    }
    compiled.append("): ").append(returnType);
    return builder.append(compiled).toString();
  }

  public String compileLua(String table) {
    String compiled = "function " + table + '.';
    StringBuilder methodBody = new StringBuilder(method.getName() + "(");
    if (!parameters.isEmpty()) {
      for (TypeScriptMethodParameter parameter : parameters) {
        methodBody.append(parameter.name).append(", ");
      }
      methodBody = new StringBuilder(methodBody.substring(0, methodBody.length() - 2));
    }
    methodBody.append(")");

    compiled += methodBody;

    if (!returnType.equalsIgnoreCase("void")) {
      compiled += " return " + methodBody;
    } else {
      compiled += ' ' + methodBody.toString();
    }

    compiled += " end";
    return compiled;
  }

  public TypeScriptElement getContainer() {
    return container;
  }

  public Method getMethod() {
    return method;
  }

  public boolean hasWalked() {
    return walked;
  }
}
