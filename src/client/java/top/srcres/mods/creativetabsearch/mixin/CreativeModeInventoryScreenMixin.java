package top.srcres.mods.creativetabsearch.mixin;

import net.fabricmc.fabric.impl.itemgroup.FabricItemGroup;
import net.fabricmc.fabric.mixin.itemgroup.ItemGroupAccessor;
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
            this.tabSearch_editBox = new EditBox(this.font, this.leftPos, this.topPos - 45,
                    this.imageWidth - 20, 15, this.tabSearch_editBox, Component.literal(""));
            this.tabSearch_editBox.setResponder(this::tabSearch_updateTabSearch);
            this.addWidget(this.tabSearch_editBox);
            this.tabSearch_clearButton = Button.builder(Component.literal("X"), this::tabSearch_clearButtonClicked)
                    .bounds(this.leftPos + this.imageWidth - 20, this.topPos - 45, 20, 15).build();
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
            tabSearch_resetCreativeTabPages(BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList());
            this.tabSearch_clearButton.active = false;
        } else {
            List<CreativeModeTab> result = tabSearch_getMatchingTabs(pNewText);
            if (result.isEmpty()) {
                this.tabSearch_editBox.setTextColor(0xff0000);
                tabSearch_resetCreativeTabPages(BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList());
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
                                for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList()) {
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
                                for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList()) {
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
            for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList()) {
                if (tab.getDisplayName().getString().contains(searchStr)) {
                    result.add(tab);
                }
            }
            return result;
        }
    }

    @Unique
    private void tabSearch_resetCreativeTabPages(List<CreativeModeTab> tabList) {
        int curPage = 0;
        int curIndex = 0;
        for (CreativeModeTab tab : tabList) {
            // Only do filtering for CATEGORY creative tabs rather than other types of tabs
            // such as the "survival mode inventory" tab.
            if (!tab.getType().equals(CreativeModeTab.Type.CATEGORY))
                continue;

            // Judge if the subtypes are able to convert to for the sake of avoiding possible exceptions.
            if (!(tab instanceof ItemGroupAccessor && tab instanceof FabricItemGroup)) {
                throw new IllegalStateException("The CreativeModeTab object is not a instance of ItemGroupAccessor" +
                        "and FabricItemGroup, which should never happen within Fabric environment.");
            }

            ItemGroupAccessor tabAccessor = (ItemGroupAccessor) tab;
            FabricItemGroup tabGroup = (FabricItemGroup) tab;
            tabAccessor.setRow(curIndex < 5 ? CreativeModeTab.Row.TOP : CreativeModeTab.Row.BOTTOM);
            tabAccessor.setColumn(curIndex % 5);
            tabGroup.setPage(curPage);

            if (curIndex >= 10) {
                curPage++;
                curIndex = 0;
            } else {
                curIndex++;
            }
        }

        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList()) {
            if (!tabList.contains(tab)) {
                if (tab instanceof FabricItemGroup) {
                    // Set the invisible tab page number to the max integer value in order to simulate it is
                    // invisible and make the page shifting button visible as well.
                    ((FabricItemGroup) tab).setPage(Integer.MAX_VALUE);
                } else {
                    throw new IllegalStateException("The CreativeModeTab object is not a instance of FabricItemGroup," +
                            "which should never happen within Fabric environment.");
                }
            }
        }

        // Auto-select the first CATEGORY tab.
        for (CreativeModeTab tab : tabList) {
            if (tab.getType().equals(CreativeModeTab.Type.CATEGORY)) {
                this.selectTab(tab);
                break;
            }
        }
    }

    @Unique
    private void tabSearch_clearButtonClicked(Button button) {
        this.tabSearch_editBox.setValue("");
        button.active = false;
    }
}
