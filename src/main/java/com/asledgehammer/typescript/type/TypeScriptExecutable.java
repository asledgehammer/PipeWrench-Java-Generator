package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.util.ClazzUtils;
import com.asledgehammer.typescript.util.DocBuilder;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

public class TypeScriptExecutable implements TypeScriptWalkable, TypeScriptCompilable {

  private final Executable[] executables;

  private final List<List<String>> allParameterTypes = new ArrayList<>();
  private final List<List<Parameter>> allParameters = new ArrayList<>();
  private final TypeScriptElement element;
  private final String executableName;
  private boolean exists = false;
  private int minParamCount = Integer.MAX_VALUE;

  public TypeScriptExecutable(TypeScriptElement element, String executableName) {
    this.element = element;
    this.executableName = executableName;

    Class<?> clazz = element.clazz;
    if (clazz == null) {
      this.executables = new Executable[0];
      return;
    }

    List<Executable> executables = new ArrayList<>();

    Method[] methods = element.clazz.getMethods();
    for (Method method : methods) {
      if (method.getName().equals(executableName)) {
        executables.add(method);
      }
    }

    this.executables = new Executable[executables.size()];
    for (int i = 0; i < executables.size(); i++) {
      this.executables[i] = executables.get(i);
    }

    this.exists = this.executables.length != 0;
    this.minParamCount = exists ? Integer.MAX_VALUE : 0;
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    Class<?> clazz = element.clazz;
    if (clazz == null) return;

    for (Executable constructor : executables) {

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

  @Override
  public String compile(String prefix) {
    Class<?> clazz = element.clazz;
    if (clazz == null || !exists) return "";

    StringBuilder builder = new StringBuilder();
    builder.append(walkDocs(prefix)).append('\n');
    builder.append(prefix).append(executableName).append("(");

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
    s = s.substring(0, s.length() - 2);

    builder.append(s).append("): ").append(clazz.getName()).append(";");
    return builder.toString();
  }

  private String walkDocs(String prefix) {
    Class<?> clazz = element.clazz;
    if (clazz == null || !exists) return "";
    DocBuilder docBuilder = new DocBuilder();
    docBuilder.appendLine("@noSelf");
    docBuilder.appendLine();
    docBuilder.appendLine("Implementations: ");
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
        docBuilder.appendLine(" - (Empty)");
      }
    }
    return docBuilder.build(prefix);
  }
}
