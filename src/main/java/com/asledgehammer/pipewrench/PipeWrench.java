package com.asledgehammer.pipewrench;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.Scanner;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;

public class PipeWrench implements Runnable {

  private Scanner scanner;
  private RenderZomboid generator;
  private StitchPipeWrench stitcher;

  private volatile boolean bDone = false;

  private PipeWrench() {

  }

  @Override
  public void run() {
    String home = System.getProperty("deployment.user.cachedir");
    if (home == null || System.getProperty("os.name").startsWith("Win")) {
      home = System.getProperty("user.home");
    }
    String outDir = Paths.get(home, "Zomboid", "PipeWrench").toString();
    this.generator = new RenderZomboid(outDir);
    scanner = new Scanner(System.in);
    while (!bDone) {
      String command;
      try {
        command = scanner.nextLine();
      } catch (Exception e) {
        return;
      }
      if (command.isEmpty()) {
        sleep();
        continue;
      }
      String commandLower = command.toLowerCase();
      String[] args = commandLower.split(" ");
      if (args.length != 2 || (!args[0].equals("pipewrench") && !args[0].equals("pw"))) {
        sendHelp();
        sleep();
        continue;
      }
      try {
        switch (args[1]) {
          case "generate", "g" -> {
            System.out.println("[PIPEWRENCH] :: Generating Typings..");
            this.generator.render();
            System.out.println("[PIPEWRENCH] :: Done.");
          }
          case "stitch", "s" -> {
            System.out.println("[PIPEWRENCH] :: Stitching Typings..");
            stitcher = new StitchPipeWrench("PipeWrench", outDir);
            stitcher.stitch();
            System.out.println("[PIPEWRENCH] :: Done.");
          }
          default -> sendHelp();
        }
      } catch (Exception e) {
        System.err.println("Failed to execute PipeWrench command: " + command);
        e.printStackTrace(System.err);
      }

      sleep();
    }

    scanner.close();
  }

  private static void sleep() {
    try {
      Thread.sleep(10L);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static void sendHelp() {
    System.out.println(
        "[PIPEWRENCH] :: Commands:\n\t- 'pipewrench generate' Generates Java TypeScript definitions, exporting them to 'Zomboid/PipeWrench/generated/.\n\t- 'pipewrench stitch' Stitches Java & Lua TypeScript Definitions, Exporting them to 'Zomboid/PipeWrench/output'.");
  }

  public static void cli(String outDir) {
    System.out.println("Exporting to " + outDir);
    RenderZomboid renderer = new RenderZomboid(outDir);
    renderer.render();
  }

  public static void live(String[] args) {
    new Thread(new PipeWrench(), "PipeWrench-Thread").start();
    invokeMain("zombie.gameStates.MainScreenState", args);
  }

  public static void main(String[] args) {
    System.out.println("### PIPEWRENCH_JAVA_TYPES_INIT ###");

    ArgumentParser parser = ArgumentParsers.newFor("PipeWrench").build();
    Subparsers subparsers = parser.addSubparsers().help("sub-command help").dest("subparser_name");
    Subparser cli = subparsers.addParser("cli");
    cli.addArgument("--out").dest("outDir").setDefault("./dist");
    subparsers.addParser("live");
    try {
      Namespace res = parser.parseArgs(args);
      String mode = res.get("subparser_name");
      if (mode == "cli") {
        cli(res.get("outDir"));
      }
      if (mode == "live") {
        live(args);
      }
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.exit(1);
    }
  }

  private static void invokeMain(String path, String[] args) {
    try {
      Class.forName(path)
          .getDeclaredMethod("main", String[].class)
          .invoke(null, new Object[] { args });
    } catch (IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}
