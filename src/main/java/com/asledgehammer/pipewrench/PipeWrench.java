package com.asledgehammer.pipewrench;

import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;

public class PipeWrench implements Runnable {

  private Scanner scanner;
  private StitchPipeWrench stitcher;

  private volatile boolean bDone = false;

  private PipeWrench() {

  }

  @Override
  public void run() {

    scanner = new Scanner(System.in);
    while(!bDone) {
      String command;
      try {
        command = scanner.nextLine();
      } catch (Exception e) {
        return;
      }
      if(command.isEmpty()) {
        sleep();
        continue;
      }
      String commandLower = command.toLowerCase();
      String[] args = commandLower.split(" ");
      if(args.length != 2 || (!args[0].equals("pipewrench") && !args[0].equals("pw"))) {
        sendHelp();
        sleep();
        continue;
      }
      try {
        switch (args[1]) {
          case "generate", "g" -> {
            System.out.println("[PIPEWRENCH] :: Generating Typings..");
            RenderZomboid.render();
            System.out.println("[PIPEWRENCH] :: Done.");
          }
          case "stitch", "s" -> {
            System.out.println("[PIPEWRENCH] :: Stitching Typings..");
            stitcher = new StitchPipeWrench("PipeWrench");
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
    System.out.println("[PIPEWRENCH] :: Commands:\n\t- 'pipewrench generate' Generates Java TypeScript definitions, exporting them to 'Zomboid/PipeWrench/generated/.\n\t- 'pipewrench stitch' Stitches Java & Lua TypeScript Definitions, Exporting them to 'Zomboid/PipeWrench/output'.");
  }

  public static void main(String[] args) {
    System.out.println("### PIPEWRENCH_JAVA_TYPES_INIT ###");
    new Thread(new PipeWrench(), "PipeWrench-Thread").start();
    invokeMain("zombie.gameStates.MainScreenState", args);
  }

  private static void invokeMain(String path, String[] args) {
    try {
      Class.forName(path)
          .getDeclaredMethod("main", String[].class)
          .invoke(null, new Object[] {args});
    } catch (IllegalAccessException
             | InvocationTargetException
             | NoSuchMethodException
             | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}
