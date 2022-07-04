package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.util.ClazzUtils;
import com.asledgehammer.typescript.util.ComplexGenericMap;
import com.asledgehammer.typescript.util.DocBuilder;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TypeScriptMethodCluster implements TypeScriptWalkable, TypeScriptCompilable {

  private final List<List<String>> allParameterTypes = new ArrayList<>();
  private final List<String> allReturnTypes = new ArrayList<>();

  private final List<List<Boolean>> canPassNull = new ArrayList<>();
  private final List<List<Boolean>> isVararg = new ArrayList<>();
  private final TypeScriptElement element;
  public final boolean isStatic;
  public boolean exists = false;
  private int minParamCount = Integer.MAX_VALUE;

  private final String methodName;

  List<Method> sortedMethods = new ArrayList<>();
  List<List<Parameter>> allParameters = new ArrayList<>();
  private boolean returnTypeContainsNonPrimitive = false;

  public TypeScriptMethodCluster(TypeScriptElement element, Method method) {
    this.element = element;
    this.methodName = method.getName();
    this.isStatic = Modifier.isStatic(method.getModifiers());
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    Class<?> clazz = element.clazz;
    if (clazz == null) return;

    ComplexGenericMap genericMap = this.element.genericMap;

    Method[] ms = clazz.getMethods();
    Collections.addAll(sortedMethods, ms);
    sortedMethods.sort(Comparator.comparingInt(Method::getParameterCount));

    this.exists = sortedMethods.size() != 0;

    this.minParamCount = exists ? Integer.MAX_VALUE : 0;

    for (Method method : sortedMethods) {

      if (!methodName.equals(method.getName())) continue;
      if (Modifier.isStatic(method.getModifiers()) != isStatic) continue;

      Parameter[] parameters = method.getParameters();
      Type[] types = method.getGenericParameterTypes();
      if (minParamCount > types.length) {
        minParamCount = types.length;
      }

      if (types.length != 0) {
        for (int i = 0; i < types.length; i++) {
          Type argType = types[i];

          List<String> argSlot;
          if (allParameterTypes.size() > i) {
            argSlot = allParameterTypes.get(i);
          } else {
            argSlot = new ArrayList<>();
            allParameterTypes.add(argSlot);
          }

          String tName = argType.getTypeName();
          if (genericMap != null) {
            tName = ClazzUtils.walkTypesRecursively(genericMap, method.getDeclaringClass(), tName);
          }

          tName = TypeScriptElement.inspect(graph, tName);
          tName = TypeScriptElement.adaptType(tName);

          graph.add(parameters[i].getType());

          // Add any missing parameters if not defined.
          if (!tName.contains("<")) {
            TypeVariable[] params = parameters[i].getType().getTypeParameters();
            if (params.length != 0) {
              tName += "<";
              for (int k = 0; k < params.length; k++) {
                tName += "any, ";
              }
              tName = tName.substring(0, tName.length() - 2) + ">";
            }
          }

          if (!argSlot.contains(tName)) {
            argSlot.add(tName);
          }
        }
      }

      if (parameters.length != 0) {
        for (int i = 0; i < parameters.length; i++) {
          Parameter argParameter = parameters[i];

          List<Parameter> argSlot;
          if (allParameters.size() > i) {
            argSlot = allParameters.get(i);
          } else {
            argSlot = new ArrayList<>();
            allParameters.add(argSlot);
          }

          List<Boolean> nullSlot;
          if (canPassNull.size() > i) {
            nullSlot = canPassNull.get(i);
          } else {
            nullSlot = new ArrayList<>();
            canPassNull.add(nullSlot);
          }

          List<Boolean> varg;
          if (isVararg.size() > i) {
            varg = isVararg.get(i);
          } else {
            varg = new ArrayList<>();
            isVararg.add(varg);
          }

          argSlot.add(argParameter);
          nullSlot.add(argParameter.getType().isPrimitive());
          varg.add(argParameter.isVarArgs());
        }
      }

      for (Parameter parameter : parameters) {
        graph.add(parameter.getType());
      }

      String returnType;
      if (genericMap != null) {
        Class<?> declClazz = method.getDeclaringClass();
        String before = method.getGenericReturnType().getTypeName();
        returnType = ClazzUtils.walkTypesRecursively(genericMap, declClazz, before);
      } else {
        returnType = method.getGenericReturnType().getTypeName();
      }

      if (!method.getGenericReturnType().getClass().isPrimitive()) {
        this.returnTypeContainsNonPrimitive = true;
      }

      returnType = TypeScriptElement.adaptType(returnType);
      returnType = TypeScriptElement.inspect(graph, returnType);

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

      if (!this.allReturnTypes.contains(returnType)) {
        this.allReturnTypes.add(returnType);
      }

      graph.add(returnClazz);
    }
  }

  private String walkDocs(String prefix) {
    Class<?> clazz = element.clazz;
    if (clazz == null || !exists) return "";
    DocBuilder docBuilder = new DocBuilder();
    if (isStatic) {
      docBuilder.appendLine("@noSelf");
      docBuilder.appendLine();
    }
    docBuilder.appendLine("Method Parameters: ");

    for (Method method : sortedMethods) {

      if (!methodName.equals(method.getName())) continue;
      if (Modifier.isStatic(method.getModifiers()) != isStatic) continue;

      Parameter[] parameters = method.getParameters();

      if (parameters.length != 0) {
        String compiled = "(";
        for (Parameter parameter : method.getParameters()) {
          String tName =
              (parameter.isVarArgs()
                      ? parameter.getType().getComponentType().getSimpleName() + "..."
                      : parameter.getType().getSimpleName())
                  + " "
                  + parameter.getName();
          if (element.genericMap != null) {
            tName = ClazzUtils.walkTypesRecursively(element.genericMap, element.clazz, tName);
          }
          tName = TypeScriptElement.adaptType(tName);
          tName = TypeScriptElement.inspect(element.namespace.getGraph(), tName);
          compiled += tName + ", ";
        }
        compiled = compiled.substring(0, compiled.length() - 2) + ')';
        String returnType = ClazzUtils.walkTypesRecursively(element.genericMap, element.clazz, method.getGenericReturnType().getTypeName());
        returnType = TypeScriptElement.adaptType(returnType);
        returnType = TypeScriptElement.inspect(element.namespace.getGraph(), returnType);
        docBuilder.appendLine(" - " + compiled + ": " + returnType);
      } else {
        String compiled = "(Empty)";
        String returnType = ClazzUtils.walkTypesRecursively(element.genericMap, element.clazz, method.getGenericReturnType().getTypeName());
        returnType = TypeScriptElement.adaptType(returnType);
        returnType = TypeScriptElement.inspect(element.namespace.getGraph(), returnType);
        docBuilder.appendLine(" - " + compiled + ": " + returnType);
      }
    }
    return docBuilder.build(prefix);
  }

  @Override
  public String compile(String prefix) {

    StringBuilder builder = new StringBuilder();
    builder.append(walkDocs(prefix)).append('\n');
    builder.append(prefix);

    if (isStatic) builder.append("static ");
    builder.append(methodName);

    String genericParamsBody = "";

    List<String> genericTypeNames = new ArrayList<>();

    List<TypeVariable<?>> genericTypes = new ArrayList<>();
    for (Method m : sortedMethods) {
      TypeVariable<?>[] tvs = m.getTypeParameters();
      if (tvs.length != 0) {
        for (TypeVariable<?> tv : tvs) {
          if (genericTypeNames.contains(tv.getTypeName())) continue;
          genericTypeNames.add(tv.getTypeName());
          genericTypes.add(tv);
        }
      }
    }

    if (genericTypes.size() != 0) {
      genericParamsBody = "<";
      for (TypeVariable<?> variable : genericTypes) {
        genericParamsBody += variable.getTypeName() + ", ";
      }
      genericParamsBody = genericParamsBody.substring(0, genericParamsBody.length() - 2) + '>';
      builder.append(genericParamsBody);
    }

    builder.append('(');
    String s = "";
    for (int i = 0; i < allParameterTypes.size(); i++) {

      String sEntry = "arg" + i;
      if (i > minParamCount - 1) sEntry += '?';
      sEntry += ": ";

      String sParams = "";
      List<Parameter> params = allParameters.get(i);
      List<String> argSlot = allParameterTypes.get(i);
      for (int j = 0; j < argSlot.size(); j++) {
        String argSlotEntry = argSlot.get(j);
        sParams +=
            ClazzUtils.walkTypesRecursively(
                    element.genericMap,
                    params.get(j).getDeclaringExecutable().getDeclaringClass(),
                    argSlotEntry)
                + " | ";
      }

      s += sEntry + sParams.substring(0, sParams.length() - 3);

      boolean isPrimitive = false;
      List<Boolean> paramPrimitiveList = canPassNull.get(i);
      for (Boolean aBoolean : paramPrimitiveList) {
        if (aBoolean) {
          isPrimitive = true;
          break;
        }
      }
      if (!isPrimitive) s += " | null";

      s += ", ";
    }

    if (s.length() != 0) s = s.substring(0, s.length() - 2);

    builder.append(s).append("): ");

    String returned = "";
    for (String returnType : allReturnTypes) {
      if (!returned.isEmpty()) {
        returned += " | ";
      }
      returned += returnType;
    }

    if (returnTypeContainsNonPrimitive) {
      if (!returned.isEmpty()) {
        returned += " | ";
      }
      returned += "null";
    }

    builder.append(returned).append(';');

    return builder.toString();
  }

  public String compileLua(String table) {
    //    String compiled = "function " + table + '.';
    //    String methodBody = methodName + "(";
    //    if (!parameters.isEmpty()) {
    //      for (TypeScriptMethod.TypeScriptMethodParameter parameter : parameters) {
    //        methodBody += parameter.name + ", ";
    //      }
    //      methodBody = methodBody.substring(0, methodBody.length() - 2);
    //    }
    //    methodBody += ")";
    //
    //    compiled += methodBody;
    //
    //    if (!returnType.equalsIgnoreCase("void")) {
    //      compiled += " return " + methodBody;
    //    } else {
    //      compiled += ' ' + methodBody;
    //    }
    //
    //    compiled += " end";
    //    return compiled;
    return "";
  }

  public String compileTypeScriptFunction(String prefix) {
    //    StringBuilder builder = new StringBuilder();
    //
    //    DocBuilder doc = new DocBuilder();
    //    if (bStatic) doc.appendLine("@noSelf");
    //    if (!parameters.isEmpty()) {
    //      if (!doc.isEmpty()) doc.appendLine();
    //      String compiled = "(";
    //
    //      ComplexGenericMap genericMap = container.genericMap;
    //      Parameter[] parameters = method.getParameters();
    //      for (Parameter parameter : parameters) {
    //        String tName =
    //                (parameter.isVarArgs()
    //                        ? parameter.getType().getComponentType().getSimpleName() + "..."
    //                        : parameter.getType().getSimpleName())
    //                        + " "
    //                        + parameter.getName();
    //        if (genericMap != null) {
    //          tName = ClazzUtils.walkTypesRecursively(genericMap, container.clazz, tName);
    //        }
    //        tName = TypeScriptElement.adaptType(tName);
    //        tName = TypeScriptElement.inspect(container.namespace.getGraph(), tName);
    //        compiled += tName + ", ";
    //      }
    //      compiled =
    //              compiled.substring(0, compiled.length() - 2)
    //                      + "): "
    //                      + method.getReturnType().getSimpleName();
    //
    //      doc.appendLine(compiled);
    //    }
    //
    //    if (!doc.isEmpty()) {
    //      builder.append(doc.build(prefix)).append('\n');
    //    }
    //    String compiled = "export function " + method.getName() + "(";
    //
    //    if (!parameters.isEmpty()) {
    //      compiled += "this: void, ";
    //      for (TypeScriptMethod.TypeScriptMethodParameter parameter : parameters) {
    //        compiled += parameter.compile("") + ", ";
    //      }
    //      compiled = compiled.substring(0, compiled.length() - 2);
    //    }
    //    compiled += "): " + returnType;
    //    return builder.append(compiled).toString();
    return "";
  }
}
