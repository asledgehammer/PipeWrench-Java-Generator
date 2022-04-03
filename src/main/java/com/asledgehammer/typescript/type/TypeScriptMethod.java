package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

public class TypeScriptMethod implements TypeScriptCompilable, TypeScriptWalkable {

  private final TypeScriptElement container;
  private final Method method;
  private final List<TypeScriptMethodParameter> parameters = new ArrayList<>();
  private boolean walked = false;
  private String returnType;

  public TypeScriptMethod(TypeScriptElement container, Method method) {
    this.container = container;
    this.method = method;
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

    this.returnType = TypeScriptElement.adaptType(method.getGenericReturnType().getTypeName());
    this.returnType = TypeScriptElement.inspect(graph, this.returnType);

    Class<?> returnClazz = method.getReturnType();

    if (!returnType.contains("<")) {
      TypeVariable[] params = returnClazz.getTypeParameters();
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
    //    return compileLuaAPIVersion();
    //    return compileTypeScriptFunction(prefix);
    return compileTypeScriptDefinition(prefix);
  }

  private String compileTypeScriptDefinition(String prefix) {
    String compiled = prefix + method.getName() + "(";

    if (!parameters.isEmpty()) {
      for (TypeScriptMethodParameter parameter : parameters) {
        compiled += parameter.compile("") + ", ";
      }
      compiled = compiled.substring(0, compiled.length() - 2);
    }
    compiled += "): " + returnType + ';';
    return compiled;
  }

  private String compileTypeScriptFunction(String prefix) {
    String compiled = "export function " + method.getName() + "(";

    if (!parameters.isEmpty()) {
      for (TypeScriptMethodParameter parameter : parameters) {
        compiled += parameter.compile("") + ", ";
      }
      compiled = compiled.substring(0, compiled.length() - 2);
    }
    compiled += "): " + returnType;
    return compiled;
  }

  private String compileLuaAPIVersion(String prefix) {
    String compiled = "function API.";
    String methodBody = method.getName() + "(";
    if (!parameters.isEmpty()) {
      for (TypeScriptMethodParameter parameter : parameters) {
        methodBody += parameter.name + ", ";
      }
      methodBody = methodBody.substring(0, methodBody.length() - 2);
    }
    methodBody += ")";

    compiled += methodBody;

    if (!returnType.equalsIgnoreCase("void")) {
      compiled += " return " + methodBody;
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

  private static class TypeScriptMethodParameter
      implements TypeScriptWalkable, TypeScriptCompilable {

    private final TypeScriptMethod method;
    private final Type type;
    private final String name;
    private final Parameter parameter;
    private boolean walked = false;
    private String returnType;

    TypeScriptMethodParameter(TypeScriptMethod method, Parameter parameter, Type type) {
      this.method = method;
      this.parameter = parameter;
      this.type = type;
      this.name = parameter.getName();
    }

    @Override
    public void walk(TypeScriptGraph graph) {
      this.returnType = TypeScriptElement.inspect(graph, type.getTypeName());
      this.returnType = TypeScriptElement.adaptType(this.returnType);
      graph.add(method.method.getReturnType());

      if (!returnType.contains("<")) {
        TypeVariable[] params = parameter.getType().getTypeParameters();
        if (params.length != 0) {
          returnType += "<";
          for (int i = 0; i < params.length; i++) {
            returnType += "any, ";
          }
          returnType = returnType.substring(0, returnType.length() - 2) + ">";
        }
      }

      walked = true;
    }

    @Override
    public String compile(String prefix) {
      return name + ": " + returnType;
    }

    public boolean hasWalked() {
      return walked;
    }
  }
}
