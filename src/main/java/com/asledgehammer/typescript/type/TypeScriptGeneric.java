package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;

import java.lang.reflect.TypeVariable;

public class TypeScriptGeneric implements TypeScriptWalkable, TypeScriptCompilable {

  private final TypeVariable<?> type;

  public TypeScriptGeneric(TypeVariable<?> type) {
    this.type = type;
  }

  @Override
  public void walk(TypeScriptGraph graph) {
  }

  @Override
  public String compile(String prefix) {
    return this.type.getTypeName();
  }
}
