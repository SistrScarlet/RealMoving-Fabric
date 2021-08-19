package net.sistr.realmoving.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.sistr.realmoving.RealMovingMod;
import net.sistr.realmoving.util.IActionable;

public class Networking {
    public static final Identifier PRESS_ACTION_PACKET_ID = new Identifier(RealMovingMod.MODID, "action");

    public static void sendPressAction(ActionType type) {
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
        passedData.writeEnumConstant(type);
        ClientSidePacketRegistry.INSTANCE.sendToServer(PRESS_ACTION_PACKET_ID, passedData);
    }

    public static void serverPacketRegister() {
        ServerSidePacketRegistry.INSTANCE.register(PRESS_ACTION_PACKET_ID, (packetContext, attachedData) -> {
            ActionType type = attachedData.readEnumConstant(ActionType.class);
            packetContext.getTaskQueue().execute(() -> {
                PlayerEntity player = packetContext.getPlayer();
                if (player == null) {
                    return;
                }
                switch (type) {
                    case ACTION_TRUE:
                        ((IActionable)player).setActioning_RealMoving(true);
                        break;
                    case ACTION_FALSE:
                        ((IActionable)player).setActioning_RealMoving(false);
                        break;
                    case CRAWLING_TRUE:
                        ((IActionable)player).setCrawling_RealMoving(true);
                        break;
                    case CRAWLING_FALSE:
                        ((IActionable)player).setCrawling_RealMoving(false);
                        break;
                    case CLIMBING_TRUE:
                        ((IActionable)player).setClimbing_RealMoving(true);
                        break;
                    case CLIMBING_FALSE:
                        ((IActionable)player).setClimbing_RealMoving(false);
                        break;
                }

            });
        });
    }

    public enum ActionType {
        ACTION_TRUE,
        ACTION_FALSE,
        CRAWLING_TRUE,
        CRAWLING_FALSE,
        CLIMBING_TRUE,
        CLIMBING_FALSE
    }
}
