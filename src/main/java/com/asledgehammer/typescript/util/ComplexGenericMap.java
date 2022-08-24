package com.asledgehammer.typescript.util;

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
  private final Class<?> clazz;

  public ComplexGenericMap(Class<?> clazz) {
    this(null, clazz);
  }

  private ComplexGenericMap(ComplexGenericMap sub, Class<?> clazz) {
    this.clazz = clazz;
    this.sub = sub;
    if (clazz != null) {
      this.paramDeclarations = ClazzUtils.extractTypeDeclarations(clazz);
      Class<?> superClazz = clazz.getSuperclass();
      if (superClazz != null) {
        superMap.put(superClazz, new ComplexGenericMap(this, superClazz));
      }
      Class<?>[] interfaces = clazz.getInterfaces();
      for (Class<?> iClazz : interfaces) {
        superMap.put(iClazz, new ComplexGenericMap(this, iClazz));
      }
    } else {
      this.paramDeclarations = new ArrayList<>();
    }
  }

  public ComplexGenericMap getSuper(Class<?> superClazz) {
    for (Class<?> key : superMap.keySet()) {
      if (key.equals(superClazz)) {
        return superMap.get(key);
      }
      ComplexGenericMap found = superMap.get(key).getSuper(superClazz);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  public String resolveDeclaredType(Class<?> declaredClazz, Type paramType) {
    return resolveDeclaredType(declaredClazz, paramType.getTypeName());
  }

  public String resolveDeclaredType(Class<?> declaredClazz, String paramTypeName) {
    ComplexGenericMap declarationMap = getSuper(declaredClazz);
    if (declarationMap == null) {
      return paramTypeName;
    }
    TypeVariable<?>[] clazzParams = declaredClazz.getTypeParameters();
    for (int i = 0; i < clazzParams.length; i++) {
      TypeVariable<?> v = clazzParams[i];
      if (v.getTypeName().equals(paramTypeName)) {
        ParameterChain chainRoot = new ParameterChain(declarationMap, i);
        return chainRoot.resolve();
      }
    }
    return paramTypeName;
  }

  public String resolveDeclaredParameter(Parameter parameter) {
    return resolveDeclaredType(
        parameter.getDeclaringExecutable().getDeclaringClass(), parameter.getParameterizedType());
  }

  private static class ParameterChain {

    private Class<?> typeClazz;
    private ParameterChain subChainLink;

    private ParameterChain(ComplexGenericMap container, int index) {
      try {
        this.typeClazz = Class.forName(container.paramDeclarations.get(index));
      } catch (Exception ignored) {
      }
      if (container.sub != null) {
        int newIndex = getIndexOfSuper(container.clazz, container.sub.clazz, index);
        subChainLink = new ParameterChain(container.sub, newIndex);
      }
    }

    public String resolve() {
      if (typeClazz != null) {
        return typeClazz.getName();
      } else if (subChainLink != null) {
        return subChainLink.resolve();
      }
      return null;
    }

    private static int getIndexOfSuper(Class<?> superClazz, Class<?> subClazz, int knownIndex) {
      Type[] t = superClazz.getTypeParameters();
      if (knownIndex >= t.length) {
        return knownIndex;
      }
      String knownParamName = t[knownIndex].getTypeName();
      TypeVariable<?>[] subVars = subClazz.getTypeParameters();
      for (int subIndex = 0; subIndex < subVars.length; subIndex++) {
        TypeVariable<?> subVar = subVars[subIndex];
        if (subVar.getTypeName().equals(knownParamName)) {
          return subIndex;
        }
      }
      return knownIndex;
    }
  }
}
