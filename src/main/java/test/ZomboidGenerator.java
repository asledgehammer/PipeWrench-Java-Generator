package test;

import com.asledgehammer.typescript.TypeScriptCompiler;
import com.asledgehammer.typescript.settings.Recursion;
import com.asledgehammer.typescript.settings.TypeScriptSettings;
import com.asledgehammer.typescript.type.TypeScriptClass;
import com.asledgehammer.typescript.type.TypeScriptMethod;
import fmod.fmod.EmitterType;
import fmod.fmod.FMODAudio;
import fmod.fmod.FMODSoundBank;
import fmod.fmod.FMODSoundEmitter;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
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
import zombie.characters.AttachedItems.*;
import zombie.characters.BodyDamage.*;
import zombie.characters.*;
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
import java.lang.reflect.TypeVariable;
import java.nio.IntBuffer;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class ZomboidGenerator {

  private static final List<Class<?>> clazzes = new ArrayList<>();
  private static final File resourceDir = new File("src/main/resources");
  private static final File generatedDir = new File(resourceDir, "partials/generated");
  private static final File stitchedDir = new File(resourceDir, "stitched/generated");

  public static void main(String[] args) throws IOException {
    generateJava();
    generateGlobalAPI();
    generateNativeClassReferences();
  }

  // The window handle
  private static long window;

  private static void generateJava() throws IOException {
    TypeScriptSettings settings = new TypeScriptSettings();
    settings.recursion = Recursion.NONE;
    settings.readOnly = true;

    TypeScriptCompiler compiler = new TypeScriptCompiler(settings);
    for (Class<?> clazz : clazzes) {
      compiler.add(clazz);
    }

    init();
    compiler.walk();

    FileWriter writer = new FileWriter(new File(generatedDir, "java.d.ts"));
    writer.write("// [PARTIAL:START]\n");
    writer.write("export type KahluaTable = any;\n");
    writer.write(compiler.compile());
    writer.write("// [PARTIAL:STOP]\n");
    writer.flush();
    writer.close();
  }

  private static void generateGlobalAPI() throws IOException {
    TypeScriptSettings settings = new TypeScriptSettings();
    settings.recursion = Recursion.NONE;

    TypeScriptCompiler compiler = new TypeScriptCompiler(settings);

    compiler.add(LuaManager.GlobalObject.class);
    compiler.walk();

    FileWriter writer = new FileWriter(new File(generatedDir, "globalobject.d.ts"));
    writer.write("// [PARTIAL:START]\n");

    TypeScriptClass globalObject =
        (TypeScriptClass) compiler.resolve(LuaManager.GlobalObject.class);

    Map<String, TypeScriptMethod> methods = globalObject.getMethods();
    List<String> methodNames = new ArrayList<>(methods.keySet());
    methodNames.sort(Comparator.naturalOrder());

    for (String methodName : methodNames) {
      TypeScriptMethod method = methods.get(methodName);
      writer.write("export function " + method.compile("") + '\n');
    }

    writer.write("// [PARTIAL:STOP]\n");
    writer.flush();
    writer.close();

    writer = new FileWriter(new File(generatedDir, "globalobject.lua"));
    writer.write("local Exports = {}\n\n-- [PARTIAL:START]\n");
    writer.write(compiler.resolve(LuaManager.GlobalObject.class).compileLua("Exports"));
    writer.write("-- [PARTIAL:STOP]\n\nreturn Exports\n");
    writer.flush();
    writer.close();
  }

  // Fix for LWJGL environment-required classes.
  public static void generateNativeClassReferences() throws IOException {
    StringBuilder string = new StringBuilder();
    string.append("import { java, se, zombie } from \"./java\";\n\n");
    string.append("// [PARTIAL:START]\n");

    List<Class<?>> known = new ArrayList<>();
    for (Class<?> clazz : clazzes) {
      if (known.contains(clazz)) continue;
      else known.add(clazz);

      String className = clazz.getSimpleName();
      TypeVariable<?>[] vars = clazz.getTypeParameters();
      StringBuilder varsName = new StringBuilder();
      if (vars.length != 0) {
        varsName.append("<");
        for (TypeVariable<?> var : vars) {
          varsName.append("any, ");
        }
        varsName = new StringBuilder(varsName.substring(0, varsName.length() - 2) + ">");
      }

      string
          .append("export const ")
          .append(className)
          .append(": ")
          .append(clazz.getName())
          .append(varsName)
          .append(";\n");
    }

    string.append("// [PARTIAL:STOP]\n");
    // Commented to prevent overwriting.
    FileWriter writer = new FileWriter(new File(generatedDir, "class_vars.d.ts"));
    writer.write(string.toString());
    writer.flush();
    writer.close();
  }

  private static void init() {
    // Setup an error callback. The default implementation
    // will print the error message in System.err.
    GLFWErrorCallback.createPrint(System.err).set();

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

    // Configure GLFW
    glfwDefaultWindowHints(); // optional, the current window hints are already the default
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

    // Create the window
    window = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL);
    if (window == NULL) throw new RuntimeException("Failed to create the GLFW window");

    //    // Setup a key callback. It will be called every time a key is pressed, repeated or
    // released.
    //    glfwSetKeyCallback(
    //        window,
    //        (window, key, scancode, action, mods) -> {
    //          if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
    //            glfwSetWindowShouldClose(window, true); // We will detect this in the rendering
    // loop
    //        });

    // Get the thread stack and push a new frame
    try (MemoryStack stack = stackPush()) {
      IntBuffer pWidth = stack.mallocInt(1); // int*
      IntBuffer pHeight = stack.mallocInt(1); // int*

      // Get the window size passed to glfwCreateWindow
      glfwGetWindowSize(window, pWidth, pHeight);

      // Get the resolution of the primary monitor
      GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

      // Center the window
      glfwSetWindowPos(
          window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
    } // the stack frame is popped automatically

    // Make the OpenGL context current
    glfwMakeContextCurrent(window);
    // Enable v-sync
    glfwSwapInterval(1);

    // Make the window visible
    //    glfwShowWindow(window);

    GL.createCapabilities();
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

    clazzes.add(IsoPlayer.class);
    clazzes.add(Vehicle.class);
    clazzes.add(BaseVehicle.class);
    clazzes.add(BufferedReader.class);
    clazzes.add(BufferedWriter.class);
    clazzes.add(DataInputStream.class);
    clazzes.add(DataOutputStream.class);
    clazzes.add(Math.class);
    clazzes.add(SimpleDateFormat.class);
    clazzes.add(ArrayList.class);
    clazzes.add(EnumMap.class);
    clazzes.add(HashMap.class);
    clazzes.add(LinkedList.class);
    clazzes.add(Stack.class);
    clazzes.add(Vector.class);
    clazzes.add(Iterator.class);
    clazzes.add(EmitterType.class);
    clazzes.add(FMODAudio.class);
    clazzes.add(FMODSoundBank.class);
    clazzes.add(FMODSoundEmitter.class);
    clazzes.add(Vector2f.class);
    clazzes.add(Vector3f.class);
    clazzes.add(KahluaUtil.class);
    clazzes.add(DummySoundBank.class);
    clazzes.add(DummySoundEmitter.class);
    clazzes.add(BaseSoundEmitter.class);
    clazzes.add(GameSound.class);
    clazzes.add(GameSoundClip.class);
    clazzes.add(AttackState.class);
    clazzes.add(BurntToDeath.class);
    clazzes.add(ClimbDownSheetRopeState.class);
    clazzes.add(ClimbOverFenceState.class);
    clazzes.add(ClimbOverWallState.class);
    clazzes.add(ClimbSheetRopeState.class);
    clazzes.add(ClimbThroughWindowState.class);
    clazzes.add(CloseWindowState.class);
    clazzes.add(CrawlingZombieTurnState.class);
    clazzes.add(FakeDeadAttackState.class);
    clazzes.add(FakeDeadZombieState.class);
    clazzes.add(FishingState.class);
    clazzes.add(FitnessState.class);
    clazzes.add(IdleState.class);
    clazzes.add(LungeState.class);
    clazzes.add(OpenWindowState.class);
    clazzes.add(PathFindState.class);
    clazzes.add(PlayerActionsState.class);
    clazzes.add(PlayerAimState.class);
    clazzes.add(PlayerEmoteState.class);
    clazzes.add(PlayerExtState.class);
    clazzes.add(PlayerFallDownState.class);
    clazzes.add(PlayerFallingState.class);
    clazzes.add(PlayerGetUpState.class);
    clazzes.add(PlayerHitReactionPVPState.class);
    clazzes.add(PlayerHitReactionState.class);
    clazzes.add(PlayerKnockedDown.class);
    clazzes.add(PlayerOnGroundState.class);
    clazzes.add(PlayerSitOnGroundState.class);
    clazzes.add(PlayerStrafeState.class);
    clazzes.add(SmashWindowState.class);
    clazzes.add(StaggerBackState.class);
    clazzes.add(SwipeStatePlayer.class);
    clazzes.add(ThumpState.class);
    clazzes.add(WalkTowardState.class);
    clazzes.add(ZombieFallDownState.class);
    clazzes.add(ZombieGetDownState.class);
    clazzes.add(ZombieGetUpState.class);
    clazzes.add(ZombieIdleState.class);
    clazzes.add(ZombieOnGroundState.class);
    clazzes.add(ZombieReanimateState.class);
    clazzes.add(ZombieSittingState.class);
    clazzes.add(GameCharacterAIBrain.class);
    clazzes.add(MapKnowledge.class);
    clazzes.add(BodyPartType.class);
    clazzes.add(BodyPart.class);
    clazzes.add(BodyDamage.class);
    clazzes.add(Thermoregulator.class);
    clazzes.add(Thermoregulator.ThermalNode.class);
    clazzes.add(Metabolics.class);
    clazzes.add(Fitness.class);
    clazzes.add(GameKeyboard.class);
    clazzes.add(LuaTimedAction.class);
    clazzes.add(LuaTimedActionNew.class);
    clazzes.add(Moodle.class);
    clazzes.add(Moodles.class);
    clazzes.add(MoodleType.class);
    clazzes.add(ProfessionFactory.class);
    clazzes.add(ProfessionFactory.Profession.class);
    clazzes.add(PerkFactory.class);
    clazzes.add(PerkFactory.Perk.class);
    clazzes.add(PerkFactory.Perks.class);
    clazzes.add(ObservationFactory.class);
    clazzes.add(ObservationFactory.Observation.class);
    clazzes.add(TraitFactory.class);
    clazzes.add(TraitFactory.Trait.class);
    clazzes.add(IsoDummyCameraCharacter.class);
    clazzes.add(Stats.class);
    clazzes.add(SurvivorDesc.class);
    clazzes.add(SurvivorFactory.class);
    clazzes.add(SurvivorFactory.SurvivorType.class);
    clazzes.add(IsoGameCharacter.class);
    clazzes.add(IsoGameCharacter.PerkInfo.class);
    clazzes.add(IsoGameCharacter.XP.class);
    clazzes.add(IsoGameCharacter.CharacterTraits.class);
    clazzes.add(TraitCollection.TraitSlot.class);
    clazzes.add(TraitCollection.class);
    clazzes.add(IsoPlayer.class);
    clazzes.add(IsoSurvivor.class);
    clazzes.add(IsoZombie.class);
    clazzes.add(CharacterActionAnims.class);
    clazzes.add(HaloTextHelper.class);
    clazzes.add(HaloTextHelper.ColorRGB.class);
    clazzes.add(NetworkAIParams.class);
    clazzes.add(BloodBodyPartType.class);
    clazzes.add(Clipboard.class);
    clazzes.add(AngelCodeFont.class);
    clazzes.add(ZLogger.class);
    clazzes.add(PropertyContainer.class);
    clazzes.add(ClothingItem.class);
    clazzes.add(AnimatorDebugMonitor.class);
    clazzes.add(ColorInfo.class);
    clazzes.add(Texture.class);
    clazzes.add(SteamFriend.class);
    clazzes.add(SteamUGCDetails.class);
    clazzes.add(SteamWorkshopItem.class);
    clazzes.add(Color.class);
    clazzes.add(Colors.class);
    clazzes.add(Core.class);
    clazzes.add(GameVersion.class);
    clazzes.add(ImmutableColor.class);
    clazzes.add(Language.class);
    clazzes.add(PerformanceSettings.class);
    clazzes.add(SpriteRenderer.class);
    clazzes.add(Translator.class);
    clazzes.add(PZMath.class);
    clazzes.add(DebugLog.class);
    clazzes.add(DebugOptions.class);
    clazzes.add(BooleanDebugOption.class);
    clazzes.add(DebugType.class);
    clazzes.add(ErosionConfig.class);
    clazzes.add(ErosionConfig.Debug.class);
    clazzes.add(ErosionConfig.Season.class);
    clazzes.add(ErosionConfig.Seeds.class);
    clazzes.add(ErosionConfig.Time.class);
    clazzes.add(ErosionMain.class);
    clazzes.add(ErosionSeason.class);
    clazzes.add(AnimationViewerState.class);
    clazzes.add(AnimationViewerState.BooleanDebugOption.class);
    clazzes.add(AttachmentEditorState.class);
    clazzes.add(ChooseGameInfo.Mod.class);
    clazzes.add(DebugChunkState.class);
    clazzes.add(zombie.gameStates.DebugChunkState.BooleanDebugOption.class);
    clazzes.add(DebugGlobalObjectState.class);
    clazzes.add(GameLoadingState.class);
    clazzes.add(LoadingQueueState.class);
    clazzes.add(MainScreenState.class);
    clazzes.add(CGlobalObject.class);
    clazzes.add(CGlobalObjects.class);
    clazzes.add(CGlobalObjectSystem.class);
    clazzes.add(SGlobalObject.class);
    clazzes.add(SGlobalObjects.class);
    clazzes.add(SGlobalObjectSystem.class);
    clazzes.add(Mouse.class);
    clazzes.add(AlarmClock.class);
    clazzes.add(AlarmClockClothing.class);
    clazzes.add(Clothing.class);
    clazzes.add(Clothing.ClothingPatch.class);
    clazzes.add(Clothing.ClothingPatchFabricType.class);
    clazzes.add(ComboItem.class);
    clazzes.add(Drainable.class);
    clazzes.add(DrainableComboItem.class);
    clazzes.add(Food.class);
    clazzes.add(HandWeapon.class);
    clazzes.add(InventoryContainer.class);
    clazzes.add(Key.class);
    clazzes.add(KeyRing.class);
    clazzes.add(Literature.class);
    clazzes.add(MapItem.class);
    clazzes.add(Moveable.class);
    clazzes.add(Radio.class);
    clazzes.add(WeaponPart.class);
    clazzes.add(ItemContainer.class);
    clazzes.add(ItemPickerJava.class);
    clazzes.add(InventoryItem.class);
    clazzes.add(InventoryItemFactory.class);
    clazzes.add(FixingManager.class);
    clazzes.add(RecipeManager.class);
    clazzes.add(IsoRegions.class);
    clazzes.add(IsoRegionsLogger.class);
    clazzes.add(IsoRegionsLogger.IsoRegionLog.class);
    clazzes.add(IsoRegionLogType.class);
    clazzes.add(DataCell.class);
    clazzes.add(DataChunk.class);
    clazzes.add(IsoChunkRegion.class);
    clazzes.add(IsoWorldRegion.class);
    clazzes.add(IsoRegionsRenderer.class);
    clazzes.add(zombie.iso.areas.isoregion.IsoRegionsRenderer.BooleanDebugOption.class);
    clazzes.add(IsoBuilding.class);
    clazzes.add(IsoRoom.class);
    clazzes.add(SafeHouse.class);
    clazzes.add(BarricadeAble.class);
    clazzes.add(IsoBarbecue.class);
    clazzes.add(IsoBarricade.class);
    clazzes.add(IsoBrokenGlass.class);
    clazzes.add(IsoClothingDryer.class);
    clazzes.add(IsoClothingWasher.class);
    clazzes.add(IsoCombinationWasherDryer.class);
    clazzes.add(IsoStackedWasherDryer.class);
    clazzes.add(IsoCurtain.class);
    clazzes.add(IsoCarBatteryCharger.class);
    clazzes.add(IsoDeadBody.class);
    clazzes.add(IsoDoor.class);
    clazzes.add(IsoFire.class);
    clazzes.add(IsoFireManager.class);
    clazzes.add(IsoFireplace.class);
    clazzes.add(IsoGenerator.class);
    clazzes.add(IsoJukebox.class);
    clazzes.add(IsoLightSwitch.class);
    clazzes.add(IsoMannequin.class);
    clazzes.add(IsoMolotovCocktail.class);
    clazzes.add(IsoWaveSignal.class);
    clazzes.add(IsoRadio.class);
    clazzes.add(IsoTelevision.class);
    clazzes.add(IsoStackedWasherDryer.class);
    clazzes.add(IsoStove.class);
    clazzes.add(IsoThumpable.class);
    clazzes.add(IsoTrap.class);
    clazzes.add(IsoTree.class);
    clazzes.add(IsoWheelieBin.class);
    clazzes.add(IsoWindow.class);
    clazzes.add(IsoWindowFrame.class);
    clazzes.add(IsoWorldInventoryObject.class);
    clazzes.add(IsoZombieGiblets.class);
    clazzes.add(RainManager.class);
    clazzes.add(ObjectRenderEffects.class);
    clazzes.add(HumanVisual.class);
    clazzes.add(ItemVisual.class);
    clazzes.add(ItemVisuals.class);
    clazzes.add(IsoSprite.class);
    clazzes.add(IsoSpriteInstance.class);
    clazzes.add(IsoSpriteManager.class);
    clazzes.add(IsoSpriteGrid.class);
    clazzes.add(IsoFlagType.class);
    clazzes.add(IsoObjectType.class);
    clazzes.add(ClimateManager.class);
    clazzes.add(ClimateManager.DayInfo.class);
    clazzes.add(ClimateManager.ClimateFloat.class);
    clazzes.add(ClimateManager.ClimateColor.class);
    clazzes.add(ClimateManager.ClimateBool.class);
    clazzes.add(WeatherPeriod.class);
    clazzes.add(WeatherPeriod.WeatherStage.class);
    clazzes.add(WeatherPeriod.StrLerpVal.class);
    clazzes.add(ClimateManager.AirFront.class);
    clazzes.add(ThunderStorm.class);
    clazzes.add(ThunderStorm.ThunderCloud.class);
    clazzes.add(IsoWeatherFX.class);
    clazzes.add(Temperature.class);
    clazzes.add(ClimateColorInfo.class);
    clazzes.add(ClimateValues.class);
    clazzes.add(ClimateForecaster.class);
    clazzes.add(ClimateForecaster.DayForecast.class);
    clazzes.add(ClimateForecaster.ForecastValue.class);
    clazzes.add(ClimateHistory.class);
    clazzes.add(WorldFlares.class);
    clazzes.add(WorldFlares.Flare.class);
    clazzes.add(ImprovedFog.class);
    clazzes.add(ClimateMoon.class);
    clazzes.add(IsoPuddles.class);
    clazzes.add(IsoPuddles.PuddlesFloat.class);
    clazzes.add(BentFences.class);
    clazzes.add(BrokenFences.class);
    clazzes.add(ContainerOverlays.class);
    clazzes.add(IsoChunk.class);
    clazzes.add(BuildingDef.class);
    clazzes.add(IsoCamera.class);
    clazzes.add(IsoCell.class);
    clazzes.add(IsoChunkMap.class);
    clazzes.add(IsoDirections.class);
    clazzes.add(IsoDirectionSet.class);
    clazzes.add(IsoGridSquare.class);
    clazzes.add(IsoHeatSource.class);
    clazzes.add(IsoLightSource.class);
    clazzes.add(IsoLot.class);
    clazzes.add(IsoLuaMover.class);
    clazzes.add(IsoMetaChunk.class);
    clazzes.add(IsoMetaCell.class);
    clazzes.add(IsoMetaGrid.class);
    clazzes.add(IsoMetaGrid.Trigger.class);
    clazzes.add(IsoMetaGrid.VehicleZone.class);
    clazzes.add(IsoMetaGrid.Zone.class);
    clazzes.add(IsoMovingObject.class);
    clazzes.add(IsoObject.class);
    clazzes.add(IsoObjectPicker.class);
    clazzes.add(IsoPushableObject.class);
    clazzes.add(IsoUtils.class);
    clazzes.add(IsoWorld.class);
    clazzes.add(LosUtil.class);
    clazzes.add(MetaObject.class);
    clazzes.add(RoomDef.class);
    clazzes.add(SliceY.class);
    clazzes.add(TileOverlays.class);
    clazzes.add(Vector2.class);
    clazzes.add(Vector3.class);
    clazzes.add(WorldMarkers.class);
    clazzes.add(WorldMarkers.DirectionArrow.class);
    clazzes.add(WorldMarkers.GridSquareMarker.class);
    clazzes.add(WorldMarkers.PlayerHomingPoint.class);
    clazzes.add(SearchMode.class);
    clazzes.add(SearchMode.PlayerSearchMode.class);
    clazzes.add(SearchMode.SearchModeFloat.class);
    clazzes.add(IsoMarkers.class);
    clazzes.add(IsoMarkers.IsoMarker.class);
    clazzes.add(IsoMarkers.CircleIsoMarker.class);
    clazzes.add(LuaEventManager.class);
    clazzes.add(MapObjects.class);
    clazzes.add(ActiveMods.class);
    clazzes.add(Server.class);
    clazzes.add(ServerOptions.class);
    clazzes.add(ServerOptions.BooleanServerOption.class);
    clazzes.add(ServerOptions.DoubleServerOption.class);
    clazzes.add(ServerOptions.IntegerServerOption.class);
    clazzes.add(ServerOptions.StringServerOption.class);
    clazzes.add(ServerOptions.TextServerOption.class);
    clazzes.add(ServerSettings.class);
    clazzes.add(ServerSettingsManager.class);
    clazzes.add(ZombiePopulationManager.class);
    clazzes.add(ZombiePopulationRenderer.BooleanDebugOption.class);
    clazzes.add(RadioAPI.class);
    clazzes.add(DeviceData.class);
    clazzes.add(DevicePresets.class);
    clazzes.add(PresetEntry.class);
    clazzes.add(ZomboidRadio.class);
    clazzes.add(RadioData.class);
    clazzes.add(RadioScriptManager.class);
    clazzes.add(DynamicRadioChannel.class);
    clazzes.add(RadioChannel.class);
    clazzes.add(RadioBroadCast.class);
    clazzes.add(RadioLine.class);
    clazzes.add(RadioScript.class);
    clazzes.add(RadioScript.ExitOption.class);
    clazzes.add(ChannelCategory.class);
    clazzes.add(SLSoundManager.class);
    clazzes.add(StorySound.class);
    clazzes.add(StorySoundEvent.class);
    clazzes.add(EventSound.class);
    clazzes.add(DataPoint.class);
    clazzes.add(RecordedMedia.class);
    clazzes.add(MediaData.class);
    clazzes.add(EvolvedRecipe.class);
    clazzes.add(Fixing.class);
    clazzes.add(Fixing.Fixer.class);
    clazzes.add(Fixing.FixerSkill.class);
    clazzes.add(GameSoundScript.class);
    clazzes.add(Item.class);
    clazzes.add(zombie.scripting.objects.Item.Type.class);
    clazzes.add(ItemRecipe.class);
    clazzes.add(ModelAttachment.class);
    clazzes.add(ModelScript.class);
    clazzes.add(MovableRecipe.class);
    clazzes.add(Recipe.class);
    clazzes.add(Recipe.RequiredSkill.class);
    clazzes.add(Recipe.Result.class);
    clazzes.add(Recipe.Source.class);
    clazzes.add(ScriptModule.class);
    clazzes.add(VehicleScript.class);
    clazzes.add(VehicleScript.Area.class);
    clazzes.add(VehicleScript.Model.class);
    clazzes.add(VehicleScript.Part.class);
    clazzes.add(VehicleScript.Passenger.class);
    clazzes.add(VehicleScript.PhysicsShape.class);
    clazzes.add(VehicleScript.Position.class);
    clazzes.add(VehicleScript.Wheel.class);
    clazzes.add(ScriptManager.class);
    clazzes.add(ActionProgressBar.class);
    clazzes.add(Clock.class);
    clazzes.add(UIDebugConsole.class);
    clazzes.add(ModalDialog.class);
    clazzes.add(MoodlesUI.class);
    clazzes.add(NewHealthPanel.class);
    clazzes.add(ObjectTooltip.class);
    clazzes.add(ObjectTooltip.Layout.class);
    clazzes.add(ObjectTooltip.LayoutItem.class);
    clazzes.add(RadarPanel.class);
    clazzes.add(RadialMenu.class);
    clazzes.add(RadialProgressBar.class);
    clazzes.add(SpeedControls.class);
    clazzes.add(TextManager.class);
    clazzes.add(UI3DModel.class);
    clazzes.add(UIElement.class);
    clazzes.add(UIFont.class);
    clazzes.add(UITransition.class);
    clazzes.add(UIManager.class);
    clazzes.add(UIServerToolbox.class);
    clazzes.add(UITextBox2.class);
    clazzes.add(VehicleGauge.class);
    clazzes.add(TextDrawObject.class);
    clazzes.add(PZArrayList.class);
    clazzes.add(PZCalendar.class);
    clazzes.add(BaseVehicle.class);
    clazzes.add(EditVehicleState.class);
    clazzes.add(PathFindBehavior2.BehaviorResult.class);
    clazzes.add(PathFindBehavior2.class);
    clazzes.add(PathFindState2.class);
    clazzes.add(UI3DScene.class);
    clazzes.add(VehicleDoor.class);
    clazzes.add(VehicleLight.class);
    clazzes.add(VehiclePart.class);
    clazzes.add(VehicleType.class);
    clazzes.add(VehicleWindow.class);
    clazzes.add(AttachedItem.class);
    clazzes.add(AttachedItems.class);
    clazzes.add(AttachedLocation.class);
    clazzes.add(AttachedLocationGroup.class);
    clazzes.add(AttachedLocations.class);
    clazzes.add(WornItems.class);
    clazzes.add(WornItem.class);
    clazzes.add(BodyLocation.class);
    clazzes.add(BodyLocationGroup.class);
    clazzes.add(BodyLocations.class);
    clazzes.add(DummySoundManager.class);
    clazzes.add(GameSounds.class);
    clazzes.add(GameTime.class);
    clazzes.add(GameWindow.class);
    clazzes.add(SandboxOptions.class);
    clazzes.add(SandboxOptions.BooleanSandboxOption.class);
    clazzes.add(SandboxOptions.DoubleSandboxOption.class);
    clazzes.add(SandboxOptions.StringSandboxOption.class);
    clazzes.add(SandboxOptions.EnumSandboxOption.class);
    clazzes.add(SandboxOptions.IntegerSandboxOption.class);
    clazzes.add(SoundManager.class);
    clazzes.add(SystemDisabler.class);
    clazzes.add(VirtualZombieManager.class);
    clazzes.add(WorldSoundManager.class);
    clazzes.add(WorldSoundManager.WorldSound.class);
    clazzes.add(DummyCharacterSoundEmitter.class);
    clazzes.add(CharacterSoundEmitter.class);
    clazzes.add(SoundManager.AmbientSoundEffect.class);
    clazzes.add(BaseAmbientStreamManager.class);
    clazzes.add(AmbientStreamManager.class);
    clazzes.add(Nutrition.class);
    clazzes.add(BSFurnace.class);
    clazzes.add(MultiStageBuilding.class);
    clazzes.add(MultiStageBuilding.Stage.class);
    clazzes.add(SleepingEvent.class);
    clazzes.add(IsoCompost.class);
    clazzes.add(Userlog.class);
    clazzes.add(Userlog.UserlogType.class);
    clazzes.add(ConfigOption.class);
    clazzes.add(BooleanConfigOption.class);
    clazzes.add(DoubleConfigOption.class);
    clazzes.add(EnumConfigOption.class);
    clazzes.add(IntegerConfigOption.class);
    clazzes.add(StringConfigOption.class);
    clazzes.add(Faction.class);
    clazzes.add(LuaManager.GlobalObject.LuaFileWriter.class);
    clazzes.add(Keyboard.class);
    clazzes.add(DBResult.class);
    clazzes.add(NonPvpZone.class);
    clazzes.add(DBTicket.class);
    clazzes.add(StashSystem.class);
    clazzes.add(StashBuilding.class);
    clazzes.add(Stash.class);
    clazzes.add(ItemType.class);
    clazzes.add(RandomizedWorldBase.class);
    clazzes.add(RandomizedBuildingBase.class);
    clazzes.add(RBBurntFireman.class);
    clazzes.add(RBBasic.class);
    clazzes.add(RBBurnt.class);
    clazzes.add(RBOther.class);
    clazzes.add(RBStripclub.class);
    clazzes.add(RBSchool.class);
    clazzes.add(RBSpiffo.class);
    clazzes.add(RBPizzaWhirled.class);
    clazzes.add(RBOffice.class);
    clazzes.add(RBHairSalon.class);
    clazzes.add(RBClinic.class);
    clazzes.add(RBPileOCrepe.class);
    clazzes.add(RBCafe.class);
    clazzes.add(RBBar.class);
    clazzes.add(RBLooted.class);
    clazzes.add(RBSafehouse.class);
    clazzes.add(RBBurntCorpse.class);
    clazzes.add(RBShopLooted.class);
    clazzes.add(RBKateAndBaldspot.class);
    clazzes.add(RandomizedDeadSurvivorBase.class);
    clazzes.add(RDSZombiesEating.class);
    clazzes.add(RDSBleach.class);
    clazzes.add(RDSDeadDrunk.class);
    clazzes.add(RDSGunmanInBathroom.class);
    clazzes.add(RDSGunslinger.class);
    clazzes.add(RDSZombieLockedBathroom.class);
    clazzes.add(RDSBandPractice.class);
    clazzes.add(RDSBathroomZed.class);
    clazzes.add(RDSBedroomZed.class);
    clazzes.add(RDSFootballNight.class);
    clazzes.add(RDSHenDo.class);
    clazzes.add(RDSStagDo.class);
    clazzes.add(RDSStudentNight.class);
    clazzes.add(RDSPokerNight.class);
    clazzes.add(RDSSuicidePact.class);
    clazzes.add(RDSPrisonEscape.class);
    clazzes.add(RDSPrisonEscapeWithPolice.class);
    clazzes.add(RDSSkeletonPsycho.class);
    clazzes.add(RDSCorpsePsycho.class);
    clazzes.add(RDSSpecificProfession.class);
    clazzes.add(RDSPoliceAtHouse.class);
    clazzes.add(RDSHouseParty.class);
    clazzes.add(RDSTinFoilHat.class);
    clazzes.add(RDSHockeyPsycho.class);
    clazzes.add(RandomizedVehicleStoryBase.class);
    clazzes.add(RVSCarCrash.class);
    clazzes.add(RVSBanditRoad.class);
    clazzes.add(RVSAmbulanceCrash.class);
    clazzes.add(RVSCrashHorde.class);
    clazzes.add(RVSCarCrashCorpse.class);
    clazzes.add(RVSPoliceBlockade.class);
    clazzes.add(RVSPoliceBlockadeShooting.class);
    clazzes.add(RVSBurntCar.class);
    clazzes.add(RVSConstructionSite.class);
    clazzes.add(RVSUtilityVehicle.class);
    clazzes.add(RVSChangingTire.class);
    clazzes.add(RVSFlippedCrash.class);
    clazzes.add(RVSTrailerCrash.class);
    clazzes.add(RandomizedZoneStoryBase.class);
    clazzes.add(RZSForestCamp.class);
    clazzes.add(RZSForestCampEaten.class);
    clazzes.add(RZSBuryingCamp.class);
    clazzes.add(RZSBeachParty.class);
    clazzes.add(RZSFishingTrip.class);
    clazzes.add(RZSBBQParty.class);
    clazzes.add(RZSHunterCamp.class);
    clazzes.add(RZSSexyTime.class);
    clazzes.add(RZSTrapperCamp.class);
    clazzes.add(RZSBaseball.class);
    clazzes.add(RZSMusicFestStage.class);
    clazzes.add(RZSMusicFest.class);
    clazzes.add(MapGroups.class);
    clazzes.add(BeardStyles.class);
    clazzes.add(BeardStyle.class);
    clazzes.add(HairStyles.class);
    clazzes.add(HairStyle.class);
    clazzes.add(BloodClothingType.class);
    clazzes.add(WeaponType.class);
    clazzes.add(IsoWaterGeometry.class);
    clazzes.add(ModData.class);
    clazzes.add(WorldMarkers.class);
    clazzes.add(ChatMessage.class);
    clazzes.add(ChatBase.class);
    clazzes.add(ChatTab.class);
    clazzes.add(ServerChatMessage.class);
    clazzes.add(ChatTab.class);
    clazzes.add(LuaManager.GlobalObject.class);

    clazzes.sort(Comparator.comparing(Class::getSimpleName));
  }
}
