package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;

public class TypeScriptEnum extends TypeScriptElement implements TypeScriptCompilable {

  protected TypeScriptEnum(TypeScriptNamespace namespace, Class<?> clazz) {
    super(namespace, clazz);
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    this.walked = true;
  }

  @Override
  public String compile(String prefixOriginal) {

    String prefix = prefixOriginal + "  ";
    String compiled = prefixOriginal + "export enum " + getName() + " {\n";

    for (Enum<?> value : (Enum<?>[]) (clazz.getEnumConstants())) {
      compiled += prefix + value.name() + ", \n";
    }

    compiled += prefixOriginal + "}";
    return compiled;
  }
}
