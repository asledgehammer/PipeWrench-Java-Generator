package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.util.ClazzUtils;
import com.asledgehammer.typescript.util.DocBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class TypeScriptConstructor implements TypeScriptWalkable, TypeScriptCompilable {

  private final List<List<String>> allParameterTypes = new ArrayList<>();
  private final List<List<Parameter>> allParameters = new ArrayList<>();
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

          argSlot.add(argParameter);
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
          String tName = parameter.getType().getSimpleName() + " " + parameter.getName();
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
    builder.append(prefix).append("static new(");

    String s = "";
    for (int i = 0; i < allParameterTypes.size(); i++) {
      List<String> argSlot = allParameterTypes.get(i);

      String sEntry = "arg" + i;
      if (i > minParamCount - 1) sEntry += '?';
      sEntry += ": ";

      for (String argSlotEntry : argSlot) {
        sEntry += argSlotEntry + " | ";
      }
      s += sEntry.substring(0, sEntry.length() - 3) + ", ";
    }

    if (s.length() != 0) s = s.substring(0, s.length() - 2);

    builder.append(s).append("): ").append(clazz.getName()).append(";");
    return builder.toString();
  }
}
