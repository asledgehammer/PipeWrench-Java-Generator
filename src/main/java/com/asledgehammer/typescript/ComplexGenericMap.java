package com.asledgehammer.typescript;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComplexGenericMap {

  private final Map<Class<?>, ComplexGenericMap> superMap = new HashMap<>();
  private final List<String> paramDeclarations;
  private final ComplexGenericMap sub;

  public ComplexGenericMap(Class<?> clazz) {
    this(null, clazz);
  }

  private ComplexGenericMap(ComplexGenericMap sub, Class<?> clazz) {
    this.sub = sub;
    this.paramDeclarations = extractTypeDeclarations(clazz);
    Class<?> superClazz = clazz.getSuperclass();
    if (superClazz != null) superMap.put(superClazz, new ComplexGenericMap(this, superClazz));
    Class<?>[] interfaces = clazz.getInterfaces();
    for (Class<?> interfaze : interfaces) superMap.put(interfaze, new ComplexGenericMap(interfaze));
  }

  public ComplexGenericMap getSuper(Class<?> superClazz) {
    for (Class<?> key : superMap.keySet()) {
      if (key.equals(superClazz)) return superMap.get(key);
      ComplexGenericMap found = superMap.get(key).getSuper(superClazz);
      if (found != null) return found;
    }
    return null;
  }

  public String resolveDeclaredType(Class<?> declClazz, Type paramType) {
    String paramTypeName = paramType.getTypeName();
    ComplexGenericMap declMap = getSuper(declClazz);
    if (declMap == null) return paramTypeName;
    TypeVariable<?>[] clazzParams = declClazz.getTypeParameters();
    int index = -1;
    for (int i = 0; i < clazzParams.length; i++) {
      TypeVariable<?> v = clazzParams[i];
      if (v.getTypeName().equals(paramTypeName)) {
        index = i;
        break;
      }
    }
    if (index == -1) return paramTypeName;
    ParameterChain chainRoot = new ParameterChain(declMap, index);
    return chainRoot.resolve();
  }

  public String resolveDeclaredParameter(Parameter parameter) {
    return resolveDeclaredType(
        parameter.getDeclaringExecutable().getDeclaringClass(), parameter.getParameterizedType());
  }

  public static List<String> extractTypeDeclarations(Class<?> clazz) {
    List<String> list = new ArrayList<>();
    Type superClazz = clazz.getGenericSuperclass();
    if (superClazz == null) return list;
    String raw = superClazz.getTypeName();
    int indexOf = raw.indexOf("<");
    if (indexOf == -1) return list;
    int indexCurrent = indexOf + 1;
    int inside = 1;
    StringBuilder builder = new StringBuilder();
    while (inside != 0) {
      char next = raw.charAt(indexCurrent++);
      switch (next) {
        case '<' -> {
          inside++;
          builder.append(next);
        }
        case '>' -> {
          inside--;
          if (inside == 0) {
            if (builder.length() != 0) list.add(builder.toString().trim());
          } else if (inside == 1) {
            list.add(builder.toString().trim());
            builder = new StringBuilder();
          } else builder.append(next);
        }
        case ',' -> {
          if (inside == 1) {
            list.add(builder.toString().trim());
            builder = new StringBuilder();
          } else builder.append(next);
        }
        default -> builder.append(next);
      }
    }
    return list;
  }

  public static class ParameterChain {

    private Class<?> typeClazz;
    private ParameterChain subChainLink;

    ParameterChain(ComplexGenericMap container, int index) {
      try {
        this.typeClazz = Class.forName(container.paramDeclarations.get(index));
      } catch (Exception ignored) {
      }
      if (container.sub != null) subChainLink = new ParameterChain(container.sub, index);
    }

    public String resolve() {
      if (typeClazz != null) return typeClazz.getName();
      else if (subChainLink != null) return subChainLink.resolve();
      return null;
    }
  }
}
