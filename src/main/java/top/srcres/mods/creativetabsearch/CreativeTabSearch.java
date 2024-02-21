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

    public static CreativeTabSearch instance;

    private Logger logger;

    public CreativeTabSearch(IEventBus modEventBus) {
        // This mod is only client-sided.
        if (!FMLEnvironment.dist.equals(Dist.CLIENT))
            throw new IllegalDistException();

        instance = this;

        logger = LogUtils.getLogger();
    }

    public static CreativeTabSearch getInstance() {
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }
}
