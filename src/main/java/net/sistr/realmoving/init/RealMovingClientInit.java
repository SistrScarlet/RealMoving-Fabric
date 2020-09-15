package net.sistr.realmoving.init;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.sistr.realmoving.RealMovingMod;
import org.lwjgl.glfw.GLFW;

public class RealMovingClientInit implements ClientModInitializer {
    public static KeyBinding action;

    @Override
    public void onInitializeClient() {
        action = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                RealMovingMod.MODID + ".key.action", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_V, // The keycode of the key
                "key.categories.movement" // The translation key of the keybinding's category.
        ));
    }
}