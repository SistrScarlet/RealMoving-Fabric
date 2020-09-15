package net.sistr.realmoving;

import net.fabricmc.api.ModInitializer;
import net.sistr.realmoving.network.Networking;

public class RealMovingMod implements ModInitializer {
	public static final String MODID = "realmoving";

	@Override
	public void onInitialize() {
		Networking.serverPacketRegister();
	}
}
