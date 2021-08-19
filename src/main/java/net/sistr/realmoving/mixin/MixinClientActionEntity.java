package net.sistr.realmoving.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.sistr.realmoving.init.RealMovingClientInit;
import net.sistr.realmoving.network.Networking;
import net.sistr.realmoving.util.IActionable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientActionEntity extends MixinActionEntity {

    private boolean actioning;
    private boolean actioningSend;

    protected MixinClientActionEntity(EntityType<? extends LivingEntity> type, World worldIn) {
        super(type, worldIn);
    }

    @Inject(at = @At("RETURN"), method = "tickMovement")
    public void onLivingTick(CallbackInfo ci) {
        actioning = RealMovingClientInit.action.isPressed();
    }

    @Inject(at = @At("RETURN"), method = "tick")
    public void postTick(CallbackInfo ci) {
        if (!this.hasVehicle()) {
            boolean isActioning = ((IActionable) this).isActioning_RealMoving();
            if (isActioning != this.actioningSend) {
                Networking.sendPressAction(isActioning ? Networking.ActionType.ACTION_TRUE : Networking.ActionType.ACTION_FALSE);
                this.actioningSend = isActioning;
            }
        }
    }

    @Inject(at = @At("RETURN"), method = "isInSneakingPose", cancellable = true)
    public void onIsCrouching(CallbackInfoReturnable<Boolean> cir) {
        if (isCrawling_RealMoving() || isClimbing_RealMoving()) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public boolean isActioning_RealMoving() {
        return actioning;
    }

}
