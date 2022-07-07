package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.settings.TypeScriptSettings;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class TypeScriptEnum extends TypeScriptElement implements TypeScriptCompilable {

  private final Map<String, TypeScriptField> fields = new HashMap<>();
  private final Map<String, TypeScriptMethod> methods = new HashMap<>();

  protected TypeScriptEnum(TypeScriptNamespace namespace, Class<?> clazz) {
    super(namespace, clazz);
  }

  @Override
  public void walk(TypeScriptGraph graph) {
    if (walked) return;
    System.out.println("Walking " + getName());
    walkFields(graph);
    walkMethods(graph);
    this.walked = true;
  }

  private void walkFields(TypeScriptGraph graph) {
    if (clazz == null) return;
    for (Field field : clazz.getFields()) {
      if (!field.getDeclaringClass().equals(clazz)) continue;
      if (Modifier.isPublic(field.getModifiers())) {
        fields.put(field.getName(), new TypeScriptField(this, field));
      }
    }
    for (TypeScriptField field : fields.values()) field.walk(graph);
  }

  private void walkMethods(TypeScriptGraph graph) {
    if (clazz == null) return;
    for (Method method : clazz.getMethods()) {
      if (!method.getDeclaringClass().equals(clazz)) continue;
      if (Modifier.isPublic(method.getModifiers())) {
        methods.put(method.getName(), new TypeScriptMethod(this, method));
      }
    }
    for (TypeScriptMethod method : methods.values()) method.walk(graph);
  }

  @Override
  public String compile(String prefixOriginal) {
    if (clazz == null) return "";

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
        if (fields.get(key).isStatic()) toRemove.add(key);
      }
      if (!toRemove.isEmpty()) {
        for (String key : toRemove) fields.remove(key);
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

      ArrayList<String> toRemove = new ArrayList<>();
      Map<String, TypeScriptMethod> methods = new HashMap<>(this.methods);
      for (String key : methods.keySet()) {
        if (methods.get(key).isStatic()) toRemove.add(key);
      }
      if (!toRemove.isEmpty()) {
        for (String key : toRemove) methods.remove(key);
      }

      if (!methods.isEmpty()) {
        List<String> names = new ArrayList<>(methods.keySet());
        names.sort(Comparator.naturalOrder());
        for (String name : names) {
          TypeScriptMethod method = methods.get(name);
          if (!method.isStatic()) {
            stringBuilder.append(method.compile(prefix)).append("\n\n");
          }
        }
      }
    }

    stringBuilder.append(prefixOriginal).append('}');
    return stringBuilder.toString();
  }
}
