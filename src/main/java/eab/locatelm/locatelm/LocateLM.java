package eab.locatelm.locatelm;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import eab.locatelm.locatelm.lib.CommandLocateNew;

@Mod(modid = "locatelm", name = "Locate Legacy Mod", version = "1.1.0", dependencies = "required-after:eab_structure_api")
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