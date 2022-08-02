package com.asledgehammer.pipewrench;

import java.io.File;

public class FileSystem {
  public static final File dirMain;
  public static final File dirGenerated;
  public static final File dirPartials;
  public static final File dirOutput;
  public static final File dirJava;
  public static final File dirLua;

  static {
    String home = System.getProperty("deployment.user.cachedir");
    if (home == null || System.getProperty("os.name").startsWith("Win")) {
      home = System.getProperty("user.home");
    }

    dirMain = new File(home + File.separator + "Zomboid/PipeWrench/");
    dirGenerated = new File(dirMain, "generated");
    dirPartials = new File(dirGenerated, "partials");
    dirOutput = new File(dirMain, "output");
    dirJava = new File(dirOutput, "java");
    dirLua = new File(dirOutput, "lua");

    if (!dirMain.exists()) dirMain.mkdirs();
    if (!dirGenerated.exists()) dirGenerated.mkdirs();
    if (!dirPartials.exists()) dirPartials.mkdirs();
    if (!dirOutput.exists()) dirOutput.mkdirs();
    if (!dirJava.exists()) dirJava.mkdirs();
    if (!dirLua.exists()) dirLua.mkdirs();
  }
}
