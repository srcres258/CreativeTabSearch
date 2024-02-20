package top.srcres.mods.creativetabsearch.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.client.gui.CreativeTabsScreenPage;
import net.neoforged.neoforge.common.CreativeModeTabRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.srcres.mods.creativetabsearch.CreativeTabSearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow public abstract void resize(Minecraft pMinecraft, int pWidth, int pHeight);

    protected EditBox tabSearchEditBox;
    @Shadow
    @Final
    private List<CreativeTabsScreenPage> pages;
    @Shadow
    private CreativeTabsScreenPage currentPage;

    @Shadow private static CreativeModeTab selectedTab;

    public CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Inject(method = "init", at = @At("RETURN"))
    protected void initTabSearch(CallbackInfo ci) {
        // Only enable the creative tab searching ability when the specific game mode is satisfied
        // as mentioned in vanilla code.
        if (this.minecraft.gameMode.hasInfiniteItems()) {
            // Ensure the number of creative tab pages is more than one.
            if (CreativeModeTabRegistry.getSortedCreativeModeTabs().size() > 10) {
                this.tabSearchEditBox = new EditBox(this.font, this.leftPos, this.topPos - 65,
                        this.imageWidth, 15, this.tabSearchEditBox, Component.literal("test"));
                this.tabSearchEditBox.setResponder(this::tabSearch_updateTabSearch);
                this.addWidget(this.tabSearchEditBox);
                this.setInitialFocus(this.tabSearchEditBox);
            }
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    protected void renderTabSearch(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        if (this.tabSearchEditBox != null)
            this.tabSearchEditBox.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Inject(method = "charTyped", at = @At("HEAD"))
    protected void charTypedTabSearch(char pCodePoint, int pModifiers, CallbackInfoReturnable<Boolean> cir) {
        // The callback has to be invoked manually since the origin listener implementation has been overwritten
        // by CreativeModeInventoryScreen within method CreativeModeInventoryScreen#charTyped.
        this.tabSearchEditBox.charTyped(pCodePoint, pModifiers);
    }

    @Unique
    private void tabSearch_updateTabSearch(String pNewText) {
        CreativeModeTabRegistry.getSortedCreativeModeTabs().forEach(
                (tab) -> CreativeTabSearch.getInstance().getLogger().info("Found creative tab: {}", tab.getDisplayName().getString()));
        List<CreativeModeTab> result = tabSearch_getMatchingTabs(pNewText);
        if (result.isEmpty())
            tabSearch_resetCreativeTabPages(CreativeModeTabRegistry.getSortedCreativeModeTabs());
        else
            tabSearch_resetCreativeTabPages(result);
    }

    @Unique
    private static List<CreativeModeTab> tabSearch_getMatchingTabs(String searchStr) {
        ArrayList<CreativeModeTab> result = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
            if (tab.getDisplayName().getString().contains(searchStr)) {
                result.add(tab);
            }
        }
        return result;
    }

    @Unique
    private void tabSearch_resetCreativeTabPages(List<CreativeModeTab> tabList) {
        this.pages.clear();
        int tabIndex = 0;
        List<CreativeModeTab> currentPage = new ArrayList<>();

        for (CreativeModeTab sortedCreativeModeTab : tabList) {
            currentPage.add(sortedCreativeModeTab);
            ++tabIndex;
            if (tabIndex == 10) {
                this.pages.add(new CreativeTabsScreenPage(currentPage));
                currentPage = new ArrayList();
                tabIndex = 0;
            }
        }

        if (tabIndex != 0) {
            this.pages.add(new CreativeTabsScreenPage(currentPage));
        }

        if (this.pages.isEmpty()) {
            this.currentPage = new CreativeTabsScreenPage(new ArrayList<>());
        } else {
            this.currentPage = this.pages.get(0);
        }

        selectedTab = tabList.get(0);
    }
}
