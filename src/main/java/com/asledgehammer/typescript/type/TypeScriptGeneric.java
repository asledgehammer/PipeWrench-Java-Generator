package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;

import java.lang.reflect.TypeVariable;

public class TypeScriptGeneric implements TypeScriptWalkable, TypeScriptCompilable {

  private final TypeVariable<?> type;
  private boolean walked = false;

  public TypeScriptGeneric(TypeVariable<?> type) {
    this.type = type;
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    // TODO: Split up nested generic types and walk them.
    this.walked = true;
  }

  @Override
  public String compile(String prefix) {
    return this.type.getTypeName();
  }

  public boolean hasWalked() {
    return walked;
  }
}
