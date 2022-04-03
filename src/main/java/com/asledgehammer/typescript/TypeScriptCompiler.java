package com.asledgehammer.typescript;

import com.asledgehammer.typescript.settings.TypeScriptSettings;

public class TypeScriptCompiler {

  private final TypeScriptSettings settings;
  final TypeScriptGraph graph;

  public TypeScriptCompiler(TypeScriptSettings settings) {
    this.settings = settings;
    this.graph = new TypeScriptGraph(this);
  }

  public void add(Class<?>... clazzes) {
    graph.add(clazzes);
  }

  public String compile() {
    return graph.compile();
  }

  public void walk() {
    graph.walk();
  }

  public TypeScriptSettings getSettings() {
    return settings;
  }
}
