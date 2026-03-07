package eab.locatelm.locatelm;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;


@Mod(modid = "locatelm", name = "Locate Legacy Mod", version = "1.0")
public class LocateLM {

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Some common initialization can go here
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandLocateNew());
    }

}
