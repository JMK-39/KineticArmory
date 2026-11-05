package dev.xyat.kineticarmoryvalidation;

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

/** Uses the existing client and isolated unsaved page drafts; never sends a server-save packet. */
public final class GuiLongTextValidation {
    private static final Logger LOG=LoggerFactory.getLogger(GuiLongTextValidation.class);
    private static final String ROOT=System.getProperty("kineticarmory.guiValidation.output","D:/IDEAWork/KineticArmory/build/ui-fixes-20261016/gui-layout/");
    private static final String[] NAMES={"armor-edit","bonus-empty","bonus","bonus-warning","variants","variants-empty-only","variants-any","variants-empty","tips","tips-edit","tips-drag","detail","commands","commands-edit","entity-global","entity-set","conditions-empty","conditions","condition-attr","condition-item","condition-time","condition-dimension","attribute","potion","immunity","attack-effect","effect-immunity","damage-conversion","attack-damage","components","components-invalid","set-list","entity-rules","set-delete-confirm"};
    private static final List<String> checks = new ArrayList<>();
    private static boolean installed,started,screenshot,finished,originalFullscreen;
    private static String originalLanguage;
    private static int originalScale,originalWidth,originalHeight,phase=-1,page=-1,captures,failures;
    private static long due;
    private static final BitSet capturedPages = new BitSet();
    private static CompletableFuture<Void> reload;
    private static Language stressOriginal;
    private static Path pendingCapture;
    private static volatile boolean screenshotComplete;
    private static volatile Throwable screenshotFailure;

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
            if(pendingCapture!=null && !screenshotComplete)return;
            long now=System.currentTimeMillis();
            if(!screenshot && now>=due) {
                if(page==31){checkSetSearch();verifySetDeletionAndTooltip();}verifyUpdatedLayout();capture("start");layout();
                if(page==31)((dev.xyat.kineticcore.api.client.gui.widget.list.KineticRowList<?>)field(KineticGui.currentPage(),"listWidget")).setScrollOffset(Integer.MAX_VALUE);
                if(page==14 || page==15)((dev.xyat.kineticcore.api.client.gui.widget.list.KineticEntityGrid)field(KineticGui.currentPage(),"entityGrid")).setScrollOffset(Integer.MAX_VALUE);
                screenshot=true;due=now+(phase==4?3400:650);return;
            }
            if(screenshot && now>=due) {
                if(page==31)verifyLastSetRow();
                if(phase==4 || page==31 || page==14 || page==15)capture("scroll");
                nextPage();
            }
        } catch(CapturePending ignored) {
            // Continue this phase after the 26.1 renderer completes its asynchronous readback.
        } catch(Throwable error) {
            failures++;LOG.error("ARMORY_GUI_FAIL phase="+phase+" page="+page,error);
            var trace=new java.io.StringWriter();error.printStackTrace(new java.io.PrintWriter(trace));checks.add(trace.toString());
            finish();
        }
    }
    private static void nextPhase() {
        if(stressOriginal!=null){Language.inject(stressOriginal);stressOriginal=null;}
        phase++;page=-1;
        // One English 1080p round by default; list phases (0-4) in kineticarmory.guiValidation.phases to run more.
        String selectedPhases=System.getProperty("kineticarmory.guiValidation.phases","1,3");
        while(phase<5 && !Boolean.getBoolean("kineticarmory.guiValidation.fullHdOnly") && !List.of(selectedPhases.split(",")).contains(String.valueOf(phase)))phase++;
        if (Boolean.getBoolean("kineticarmory.guiValidation.fullHdOnly")) {
            while (phase < 5 && phase != 1 && phase != 3) phase++;
        }
        if(phase>=5){finish();return;}
        var mc=Minecraft.getInstance();
        mc.setScreen(null);
        String lang=phase==2 || phase==3?"zh_cn":"en_us";
        mc.getLanguageManager().setSelected(lang);
        mc.options.languageCode=lang;
        int width=phase==1 || phase==3?1920:854,height=phase==1 || phase==3?1080:480;
        mc.getWindow().setWindowed(width,height);mc.resizeDisplay();
        reload=mc.reloadResourcePacks();
        LOG.info("ARMORY_GUI_PHASE phase={} language={} requested={}x{} autoScale=true",phase,lang,width,height);
    }
    private static void nextPage() throws Exception {
        page++;
        String selectedPages=System.getProperty("kineticarmory.guiValidation.pages", "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,31,32,33");
        while(page<NAMES.length && !selectedPages.isBlank() && !List.of(selectedPages.split(",")).contains(String.valueOf(page)))page++;
        if(page>=NAMES.length){nextPhase();return;}
        openPage(page);
        screenshot=false;due=System.currentTimeMillis()+1000;
        LOG.info("ARMORY_GUI_OPEN phase={} case={} page={}",phase,NAMES[page],(KineticGui.currentPage()!=null?KineticGui.currentPage().getClass().getName():String.valueOf(Minecraft.getInstance().screen)));
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
            case 15 -> {verifyEntityApplyAndCancel();KineticGui.open(new ArmorEntityFilterPage(config));}
            case 32 -> {
                config.allowedEntityTypes.add("#example:unknown_entity_tag");config.allowedEntityTypes.add("@example");
                var p=new ArmorEntityFilterPage(config);KineticGui.open(p);invoke(p,"openRules");
            }
            case 16,17 -> {var owner=new ArmorDataConfig.AttributeModifierData();if(index==17)owner.conditions.add(new ConditionData("ATTR_RANGE"));KineticGui.open(new ConditionListPage(owner));}
            case 18,19,20,21 -> KineticGui.open(new ConditionEditPage(new ArrayList<>(),new ConditionData(new String[]{"ATTR_RANGE","ON_BLOCK","TIME_RANGE","DIMENSION"}[index-18]),true));
            case 22 -> KineticGui.open(new AttributeEditor(config,null));
            case 23 -> {var p=new PotionEditor(config,null);KineticGui.open(p);invoke(p,"focus",field(p,"idInput"));}
            case 24 -> KineticGui.open(new ImmunityEditor(config,null));
            case 25 -> KineticGui.open(new AttackEffectEditor(config,null));
            case 26 -> {var p=new PotionImmunityEditor(config,null);KineticGui.open(p);invoke(p,"focus",field(p,"idInput"));}
            case 27 -> KineticGui.open(new DamageConversionEditor(config,null));
            case 28 -> KineticGui.open(new AttackDamageEditor(config,null));
            case 31,33 -> {
                var p=new ArmorListPage();
                var entries=(List<ArmorDataConfig>)field(p,"allEntries");entries.clear();
                for(int n=0;n<40;n++){
                    var entry=sample();entry.id="gui_validation_"+n;entry.displayName="套装布局测试 "+n;
                    for(int i=0;i<20;i++)entry.curios.add(ArmorDataConfig.ItemReq.create(i%2==0?"minecraft:emerald":"minecraft:diamond_sword"));
                    var named=new ItemStack(Items.DIAMOND);
                    //? if >=1.20.5 {
                    /*named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Native validation diamond"));*/
                    //?} else {
                    named.setHoverName(Component.literal("Native validation diamond"));
                    //?}
                    entry.curios.get(0).setItemData(dev.xyat.kineticarmory.armorsets.data.ArmorItemData.format(named));
                    entries.add(entry);
                }
                setField(p,"displayEntries",new ArrayList<>(entries));KineticGui.open(p);
                if(index==33) {
                    var list=(dev.xyat.kineticcore.api.client.gui.widget.list.KineticRowList<?>)field(p,"listWidget");
                    list.setScrollOffset(0);
                    click(list.controlX()+1,list.rowTop(0)+1,1);
                }
            }
            case 29,30 -> {
                //? if >=1.21 {
                /*dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors.openNbtEditor(index==29?"[damage=1]":"[invalid=]",text->{try{dev.xyat.kineticarmory.armorsets.data.ArmorItemData.compile("minecraft:diamond_sword",text);return null;}catch(RuntimeException invalid){return String.valueOf(invalid.getMessage());}},value->{});
                *///?} else
                dev.xyat.kineticcore.api.client.gui.selector.KineticSelectors.openNbtEditor(index==29?"{Damage:1}":"{Damage:}",text->{try{net.minecraft.nbt.TagParser.parseTag(text);return null;}catch(Exception invalid){return String.valueOf(invalid.getMessage());}},value->{});
            }
        }
    }
    private static void checkSetSearch() throws Exception {
        Object owner=KineticGui.currentPage();
        var search=(dev.xyat.kineticcore.api.client.gui.widget.KineticTextField)field(owner,"searchBox");
        search.setTextValue("gui_validation_17");
        var results=(List<?>)field(owner,"displayEntries");
        if(results.size()!=1 || !"gui_validation_17".equals(((ArmorDataConfig)results.get(0)).id))throw new AssertionError("Set search failed");
        search.setTextValue("");
        if(((List<?>)field(owner,"displayEntries")).size()!=40)throw new AssertionError("Set search did not restore all rows");
        if(search.controlX()!=48 || search.controlY()!=4 || search.controlWidth()!=368)throw new AssertionError("Set toolbar uses the top inset and a search field widened by forty pixels");
        if(((dev.xyat.kineticcore.api.client.gui.widget.KineticControl)field(owner,"listWidget")).controlY()!=24)throw new AssertionError("Set rows did not reclaim the redundant title space");
    }
    private static void verifyLastSetRow() throws Exception {
        var list=(dev.xyat.kineticcore.api.client.gui.widget.list.KineticRowList<?>)field(KineticGui.currentPage(),"listWidget");
        int last=list.items().size()-1;
        int top=list.rowTop(last),bottom=list.controlY()+list.controlHeight();
        require(top>=list.controlY() && top+44<=bottom,"last set row is fully visible");
        require(Math.abs(top+list.rowHeight()-bottom)<=1,"set list ends at the exact content pixel without a blank row");
        require(list.rowAt(list.controlX()+1,bottom-3)==last && list.rowAt(list.controlX()+1,bottom+1)==-1,"last set row and viewport boundary hit testing");
        checks.add("PASS phase="+phase+" set-list exact end lastTop="+top+" bottom="+bottom);
    }
    private static void verifyUpdatedLayout() throws Exception {
        var owner=KineticGui.currentPage();
        switch(page) {
            case 33 -> {
                var overlays=((dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost)Minecraft.getInstance().screen).kineticRuntime().overlays();
                require(field(overlays,"dialog")!=null && ((Set<?>)field(owner,"pendingDeletedIds")).isEmpty(),"visible set deletion dialog leaves the draft unchanged before confirmation");
            }
            case 0 -> {
                var id=(dev.xyat.kineticcore.api.client.gui.widget.KineticControl)field(owner,"idBox");
                var name=(dev.xyat.kineticcore.api.client.gui.widget.KineticControl)field(owner,"nameBox");
                require(hasButton(4,4,40) && hasButton(596,4,40) && id.controlX()==4 && id.controlY()==28
                        && name.controlX()+name.controlWidth()==636,"large set editor fills the canvas with four-pixel insets");
                require((int)field(owner,"EXT_COLS")==25 && (int)field(owner,"EXT_ROWS")==4,"accessory previews reclaim the expanded width and height");
            }
            case 1,2,3 -> require((int)field(owner,"leftX")==4 && (int)field(owner,"rightX")+(int)field(owner,"rightW")==636
                    && (int)field(owner,"leftY")+(int)field(owner,"leftH")==356,"piece bonus columns fill the browsing canvas");
            case 4,5,6,7 -> require((int)field(owner,"listX")==4 && (int)field(owner,"listY")==28
                    && (int)field(owner,"listW")==632 && (int)field(owner,"listH")==328 && hasButton(4,4,40),"equipment variant browser fills the canvas below the compact header");
            case 11 -> {
                var list=(dev.xyat.kineticcore.api.client.gui.widget.list.KineticRowList<?>)field(owner,"listWidget");
                require(list.controlX()==4 && list.controlY()==28 && list.controlWidth()==632,"effect overview fills the canvas below the top actions");
                for(var button:buttons()) require(button.controlY()==4 && button.controlX()>=4 && button.controlX()+button.controlWidth()<=636,"all overview buttons fit on the four-pixel top row");
            }
            case 14,15 -> {
                var search=(dev.xyat.kineticcore.api.client.gui.widget.KineticTextField)field(owner,"searchBox");
                var grid=(dev.xyat.kineticcore.api.client.gui.widget.list.KineticEntityGrid)field(owner,"entityGrid");
                require(search.controlX()==48 && search.controlY()==4 && search.controlWidth()==416,"entity search directly follows Back and reaches the Rules button");
                require(grid.controlX()==4 && grid.controlY()==28 && grid.controlWidth()==632 && grid.controlHeight()==328,"one full-width Core entity grid");
                int size=dev.xyat.kineticcore.api.client.gui.layout.KineticLayout.ENTITY_CARD_SIZE;
                int gap=dev.xyat.kineticcore.api.client.gui.layout.KineticLayout.ENTITY_CARD_GAP;
                require(size==50 && gap==2 && grid.columns()==12,"fifty-pixel entity cards fill twelve columns");
                require(grid.columns()==(grid.controlWidth()-6+gap)/(size+gap) && grid.itemAt(4+size+.5,29)==-1,"shared entity card size and two-pixel gaps fill the viewport");
                require(buttons().stream().allMatch(button->button.controlY()==4),"entity filter has only top toolbar actions");
            }
            case 22,23,24,25,26,27,28 -> {
                var panel=invoke(owner,"panel");int left=(int)field(panel,"left"),top=(int)field(panel,"top"),width=(int)field(panel,"width");
                require(hasButton(left+4,top+4,40) && hasButton(left+width-44,top+4,40),"effect editor has Back and Save in its compact header");
                if(page==23 || page==26) {
                    var input=(dev.xyat.kineticcore.api.client.gui.widget.KineticAutoCompleteField)field(owner,"idInput");
                    require(input.controlWidth()==392,"potion choices have room for icon, ID and translation");
                    require(input.isSuggestionPopupOpen(),"native potion suggestion popup is visible");
                }
            }
            case 16,17 -> {
                var list=(dev.xyat.kineticcore.api.client.gui.widget.KineticControl)field(owner,"listWidget");
                require(buttons().stream().allMatch(button->button.controlY()<list.controlY()),"condition list actions share the header");
            }
            case 18,19,20,21 -> {
                var save=(dev.xyat.kineticcore.api.client.gui.widget.KineticControl)field(owner,"saveBtn");
                var back=(dev.xyat.kineticcore.api.client.gui.widget.KineticControl)field(owner,"backBtn");
                require(save.controlY()==back.controlY() && save.controlWidth()==40 && back.controlWidth()==40,"predicate Back and Save share the top row");
            }
            case 8,9,10 -> require(buttons().stream().allMatch(button->button.controlY()==4),"tip editor has no bottom action buttons");
            case 12,13 -> require(buttons().stream().allMatch(button->button.controlY()<=28),"command editor actions use a compact header");
            default -> { return; }
        }
        checks.add("PASS phase="+phase+" page="+page+" "+NAMES[page]);
    }
    private static List<dev.xyat.kineticcore.api.client.gui.widget.KineticButton> buttons() {
        return Minecraft.getInstance().screen.children().stream()
                .filter(child->child instanceof dev.xyat.kineticcore.api.client.gui.widget.KineticButton)
                .map(child->(dev.xyat.kineticcore.api.client.gui.widget.KineticButton)child).toList();
    }
    private static boolean hasButton(int x,int y,int width) {
        return buttons().stream().anyMatch(button->button.controlX()==x && button.controlY()==y && button.controlWidth()==width);
    }
    @SuppressWarnings("unchecked")
    private static void verifySetDeletionAndTooltip() throws Exception {
        Object owner=KineticGui.currentPage();
        var list=(dev.xyat.kineticcore.api.client.gui.widget.list.KineticRowList<ArmorDataConfig>)field(owner,"listWidget");
        var entries=(List<ArmorDataConfig>)field(owner,"allEntries");
        var deleted=(Set<String>)field(owner,"pendingDeletedIds");
        Object overlays=((dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost)Minecraft.getInstance().screen).kineticRuntime().overlays();
        list.setSelectedIndex(0);
        invoke(owner,"focus",field(owner,"searchBox"));key(261);
        require(field(overlays,"dialog")==null && entries.size()==40,"Delete edits a focused search field without deleting a set");
        invoke(owner,"clearFocus");
        key(257);key(335);
        require(KineticGui.currentPage()==owner && field(overlays,"dialog")==null,"Enter over a set row must not open an editor or request deletion");
        invoke(owner,"focus",list);key(257);key(335);
        require(KineticGui.currentPage()==owner && field(overlays,"dialog")==null,"Enter must not activate a focused set list outside a confirmation");
        invoke(owner,"clearFocus");
        var first=entries.get(0);
        click(list.controlX()+1,list.rowTop(0)+1,1);
        require(field(overlays,"dialog")!=null && entries.size()==40 && deleted.isEmpty(),"row right-click requests confirmation before changing the set draft");
        key(256);require(field(overlays,"dialog")==null && entries.size()==40,"Escape cancels set deletion");
        key(261);require(field(overlays,"dialog")!=null,"Delete reuses the same selected-set confirmation");
        key(257);require(entries.size()==39 && deleted.contains(first.id),"Enter confirms only a pending local deletion draft");
        entries.add(0,first);deleted.clear();invoke(owner,"updateSearch","");
        int x=list.controlX()+260,y=list.rowTop(0)+4;
        var stack=(ItemStack)invoke(list,"previewAt",(double)x,(double)y);
        require(stack.is(Items.DIAMOND) && stack.getHoverName().getString().equals("Native validation diamond"),"accessory preview preserves the native configured stack data");
        invoke(owner,"renderTooltips",x,y);
        require(field(overlays,"tooltip").getClass().getSimpleName().equals("ItemTooltip"),"preview requests Core native item tooltip instead of handmade name lines");
        checks.add("PASS phase="+phase+" set right-click/Delete confirmation, ESC cancel, Enter confirm, text-focus guard and native item tooltip");
    }
    private static void click(double x,double y,int button) {
        var screen=Minecraft.getInstance().screen;
        var canvas=((dev.xyat.kineticcore.internal.client.gui.screen.KineticScreenHost)screen).kineticRuntime().canvas();
        double sx=canvas.toScreenX(x),sy=canvas.toScreenY(y);
        //? if >=26.1 {
        /*var event=new net.minecraft.client.input.MouseButtonEvent(sx,sy,new net.minecraft.client.input.MouseButtonInfo(button,0));
        screen.mouseClicked(event,false);screen.mouseReleased(event);*/
        //?} else {
        screen.mouseClicked(sx,sy,button);screen.mouseReleased(sx,sy,button);
        //?}
    }
    private static void key(int code) {
        var screen=Minecraft.getInstance().screen;
        //? if >=26.1 {
        /*screen.keyPressed(new net.minecraft.client.input.KeyEvent(code,0,0));*/
        //?} else {
        screen.keyPressed(code,0,0);
        //?}
    }
    @SuppressWarnings("unchecked")
    private static void verifyEntityApplyAndCancel() throws Exception {
        var config=sample();config.allowedEntityTypes.add("#example:unknown_entity_tag");config.allowedEntityTypes.add("@example");
        var page=new ArmorEntityFilterPage(config);KineticGui.open(page);
        clickEntity(page,"minecraft:cow");clickEntity(page,"minecraft:pig");
        var rules=(List<String>)invoke(page,"buildRules");
        require(rules.containsAll(List.of("minecraft:cow","minecraft:pig","example:missing_entity","#example:unknown_entity_tag","@example")),"multiple selection retains unknown, tag and mod rules");
        invoke(page,"removeRule","#example:unknown_entity_tag");
        require(!((List<String>)invoke(page,"buildRules")).contains("#example:unknown_entity_tag"),"unknown tag rule can be removed");
        invoke(page,"addRule","#example:unknown_entity_tag");
        setField(page,"setFilterEnabled",false);invoke(page,"save");
        require(!config.entityWhitelistEnabled && config.allowedEntityTypes.containsAll(rules),"Apply updates only the supplied set draft with multiple selections and enable state");
        var cancelConfig=sample();var before=new ArrayList<>(cancelConfig.allowedEntityTypes);
        var cancel=new ArmorEntityFilterPage(cancelConfig);KineticGui.open(cancel);
        clickEntity(cancel,"minecraft:cow");setField(cancel,"setFilterEnabled",false);invoke(cancel,"backWithoutSave");
        require(cancelConfig.entityWhitelistEnabled && cancelConfig.allowedEntityTypes.equals(before),"Back cancels entity selection and enable changes");
        checks.add("PASS entity-grid native multi-click, Apply, Back cancellation, unresolved/tag/mod rule management");
    }
    private static void clickEntity(Object page,String id) throws Exception {
        var search=(dev.xyat.kineticcore.api.client.gui.widget.KineticTextField)field(page,"searchBox");search.setTextValue(id);
        var grid=(dev.xyat.kineticcore.api.client.gui.widget.list.KineticEntityGrid)field(page,"entityGrid");
        int index=-1;for(int i=0;i<grid.items().size();i++)if(grid.items().get(i).entityId().equals(id)){index=i;break;}
        require(index>=0,"requested entity is available in the native grid: "+id);
        int pitch=dev.xyat.kineticcore.api.client.gui.layout.KineticLayout.ENTITY_CARD_SIZE
                +dev.xyat.kineticcore.api.client.gui.layout.KineticLayout.ENTITY_CARD_GAP;
        var click=new dev.xyat.kineticcore.api.client.gui.input.MouseInput(grid.controlX()+index%grid.columns()*pitch+1,
                grid.controlY()+index/grid.columns()*pitch+1,dev.xyat.kineticcore.api.client.gui.input.MouseButton.LEFT,0,0);
        invoke(grid,"onMouseClick",click);
        require(((Set<String>)field(page,"selectedIds")).contains(id),"native card click selects "+id);
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
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
        c.tipOverrides.put("attr:0", "§a§lA long overridden effect with [item:minecraft:diamond] followed by text ".repeat(5));
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
        for(int i=0;i<types.length;i++)if(args[i]!=null && !(types[i].isInstance(args[i]) || types[i]==int.class && args[i] instanceof Integer || types[i]==double.class && args[i] instanceof Double || types[i]==boolean.class && args[i] instanceof Boolean))return false;
        return true;
    }
    private static void capture(String frame)throws Exception {
        var mc=Minecraft.getInstance();Path path=Path.of(ROOT,String.format("%d-%02d-%s-%s.png",phase,page,NAMES[page],frame));Files.createDirectories(path.getParent());
        //? if >=26.1 {
        /*if(pendingCapture==null) {
            pendingCapture=path;screenshotComplete=false;screenshotFailure=null;
            Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{
                try(image){image.writeToFile(path);}catch(Throwable error){screenshotFailure=error;}
                finally{screenshotComplete=true;}
            });
            throw new CapturePending();
        }
        require(pendingCapture.equals(path),"asynchronous capture belongs to the current page");
        if(screenshotFailure!=null)throw new java.io.IOException("screenshot failed",screenshotFailure);
        pendingCapture=null;screenshotComplete=false;
        *///?} else {
        try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){image.writeToFile(path);}
        //?}
        capturedPages.set(page);captures++;LOG.info("ARMORY_GUI_CAPTURE phase={} case={} image={}x{}",phase,NAMES[page],mc.getWindow().getWidth(),mc.getWindow().getHeight());
        Files.writeString(Path.of(ROOT,"result.txt"),"RUNNING phase="+phase+" page="+page+" captures="+captures+System.lineSeparator()+String.join(System.lineSeparator(),checks));
    }
    private static final class CapturePending extends RuntimeException {
        private CapturePending(){super(null,null,false,false);}
    }
    /*** Logs controls of the open screen that overlap or sit closer than 2 px (Core layout check). */
    private static void layout() {
        try {
            for(Object problem:(List<?>)Class.forName("dev.xyat.kineticcore.internal.client.gui.LayoutCheck").getMethod("currentScreenProblems").invoke(null))
                LOG.warn("ARMORY_GUI_LAYOUT phase={} case={} {}",phase,NAMES[page],problem);
            LOG.info("ARMORY_GUI_LAYOUT_CHECKED phase={} case={} controls={}",phase,NAMES[page],Class.forName("dev.xyat.kineticcore.internal.client.gui.LayoutCheck").getField("lastCheckedControls").get(null));
        } catch(ReflectiveOperationException unavailable) { LOG.warn("ARMORY_GUI_LAYOUT unavailable",unavailable); }
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
        LOG.info("ARMORY_GUI_{} pages={} captures={} failures={} userSettingsRestored=true",failures==0?"PASS":"FAIL",capturedPages.cardinality(),captures,failures);
        try {Files.createDirectories(Path.of(ROOT));Files.writeString(Path.of(ROOT,"result.txt"),(failures==0?"PASS":"FAIL")+" pages="+capturedPages+" captures="+captures+" failures="+failures+System.lineSeparator()+String.join(System.lineSeparator(),checks));}
        catch(Exception error){throw new IllegalStateException(error);}
        String next=System.getProperty("kineticarmory.guiValidation.thenValidation", "");
        if(failures==0 && !next.isBlank())try{if(next.endsWith(".install()"))next=next.substring(0,next.length()-10);Class.forName(next).getMethod("install").invoke(null);return;}catch(Exception error){
            failures++;checks.add("CHAIN_FAIL "+error);
            try{Files.writeString(Path.of(ROOT,"result.txt"),"FAIL"+System.lineSeparator()+String.join(System.lineSeparator(),checks));}catch(Exception output){LOG.error("ARMORY_GUI_RESULT_FAIL",output);}
        }
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
