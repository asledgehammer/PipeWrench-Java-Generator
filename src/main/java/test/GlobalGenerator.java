package test;

import com.asledgehammer.typescript.TypeScriptCompiler;
import com.asledgehammer.typescript.settings.Recursion;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import zombie.network.PacketTypes;

import java.io.FileWriter;
import java.io.IOException;

public class GlobalGenerator {

  public static void main(String[] args) throws IOException {
    TypeScriptSettings settings = new TypeScriptSettings();
    settings.recursion = Recursion.NONE;

    TypeScriptCompiler compiler = new TypeScriptCompiler(settings);

    compiler.add(PacketTypes.PacketType.class);
    //        compiler.add(LuaManager.GlobalObject.class);
    compiler.walk();

    FileWriter writer = new FileWriter("globalobject.d.ts");
    writer.write(compiler.compile());
    writer.flush();
    writer.close();
  }
}
