package com.asledgehammer.typescript;

import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.type.TypeScriptElement;
import com.asledgehammer.typescript.type.TypeScriptNamespace;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
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

  public Map<TypeScriptNamespace, String> compileNamespacesSeparately(String prefix) {
    return graph.compileNamespacesSeparately(prefix);
  }

  public String compile(String prefix) {
    return graph.compile(prefix);
  }

  public void walk() {
    if (!graph.isReadOnly()) graph.walk();
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

  public List<TypeScriptElement> getAllGeneratedElements() {
    return graph.getAllGeneratedElements();
  }
}
