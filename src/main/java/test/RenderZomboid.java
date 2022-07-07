package test;

import com.asledgehammer.typescript.TypeScriptCompiler;
import com.asledgehammer.typescript.settings.Recursion;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.type.*;
import com.asledgehammer.typescript.util.DocBuilder;
import fmod.fmod.EmitterType;
import fmod.fmod.FMODAudio;
import fmod.fmod.FMODSoundBank;
import fmod.fmod.FMODSoundEmitter;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjglx.input.Keyboard;
import se.krka.kahlua.vm.KahluaUtil;
import zombie.*;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.Lua.MapObjects;
import zombie.ai.GameCharacterAIBrain;
import zombie.ai.MapKnowledge;
import zombie.ai.sadisticAIDirector.SleepingEvent;
import zombie.ai.states.*;
import zombie.audio.*;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.*;
import zombie.characters.AttachedItems.*;
import zombie.characters.BodyDamage.*;
import zombie.characters.CharacterTimedActions.LuaTimedAction;
import zombie.characters.CharacterTimedActions.LuaTimedActionNew;
import zombie.characters.Moodles.Moodle;
import zombie.characters.Moodles.MoodleType;
import zombie.characters.Moodles.Moodles;
import zombie.characters.WornItems.*;
import zombie.characters.professions.ProfessionFactory;
import zombie.characters.skills.PerkFactory;
import zombie.characters.traits.ObservationFactory;
import zombie.characters.traits.TraitCollection;
import zombie.characters.traits.TraitFactory;
import zombie.chat.ChatBase;
import zombie.chat.ChatMessage;
import zombie.chat.ChatTab;
import zombie.chat.ServerChatMessage;
import zombie.config.*;
import zombie.core.*;
import zombie.core.fonts.AngelCodeFont;
import zombie.core.logger.ZLogger;
import zombie.core.math.PZMath;
import zombie.core.properties.PropertyContainer;
import zombie.core.skinnedmodel.advancedanimation.debug.AnimatorDebugMonitor;
import zombie.core.skinnedmodel.population.*;
import zombie.core.skinnedmodel.visual.HumanVisual;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.stash.Stash;
import zombie.core.stash.StashBuilding;
import zombie.core.stash.StashSystem;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.znet.SteamFriend;
import zombie.core.znet.SteamUGCDetails;
import zombie.core.znet.SteamWorkshopItem;
import zombie.debug.BooleanDebugOption;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.debug.DebugType;
import zombie.erosion.ErosionConfig;
import zombie.erosion.ErosionMain;
import zombie.erosion.season.ErosionSeason;
import zombie.gameStates.*;
import zombie.globalObjects.*;
import zombie.input.GameKeyboard;
import zombie.input.Mouse;
import zombie.inventory.*;
import zombie.inventory.types.*;
import zombie.iso.*;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.SpriteDetails.IsoObjectType;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.areas.IsoRoom;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.areas.SafeHouse;
import zombie.iso.areas.isoregion.IsoRegionLogType;
import zombie.iso.areas.isoregion.IsoRegions;
import zombie.iso.areas.isoregion.IsoRegionsLogger;
import zombie.iso.areas.isoregion.IsoRegionsRenderer;
import zombie.iso.areas.isoregion.data.DataCell;
import zombie.iso.areas.isoregion.data.DataChunk;
import zombie.iso.areas.isoregion.regions.IsoChunkRegion;
import zombie.iso.areas.isoregion.regions.IsoWorldRegion;
import zombie.iso.objects.*;
import zombie.iso.objects.interfaces.BarricadeAble;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteGrid;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.iso.weather.*;
import zombie.iso.weather.fog.ImprovedFog;
import zombie.iso.weather.fx.IsoWeatherFX;
import zombie.modding.ActiveMods;
import zombie.network.*;
import zombie.network.packets.hit.Vehicle;
import zombie.popman.ZombiePopulationManager;
import zombie.popman.ZombiePopulationRenderer;
import zombie.radio.ChannelCategory;
import zombie.radio.RadioAPI;
import zombie.radio.RadioData;
import zombie.radio.StorySounds.*;
import zombie.radio.ZomboidRadio;
import zombie.radio.devices.DeviceData;
import zombie.radio.devices.DevicePresets;
import zombie.radio.devices.PresetEntry;
import zombie.radio.media.MediaData;
import zombie.radio.media.RecordedMedia;
import zombie.radio.scripting.*;
import zombie.randomizedWorld.RandomizedWorldBase;
import zombie.randomizedWorld.randomizedBuilding.*;
import zombie.randomizedWorld.randomizedDeadSurvivor.*;
import zombie.randomizedWorld.randomizedVehicleStory.*;
import zombie.randomizedWorld.randomizedZoneStory.*;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.*;
import zombie.ui.*;
import zombie.util.PZCalendar;
import zombie.util.list.PZArrayList;
import zombie.vehicles.*;
import zombie.world.moddata.ModData;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings({"ResultOfMethodCallIgnored", "SpellCheckingInspection", "unused"})
public class RenderZomboid {

  private static final String[] LICENSE =
new String[] {"MIT License",
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

  private static final DateFormat dateFormat = new SimpleDateFormat("EEEEE MMMMM yyyy HH:mm:ss.SSSZ");
  private static final List<Class<?>> classes = new ArrayList<>();
  private final TypeScriptCompiler tsCompiler;
  private final File dirGenerated;

  public RenderZomboid() {

    File dirMain = new File("pipewrench");
    dirGenerated = new File(dirMain, "generated");
    if (!dirMain.exists()) dirMain.mkdirs();
    if (!dirGenerated.exists()) dirGenerated.mkdirs();

    GLInitializer.init();

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
    for (Class<?> clazz : classes) tsCompiler.add(clazz);

    tsCompiler.walk();
  }

  public void render() {
    renderZomboidAsMultiFile();
    renderLuaZomboid();
  }

  private String generateTSLicense() {
    Calendar calendar = Calendar.getInstance();
    DocBuilder docBuilder = new DocBuilder();
    for(String line : LICENSE) {
      docBuilder.appendLine(line.replaceAll("\\$YEAR\\$", "" + calendar.get(Calendar.YEAR)));
    }
    docBuilder.appendLine();
    docBuilder.appendLine("File generated at " + dateFormat.format(new Date()));
    return docBuilder.build("");
  }

  private String generateLuaLicense() {
    Calendar calendar = Calendar.getInstance();
    StringBuilder built = new StringBuilder();
    for(String line : LICENSE) {
      built.append("--- ").append(line.replaceAll("\\$YEAR\\$", "" + calendar.get(Calendar.YEAR))).append('\n');
    }
    built.append("---\n");
    built.append("--- File generated at ").append(dateFormat.format(new Date())).append('\n');
    return built.toString();
  }

  private void renderZomboidAsMultiFile() {

    Map<TypeScriptNamespace, String> compiledNamespaces = tsCompiler.compileNamespacesSeparately("  ");

    // Write all references to a file to refer to for all files.
    List<String> references = new ArrayList<>();
    references.add("/// <reference path=\"Zomboid_API.d.ts\" />\n");
    for(TypeScriptNamespace namespace : compiledNamespaces.keySet()) {
      String fileName = "Zomboid__" + namespace.getFullPath().replaceAll("\\.", "_") + ".d.ts";
      references.add("/// <reference path=\"" + fileName + "\" />\n");
    }

    references.sort(Comparator.naturalOrder());

    StringBuilder referenceBuilder = new StringBuilder();
    for(String s : references) {
      referenceBuilder.append(s);
    }

    String output = generateTSLicense() + "\n\n" + referenceBuilder;
    write(new File(dirGenerated, "Zomboid_References.d.ts"), output);

    for(TypeScriptNamespace namespace : compiledNamespaces.keySet()) {
      output = "/** @noResolution @noSelfInFile */\n";
      output += "/// <reference path=\"Zomboid_References.d.ts\" />\n";
      output += "declare module 'Zomboid' {\n";
      output += compiledNamespaces.get(namespace) + "\n";

      List<TypeScriptElement> elements = namespace.getAllGeneratedElements();
      List<String> knownNames = new ArrayList<>();
      List<TypeScriptElement> prunedElements = new ArrayList<>();
      for (int index = elements.size() - 1; index >= 0; index--) {
        TypeScriptElement element = elements.get(index);
        Class<?> clazz = element.getClazz();
        if (clazz == null) continue;
        String name = clazz.getSimpleName();
        if (name.contains("$")) {
          String[] split = name.split("\\$");
          name = split[split.length - 1];
        }
        if (!knownNames.contains(name)) {
          prunedElements.add(element);
          knownNames.add(name);
        }
      }
      prunedElements.sort(nameSorter);

      output += "}\n";

      String fileName = "Zomboid__" + namespace.getFullPath().replaceAll("\\.", "_") + ".d.ts";
      System.out.println("Writing file: " + fileName + "..");
      write(new File(dirGenerated, fileName), generateTSLicense() + "\n\n" + output);
    }

    String prepend = "/** @noResolution @noSelfInFile */\n";
    prepend += "/// <reference path=\"Zomboid_References.d.ts\" />\n";
    prepend += "declare module 'Zomboid' {\n";
    TypeScriptClass globalObject =
            (TypeScriptClass) tsCompiler.resolve(LuaManager.GlobalObject.class);

    List<TypeScriptElement> elements = tsCompiler.getAllGeneratedElements();
    List<String> knownNames = new ArrayList<>();
    List<TypeScriptElement> prunedElements = new ArrayList<>();

    for (int index = elements.size() - 1; index >= 0; index--) {
      TypeScriptElement element = elements.get(index);
      Class<?> clazz = element.getClazz();
      if (clazz == null) continue;
      String name = clazz.getSimpleName();
      if (name.contains("$")) {
        String[] split = name.split("\\$");
        name = split[split.length - 1];
      }
      if (!knownNames.contains(name)) {
        prunedElements.add(element);
        knownNames.add(name);
      }
    }

    prunedElements.sort(nameSorter);

    StringBuilder builderTypes = new StringBuilder();
    StringBuilder builderClasses = new StringBuilder();
    StringBuilder builderMethods = new StringBuilder();
    for (TypeScriptElement element : prunedElements) {

      String name = element.getClazz().getSimpleName();
      if (name.contains("$")) {
        String[] split = name.split("\\$");
        name = split[split.length - 1];
      }

      int genParams = element.getClazz().getTypeParameters().length;
      StringBuilder params = new StringBuilder();
      if(genParams != 0) {
        params.append("<");
        for(int x = 0; x < genParams; x++) {
          if(x == 0) {
            params.append("any");
          } else {
            params.append(", any");
          }
        }
        params.append(">");
      }

      String s;
      if(element instanceof TypeScriptType) {
        String fullPath = element.getClazz().getName();
        fullPath = fullPath.replaceAll(".function.", "._function_.");
        s = "  export type " + name + " = " + fullPath + params + '\n';
        builderTypes.append(s);
      } else {
        s = "  /** @customConstructor " + name + ".new */\n";
        s += "  export class " + name + " extends " + element.getClazz().getName() + params + " {}\n";
        builderClasses.append(s);
      }
    }

    Map<String, TypeScriptMethodCluster> methods = globalObject.getStaticMethods();
    List<String> methodNames = new ArrayList<>(methods.keySet());
    methodNames.sort(Comparator.naturalOrder());

    for (String methodName : methodNames) {
      TypeScriptMethodCluster method = methods.get(methodName);
      String s = method.compileTypeScriptFunction("  ") + '\n';
      builderMethods.append(s);
    }

    File fileZomboid = new File(dirGenerated, "Zomboid_API.d.ts");
    String content = prepend + builderClasses + '\n' + builderTypes + '\n' + builderMethods + "}\n";
    System.out.println("Writing file: Zomboid_API.d.ts..");
    write(fileZomboid, generateTSLicense() + "\n\n" + content);
  }

  private void renderZomboidAsOneFile() {
    String prepend = "/** @noResolution @noSelfInFile */\n";
    prepend += "declare module 'Zomboid' {";
    String output = tsCompiler.compile("  ");
    TypeScriptClass globalObject =
            (TypeScriptClass) tsCompiler.resolve(LuaManager.GlobalObject.class);

    List<TypeScriptElement> elements = tsCompiler.getAllGeneratedElements();
    List<String> knownNames = new ArrayList<>();
    List<TypeScriptElement> prunedElements = new ArrayList<>();

    for (int index = elements.size() - 1; index >= 0; index--) {
      TypeScriptElement element = elements.get(index);
      Class<?> clazz = element.getClazz();
      if (clazz == null) continue;
      String name = clazz.getSimpleName();
      if (name.contains("$")) {
        String[] split = name.split("\\$");
        name = split[split.length - 1];
      }
      if (!knownNames.contains(name)) {
        prunedElements.add(element);
        knownNames.add(name);
      }
    }

    prunedElements.sort(nameSorter);

    StringBuilder builderTypes = new StringBuilder();
    StringBuilder builderClasses = new StringBuilder();
    StringBuilder builderMethods = new StringBuilder();
    for (TypeScriptElement element : prunedElements) {

      String name = element.getClazz().getSimpleName();
      if (name.contains("$")) {
        String[] split = name.split("\\$");
        name = split[split.length - 1];
      }

      int genParams = element.getClazz().getTypeParameters().length;
      StringBuilder params = new StringBuilder();
      if(genParams != 0) {
        params.append("<");
        for(int x = 0; x < genParams; x++) {
          if(x == 0) {
            params.append("any");
          } else {
            params.append(", any");
          }
        }
        params.append(">");
      }

      String s;
      if(element instanceof TypeScriptType) {
        String fullPath = element.getClazz().getName();
        fullPath = fullPath.replaceAll(".function.", "._function_.");
        s = "  export type " + name + " = " + fullPath + params + '\n';
        builderTypes.append(s);
      } else {
        s = "  /** @customConstructor " + name + ".new */\n";
        s += "  export class " + name + " extends " + element.getClazz().getName() + params + " {}\n";
        builderClasses.append(s);
      }
    }

    Map<String, TypeScriptMethodCluster> methods = globalObject.getStaticMethods();
    List<String> methodNames = new ArrayList<>(methods.keySet());
    methodNames.sort(Comparator.naturalOrder());

    for (String methodName : methodNames) {
      TypeScriptMethodCluster method = methods.get(methodName);
      String s = method.compileTypeScriptFunction("  ") + '\n';
      builderMethods.append(s);
    }

    File fileZomboid = new File(dirGenerated, "Zomboid_API.d.ts");
    String content = prepend + '\n' + output + '\n' + builderClasses + '\n' + builderTypes + '\n' + builderMethods + "\n}";
    write(fileZomboid, generateTSLicense() + "\n\n" + content);
  }

  private void renderLuaZomboid() {

    List<TypeScriptElement> elements = tsCompiler.getAllGeneratedElements();
    elements.sort(nameSorter);

    String s =
            """
            local Exports = {}
            function Exports.tonumber(arg) return tonumber(arg) end
            function Exports.tostring(arg) return tostring(arg) end
            function Exports.global(id) return _G[id] end
            function Exports.loadstring(lua) return loadstring(lua) end
            function Exports.execute(lua) return loadstring(lua)() end
            function Exports.addEventListener(id, func) Events[id].Add(func) end
            function Exports.removeEventListener(id, func) Events[id].Add(func) end
            """;

    StringBuilder builder = new StringBuilder(s);
    builder.append(tsCompiler.resolve(LuaManager.GlobalObject.class).compileLua("Exports"));

    for (TypeScriptElement element : elements) {
      if (element instanceof TypeScriptClass || element instanceof TypeScriptEnum) {
        String name = element.name;
        if (name.contains("$")) {
          String[] split = name.split("\\$");
          name = split[split.length - 1];
        }
        String line = "Exports." + name + " = loadstring(\"return _G['" + name + "']\")()\n";
        builder.append(line);
      }
    }

    builder.append("return Exports\n");

    File fileZomboidLua = new File(dirGenerated, "Zomboid_API.lua");
    write(fileZomboidLua, generateLuaLicense() + "\n" + builder);
  }

  static {
    addClass(IsoPlayer.class);
    addClass(Vehicle.class);
    addClass(BaseVehicle.class);
    addClass(BufferedReader.class);
    addClass(BufferedWriter.class);
    addClass(DataInputStream.class);
    addClass(DataOutputStream.class);
    addClass(Math.class);
    addClass(SimpleDateFormat.class);
    addClass(ArrayList.class);
    addClass(EnumMap.class);
    addClass(HashMap.class);
    addClass(LinkedList.class);
    addClass(Stack.class);
    addClass(Vector.class);
    addClass(Iterator.class);
    addClass(EmitterType.class);
    addClass(FMODAudio.class);
    addClass(FMODSoundBank.class);
    addClass(FMODSoundEmitter.class);
    addClass(Vector2f.class);
    addClass(Vector3f.class);
    addClass(KahluaUtil.class);
    addClass(DummySoundBank.class);
    addClass(DummySoundEmitter.class);
    addClass(BaseSoundEmitter.class);
    addClass(GameSound.class);
    addClass(GameSoundClip.class);
    addClass(AttackState.class);
    addClass(BurntToDeath.class);
    addClass(ClimbDownSheetRopeState.class);
    addClass(ClimbOverFenceState.class);
    addClass(ClimbOverWallState.class);
    addClass(ClimbSheetRopeState.class);
    addClass(ClimbThroughWindowState.class);
    addClass(CloseWindowState.class);
    addClass(CrawlingZombieTurnState.class);
    addClass(FakeDeadAttackState.class);
    addClass(FakeDeadZombieState.class);
    addClass(FishingState.class);
    addClass(FitnessState.class);
    addClass(IdleState.class);
    addClass(LungeState.class);
    addClass(OpenWindowState.class);
    addClass(PathFindState.class);
    addClass(PlayerActionsState.class);
    addClass(PlayerAimState.class);
    addClass(PlayerEmoteState.class);
    addClass(PlayerExtState.class);
    addClass(PlayerFallDownState.class);
    addClass(PlayerFallingState.class);
    addClass(PlayerGetUpState.class);
    addClass(PlayerHitReactionPVPState.class);
    addClass(PlayerHitReactionState.class);
    addClass(PlayerKnockedDown.class);
    addClass(PlayerOnGroundState.class);
    addClass(PlayerSitOnGroundState.class);
    addClass(PlayerStrafeState.class);
    addClass(SmashWindowState.class);
    addClass(StaggerBackState.class);
    addClass(SwipeStatePlayer.class);
    addClass(ThumpState.class);
    addClass(WalkTowardState.class);
    addClass(ZombieFallDownState.class);
    addClass(ZombieGetDownState.class);
    addClass(ZombieGetUpState.class);
    addClass(ZombieIdleState.class);
    addClass(ZombieOnGroundState.class);
    addClass(ZombieReanimateState.class);
    addClass(ZombieSittingState.class);
    addClass(GameCharacterAIBrain.class);
    addClass(MapKnowledge.class);
    addClass(BodyPartType.class);
    addClass(BodyPart.class);
    addClass(BodyDamage.class);
    addClass(Thermoregulator.class);
    addClass(Thermoregulator.ThermalNode.class);
    addClass(Metabolics.class);
    addClass(Fitness.class);
    addClass(GameKeyboard.class);
    addClass(LuaTimedAction.class);
    addClass(LuaTimedActionNew.class);
    addClass(Moodle.class);
    addClass(Moodles.class);
    addClass(MoodleType.class);
    addClass(ProfessionFactory.class);
    addClass(ProfessionFactory.Profession.class);
    addClass(PerkFactory.class);
    addClass(PerkFactory.Perk.class);
    addClass(PerkFactory.Perks.class);
    addClass(ObservationFactory.class);
    addClass(ObservationFactory.Observation.class);
    addClass(TraitFactory.class);
    addClass(TraitFactory.Trait.class);
    addClass(IsoDummyCameraCharacter.class);
    addClass(Stats.class);
    addClass(SurvivorDesc.class);
    addClass(SurvivorFactory.class);
    addClass(SurvivorFactory.SurvivorType.class);
    addClass(IsoGameCharacter.class);
    addClass(IsoGameCharacter.PerkInfo.class);
    addClass(IsoGameCharacter.XP.class);
    addClass(IsoGameCharacter.CharacterTraits.class);
    addClass(TraitCollection.TraitSlot.class);
    addClass(TraitCollection.class);
    addClass(IsoPlayer.class);
    addClass(IsoSurvivor.class);
    addClass(IsoZombie.class);
    addClass(CharacterActionAnims.class);
    addClass(HaloTextHelper.class);
    addClass(HaloTextHelper.ColorRGB.class);
    addClass(NetworkAIParams.class);
    addClass(BloodBodyPartType.class);
    addClass(Clipboard.class);
    addClass(AngelCodeFont.class);
    addClass(ZLogger.class);
    addClass(PropertyContainer.class);
    addClass(ClothingItem.class);
    addClass(AnimatorDebugMonitor.class);
    addClass(ColorInfo.class);
    addClass(Texture.class);
    addClass(SteamFriend.class);
    addClass(SteamUGCDetails.class);
    addClass(SteamWorkshopItem.class);
    addClass(Color.class);
    addClass(Colors.class);
    addClass(Core.class);
    addClass(GameVersion.class);
    addClass(ImmutableColor.class);
    addClass(Language.class);
    addClass(PerformanceSettings.class);
    addClass(SpriteRenderer.class);
    addClass(Translator.class);
    addClass(PZMath.class);
    addClass(DebugLog.class);
    addClass(DebugOptions.class);
    addClass(BooleanDebugOption.class);
    addClass(DebugType.class);
    addClass(ErosionConfig.class);
    addClass(ErosionConfig.Debug.class);
    addClass(ErosionConfig.Season.class);
    addClass(ErosionConfig.Seeds.class);
    addClass(ErosionConfig.Time.class);
    addClass(ErosionMain.class);
    addClass(ErosionSeason.class);
    addClass(AnimationViewerState.class);
    addClass(AnimationViewerState.BooleanDebugOption.class);
    addClass(AttachmentEditorState.class);
    addClass(ChooseGameInfo.Mod.class);
    addClass(DebugChunkState.class);
    addClass(zombie.gameStates.DebugChunkState.BooleanDebugOption.class);
    addClass(DebugGlobalObjectState.class);
    addClass(GameLoadingState.class);
    addClass(LoadingQueueState.class);
    addClass(MainScreenState.class);
    addClass(CGlobalObject.class);
    addClass(CGlobalObjects.class);
    addClass(CGlobalObjectSystem.class);
    addClass(SGlobalObject.class);
    addClass(SGlobalObjects.class);
    addClass(SGlobalObjectSystem.class);
    addClass(Mouse.class);
    addClass(AlarmClock.class);
    addClass(AlarmClockClothing.class);
    addClass(Clothing.class);
    addClass(Clothing.ClothingPatch.class);
    addClass(Clothing.ClothingPatchFabricType.class);
    addClass(ComboItem.class);
    addClass(Drainable.class);
    addClass(DrainableComboItem.class);
    addClass(Food.class);
    addClass(HandWeapon.class);
    addClass(InventoryContainer.class);
    addClass(Key.class);
    addClass(KeyRing.class);
    addClass(Literature.class);
    addClass(MapItem.class);
    addClass(Moveable.class);
    addClass(Radio.class);
    addClass(WeaponPart.class);
    addClass(ItemContainer.class);
    addClass(ItemPickerJava.class);
    addClass(InventoryItem.class);
    addClass(InventoryItemFactory.class);
    addClass(FixingManager.class);
    addClass(RecipeManager.class);
    addClass(IsoRegions.class);
    addClass(IsoRegionsLogger.class);
    addClass(IsoRegionsLogger.IsoRegionLog.class);
    addClass(IsoRegionLogType.class);
    addClass(DataCell.class);
    addClass(DataChunk.class);
    addClass(IsoChunkRegion.class);
    addClass(IsoWorldRegion.class);
    addClass(IsoRegionsRenderer.class);
    addClass(zombie.iso.areas.isoregion.IsoRegionsRenderer.BooleanDebugOption.class);
    addClass(IsoBuilding.class);
    addClass(IsoRoom.class);
    addClass(SafeHouse.class);
    addClass(BarricadeAble.class);
    addClass(IsoBarbecue.class);
    addClass(IsoBarricade.class);
    addClass(IsoBrokenGlass.class);
    addClass(IsoClothingDryer.class);
    addClass(IsoClothingWasher.class);
    addClass(IsoCombinationWasherDryer.class);
    addClass(IsoStackedWasherDryer.class);
    addClass(IsoCurtain.class);
    addClass(IsoCarBatteryCharger.class);
    addClass(IsoDeadBody.class);
    addClass(IsoDoor.class);
    addClass(IsoFire.class);
    addClass(IsoFireManager.class);
    addClass(IsoFireplace.class);
    addClass(IsoGenerator.class);
    addClass(IsoJukebox.class);
    addClass(IsoLightSwitch.class);
    addClass(IsoMannequin.class);
    addClass(IsoMolotovCocktail.class);
    addClass(IsoWaveSignal.class);
    addClass(IsoRadio.class);
    addClass(IsoTelevision.class);
    addClass(IsoStackedWasherDryer.class);
    addClass(IsoStove.class);
    addClass(IsoThumpable.class);
    addClass(IsoTrap.class);
    addClass(IsoTree.class);
    addClass(IsoWheelieBin.class);
    addClass(IsoWindow.class);
    addClass(IsoWindowFrame.class);
    addClass(IsoWorldInventoryObject.class);
    addClass(IsoZombieGiblets.class);
    addClass(RainManager.class);
    addClass(ObjectRenderEffects.class);
    addClass(HumanVisual.class);
    addClass(ItemVisual.class);
    addClass(ItemVisuals.class);
    addClass(IsoSprite.class);
    addClass(IsoSpriteInstance.class);
    addClass(IsoSpriteManager.class);
    addClass(IsoSpriteGrid.class);
    addClass(IsoFlagType.class);
    addClass(IsoObjectType.class);
    addClass(ClimateManager.class);
    addClass(ClimateManager.DayInfo.class);
    addClass(ClimateManager.ClimateFloat.class);
    addClass(ClimateManager.ClimateColor.class);
    addClass(ClimateManager.ClimateBool.class);
    addClass(ClimateManager.AirFront.class);
    addClass(ClimateColorInfo.class);
    addClass(ClimateValues.class);
    addClass(ClimateForecaster.class);
    addClass(ClimateForecaster.DayForecast.class);
    addClass(ClimateForecaster.ForecastValue.class);
    addClass(ClimateHistory.class);
    addClass(ClimateMoon.class);
    addClass(WeatherPeriod.class);
    addClass(WeatherPeriod.WeatherStage.class);
    addClass(WeatherPeriod.StrLerpVal.class);
    addClass(ThunderStorm.class);
    addClass(ThunderStorm.ThunderCloud.class);
    addClass(IsoWeatherFX.class);
    addClass(Temperature.class);
    addClass(WorldFlares.class);
    addClass(WorldFlares.Flare.class);
    addClass(ImprovedFog.class);
    addClass(IsoPuddles.class);
    addClass(IsoPuddles.PuddlesFloat.class);
    addClass(BentFences.class);
    addClass(BrokenFences.class);
    addClass(ContainerOverlays.class);
    addClass(IsoChunk.class);
    addClass(BuildingDef.class);
    addClass(IsoCamera.class);
    addClass(IsoCell.class);
    addClass(IsoChunkMap.class);
    addClass(IsoDirections.class);
    addClass(IsoDirectionSet.class);
    addClass(IsoGridSquare.class);
    addClass(IsoHeatSource.class);
    addClass(IsoLightSource.class);
    addClass(IsoLot.class);
    addClass(IsoLuaMover.class);
    addClass(IsoMetaChunk.class);
    addClass(IsoMetaCell.class);
    addClass(IsoMetaGrid.class);
    addClass(IsoMetaGrid.Trigger.class);
    addClass(IsoMetaGrid.VehicleZone.class);
    addClass(IsoMetaGrid.Zone.class);
    addClass(IsoMovingObject.class);
    addClass(IsoObject.class);
    addClass(IsoObjectPicker.class);
    addClass(IsoPushableObject.class);
    addClass(IsoUtils.class);
    addClass(IsoWorld.class);
    addClass(LosUtil.class);
    addClass(MetaObject.class);
    addClass(RoomDef.class);
    addClass(SliceY.class);
    addClass(TileOverlays.class);
    addClass(Vector2.class);
    addClass(Vector3.class);
    addClass(WorldMarkers.class);
    addClass(WorldMarkers.DirectionArrow.class);
    addClass(WorldMarkers.GridSquareMarker.class);
    addClass(WorldMarkers.PlayerHomingPoint.class);
    addClass(SearchMode.class);
    addClass(SearchMode.PlayerSearchMode.class);
    addClass(SearchMode.SearchModeFloat.class);
    addClass(IsoMarkers.class);
    addClass(IsoMarkers.IsoMarker.class);
    addClass(IsoMarkers.CircleIsoMarker.class);
    addClass(LuaEventManager.class);
    addClass(MapObjects.class);
    addClass(ActiveMods.class);
    addClass(Server.class);
    addClass(ServerOptions.class);
    addClass(ServerOptions.BooleanServerOption.class);
    addClass(ServerOptions.DoubleServerOption.class);
    addClass(ServerOptions.IntegerServerOption.class);
    addClass(ServerOptions.StringServerOption.class);
    addClass(ServerOptions.TextServerOption.class);
    addClass(ServerSettings.class);
    addClass(ServerSettingsManager.class);
    addClass(ZombiePopulationManager.class);
    addClass(ZombiePopulationRenderer.BooleanDebugOption.class);
    addClass(RadioAPI.class);
    addClass(DeviceData.class);
    addClass(DevicePresets.class);
    addClass(PresetEntry.class);
    addClass(ZomboidRadio.class);
    addClass(RadioData.class);
    addClass(RadioScriptManager.class);
    addClass(DynamicRadioChannel.class);
    addClass(RadioChannel.class);
    addClass(RadioBroadCast.class);
    addClass(RadioLine.class);
    addClass(RadioScript.class);
    addClass(RadioScript.ExitOption.class);
    addClass(ChannelCategory.class);
    addClass(SLSoundManager.class);
    addClass(StorySound.class);
    addClass(StorySoundEvent.class);
    addClass(EventSound.class);
    addClass(DataPoint.class);
    addClass(RecordedMedia.class);
    addClass(MediaData.class);
    addClass(EvolvedRecipe.class);
    addClass(Fixing.class);
    addClass(Fixing.Fixer.class);
    addClass(Fixing.FixerSkill.class);
    addClass(GameSoundScript.class);
    addClass(Item.class);
    addClass(zombie.scripting.objects.Item.Type.class);
    addClass(ItemRecipe.class);
    addClass(ModelAttachment.class);
    addClass(ModelScript.class);
    addClass(MovableRecipe.class);
    addClass(Recipe.class);
    addClass(Recipe.RequiredSkill.class);
    addClass(Recipe.Result.class);
    addClass(Recipe.Source.class);
    addClass(ScriptModule.class);
    addClass(VehicleScript.class);
    addClass(VehicleScript.Area.class);
    addClass(VehicleScript.Model.class);
    addClass(VehicleScript.Part.class);
    addClass(VehicleScript.Passenger.class);
    addClass(VehicleScript.PhysicsShape.class);
    addClass(VehicleScript.Position.class);
    addClass(VehicleScript.Wheel.class);
    addClass(ScriptManager.class);
    addClass(ActionProgressBar.class);
    addClass(Clock.class);
    addClass(UIDebugConsole.class);
    addClass(ModalDialog.class);
    addClass(MoodlesUI.class);
    addClass(NewHealthPanel.class);
    addClass(ObjectTooltip.class);
    addClass(ObjectTooltip.Layout.class);
    addClass(ObjectTooltip.LayoutItem.class);
    addClass(RadarPanel.class);
    addClass(RadialMenu.class);
    addClass(RadialProgressBar.class);
    addClass(SpeedControls.class);
    addClass(TextManager.class);
    addClass(UI3DModel.class);
    addClass(UIElement.class);
    addClass(UIFont.class);
    addClass(UITransition.class);
    addClass(UIManager.class);
    addClass(UIServerToolbox.class);
    addClass(UITextBox2.class);
    addClass(VehicleGauge.class);
    addClass(TextDrawObject.class);
    addClass(PZArrayList.class);
    addClass(PZCalendar.class);
    addClass(BaseVehicle.class);
    addClass(EditVehicleState.class);
    addClass(PathFindBehavior2.BehaviorResult.class);
    addClass(PathFindBehavior2.class);
    addClass(PathFindState2.class);
    addClass(UI3DScene.class);
    addClass(VehicleDoor.class);
    addClass(VehicleLight.class);
    addClass(VehiclePart.class);
    addClass(VehicleType.class);
    addClass(VehicleWindow.class);
    addClass(AttachedItem.class);
    addClass(AttachedItems.class);
    addClass(AttachedLocation.class);
    addClass(AttachedLocationGroup.class);
    addClass(AttachedLocations.class);
    addClass(WornItems.class);
    addClass(WornItem.class);
    addClass(BodyLocation.class);
    addClass(BodyLocationGroup.class);
    addClass(BodyLocations.class);
    addClass(DummySoundManager.class);
    addClass(GameSounds.class);
    addClass(GameTime.class);
    addClass(GameWindow.class);
    addClass(SandboxOptions.class);
    addClass(SandboxOptions.BooleanSandboxOption.class);
    addClass(SandboxOptions.DoubleSandboxOption.class);
    addClass(SandboxOptions.StringSandboxOption.class);
    addClass(SandboxOptions.EnumSandboxOption.class);
    addClass(SandboxOptions.IntegerSandboxOption.class);
    addClass(SoundManager.class);
    addClass(SystemDisabler.class);
    addClass(VirtualZombieManager.class);
    addClass(WorldSoundManager.class);
    addClass(WorldSoundManager.WorldSound.class);
    addClass(DummyCharacterSoundEmitter.class);
    addClass(CharacterSoundEmitter.class);
    addClass(SoundManager.AmbientSoundEffect.class);
    addClass(BaseAmbientStreamManager.class);
    addClass(AmbientStreamManager.class);
    addClass(Nutrition.class);
    addClass(BSFurnace.class);
    addClass(MultiStageBuilding.class);
    addClass(MultiStageBuilding.Stage.class);
    addClass(SleepingEvent.class);
    addClass(IsoCompost.class);
    addClass(Userlog.class);
    addClass(Userlog.UserlogType.class);
    addClass(ConfigOption.class);
    addClass(BooleanConfigOption.class);
    addClass(DoubleConfigOption.class);
    addClass(EnumConfigOption.class);
    addClass(IntegerConfigOption.class);
    addClass(StringConfigOption.class);
    addClass(Faction.class);
    addClass(LuaManager.GlobalObject.LuaFileWriter.class);
    addClass(Keyboard.class);
    addClass(DBResult.class);
    addClass(NonPvpZone.class);
    addClass(DBTicket.class);
    addClass(StashSystem.class);
    addClass(StashBuilding.class);
    addClass(Stash.class);
    addClass(ItemType.class);
    addClass(RandomizedWorldBase.class);
    addClass(RandomizedBuildingBase.class);
    addClass(RBBurntFireman.class);
    addClass(RBBasic.class);
    addClass(RBBurnt.class);
    addClass(RBOther.class);
    addClass(RBStripclub.class);
    addClass(RBSchool.class);
    addClass(RBSpiffo.class);
    addClass(RBPizzaWhirled.class);
    addClass(RBOffice.class);
    addClass(RBHairSalon.class);
    addClass(RBClinic.class);
    addClass(RBPileOCrepe.class);
    addClass(RBCafe.class);
    addClass(RBBar.class);
    addClass(RBLooted.class);
    addClass(RBSafehouse.class);
    addClass(RBBurntCorpse.class);
    addClass(RBShopLooted.class);
    addClass(RBKateAndBaldspot.class);
    addClass(RandomizedDeadSurvivorBase.class);
    addClass(RDSZombiesEating.class);
    addClass(RDSBleach.class);
    addClass(RDSDeadDrunk.class);
    addClass(RDSGunmanInBathroom.class);
    addClass(RDSGunslinger.class);
    addClass(RDSZombieLockedBathroom.class);
    addClass(RDSBandPractice.class);
    addClass(RDSBathroomZed.class);
    addClass(RDSBedroomZed.class);
    addClass(RDSFootballNight.class);
    addClass(RDSHenDo.class);
    addClass(RDSStagDo.class);
    addClass(RDSStudentNight.class);
    addClass(RDSPokerNight.class);
    addClass(RDSSuicidePact.class);
    addClass(RDSPrisonEscape.class);
    addClass(RDSPrisonEscapeWithPolice.class);
    addClass(RDSSkeletonPsycho.class);
    addClass(RDSCorpsePsycho.class);
    addClass(RDSSpecificProfession.class);
    addClass(RDSPoliceAtHouse.class);
    addClass(RDSHouseParty.class);
    addClass(RDSTinFoilHat.class);
    addClass(RDSHockeyPsycho.class);
    addClass(RandomizedVehicleStoryBase.class);
    addClass(RVSCarCrash.class);
    addClass(RVSBanditRoad.class);
    addClass(RVSAmbulanceCrash.class);
    addClass(RVSCrashHorde.class);
    addClass(RVSCarCrashCorpse.class);
    addClass(RVSPoliceBlockade.class);
    addClass(RVSPoliceBlockadeShooting.class);
    addClass(RVSBurntCar.class);
    addClass(RVSConstructionSite.class);
    addClass(RVSUtilityVehicle.class);
    addClass(RVSChangingTire.class);
    addClass(RVSFlippedCrash.class);
    addClass(RVSTrailerCrash.class);
    addClass(RandomizedZoneStoryBase.class);
    addClass(RZSForestCamp.class);
    addClass(RZSForestCampEaten.class);
    addClass(RZSBuryingCamp.class);
    addClass(RZSBeachParty.class);
    addClass(RZSFishingTrip.class);
    addClass(RZSBBQParty.class);
    addClass(RZSHunterCamp.class);
    addClass(RZSSexyTime.class);
    addClass(RZSTrapperCamp.class);
    addClass(RZSBaseball.class);
    addClass(RZSMusicFestStage.class);
    addClass(RZSMusicFest.class);
    addClass(MapGroups.class);
    addClass(BeardStyles.class);
    addClass(BeardStyle.class);
    addClass(HairStyles.class);
    addClass(HairStyle.class);
    addClass(BloodClothingType.class);
    addClass(WeaponType.class);
    addClass(IsoWaterGeometry.class);
    addClass(ModData.class);
    addClass(WorldMarkers.class);
    addClass(ChatMessage.class);
    addClass(ChatBase.class);
    addClass(ChatTab.class);
    addClass(ServerChatMessage.class);
    addClass(ChatTab.class);
    addClass(LuaManager.GlobalObject.class);

    classes.sort(Comparator.comparing(Class::getSimpleName));
  }

  public static void main(String[] args) {
    new RenderZomboid().render();
  }

  private static void addClass(Class<?> clazz) {
    if (classes.contains(clazz)) return;
    classes.add(clazz);
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

  private static final Comparator<TypeScriptElement> nameSorter =
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
      };
}
