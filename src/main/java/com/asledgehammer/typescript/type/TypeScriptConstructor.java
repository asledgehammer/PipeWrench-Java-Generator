package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.util.ClazzUtils;
import com.asledgehammer.typescript.util.DocBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

public class TypeScriptConstructor implements TypeScriptWalkable, TypeScriptCompilable {

  private final List<List<String>> allParameterTypes = new ArrayList<>();
  private final List<List<Parameter>> allParameters = new ArrayList<>();
  private final List<List<Boolean>> canPassNull = new ArrayList<>();
  private final List<List<Boolean>> isVararg = new ArrayList<>();
  private final TypeScriptElement element;
  public boolean exists = false;
  private int minParamCount = Integer.MAX_VALUE;

  public TypeScriptConstructor(TypeScriptElement element) {
    this.element = element;
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    Class<?> clazz = element.clazz;
    if (clazz == null) return;

    Constructor<?>[] constructors = clazz.getConstructors();
    this.exists = constructors.length != 0;

    this.minParamCount = exists ? Integer.MAX_VALUE : 0;

    for (Constructor<?> constructor : constructors) {

      Type[] types = constructor.getGenericParameterTypes();
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
          if (element.genericMap != null) {
            tName = ClazzUtils.walkTypesRecursively(element.genericMap, element.clazz, tName);
          }

          tName = TypeScriptElement.adaptType(tName);
          tName = TypeScriptElement.inspect(graph, tName);

          if (!argSlot.contains(tName)) {
            argSlot.add(tName);
          }
        }
      }

      Parameter[] parameters = constructor.getParameters();
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
    }
  }

  private String walkDocs(String prefix) {
    Class<?> clazz = element.clazz;
    if (clazz == null || !exists) return "";
    DocBuilder docBuilder = new DocBuilder();
    docBuilder.appendLine("@noSelf");
    docBuilder.appendLine();
    docBuilder.appendLine("Constructors: ");
    for (Constructor<?> constructor : clazz.getConstructors()) {
      Parameter[] parameters = constructor.getParameters();
      if (parameters.length != 0) {
        String compiled = "(";
        for (Parameter parameter : constructor.getParameters()) {
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
        docBuilder.appendLine(" - " + compiled);
      } else {
        docBuilder.appendLine(" - (Empty Constructor)");
      }
    }
    return docBuilder.build(prefix);
  }

  @Override
  public String compile(String prefix) {
    Class<?> clazz = element.clazz;
    if (clazz == null || !exists) return "";

    StringBuilder builder = new StringBuilder();
    builder.append(walkDocs(prefix)).append('\n');
    builder.append(prefix).append("static new");

    String genericParamsBody = "";
    TypeVariable<?>[] genericTypes = element.clazz.getTypeParameters();
    if (genericTypes.length != 0) {
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
      List<String> argSlot = allParameterTypes.get(i);
      for (String argSlotEntry : argSlot) {
        sParams += argSlotEntry + " | ";
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

    builder.append(s).append("): ").append(clazz.getName()).append(genericParamsBody).append(";");
    return builder.toString();
  }
}
