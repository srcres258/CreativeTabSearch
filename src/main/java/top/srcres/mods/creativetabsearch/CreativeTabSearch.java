package top.srcres.mods.creativetabsearch;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(CreativeTabSearch.MODID)
public class CreativeTabSearch {
    public static final String MODID = "creativetabsearch";

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.examplemod")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(Items.ARROW::getDefaultInstance)
            .displayItems((parameters, output) -> {
                output.accept(Items.ARROW); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());

    public static CreativeTabSearch instance;

    private Logger logger;

    public CreativeTabSearch(IEventBus modEventBus) {
        // This mod is only client-sided.
        if (!FMLEnvironment.dist.equals(Dist.CLIENT))
            throw new IllegalDistException();

        instance = this;

        logger = LogUtils.getLogger();

        CREATIVE_MODE_TABS.register(modEventBus);
    }

    public static CreativeTabSearch getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }
}
