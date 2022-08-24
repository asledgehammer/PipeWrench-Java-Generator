package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.settings.TypeScriptSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class TypeScriptEnum extends TypeScriptElement implements TypeScriptCompilable {

  private final Map<String, TypeScriptField> fields = new HashMap<>();
  private final Map<String, TypeScriptMethodCluster> methods = new HashMap<>();
  private final Map<String, TypeScriptMethodCluster> staticMethods = new HashMap<>();

  protected TypeScriptEnum(TypeScriptNamespace namespace, Class<?> clazz) {
    super(namespace, clazz);
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    if (walked) {
      return;
    }
    System.out.println("Walking " + getName());
    walkFields(graph);
    walkMethods(graph);
    this.walked = true;
  }

  private void walkFields(TypeScriptGraph graph) {
    if (clazz == null) {
      return;
    }
    for (Field field : clazz.getFields()) {
      if (!field.getDeclaringClass().equals(clazz)) {
        continue;
      }
      if (Modifier.isPublic(field.getModifiers())) {
        fields.put(field.getName(), new TypeScriptField(this, field));
      }
    }
    for (TypeScriptField field : fields.values()) {
      field.walk(graph);
    }
  }

  private void walkMethods(TypeScriptGraph graph) {
    if (clazz == null) {
      return;
    }
    methods.clear();

    TypeScriptSettings settings = namespace.getGraph().getCompiler().getSettings();
    for (Method method : clazz.getMethods()) {
      if (settings.isBlackListed(method)) {
        continue;
      }
      if (!Modifier.isPublic(method.getModifiers())) {
        continue;
      }
      if (Modifier.isStatic(method.getModifiers())) {
        if (!staticMethods.containsKey(method.getName())) {
          staticMethods.put(method.getName(), new TypeScriptMethodCluster(this, method));
        }
      } else {
        if (!methods.containsKey(method.getName())) {
          methods.put(method.getName(), new TypeScriptMethodCluster(this, method));
        }
      }
    }

    for (TypeScriptMethodCluster method : this.methods.values()) {
      method.walk(graph);
    }
    for (TypeScriptMethodCluster method : this.staticMethods.values()) {
      method.walk(graph);
    }
  }

  @Override
  public String compile(String prefixOriginal) {
    if (clazz == null) {
      return "";
    }

    TypeScriptSettings settings = getNamespace().getGraph().getCompiler().getSettings();

    String prefix = prefixOriginal + "  ";
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
        .append(prefixOriginal)
        .append("/** [ENUM] ")
        .append(clazz.getName())
        .append(" */")
        .append('\n');

    stringBuilder.append(prefixOriginal).append("export class ").append(getName()).append(" {\n");
    stringBuilder.append(prefix).append("protected constructor();\n");

    List<Enum<?>> values = Arrays.asList((Enum<?>[]) (clazz.getEnumConstants()));
    values.sort(Comparator.comparing(Enum::name));

    if (!values.isEmpty()) {
      for (Enum<?> value : values) {
        stringBuilder
            .append(prefix)
            .append("static readonly ")
            .append(value.name())
            .append(": ")
            .append(clazz.getName())
            .append(";\n");
      }
    }

    if (!fields.isEmpty()) {

      ArrayList<String> toRemove = new ArrayList<>();
      Map<String, TypeScriptField> fields = new HashMap<>(this.fields);
      for (String key : fields.keySet()) {
        if (fields.get(key).isStatic()) {
          toRemove.add(key);
        }
      }
      if (!toRemove.isEmpty()) {
        for (String key : toRemove) {
          fields.remove(key);
        }
      }

      if (!fields.isEmpty()) {
        stringBuilder.append('\n').append(prefix).append("/* FIELDS */\n\n");
        List<String> names = new ArrayList<>(fields.keySet());
        names.sort(Comparator.naturalOrder());
        for (String name : names) {
          TypeScriptField field = fields.get(name);
          if (!field.isStatic() && settings.renderNonStaticFields) {
            stringBuilder.append(field.compile(prefix)).append("\n\n");
          }
        }
      }
    }

    stringBuilder.append(prefix).append("name(): string;\n");
    stringBuilder.append(prefix).append("ordinal(): number;\n");

    if (!methods.isEmpty()) {
      List<String> names = new ArrayList<>(methods.keySet());
      names.sort(Comparator.naturalOrder());
      // Non-Static Method(s)
      for (String name : names) {
        TypeScriptMethodCluster method = this.methods.get(name);
        stringBuilder.append(method.compile(prefix)).append('\n');
      }
    }

    if (!staticMethods.isEmpty()) {
      List<String> names = new ArrayList<>(staticMethods.keySet());
      names.sort(Comparator.naturalOrder());
      // Static Method(s)
      for (String name : names) {
        TypeScriptMethodCluster method = this.staticMethods.get(name);
        stringBuilder.append(method.compile(prefix)).append('\n');
      }
    }

    stringBuilder.append(prefixOriginal).append('}');
    return stringBuilder.toString();
  }
}
