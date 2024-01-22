package com.asledgehammer.pipewrench;

import com.asledgehammer.typescript.TypeScriptCompiler;
import com.asledgehammer.typescript.settings.Recursion;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.type.*;
import se.krka.kahlua.vm.KahluaTable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.Function;

public class RenderZomboidEmmy {

  File outDir;
  File luaDir;
  File sharedDir;
  File libraryDir;

  private final ClassBag classBag = new ClassBag();
  private final TypeScriptCompiler tsCompiler;
  private Map<String, String> classes = new HashMap<>();
  private Map<String, String> aliases = new HashMap<>();
  private List<Class<?>> knownClasses;
  private List<TypeScriptElement> elements;

  public RenderZomboidEmmy(String outDir) {
    this.outDir = new File(outDir);
    this.outDir.mkdirs();
    this.luaDir = new File(this.outDir, "lua");
    this.luaDir.mkdirs();
    this.sharedDir = new File(this.luaDir, "shared");
    this.sharedDir.mkdirs();
    this.libraryDir = new File(this.sharedDir, "library");
    this.libraryDir.mkdirs();

    TypeScriptSettings tsSettings = new TypeScriptSettings();
    tsSettings.methodsBlackListByPath.add("java.lang.Object#equals");
    tsSettings.methodsBlackListByPath.add("java.lang.Object#getClass");
    tsSettings.methodsBlackListByPath.add("java.lang.Object#hashCode");
    tsSettings.methodsBlackListByPath.add("java.lang.Object#notify");
    tsSettings.methodsBlackListByPath.add("java.lang.Object#notifyAll");
    tsSettings.methodsBlackListByPath.add("java.lang.Object#toString");
    tsSettings.methodsBlackListByPath.add("java.lang.Object#wait");
    tsSettings.recursion = Recursion.NONE;
    tsSettings.readOnly = true;

    tsCompiler = new TypeScriptCompiler(tsSettings);
  }

  public void walk() {
    tsCompiler.walk();
    knownClasses = tsCompiler.getAllKnownClasses();
    elements = tsCompiler.getAllGeneratedElements();
    elements.sort(
        (o1, o2) -> {
          String name1 = o1.getClazz() != null ? o1.getClazz().getSimpleName() : o1.getName();
          if (name1.contains("$")) {
            String[] split = name1.split("\\$");
            name1 = split[split.length - 1];
          }
          String name2 = o2.getClazz() != null ? o2.getClazz().getSimpleName() : o2.getName();
          if (name2.contains("$")) {
            String[] split = name2.split("\\$");
            name2 = split[split.length - 1];
          }
          return name1.compareTo(name2);
        });
  }

  public void render() {
    classes.clear();
    aliases.clear();
    renderJavaFile();
    renderLegacy();
  }

  private void renderLegacy() {

    List<TypeScriptElement> elementsSorted = new ArrayList<>(elements);
    elementsSorted.sort(Comparator.comparing(TypeScriptElement::getName));

    Function<Class<?>, Boolean> evalClass =
        (jClass) -> {
          TypeScriptElement tsElement = tsCompiler.resolve(jClass);

          if (jClass == ThreadLocal.class) {
            System.out.println(
                "### ThreadLocal: "
                    + elementsSorted.contains(tsElement)
                    + " "
                    + tsElement.getClazz());
          }
          if ((tsElement == null || !elementsSorted.contains(tsElement))) {

            String name = jClass.getSimpleName();
            if (name.endsWith("[]")) {
              jClass = jClass.getComponentType();
              name = jClass.getSimpleName();
            }

            if (isExemptAlias(jClass) || aliases.containsKey(name)) {
              return false;
            }

            System.out.println("### ADDING ALIAS: " + jClass.getSimpleName());

            if (!jClass.getSimpleName().startsWith("[") || !jClass.getSimpleName().endsWith("[]")) {
              aliases.put(name, "--- @alias " + name + " Object");
            }
            return true;
          }

          return false;
        };

    for (TypeScriptElement element : elementsSorted) {
      String eFullPath = element.getNamespace().getFullPath();
      if (eFullPath.startsWith("[")) continue;

      System.out.println("Next: " + element.name + " class: " + element.getClass());

      StringBuilder builder = new StringBuilder();
      builder
          .append("--- File: ")
          .append(element.getNamespace().getFullPath().replaceAll("\\.", "/"))
          .append('/')
          .append(element.name)
          .append(".java\n");
      builder.append("--- @class ").append(element.name).append('\n');

      if (element instanceof TypeScriptClass eClass) {

        Map<String, TypeScriptField> fields = eClass.getFields();

        for (String fieldName : fields.keySet()) {
          TypeScriptField eField = fields.get(fieldName);
          Field jField = eField.getField();
          Class<?> jClass = jField.getType();

          if (!eField.isbPublic()) return;

          evalClass.apply(jClass);

          builder
              .append("--- @field public ")
              .append(fieldName)
              .append(" ")
              .append(eField.getType())
              .append('\n');
        }

        String luaName = element.name;
        boolean alt = false;
        if (element.name.indexOf("$") != -1) {
          alt = true;
          luaName = "_G['" + element.name + "']";
        }
        builder.append(luaName).append(" = {};\n");

        Map<String, TypeScriptMethodCluster> methodsStatic = eClass.getStaticMethods();
        Map<String, TypeScriptMethodCluster> methods = eClass.getMethods();

        if (!methods.isEmpty() || !methodsStatic.isEmpty()) {
          builder.append('\n');
          if (alt) {
            builder.append("__temp__ = ").append(luaName).append(";\n");
          }
        }

        // STATIC METHODS
        if (!methodsStatic.isEmpty()) {
          List<String> methodNames = new ArrayList<>(methodsStatic.keySet());
          methodNames.sort(Comparator.naturalOrder());
          for (String key : methodNames) {

            if (key.equalsIgnoreCase("and")) continue;

            TypeScriptMethodCluster cluster = methodsStatic.get(key);

            // Evalulate all classes for all parameters and return type.
            List<Method> methodsSorted = cluster.getSortedMethods();
            Method methodFirst = methodsSorted.get(0);
            evalClass.apply(methodFirst.getReturnType());
            for (Method m : methodsSorted) {
              Parameter[] ps = m.getParameters();
              for (Parameter p : ps) {
                evalClass.apply(p.getType());
              }
            }

            builder.append(cluster.walkDocsLua2(true));
            if (alt) {
              builder
                  .append("function __temp__")
                  .append(".")
                  .append(key)
                  .append("(")
                  .append(cluster.compileLuaParams())
                  .append(") end\n\n");
            } else {
              builder
                  .append("function ")
                  .append(luaName)
                  .append(".")
                  .append(key)
                  .append("(")
                  .append(cluster.compileLuaParams())
                  .append(") end\n\n");
            }
          }
        }

        // METHODS
        if (!methods.isEmpty()) {
          List<String> methodNames = new ArrayList<>(methods.keySet());
          methodNames.sort(Comparator.naturalOrder());
          for (String key : methodNames) {

            if (key.equalsIgnoreCase("and")) continue;

            TypeScriptMethodCluster cluster = methods.get(key);

            // Evalulate all classes for all parameters and return type.
            List<Method> methodsSorted = cluster.getSortedMethods();
            Method methodFirst = methodsSorted.get(0);
            evalClass.apply(methodFirst.getReturnType());
            for (Method m : methodsSorted) {
              Parameter[] ps = m.getParameters();
              for (Parameter p : ps) {
                evalClass.apply(p.getType());
              }
            }

            builder.append(cluster.walkDocsLua2(false));
            if (alt) {
              builder
                  .append("function __temp__")
                  .append(":")
                  .append(key)
                  .append("(")
                  .append(cluster.compileLuaParams())
                  .append(") end\n\n");
            } else {
              builder
                  .append("function ")
                  .append(luaName)
                  .append(":")
                  .append(key)
                  .append("(")
                  .append(cluster.compileLuaParams())
                  .append(") end\n\n");
            }
          }
        }

        classes.put(element.name, builder.toString());
      } else if (element instanceof TypeScriptEnum eEnum) {
        Map<String, TypeScriptField> fields = eEnum.getFields();

        for (String fieldName : fields.keySet()) {
          TypeScriptField eField = fields.get(fieldName);
          Field jField = eField.getField();
          Class<?> jClass = jField.getType();
          if (!eField.isbPublic()) continue;

          evalClass.apply(jClass);

          builder
              .append("--- @field public ")
              .append(fieldName)
              .append(" ")
              .append(eField.getType())
              .append('\n');
        }

        String luaName = element.name;
        boolean alt = false;
        if (element.name.indexOf("$") != -1) {
          alt = true;
          luaName = "_G['" + element.name + "']";
        }

        builder.append(luaName).append(" = {};\n");

        Map<String, TypeScriptMethodCluster> methodsStatic = eEnum.getStaticMethods();
        Map<String, TypeScriptMethodCluster> methods = eEnum.getMethods();

        if (!methods.isEmpty() || !methodsStatic.isEmpty()) {
          builder.append('\n');
          if (alt) {
            builder.append("__temp__ = ").append(luaName).append(";\n");
          }
        }

        // STATIC METHODS
        if (!methodsStatic.isEmpty()) {
          List<String> methodNames = new ArrayList<>(methodsStatic.keySet());
          methodNames.sort(Comparator.naturalOrder());
          for (String key : methodNames) {

            if (key.equalsIgnoreCase("and")) continue;

            TypeScriptMethodCluster cluster = methodsStatic.get(key);

            // Evalulate all classes for all parameters and return type.
            List<Method> methodsSorted = cluster.getSortedMethods();
            Method methodFirst = methodsSorted.get(0);
            evalClass.apply(methodFirst.getReturnType());
            for (Method m : methodsSorted) {
              Parameter[] ps = m.getParameters();
              for (Parameter p : ps) {
                evalClass.apply(p.getType());
              }
            }

            builder.append(cluster.walkDocsLua2(true));
            if (alt) {
              builder
                  .append("function __temp__")
                  .append(".")
                  .append(key)
                  .append("(")
                  .append(cluster.compileLuaParams())
                  .append(") end\n\n");
            } else {
              builder
                  .append("function ")
                  .append(luaName)
                  .append(".")
                  .append(key)
                  .append("(")
                  .append(cluster.compileLuaParams())
                  .append(") end\n\n");
            }
          }
        }

        // METHODS
        if (!methods.isEmpty()) {
          List<String> methodNames = new ArrayList<>(methods.keySet());
          methodNames.sort(Comparator.naturalOrder());
          for (String key : methodNames) {

            if (key.equalsIgnoreCase("and")) continue;

            TypeScriptMethodCluster cluster = methods.get(key);

            // Evalulate all classes for all parameters and return type.
            List<Method> methodsSorted = cluster.getSortedMethods();
            Method methodFirst = methodsSorted.get(0);
            evalClass.apply(methodFirst.getReturnType());
            for (Method m : methodsSorted) {
              Parameter[] ps = m.getParameters();
              for (Parameter p : ps) {
                evalClass.apply(p.getType());
              }
            }

            builder.append(cluster.walkDocsLua2(false));
            if (alt) {
              builder
                  .append("function __temp__")
                  .append(":")
                  .append(key)
                  .append("(")
                  .append(cluster.compileLuaParams())
                  .append(") end\n\n");
            } else {
              builder
                  .append("function ")
                  .append(luaName)
                  .append(":")
                  .append(key)
                  .append("(")
                  .append(cluster.compileLuaParams())
                  .append(") end\n\n");
            }
          }
        }

        builder.append('\n');
        classes.put(element.name, builder.toString());
      } else if (element instanceof TypeScriptType eType) {
        Class<?> jClass = eType.getClazz();

        String name = jClass != null ? jClass.getSimpleName() : eType.name;
        if (name.endsWith("[]")) {
          jClass = jClass.getComponentType();
          if (jClass == null) continue;
          name = jClass.getSimpleName();
          if (isExemptAlias(jClass)) continue;
        }

        String ID = jClass != null ? jClass.getSimpleName() : name;
        if (!aliases.containsKey(ID) && !isExemptAlias(jClass) && !ID.startsWith("[")
            || !ID.endsWith("[]")) {
          aliases.put(ID, "--- @alias " + ID + " Object");
        }
      }

      //          String name = element.name;
      //          if (name.contains("$")) {
      //            String[] split = name.split("\\$");
      //            name = split[split.length - 1];
      //          }
      //          String line = "Exports." + name + " = loadstring(\"return _G['" + name +
      // "']\")()\n";
      //          builder.append(line);
    }

    StringBuilder aliasBlock = new StringBuilder();
    List<String> keys = new ArrayList<>(aliases.keySet());
    keys.sort(Comparator.naturalOrder());
    for (String key : keys) {
      if (key.startsWith("[") || key.endsWith("[]") || key.equals("char")) continue;
      String line = aliases.get(key);
      aliasBlock.append(line).append("\n");
    }

    StringBuilder classBlock = new StringBuilder();
    keys = new ArrayList<>(classes.keySet());
    keys.sort(Comparator.naturalOrder());
    for (String key : keys) {
      if (key.startsWith("[") || key.endsWith("[]") || key.equals("char")) continue;
      String line = classes.get(key);
      classBlock.append(line).append("\n");
    }

    // Here we have to name the Lua file exactly the same as the module so require
    // statements work.
    File fileZomboidLua = new File("candle.lua");
    write(
        fileZomboidLua,
        "\n---------- ALIASES ----------\n\n"
            + aliasBlock
            + "\n---------- CLASSES ----------\n\n"
            + classBlock);
  }

  public void renderJavaFile() {
    String fileContents =
        """

    ---------- JAVA ---------

    --- @alias byte number
    --- @alias short number
    --- @alias int number
    --- @alias char string
    --- @alias float number
    --- @alias double number
    --- @alias long number
    --- @alias void nil
    --- @alias Unknown Object
    --- @alias Object any
    --- @alias Void void
    --- @alias Boolean boolean
    --- @alias Short short
    --- @alias Integer int
    --- @alias Float float
    --- @alias Double double
    --- @alias Long long
    --- @alias BigInt number
    --- @alias Character string
    --- @alias String string
    --- @alias KahluaTable table
    """;

    write(new File(libraryDir, "java.lua"), fileContents);
  }

  public boolean isExemptAlias(Class<?> clazz) {
    return clazz == boolean.class
        | clazz == Boolean.class
        | clazz == byte.class
        | clazz == Byte.class
        | clazz == short.class
        | clazz == Short.class
        | clazz == int.class
        | clazz == Integer.class
        | clazz == float.class
        | clazz == Float.class
        | clazz == double.class
        | clazz == Double.class
        | clazz == long.class
        | clazz == Long.class
        | clazz == String.class
        | clazz == char.class
        | clazz == Character.class
        | clazz == Object.class
        | clazz == void.class
        | clazz == Void.class
        | clazz == KahluaTable.class;
  }

  private static void write(File file, String content) {
    try {
      FileWriter writer = new FileWriter(file);
      writer.write(content);
      writer.flush();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] yargs) {
    RenderZomboidEmmy o = new RenderZomboidEmmy("./");
    o.renderJavaFile();
  }
}

