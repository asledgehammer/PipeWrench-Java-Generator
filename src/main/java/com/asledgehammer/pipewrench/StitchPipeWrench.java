package com.asledgehammer.pipewrench;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class StitchPipeWrench {

  private final String moduleName;
  private final String targetDir;

  public StitchPipeWrench(String moduleName, String targetDir) {
    this.moduleName = moduleName;
    this.targetDir = targetDir;
  }

  public void stitch() {

    File[] files = new File(targetDir).listFiles();
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
    builder.append("/** @noResolution @noSelfInFile */\n");
    builder.append("/// <reference path=\"reference.d.ts\" />\n\n");
    builder.append("declare module '").append(moduleName).append("' {\n");

    for (File file : files) {
      String fileName = file.getName();
      String fileNameLower = fileName.toLowerCase();

      // Make sure the file is an API partial.
      if (!fileNameLower.endsWith(".api.partial.d.ts")) {
        continue;
      }
      System.out.println("\tStitching file: " + file.getName() + "..");

      List<String> lines = getPartialFromTSFile(file);
      if (lines.isEmpty()) {
        System.out.println("\t\tNo line(s) to stitch.");
        continue;
      }

      String comment = "// [PARTIAL] : " + file.getName() + " //\n";
      String border = "//" + "/".repeat(comment.length() - 5) + "//\n";

      builder.append('\n').append(border).append(comment).append(border).append('\n');

      for (String line : lines) {
        builder.append(line).append('\n');
      }
    }

    builder.append("}\n");

    File file = new File(targetDir, moduleName + ".d.ts");
    write(file, builder);

    System.out.println("\n");
  }

  private void stitchReference(File[] files) {
    System.out.println("## Stitching Reference:");

    StringBuilder builder = new StringBuilder();

    for (File file : files) {
      String fileName = file.getName();
      String fileNameLower = fileName.toLowerCase();

      // Make sure the file is an API partial.
      if (!fileNameLower.endsWith(".reference.partial.d.ts")) {
        continue;
      }
      System.out.println("\tStitching file: " + file.getName() + "..");

      List<String> lines = getPartialFromTSFile(file);
      if (lines.isEmpty()) {
        System.out.println("\t\tNo line(s) to stitch.");
        continue;
      }

      String comment = "// [PARTIAL] : " + file.getName() + " //\n";
      String border = "//" + "/".repeat(comment.length() - 5) + "//\n";

      builder.append('\n').append(border).append(comment).append(border).append('\n');

      for (String line : lines) {
        builder.append(line).append('\n');
      }
    }

    File file = new File(targetDir, "reference.d.ts");
    write(file, builder);
    System.out.println("\n");
  }

  private void stitchInterface(File[] files) {
    System.out.println("## Stitching Interface:");

    StringBuilder builder = new StringBuilder();

    builder.append("local Exports = {}\n");

    for (File file : files) {
      String fileName = file.getName();
      String fileNameLower = fileName.toLowerCase();

      // Make sure the file is an API partial.
      if (!fileNameLower.endsWith(".interface.partial.lua")) {
        continue;
      }
      System.out.println("\tStitching file: " + file.getName() + "..");

      List<String> lines = getPartialFromLuaFile(file);
      if (lines.isEmpty()) {
        System.out.println("\t\tNo line(s) to stitch.");
        continue;
      }

      String comment = "-- [PARTIAL] : " + file.getName() + " --\n";
      String border = "--" + "-".repeat(comment.length() - 5) + "--\n";
      builder.append('\n').append(border).append(comment).append(border).append('\n');

      for (String line : lines) {
        builder.append(line).append('\n');
      }
    }

    builder.append("return Exports");

    File file = new File(targetDir, moduleName + ".lua");
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
          if (lineLower.contains("[partial:start]")) {
            in = true;
          } else if (lineLower.contains("[partial:stop]")) {
            in = false;
          } else if (in) {
            lines.add(line);
          }
        } else if (in) {
          lines.add(line);
        }
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
          if (lineLower.contains("[partial:start]")) {
            in = true;
          } else if (lineLower.contains("[partial:stop]")) {
            in = false;
          } else if (in) {
            lines.add(line);
          }
        } else if (in) {
          lines.add(line);
        }
      }
      scanner.close();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    return lines;
  }

  public static void main(String[] args) {
    new StitchPipeWrench("PipeWrench", args[0]).stitch();
  }
}
