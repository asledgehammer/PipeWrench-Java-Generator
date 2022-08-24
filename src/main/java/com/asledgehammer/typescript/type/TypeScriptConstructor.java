package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.util.ClazzUtils;
import com.asledgehammer.typescript.util.DocBuilder;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TypeScriptConstructor implements TypeScriptWalkable, TypeScriptCompilable {

  private final List<List<String>> allParameterTypes = new ArrayList<>();
  private final List<List<Parameter>> allParameters = new ArrayList<>();
  private final List<List<Boolean>> canPassNull = new ArrayList<>();
  private final List<List<Boolean>> isVararg = new ArrayList<>();
  private final TypeScriptElement element;
  public boolean exists = false;
  private int minParamCount = Integer.MAX_VALUE;

  List<Constructor<?>> sortedConstructors = new ArrayList<>();

  public TypeScriptConstructor(TypeScriptElement element) {
    this.element = element;
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    Class<?> clazz = element.clazz;
    if (clazz == null) return;

    Constructor<?>[] constructors = clazz.getConstructors();
    this.exists = constructors.length != 0;

    for (Constructor<?> constructor : constructors) {
      int modifiers = constructor.getModifiers();
      if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
        sortedConstructors.add(constructor);
      }
    }

    this.minParamCount = exists ? Integer.MAX_VALUE : 0;

    if(exists) {
      sortedConstructors.sort((o1, o2) -> {

        // Try the original method first. If this is different, then we use this order.
        if(o1.getParameterCount() != o2.getParameterCount()) {
          return o1.getParameterCount() - o2.getParameterCount();
        }

        // Empty method params.
        if(o1.getParameterCount() == 0) return 0;

        // If otherwise, we go until the string comparison of type names is not zero.
        Class<?>[] o1Types = o1.getParameterTypes();
        Class<?>[] o2Types = o2.getParameterTypes();
        for(int index = 0; index < o1Types.length; index++) {
          Class<?> o1Type = o1Types[index];
          Class<?> o2Type = o2Types[index];
          int compare = o1Type.getName().compareTo(o2Type.getName());
          if(compare != 0) return compare;
        }

        return 0;
      });
    }

    for (Constructor<?> constructor : sortedConstructors) {

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
    docBuilder.appendLine("Constructors: ");

    // Sort here so that the documentation looks nice, however the method params are consistent.
    ArrayList<Constructor<?>> sortedConstructors = new ArrayList<>(this.sortedConstructors);
    sortedConstructors.sort(Comparator.comparingInt(Constructor::getParameterCount));

    for (Constructor<?> constructor : sortedConstructors) {
      Parameter[] parameters = constructor.getParameters();
      if (parameters.length != 0) {
        StringBuilder compiled = new StringBuilder("(");
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
          compiled.append(tName).append(", ");
        }
        compiled = new StringBuilder(compiled.substring(0, compiled.length() - 2) + ')');
        docBuilder.appendLine(" - " + compiled);
      } else {
        docBuilder.appendLine(" - (Empty Constructor)");
      }
    }
    return docBuilder.build(prefix);
  }

  public String compileCustomConstructor(String prefix) {
    Class<?> clazz = element.clazz;
    if (clazz == null || !exists) return "";

    TypeScriptSettings settings = element.getNamespace().getGraph().getCompiler().getSettings();

    StringBuilder builder = new StringBuilder();
    builder.append(walkDocs(prefix)).append('\n');
    builder.append(prefix);
    if (Modifier.isAbstract(clazz.getModifiers())) {
      builder.append("protected ");
    }
    builder.append("constructor");

    builder.append('(');
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < allParameterTypes.size(); i++) {

      String sEntry = "arg" + i;
      if (i > minParamCount - 1) sEntry += '?';
      sEntry += ": ";

      StringBuilder sParams = new StringBuilder();
      List<String> argSlot = allParameterTypes.get(i);
      for (String argSlotEntry : argSlot) {
        sParams.append(argSlotEntry).append(" | ");
      }

      s.append(sEntry).append(sParams.substring(0, sParams.length() - 3));

      boolean isPrimitive = false;
      List<Boolean> paramPrimitiveList = canPassNull.get(i);
      for (Boolean aBoolean : paramPrimitiveList) {
        if (aBoolean) {
          isPrimitive = true;
          break;
        }
      }
      if (settings.useNull && !isPrimitive) s.append(" | null");
      s.append(", ");
    }

    if (s.length() != 0) s = new StringBuilder(s.substring(0, s.length() - 2));

    builder.append(s).append(");");
    return builder.toString();
  }

  @Override
  public String compile(String prefix) {
    return compileCustomConstructor(prefix);
  }
}
