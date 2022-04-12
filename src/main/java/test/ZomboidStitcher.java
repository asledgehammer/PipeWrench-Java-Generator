package test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Scanner;

public class ZomboidStitcher {

  private static final File resourceDir = new File("src/main/resources");
  private static final File generatedDir = new File(resourceDir, "partials/generated");
  private static final File stitchedDir = new File(resourceDir, "stitched/generated");
  private static final File eventsDir = new File(resourceDir, "partials/api/events");
  private static final String BAR =
      "-------------------------------------------------------------------";
  private static final String BAR_TS =
      "///////////////////////////////////////////////////////////////////";

  public static void main(String[] args) throws IOException {
    stitchLua();
    stitchTypeScript();
  }

  private static void stitchLua() throws IOException {
    FileWriter writer = new FileWriter(new File(stitchedDir, "Zomboid.lua"));
    writer.write("local Exports = {}\n\n");
    writePartialLua(writer, new File(eventsDir, "helper.lua"));
    writePartialLua(writer, new File(eventsDir, "zomboid.lua"));
    writePartialLua(writer, new File(generatedDir, "globalobject.lua"));
    writer.write(BAR + "\n\n");
    writer.write("return Exports\n");
    writer.flush();
    writer.close();
  }

  public static void stitchTypeScript() throws IOException {
    FileWriter writer = new FileWriter(new File(stitchedDir, "Zomboid.d.ts"));

    writer.write("/** @noResolution @noSelfInFile */\n");
    writer.write("declare module \"Zomboid\" {\n");
    String prefix = "  ";

    writePartialTypeScript(writer, new File(generatedDir, "java.d.ts"), prefix);
    writePartialTypeScript(writer, new File(generatedDir, "globalobject.d.ts"), prefix);
    writePartialTypeScript(writer, new File(generatedDir, "class_vars.d.ts"), prefix);
    writePartialTypeScript(writer, new File(eventsDir, "helper.d.ts"), prefix);
    writer.write(prefix + BAR_TS + "\n");
    writer.write("}\n");
    writer.flush();
    writer.close();
  }

  private static void writePartialLua(FileWriter writer, File file) throws IOException {
    Scanner scanner = new Scanner(file);
    boolean inside = false;
    writer.write("-- File: " + file.getName() + '\n');
    writer.write(BAR + "\n\n");

    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.contains("-- [PARTIAL:START]")) {
        inside = true;
      } else if (line.contains("-- [PARTIAL:STOP]")) {
        inside = false;
      } else if (inside) {
        writer.write(line + '\n');
      }
    }

    writer.write("\n");
    scanner.close();
  }

  private static void writePartialTypeScript(FileWriter writer, File file, String prefix)
      throws IOException {
    Scanner scanner = new Scanner(file);
    boolean inside = false;
    writer.write(prefix + "// File: " + file.getName() + '\n');
    writer.write(prefix + BAR_TS + "\n\n");
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      if (line.contains("// [PARTIAL:START]")) {
        inside = true;
      } else if (line.contains("// [PARTIAL:STOP]")) {
        inside = false;
      } else if (inside) {
        writer.write(prefix + line + '\n');
      }
    }

    writer.write("\n");
    scanner.close();
  }

  static {
    if (!generatedDir.exists() && !generatedDir.mkdirs()) {
      try {
        throw new RemoteException("Cannot make dir: " + generatedDir.getPath());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }

    if (!stitchedDir.exists() && !stitchedDir.mkdirs()) {
      try {
        throw new RemoteException("Cannot make dir: " + stitchedDir.getPath());
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }
  }
}
