package com.asledgehammer.typescript;

import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.type.TypeScriptElement;

import java.util.List;

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

  public TypeScriptElement resolve(Class<?> clazz) {
    return graph.resolve(clazz.getName());
  }

  public List<Class<?>> getAllDeclaredClasses() {
    return graph.getAllDeclaredClasses();
  }

  public List<Class<?>> getAllKnownClasses() {
    return graph.getAllKnownClasses();
  }
}
