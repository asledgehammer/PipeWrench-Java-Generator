package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.util.DocBuilder;

import java.lang.reflect.*;
import java.util.*;

public class TypeScriptClass extends TypeScriptElement {

  private final List<TypeScriptGeneric> genericParameters = new ArrayList<>();
  private final Map<String, TypeScriptField> fields = new HashMap<>();
  private final Map<String, List<TypeScriptMethodCluster>> methods = new HashMap<>();
  private final Map<String, List<TypeScriptMethodCluster>> staticMethods = new HashMap<>();
  private TypeScriptConstructor constructor;

  protected TypeScriptClass(TypeScriptNamespace namespace, Class<?> clazz) {
    super(namespace, clazz);
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    if (this.walked) return;
    System.out.println("Walking " + getName());
    walkConstructors(graph);
    walkGenericParameters(graph);
    if(getNamespace().getGraph().getCompiler().getSettings().renderFields) {
      walkFields(graph);
    }
    walkMethods(graph);
    walkSub(graph);
    if (getNamespace().getGraph().getCompiler().getSettings().renderFields) {
      checkDuplicateFieldMethods(graph);
    }
    this.walked = true;
  }

  private void walkConstructors(TypeScriptGraph graph) {
    if (clazz == null) return;
    constructor = new TypeScriptConstructor(this);
    constructor.walk(graph);
  }

  private void checkDuplicateFieldMethods(TypeScriptGraph ignored) {

    if(!getNamespace().getGraph().getCompiler().getSettings().renderFields) {
      return;
    }
    List<String> names = new ArrayList<>(fields.keySet());
    for (String fieldName : names) {
      if (methods.containsKey(fieldName)) {
        System.out.println(
            "[" + this.name + "]: Removing duplicate field as function: " + fieldName);
        fields.remove(fieldName);
      }
    }
  }

  private void walkSub(TypeScriptGraph graph) {
    if (this.clazz == null) return;
    for (Class<?> clazz : this.clazz.getNestMembers()) {
      if (!Modifier.isPublic(clazz.getModifiers())) continue;
      graph.add(clazz);
    }
  }

  private void walkGenericParameters(TypeScriptGraph graph) {
    genericParameters.clear();
    if (clazz == null) return;
    TypeVariable<?>[] params = clazz.getTypeParameters();

    for (TypeVariable<?> param : params) {
      TypeScriptGeneric generic = new TypeScriptGeneric(param);
      genericParameters.add(generic);
    }

    for (TypeScriptGeneric param : genericParameters) {
      param.walk(graph);
    }
  }

  private void walkFields(TypeScriptGraph graph) {
    if (clazz == null) return;
    if(!getNamespace().getGraph().getCompiler().getSettings().renderFields) {
      return;
    }
    fields.clear();
    for (Field field : clazz.getDeclaredFields()) {
      if (Modifier.isPublic(field.getModifiers())) {
        fields.put(field.getName(), new TypeScriptField(this, field));
      }
    }
    for (TypeScriptField field : fields.values()) field.walk(graph);
  }

  private void walkMethods(TypeScriptGraph graph) {
    if (clazz == null) return;
    methods.clear();

    TypeScriptSettings settings = namespace.getGraph().getCompiler().getSettings();
    for (Method method : clazz.getMethods()) {
      if (settings.isBlackListed(method)) continue;
      if (!Modifier.isPublic(method.getModifiers())) continue;

      List<TypeScriptMethodCluster> ms;
      if (Modifier.isStatic(method.getModifiers())) {
        ms = staticMethods.computeIfAbsent(method.getName(), s -> new ArrayList<>());
      } else {
        ms = methods.computeIfAbsent(method.getName(), s -> new ArrayList<>());
      }
      if (ms.size() == 1) continue;
      ms.add(new TypeScriptMethodCluster(this, method));
    }

    for (List<TypeScriptMethodCluster> methods : this.methods.values()) {
      for (TypeScriptMethodCluster method : methods) method.walk(graph);
    }
    for (List<TypeScriptMethodCluster> methods : this.staticMethods.values()) {
      for (TypeScriptMethodCluster method : methods) method.walk(graph);
    }
  }

  //  public String compileStaticOnly(String prefixOriginal) {
  //    if (clazz == null) return "";
  //    String prefix = prefixOriginal + "  ";
  //
  //    String name = clazz.getSimpleName();
  //    if (name.contains("$")) {
  //      String[] split = name.split("\\$");
  //      name = split[split.length - 1];
  //    }
  //
  //    StringBuilder stringBuilder = new StringBuilder(prefixOriginal);
  //    stringBuilder.append("// ");
  //
  //    if (clazz.isInterface()) {
  //      stringBuilder.append("[INTERFACE] ");
  //    } else {
  //      stringBuilder.append("[");
  //      if (Modifier.isAbstract(clazz.getModifiers())) {
  //        stringBuilder.append("ABSTRACT ");
  //      }
  //      stringBuilder.append("CLASS] ");
  //    }
  //
  //    stringBuilder.append(name);
  //    Type genericSuperclass = clazz.getGenericSuperclass();
  //    if (genericSuperclass != null) {
  //      stringBuilder.append(" extends ").append(clazz.getGenericSuperclass().getTypeName());
  //    } else {
  //      Class<?> superClazz = clazz.getSuperclass();
  //      if (superClazz != null) {
  //        stringBuilder.append(" extends ").append(superClazz.getName());
  //      }
  //    }
  //
  //    stringBuilder.append("\n").append(prefixOriginal);
  //    stringBuilder.append("export class ").append(name);
  //    stringBuilder.append(" {\n");
  //    stringBuilder.append(prefix).append("private constructor();\n");
  //
  //    if (!fields.isEmpty()) {
  //      List<String> names = new ArrayList<>(fields.keySet());
  //      names.sort(Comparator.naturalOrder());
  //      for (String _name : names) {
  //        TypeScriptField field = fields.get(_name);
  //        if (field.isStatic()) {
  //          stringBuilder.append(field.compile(prefix)).append('\n');
  //        }
  //      }
  //      stringBuilder.append('\n');
  //    }
  //
  //    if (constructor.exists) {
  //      stringBuilder.append(constructor.compile(prefix)).append("\n\n");
  //    }
  //
  //    if (!methods.isEmpty()) {
  //      List<String> names = new ArrayList<>(methods.keySet());
  //      names.sort(Comparator.naturalOrder());
  //      for (String _name : names) {
  //        List<TypeScriptMethod> methods = this.methods.get(_name);
  //        for (TypeScriptMethod method : methods) {
  //          if (method.isStatic()) {
  //            stringBuilder.append(method.compile(prefix)).append('\n');
  //          }
  //        }
  //      }
  //    }
  //
  //    if (stringBuilder.toString().endsWith("\n"))
  //      stringBuilder.setLength(stringBuilder.length() - 1);
  //    stringBuilder.append(prefixOriginal).append("}");
  //    return stringBuilder.toString();
  //  }

  @Override
  public String compile(String prefixOriginal) {

    if(clazz == null) return "";

    DocBuilder docBuilder = new DocBuilder();
    docBuilder.appendLine("@customConstructor " + clazz.getSimpleName() + ".new");
    docBuilder.appendLine("@");

    String prefix = prefixOriginal + "  ";
    StringBuilder stringBuilder = new StringBuilder();

    if (clazz.isInterface()) {
      stringBuilder.append("[INTERFACE] ");
    } else {
      stringBuilder.append("[");
      if (Modifier.isAbstract(clazz.getModifiers())) {
        stringBuilder.append("ABSTRACT ");
      }
      stringBuilder.append("CLASS] ");
    }

    stringBuilder.append(clazz.getName());
    Class<?> superClazz = clazz.getSuperclass();


    Type genericSuperclazz = clazz.getGenericSuperclass();
    if (genericSuperclazz != null && !genericSuperclazz.equals(Object.class)) {
      stringBuilder.append(" extends ").append(clazz.getGenericSuperclass().getTypeName());
    } else {
      if (superClazz != null && !superClazz.equals(Object.class)) {
        stringBuilder.append(" extends ").append(superClazz.getName());
      }
    }

    docBuilder.appendLine(stringBuilder.toString());

    stringBuilder.setLength(0);
    stringBuilder.append(docBuilder.build(prefixOriginal)).append("\n").append(prefixOriginal);
    stringBuilder.append("export class ").append(name);

    String compiledParams = "";
    if (!genericParameters.isEmpty()) {
      compiledParams += '<';

      for (TypeScriptGeneric param : genericParameters) {
        compiledParams += param.compile("") + ", ";
      }
      compiledParams = compiledParams.substring(0, compiledParams.length() - 2) + '>';
    }
    stringBuilder.append(compiledParams);

//    if (superClazz != null && !superClazz.equals(Object.class)) {
//      stringBuilder.append(" extends ").append(superClazz.getName());
//    } else if (clazz.isInterface()) {
//      Class<?>[] interfaces = clazz.getInterfaces();
//      if (interfaces.length == 1) {
//        stringBuilder.append(" extends ").append(interfaces[0].getName());
//      }
//    }

    stringBuilder.append(" {\n");

    if (getNamespace().getGraph().getCompiler().getSettings().renderFields) {
      if (!fields.isEmpty()) {
        List<String> names = new ArrayList<>(fields.keySet());
        names.sort(Comparator.naturalOrder());
        // Static Field(s)
        for (String _name : names) {
          TypeScriptField field = fields.get(_name);
          if (field.isStatic()) {
            stringBuilder.append(field.compile(prefix)).append('\n');
          }
        }
        // Non-Static Field(s)
        for (String name : names) {
          TypeScriptField field = fields.get(name);
          if (!field.isStatic()) {
            stringBuilder.append(field.compile(prefix)).append('\n');
          }
        }
        stringBuilder.append('\n');
      }
    }

    if (clazz.isInterface()) {
      stringBuilder.append(prefix).append("protected constructor();\n");
    } else {
      stringBuilder.append(constructor.compileCustomConstructor(prefix)).append('\n');
    }

    if (!methods.isEmpty()) {
      List<String> names = new ArrayList<>(methods.keySet());
      names.sort(Comparator.naturalOrder());
      // Non-Static Method(s)
      for (String name : names) {
        List<TypeScriptMethodCluster> methods = this.methods.get(name);
        for (TypeScriptMethodCluster method : methods) {
          stringBuilder.append(method.compile(prefix)).append('\n');
        }
      }
    }

    if (!staticMethods.isEmpty()) {
      List<String> names = new ArrayList<>(staticMethods.keySet());
      names.sort(Comparator.naturalOrder());
      // Static Method(s)
      for (String name : names) {
        List<TypeScriptMethodCluster> methods = this.staticMethods.get(name);
        for (TypeScriptMethodCluster method : methods) {
          stringBuilder.append(method.compile(prefix)).append('\n');
        }
      }
    }

    if (!stringBuilder.toString().endsWith("\n")) {
      stringBuilder.append('\n');
    }
//    if (stringBuilder.toString().endsWith("\n"))
//      stringBuilder.setLength(stringBuilder.length() - 1);
    stringBuilder.append(prefixOriginal).append("}");
    return stringBuilder.toString();
  }

  @Override
  public String compileLua(String table) {
    StringBuilder stringBuilder = new StringBuilder();
    if (!methods.isEmpty()) {
      List<String> names = new ArrayList<>(methods.keySet());
      names.sort(Comparator.naturalOrder());
      for (String name : names) {
        List<TypeScriptMethodCluster> methods = this.methods.get(name);
        for (TypeScriptMethodCluster method : methods) {
          stringBuilder.append(method.compileLua(table)).append('\n');
        }
      }
    }
    return stringBuilder.toString();
  }

  public Map<String, List<TypeScriptMethodCluster>> getMethods() {
    return this.methods;
  }
}
