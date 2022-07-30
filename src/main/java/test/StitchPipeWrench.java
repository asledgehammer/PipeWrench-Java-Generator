package test;

import com.asledgehammer.typescript.util.DocBuilder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class StitchPipeWrench {

  private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");

  private final String moduleName;
  private final File dirPartials = new File("partials/");
  private final File dirOutput = new File("output/");

  public StitchPipeWrench(String moduleName) {
    this.moduleName = moduleName;

    // Initialize directories.
    if (!dirPartials.exists()) dirPartials.mkdirs();
    if (!dirOutput.exists()) dirOutput.mkdirs();

    File[] files = dirPartials.listFiles();
    if (files == null || files.length == 0) {
      System.err.println("No partial files to stitch.");
      System.exit(0);
    }

    stitchAPI(files);
    stitchReference(files);
    stitchInterface(files);
  }

  private void stitchAPI(File[] files) {
    System.out.println("## Stitching API:");

    StringBuilder builder = new StringBuilder();

    builder.append(generateTSLicense()).append("\n\n");
    builder.append("/** @noResolution @noSelfInFile */\n");
    builder.append("/// <reference path=\"reference.d.ts\" />\n\n");
    builder.append("declare module '").append(moduleName).append("' {\n");

    for (File file : files) {
      String fileName = file.getName();
      String fileNameLower = fileName.toLowerCase();

      // Make sure the file is an API partial.
      if (!fileNameLower.endsWith(".api.partial.d.ts")) continue;
      System.out.println("\tStitching file: " + file.getName() + "..");

      List<String> lines = getPartialFromTSFile(file);
      if (lines.isEmpty()) {
        System.out.println("\t\tNo line(s) to stitch.");
        continue;
      }

      String comment = "// [PARTIAL] : " + file.getName() + " //\n";
      String border = "//" + "/".repeat(comment.length() - 5) + "//\n";

      builder.append('\n').append(border).append(comment).append(border).append('\n');

      for (String line : lines) builder.append(line).append('\n');
    }

    builder.append("}\n");

    File file = new File(dirOutput, moduleName + ".d.ts");
    write(file, builder);

    System.out.println("\n");
  }

  private void stitchReference(File[] files) {
    System.out.println("## Stitching Reference:");

    StringBuilder builder = new StringBuilder();

    builder.append(generateTSLicense()).append("\n");

    for (File file : files) {
      String fileName = file.getName();
      String fileNameLower = fileName.toLowerCase();

      // Make sure the file is an API partial.
      if (!fileNameLower.endsWith(".reference.partial.d.ts")) continue;
      System.out.println("\tStitching file: " + file.getName() + "..");

      List<String> lines = getPartialFromTSFile(file);
      if (lines.isEmpty()) {
        System.out.println("\t\tNo line(s) to stitch.");
        continue;
      }

      String comment = "// [PARTIAL] : " + file.getName() + " //\n";
      String border = "//" + "/".repeat(comment.length() - 5) + "//\n";

      builder.append('\n').append(border).append(comment).append(border).append('\n');

      for (String line : lines) builder.append(line).append('\n');
    }

    File file = new File(dirOutput, "reference.d.ts");
    write(file, builder);
    System.out.println("\n");
  }

  private void stitchInterface(File[] files) {
    System.out.println("## Stitching Interface:");

    StringBuilder builder = new StringBuilder();

    builder.append(generateLuaLicense()).append("\n");
    builder.append("local Exports = {}\n");

    for (File file : files) {
      String fileName = file.getName();
      String fileNameLower = fileName.toLowerCase();

      // Make sure the file is an API partial.
      if (!fileNameLower.endsWith(".interface.partial.lua")) continue;
      System.out.println("\tStitching file: " + file.getName() + "..");

      List<String> lines = getPartialFromLuaFile(file);
      if (lines.isEmpty()) {
        System.out.println("\t\tNo line(s) to stitch.");
        continue;
      }

      String comment = "-- [PARTIAL] : " + file.getName() + " --\n";
      String border = "--" + "-".repeat(comment.length() - 5) + "--\n";
      builder.append('\n').append(border).append(comment).append(border).append('\n');

      for (String line : lines) builder.append(line).append('\n');
    }

    builder.append("return Exports");

    File file = new File(dirOutput, moduleName + ".lua");
    write(file, builder);

    System.out.println("\n");
  }

  private static void write(File file, StringBuilder builder) {
    try {
      FileWriter writer = new FileWriter(file);
      writer.write(builder.toString());
      writer.flush();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static List<String> getPartialFromTSFile(File file) {
    List<String> lines = new ArrayList<>();
    try {
      Scanner scanner = new Scanner(file);
      boolean in = false;
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String lineLower = line.toLowerCase();
        if (line.trim().startsWith("//")) {
          if (lineLower.contains("[partial:start]")) in = true;
          else if (lineLower.contains("[partial:stop]")) in = false;
          else if (in) lines.add(line);
        } else if (in) lines.add(line);
      }
      scanner.close();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    return lines;
  }

  public static List<String> getPartialFromLuaFile(File file) {
    List<String> lines = new ArrayList<>();
    try {
      Scanner scanner = new Scanner(file);
      boolean in = false;
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        String lineLower = line.toLowerCase();
        if (line.trim().startsWith("--")) {
          if (lineLower.contains("[partial:start]")) in = true;
          else if (lineLower.contains("[partial:stop]")) in = false;
          else if (in) lines.add(line);
        } else if (in) lines.add(line);
      }
      scanner.close();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    return lines;
  }

  public static void main(String[] args) {
    new StitchPipeWrench("PipeWrench");
  }

  private static final String[] LICENSE =
      new String[] {
        "MIT License",
        "",
        "Copyright (c) $YEAR$ JabDoesThings",
        "",
        "Permission is hereby granted, free of charge, to any person obtaining a copy",
        "of this software and associated documentation files (the \"Software\"), to deal",
        "in the Software without restriction, including without limitation the rights",
        "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell",
        "copies of the Software, and to permit persons to whom the Software is",
        "furnished to do so, subject to the following conditions:",
        "",
        "The above copyright notice and this permission notice shall be included in all",
        "copies or substantial portions of the Software.",
        "",
        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR",
        "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,",
        "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE",
        "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER",
        "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,",
        "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE",
        "SOFTWARE."
      };

  private String generateTSLicense() {
    Calendar calendar = Calendar.getInstance();
    DocBuilder docBuilder = new DocBuilder();
    for (String line : LICENSE) {
      docBuilder.appendLine(line.replaceAll("\\$YEAR\\$", "" + calendar.get(Calendar.YEAR)));
    }
    docBuilder.appendLine();
    docBuilder.appendLine("File generated at " + dateFormat.format(new Date()));
    return docBuilder.build("");
  }

  private String generateLuaLicense() {
    Calendar calendar = Calendar.getInstance();
    StringBuilder built = new StringBuilder();
    for (String line : LICENSE) {
      built
          .append("-- ")
          .append(line.replaceAll("\\$YEAR\\$", "" + calendar.get(Calendar.YEAR)))
          .append('\n');
    }
    built.append("--\n");
    built.append("-- File generated at ").append(dateFormat.format(new Date())).append('\n');
    return built.toString();
  }
}
