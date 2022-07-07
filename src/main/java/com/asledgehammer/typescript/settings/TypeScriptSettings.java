package com.asledgehammer.typescript.settings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TypeScriptSettings {
  public final List<Method> methodsBlackList = new ArrayList<>();
  public final List<String> methodsBlackListByPath = new ArrayList<>();
  public Recursion recursion = Recursion.NONE;
  public boolean renderNonStaticFields = false;
  public boolean renderStaticFields = true;
  public boolean useNull = false;

    public boolean isBlackListed(Method method) {
    if (methodsBlackList.contains(method)) return true;
    String methodPath = method.getDeclaringClass().getName() + '#' + method.getName();
    return methodsBlackListByPath.contains(methodPath);
  }

  /** If all classes should have private constructors. */
  public boolean readOnly = false;
}
