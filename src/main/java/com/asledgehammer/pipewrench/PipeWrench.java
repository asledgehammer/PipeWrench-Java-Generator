package com.asledgehammer.pipewrench;

import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;

public class PipeWrench implements Runnable {

  private Scanner scanner;
  private RenderZomboid generator;
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
        try {
          Thread.sleep(10L);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
      String commandLower = command.toLowerCase();
      try {
        if(commandLower.equals("pipewrench generate")) {
          System.out.println("[PIPEWRENCH] :: Generating Typings..");
          RenderZomboid.render();
          System.out.println("[PIPEWRENCH] :: Done.");
        } else if(commandLower.equals("pipewrench stitch")) {
          System.out.println("[PIPEWRENCH] :: Stitching Typings..");
          stitcher = new StitchPipeWrench("PipeWrench");
          stitcher.stitch();
          System.out.println("[PIPEWRENCH] :: Done.");
        } else {
          System.out.println("[PIPEWRENCH] :: Commands:\n\t- 'pipewrench generate' Generates Java TypeScript definitions, exporting them to 'Zomboid/PipeWrench/generated/.\n\t- 'pipewrench stitch' Stitches Java & Lua TypeScript Definitions, Exporting them to 'Zomboid/PipeWrench/output'.");
        }
      } catch (Exception e) {
        System.err.println("Failed to execute PipeWrench command: " + command);
        e.printStackTrace(System.err);
      }

      try {
        Thread.sleep(10L);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    scanner.close();
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
