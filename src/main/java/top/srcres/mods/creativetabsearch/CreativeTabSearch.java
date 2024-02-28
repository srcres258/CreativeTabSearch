package top.srcres.mods.creativetabsearch;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(CreativeTabSearch.MODID)
public class CreativeTabSearch {
    public static final String MODID = "creativetabsearch";

    public static CreativeTabSearch instance;

    private Logger logger;

    public CreativeTabSearch() {
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
