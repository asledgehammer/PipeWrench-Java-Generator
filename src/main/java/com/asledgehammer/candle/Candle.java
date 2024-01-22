package com.asledgehammer.candle;

import com.asledgehammer.candle.impl.EmmyLuaRenderer;
import zombie.Lua.LuaManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

class Candle {

  final CandleGraph graph = new CandleGraph();
  final File outDir;

  Candle() {
    File outDir = new File("./");
    outDir.mkdirs();
    File luaDir = new File(outDir, "lua");
    luaDir.mkdirs();
    File sharedDir = new File(luaDir, "shared");
    sharedDir.mkdirs();
    this.outDir = new File(sharedDir, "library");
    this.outDir.mkdirs();
  }

  void walk() {
    this.graph.walk();
  }

  void render(CandleRenderAdapter adapter) {
    this.graph.render(adapter);
    renderGlobalAPI();
  }

  private void renderGlobalAPI() {
    CandleClass candleGlobalObject = this.graph.classes.get(LuaManager.GlobalObject.class);

    Map<String, CandleExecutableCluster<CandleMethod>> methods = candleGlobalObject.getStaticMethods();

    List<String> keysSorted = new ArrayList<>(methods.keySet());
    keysSorted.sort(Comparator.naturalOrder());

    StringBuilder builder = new StringBuilder();
    for(String methodName: keysSorted) {
      CandleExecutableCluster<CandleMethod> cluster = methods.get(methodName);
      builder.append("\n").append(cluster.getRenderedCode().replaceAll("GlobalObject.", "")).append("\n");
    }

    CandleGraph.write(new File(outDir, "__global.lua"), builder.toString());
  }

  void save() {
    this.graph.save(outDir);
  }

  public static void main(String[] yargs) {
    Candle candle = new Candle();
    candle.walk();
    candle.render(new EmmyLuaRenderer());
    candle.save();
  }
}
