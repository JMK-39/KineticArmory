//? if >=1.21 {
/*package dev.xyat.kineticarmoryvalidation;

import dev.xyat.kineticcore.api.client.event.KineticClientEvents;
import dev.xyat.kineticcore.api.client.gui.KineticGui;
import dev.xyat.kineticcore.api.client.gui.page.KineticPage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import dev.xyat.kineticarmory.armorsets.data.ArmorDataConfig;
import dev.xyat.kineticarmory.armorsets.client.gui.*;
import dev.xyat.kineticarmory.armorsets.client.gui.editor.*;
import dev.xyat.kineticarmory.armorsets.predicate.*;
import dev.xyat.kineticarmory.armorsets.predicate.client.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.trading.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/^** Uses the existing client and unsaved page drafts. Never clicks or saves editor changes. *^/
public final class GuiLongTextValidation {
    private static final Logger LOG=LoggerFactory.getLogger(GuiLongTextValidation.class);
    private static final String ROOT="D:/IDEAWork/KineticArmory/.gradle/gui-long-text-20261004/";
    private static final String[] NAMES={"armor-edit","bonus-empty","bonus","bonus-warning","variants","variants-empty-only","variants-any","variants-empty","tips","tips-edit","tips-drag","detail","commands","commands-edit","entity-global","entity-set","conditions-empty","conditions","condition-attr","condition-item","condition-time","condition-dimension","attribute","potion","immunity","attack-effect","effect-immunity","damage-conversion","attack-damage","components","components-invalid"};
    private static boolean installed,started,screenshot,finished,originalFullscreen;
    private static String originalLanguage;
    private static int originalScale,originalWidth,originalHeight,phase=-1,page=-1,captures,failures;
    private static long due;
    private static CompletableFuture<Void> reload;
    private static Language stressOriginal;

    public static void install() { if(installed)return;installed=true;KineticClientEvents.onTick(KineticClientEvents.TickPhase.END,GuiLongTextValidation::tick); }
    private static void tick() {
        if(finished)return;
        try {
            var mc=Minecraft.getInstance();
            if(!started) {
                if(mc.player==null || mc.level==null || mc.getSingleplayerServer()==null)return;
                started=true;originalLanguage=mc.getLanguageManager().getSelected();originalScale=mc.options.guiScale().get();
                originalWidth=mc.getWindow().getWidth();originalHeight=mc.getWindow().getHeight();originalFullscreen=mc.getWindow().isFullscreen();
                mc.options.guiScale().set(0);
                if(originalFullscreen)mc.getWindow().toggleFullScreen();
                nextPhase();
                return;
            }
            if(reload!=null) {
                if(!reload.isDone() || mc.getOverlay()!=null)return;
                reload.join();reload=null;
                if(phase==4) {
                    stressOriginal=Language.getInstance();
                    Language.inject(new StressLanguage(stressOriginal));
                }
                nextPage();return;
            }
            long now=System.currentTimeMillis();
            if(!screenshot && now>=due) { capture("start");screenshot=true;due=now+(phase==4?3400:550);return; }
            if(screenshot && now>=due) {
                if(phase==4)capture("scroll");
                nextPage();
            }
        } catch(Throwable error) {
            failures++;LOG.error("ARMORY_GUI_FAIL phase="+phase+" page="+page,error);
            finish();
        }
    }
    private static void nextPhase() {
        if(stressOriginal!=null){Language.inject(stressOriginal);stressOriginal=null;}
        phase++;page=-1;
        if(phase>=5){finish();return;}
        var mc=Minecraft.getInstance();
        mc.setScreen(null);
        String lang=phase==2 || phase==3?"zh_cn":"en_us";
        mc.getLanguageManager().setSelected(lang);
        mc.options.languageCode=lang;
        int width=phase==1 || phase==3?1536:854,height=phase==1 || phase==3?864:480;
        mc.getWindow().setWindowed(width,height);mc.resizeDisplay();
        reload=mc.reloadResourcePacks();
        LOG.info("ARMORY_GUI_PHASE phase={} language={} requested={}x{} autoScale=true",phase,lang,width,height);
    }
    private static void nextPage() throws Exception {
        page++;
        String selectedPages=System.getProperty("kineticarmory.guiValidation.pages", "");
        while(page<NAMES.length && !selectedPages.isBlank() && !List.of(selectedPages.split(",")).contains(String.valueOf(page)))page++;
        if(page>=NAMES.length){nextPhase();return;}
        openPage(page);
        screenshot=false;due=System.currentTimeMillis()+1000;
        LOG.info("ARMORY_GUI_OPEN phase={} case={} page={}",phase,NAMES[page],KineticGui.currentPage().getClass().getName());
    }
    @SuppressWarnings("unchecked")
    private static void openPage(int index) throws Exception {
        var config = sample();
        switch(index) {
            case 0 -> KineticGui.open(new ArmorEditPage(config));
            case 1,2,3 -> {
                var p=new ArmorPieceBonusPage(index==1?new ArmorDataConfig():config);KineticGui.open(p);
                if(index==3)setField(p,"warningMessage",dev.xyat.kineticcore.api.text.KineticI18n.translatable("gui.kineticarmory.armorsets.piece.bonus.warn_value_number"));
            }
            case 4,5,6,7 -> {
                if(index==5)config.equipmentVariants.put("mainhand",new ArrayList<>(List.of(ArmorDataConfig.ItemReq.create("EMPTY"))));
                if(index==6)config.equipmentVariants.put("mainhand",new ArrayList<>(List.of(ArmorDataConfig.ItemReq.create("ANY"))));
                if(index==7){config.equipment.remove("mainhand");config.equipmentVariants.put("mainhand",new ArrayList<>());}
                KineticGui.open(new ArmorEquipmentVariantPage(config,"mainhand",dev.xyat.kineticcore.api.text.KineticI18n.translatable("gui.kineticarmory.armorsets.slot.mainhand")));
            }
            case 8,9,10 -> {
                var p=new ArmorTipEditorPage(config);KineticGui.open(p);
                if(index==9){var rows=(List<?>)field(p,"displayRows");if(!rows.isEmpty())invoke(p,"startEdit",rows.get(0));}
                if(index==10){setField(p,"draggingIndex",0);setField(p,"draggingText",config.tipLayout.get(0).text);}
            }
            case 11 -> KineticGui.open(new ArmorDetailPage(config));
            case 12,13 -> {var p=new ArmorCommandEditorPage(config);KineticGui.open(p);if(index==13)p.startEdit(config.activationCommands,0,config.activationCommands.get(0).command);}
            case 14 -> KineticGui.open(new ArmorEntityFilterPage());
            case 15 -> KineticGui.open(new ArmorEntityFilterPage(config));
            case 16,17 -> {var owner=new ArmorDataConfig.AttributeModifierData();if(index==17)owner.conditions.add(new ConditionData("ATTR_RANGE"));KineticGui.open(new ConditionListPage(owner));}
            case 18,19,20,21 -> KineticGui.open(new ConditionEditPage(new ArrayList<>(),new ConditionData(new String[]{"ATTR_RANGE","ON_BLOCK","TIME_RANGE","DIMENSION"}[index-18]),true));
            case 22 -> KineticGui.open(new AttributeEditor(config,null));
            case 23 -> KineticGui.open(new PotionEditor(config,null));
            case 24 -> KineticGui.open(new ImmunityEditor(config,null));
            case 25 -> KineticGui.open(new AttackEffectEditor(config,null));
            case 26 -> KineticGui.open(new PotionImmunityEditor(config,null));
            case 27 -> KineticGui.open(new DamageConversionEditor(config,null));
            case 28 -> KineticGui.open(new AttackDamageEditor(config,null));
            case 29,30 -> KineticGui.open(new ArmorComponentsEditorPage("minecraft:diamond_sword",index==29?"[damage=1]":"[invalid=]",value->{}));
        }
    }
    private static ArmorDataConfig sample() {
        var c=new ArmorDataConfig();c.id="gui_validation";c.displayName="A deliberately long example set name ".repeat(4);
        c.equipment.put("head",ArmorDataConfig.ItemReq.create("minecraft:diamond_helmet"));
        c.equipmentVariants.put("mainhand",new ArrayList<>(List.of(ArmorDataConfig.ItemReq.create("minecraft:diamond_sword"),ArmorDataConfig.ItemReq.create("minecraft:netherite_sword"))));
        c.curios.add(ArmorDataConfig.ItemReq.create("minecraft:diamond"));c.rejectedCurios.add(ArmorDataConfig.ItemReq.create("minecraft:apple"));
        var a=new ArmorDataConfig.AttributeModifierData();a.attribute="minecraft:generic.attack_damage";a.uuid="11c892cc-bf62-4d93-9269-f88530860b30";a.operation="ADD";a.amount=2;c.attributes.add(a);
        var effect=new ArmorDataConfig.PotionEffectData();effect.effectId="minecraft:speed";effect.duration=30;c.potionEffects.add(effect);
        var attack=new ArmorDataConfig.AttackEffectData();attack.effectId="minecraft:slowness";c.attackEffects.add(attack);
        var command=new ArmorDataConfig.CommandData();command.command="say a deliberately long example command for bounded list text ".repeat(4);c.activationCommands.add(command);
        var inactive=new ArmorDataConfig.CommandData();inactive.command=command.command;c.deactivationCommands.add(inactive);
        c.tipOverrides.put("attr:0", "A long overridden effect with [item:minecraft:diamond] followed by text ".repeat(5));
        c.manualTips=true;c.tipLayout.add(ArmorDataConfig.TipLineData.text("§aA deliberately long user-authored tip with item icons [item:minecraft:diamond] and additional words ".repeat(5)));
        c.flexiblePieces=true;var tier=ArmorDataConfig.PieceBonusGroup.create(2);tier.effectKeys.add("attr:0");tier.effectValues.put("attr:0",3.0);c.pieceBonusGroups.add(tier);
        c.entityWhitelistEnabled=true;c.allowedEntityTypes.add("minecraft:zombie");c.allowedEntityTypes.add("example:missing_entity");c.initNullFields();return c;
    }
    private static void setField(Object target,String name,Object value)throws Exception {
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(target,value);return;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static Object field(Object target,String name)throws Exception {
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())try {
            var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(target);
        }catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private static Object invoke(Object target,String name,Object...args)throws Exception {
        for(Class<?> type=target.getClass();type!=null;type=type.getSuperclass())for(var m:type.getDeclaredMethods()) {
            if(m.getName().equals(name)&&compatible(m.getParameterTypes(),args)) {
                m.setAccessible(true);return m.invoke(target,args);
            }
        }
        throw new NoSuchMethodException(name);
    }
    private static Object construct(String name,Object...args)throws Exception {
        for(var c:Class.forName(name).getDeclaredConstructors())if(compatible(c.getParameterTypes(),args)){c.setAccessible(true);return c.newInstance(args);}
        throw new NoSuchMethodException(name+" constructor");
    }
    private static boolean compatible(Class<?>[]types,Object[]args) {
        if(types.length!=args.length)return false;
        for(int i=0;i<types.length;i++)if(args[i]!=null && !(types[i].isInstance(args[i]) || types[i]==int.class && args[i] instanceof Integer || types[i]==boolean.class && args[i] instanceof Boolean))return false;
        return true;
    }
    private static void capture(String frame)throws Exception {
        var mc=Minecraft.getInstance();Path path=Path.of(ROOT,String.format("%d-%02d-%s-%s.png",phase,page,NAMES[page],frame));Files.createDirectories(path.getParent());
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(path);}
        captures++;LOG.info("ARMORY_GUI_CAPTURE phase={} case={} image={}x{}",phase,NAMES[page],mc.getWindow().getWidth(),mc.getWindow().getHeight());
    }
    private static void finish() {
        finished=true;
        var mc=Minecraft.getInstance();
        if(stressOriginal!=null){Language.inject(stressOriginal);stressOriginal=null;}
        mc.options.guiScale().set(originalScale);
        mc.getLanguageManager().setSelected(originalLanguage);mc.options.languageCode=originalLanguage;
        mc.setScreen(null);
        mc.getWindow().setWindowed(originalWidth,originalHeight);
        if(originalFullscreen && !mc.getWindow().isFullscreen())mc.getWindow().toggleFullScreen();
        LOG.info("ARMORY_GUI_{} pages={} captures={} failures={} userSettingsRestored=true",failures==0?"PASS":"FAIL",NAMES.length,captures,failures);
        dev.xyat.kineticcore.api.runtime.KineticClientRuntime.stopClient();
    }
    private static final class StressLanguage extends Language {
        private final Language delegate;
        StressLanguage(Language delegate){this.delegate=delegate;}
        @Override public String getOrDefault(String key,String fallback) {
            String text=delegate.getOrDefault(key,fallback);
            return key.startsWith("gui.kineticarmory.") && !key.contains(".value.") && !key.endsWith(".add_mark") && !key.endsWith(".remove_mark") && !key.endsWith(".common.expand") && !key.endsWith(".common.collapse")?text+" - deliberately extended translation to verify text stays inside its own region":text;
        }
        @Override public boolean has(String key){return delegate.has(key);}
        @Override public boolean isDefaultRightToLeft(){return delegate.isDefaultRightToLeft();}
        @Override public net.minecraft.util.FormattedCharSequence getVisualOrder(net.minecraft.network.chat.FormattedText text){return delegate.getVisualOrder(text);}
    }
}
*///?}
