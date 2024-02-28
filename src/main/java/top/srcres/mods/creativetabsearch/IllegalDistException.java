package top.srcres.mods.creativetabsearch;

import net.minecraftforge.api.distmarker.Dist;

public class IllegalDistException extends RuntimeException {
    private Dist dist;

    public IllegalDistException() {
        this(Dist.DEDICATED_SERVER);
    }
    public IllegalDistException(Dist dist) {
        this.dist = dist;
    }

    @Override
    public String getMessage() {
        if (dist.isClient())
            return "This mod can't be loaded in the client side.";
        else
            return "This mod can't be loaded in the server side.";
    }
}
