package top.srcres.mods.creativetabsearch.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
import top.srcres.mods.creativetabsearch.SearchHelper;

import java.util.ArrayList;
import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Unique
    protected EditBox tabSearch_editBox;
    @Unique
    protected Button tabSearch_clearButton;
    @Shadow
    @Final
    private List<CreativeTabsScreenPage> pages;
    @Shadow
    private CreativeTabsScreenPage currentPage;

    public CreativeModeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Shadow
    private void selectTab(CreativeModeTab pTab) {
    }

    @Inject(method = "init", at = @At("RETURN"))
    protected void initTabSearch(CallbackInfo ci) {
        // Only enable the creative tab searching ability when the specific game mode is satisfied
        // as mentioned in vanilla code.
        if (this.minecraft.gameMode.hasInfiniteItems()) {
            if (this.pages.size() > 1) {
                this.tabSearch_editBox = new EditBox(this.font, this.leftPos, this.topPos - 65,
                        this.imageWidth - 20, 15, this.tabSearch_editBox, Component.literal(""));
            } else {
                this.tabSearch_editBox = new EditBox(this.font, this.leftPos, this.topPos - 45,
                        this.imageWidth - 20, 15, this.tabSearch_editBox, Component.literal(""));
            }
            this.tabSearch_editBox.setResponder(this::tabSearch_updateTabSearch);
            this.addWidget(this.tabSearch_editBox);
            if (this.pages.size() > 1) {
                this.tabSearch_clearButton = Button.builder(Component.literal("X"), this::tabSearch_clearButtonClicked)
                        .bounds(this.leftPos + this.imageWidth - 20, this.topPos - 65, 20, 15).build();
            } else {
                this.tabSearch_clearButton = Button.builder(Component.literal("X"), this::tabSearch_clearButtonClicked)
                        .bounds(this.leftPos + this.imageWidth - 20, this.topPos - 45, 20, 15).build();
            }
            this.tabSearch_clearButton.active = !this.tabSearch_editBox.getValue().isEmpty();
            this.addRenderableWidget(this.tabSearch_clearButton);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    protected void renderTabSearch(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, CallbackInfo ci) {
        if (this.tabSearch_editBox != null)
            this.tabSearch_editBox.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    protected void charTypedTabSearch(char pCodePoint, int pModifiers, CallbackInfoReturnable<Boolean> cir) {
        // The callback has to be invoked manually since the origin listener implementation has been overwritten
        // by CreativeModeInventoryScreen within method CreativeModeInventoryScreen#charTyped.
        if (this.tabSearch_editBox.charTyped(pCodePoint, pModifiers)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    protected void keyPressedTabSearch(int pKeyCode, int pScanCode, int pModifiers, CallbackInfoReturnable<Boolean> cir) {
        // Need to replace vanilla key pressing logic when editing tab search EditBox.
        if (this.tabSearch_editBox.isFocused()) {
            // Handle Esc quitting screen logic at first, otherwise the player
            // may feel weird for being unable to close the screen.
            if (pKeyCode == 256 && this.shouldCloseOnEsc()) {
                this.onClose();
                cir.setReturnValue(true);
            } else {
                cir.setReturnValue(this.tabSearch_editBox.keyPressed(pKeyCode, pScanCode, pModifiers));
            }
            cir.cancel();
        }
    }

    @Unique
    private void tabSearch_updateTabSearch(String pNewText) {
        if (pNewText.isEmpty()) {
            this.tabSearch_editBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
            tabSearch_resetCreativeTabPages(CreativeModeTabRegistry.getSortedCreativeModeTabs());
            this.tabSearch_clearButton.active = false;
        } else {
            List<CreativeModeTab> result = tabSearch_getMatchingTabs(pNewText);
            if (result.isEmpty()) {
                this.tabSearch_editBox.setTextColor(0xff0000);
                tabSearch_resetCreativeTabPages(CreativeModeTabRegistry.getSortedCreativeModeTabs());
            } else {
                this.tabSearch_editBox.setTextColor(EditBox.DEFAULT_TEXT_COLOR);
                tabSearch_resetCreativeTabPages(result);
            }
            this.tabSearch_clearButton.active = true;
        }
    }

    @Unique
    private static List<CreativeModeTab> tabSearch_getMatchingTabs(String searchStr) {
        if (searchStr.startsWith("@")) {
            if (SearchHelper.validateSearchCommand(searchStr)) {
                searchStr = SearchHelper.cleanSearchCommand(searchStr);
                String[] parts = searchStr.split(" ");
                return switch (parts[0].toLowerCase()) {
                    case "modid" -> {
                        if (parts.length < 2) {
                            // Necessary arguments are missing
                            yield new ArrayList<>();
                        } else {
                            ArrayList<CreativeModeTab> result = new ArrayList<>();
                            for (int i = 1; i < parts.length; i++) {
                                String modid = parts[i];
                                for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
                                    String[] arr = tab.getIconItem().getDescriptionId().split("\\.");
                                    if (BuiltInRegistries.ITEM.getKey(tab.getIconItem().getItem()).getNamespace().contains(modid)) {
                                        result.add(tab);
                                    } else if (arr.length > 1 && arr[1].contains(modid)) {
                                        result.add(tab);
                                    }
                                }
                            }
                            yield result;
                        }
                    }
                    case "itemid" -> {
                        if (parts.length < 2) {
                            // Necessary arguments are missing
                            yield new ArrayList<>();
                        } else {
                            ArrayList<CreativeModeTab> result = new ArrayList<>();
                            for (int i = 1; i < parts.length; i++) {
                                String itemid = parts[i];
                                for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
                                    for (ItemStack stack : tab.getDisplayItems()) {
                                        if (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains(itemid)) {
                                            result.add(tab);
                                            break;
                                        }
                                    }
                                }
                            }
                            yield result;
                        }
                    }
                    default -> // No command matches; search failed
                            new ArrayList<>();
                };
            } else {
                // Command is wrong
                return new ArrayList<>();
            }
        } else {
            ArrayList<CreativeModeTab> result = new ArrayList<>();
            for (CreativeModeTab tab : CreativeModeTabRegistry.getSortedCreativeModeTabs()) {
                if (tab.getDisplayName().getString().contains(searchStr)) {
                    result.add(tab);
                }
            }
            return result;
        }
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

        this.selectTab(tabList.get(0));
    }

    @Unique
    private void tabSearch_clearButtonClicked(Button button) {
        this.tabSearch_editBox.setValue("");
        button.active = false;
    }
}
