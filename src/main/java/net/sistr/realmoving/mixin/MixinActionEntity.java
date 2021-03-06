package net.sistr.realmoving.mixin;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import net.sistr.realmoving.network.Networking;
import net.sistr.realmoving.util.ClimbBlockData;
import net.sistr.realmoving.util.IActionable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class MixinActionEntity extends LivingEntity implements IActionable {
    @Shadow public abstract EntityDimensions getDimensions(EntityPose pose);

    private final ClimbBlockData[] climbBlockData = new ClimbBlockData[4];
    private boolean actioning;
    private boolean crawling;
    private boolean climbing;

    private int slideTime;
    private float climbHeightCache;

    protected MixinActionEntity(EntityType<? extends LivingEntity> type, World worldIn) {
        super(type, worldIn);
    }

    @Inject(at = @At("HEAD"), method = "updateSize", cancellable = true)
    public void onUpdatePose(CallbackInfo ci) {
        updateClimbing();
        updateCrawling();
        if (((IActionable) this).isCrawling() && this.wouldPoseNotCollide(EntityPose.SWIMMING)) {
            setPose(EntityPose.SWIMMING);
            ci.cancel();
        }
    }

    @Inject(at = @At("RETURN"), method = "tick")
    public void postTick(CallbackInfo ci) {
        if (isSliding()) {
            Vec3d motion = getVelocity();
            motion = motion.normalize().multiply((1F - (slideTime / 20F) * (slideTime / 20F)) * 0.2F);
            setVelocity(getVelocity().multiply(0.6D).add(motion));
            if (20 < ++slideTime) {
                setSprinting(false);
            }
        } else {
            slideTime = 0;
        }
        if (isClimbing() && !isSliding()) {
            fallDistance = 0;
            setVelocity(getVelocity().multiply(1D, 0, 1D));
            float climbHeight = getClimbHeight();
            if (horizontalCollision) {
                if (0.1 < climbHeight) {
                    setVelocity(getVelocity().add(0, 0.1D, 0));
                } else if (isCrawling() &&
                        (canHook(climbBlockData[0], climbBlockData[1], getHeight())
                                || canHook(climbBlockData[1], climbBlockData[2], getHeight()))
                        || (canStand(climbBlockData[0], climbBlockData[1], climbBlockData[2], getHeight())
                        || canStand(climbBlockData[1], climbBlockData[2], climbBlockData[3], getHeight()))) {
                    setVelocity(getVelocity().add(0, 0.2, 0));
                    setClimbing(false);
                }
                //サーバー側ではうまく発動しない(原因不明)
                if (world.isClient && isSneaking() && climbHeight + getDimensions(EntityPose.SWIMMING).height < getHeight()) {
                    setCrawling(true);
                    Networking.sendPressAction(Networking.ActionType.CRAWLING_TRUE);
                }
            } else if (climbHeight < 2 - 0.2) {
                setVelocity(getVelocity().subtract(0, 0.1D, 0));
            }
        }
    }

    public void updateCrawling() {
        if (isCrawling()) {
            setCrawling((!isTouchingWater() && !hasVehicle() && (isSneaking() || isActioning())));
        } else {
            setCrawling(onGround && !isTouchingWater() && !hasVehicle() && isSneaking() && isActioning() && !isClimbing());
        }
    }

    public void updateClimbing() {
        climbHeightCache = -1;
        //伏せを優先するため、着地かつスニーク中は発動しない
        if (!(onGround && isSneaking() && !isCrawling()) && !hasVehicle() && !isTouchingWater() && isActioning()) {
            float climbHeight = getClimbHeight();
            boolean climbing = stepHeight < climbHeight || (isClimbing() && 0 < climbHeight);
            setClimbing(climbing);
            if (climbing) {
                this.climbHeightCache = climbHeight;
            }
        } else {
            setClimbing(false);
        }
    }

    //登れるブロックの上面がエンティティの底面よりどれだけ高いかを返す
    //-1 < x < 2
    public float getClimbHeight() {
        if (this.climbHeightCache != -1) {
            return climbHeightCache;
        }
        BlockPos base = getBaseClimbBlock();
        ClimbBlockData downData = climbBlockData[0] = getClimbBlockData(base.down());
        ClimbBlockData standData = climbBlockData[1] = getClimbBlockData(base);
        ClimbBlockData upData = climbBlockData[2] = getClimbBlockData(base.up());
        ClimbBlockData toData = climbBlockData[3] = getClimbBlockData(base.up(2));

        //登れるブロックの選択
        EntityDimensions swimSize = getDimensions(EntityPose.SWIMMING);
        float swimHeight = swimSize.height;
        if (canHook(standData, upData, swimHeight)) {
            return standData.getUp() - (float) getY();
        } else if (!isCrawling() && canHook(upData, toData, swimHeight)) {
            return upData.getUp() - (float) getY();
        } else if (canHook(downData, standData, swimHeight)) {
            return downData.getUp() - (float) getY();
        }
        return -1;
    }

    public boolean canHook(ClimbBlockData climb, ClimbBlockData up, float height) {
        return !climb.isEmpty() //床アリ
                && (up.isEmpty() || height < up.getDown() - climb.getUp());//下スキマ
    }

    public boolean canStand(ClimbBlockData climb, ClimbBlockData up, ClimbBlockData to, float height) {
        return !climb.isEmpty() //床アリ
                && up.isEmpty() && (to.isEmpty() || height < to.getDown() - climb.getUp());//下空上空またはスキマ
    }

    public ClimbBlockData getClimbBlockData(BlockPos pos) {
        VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
        return shape.isEmpty() ? ClimbBlockData.DUMMY :
                new ClimbBlockData(pos.getY(), (float) shape.getMaximum(Direction.Axis.Y), (float) shape.getMinimum(Direction.Axis.Y));
    }

    public BlockPos getBaseClimbBlock() {
        Vec3d look = this.getRotationVector(0, this.yaw);
        return new BlockPos(getPos().add(look.multiply(0.5D)));
    }

    @Override
    public void setActioning(boolean actioning) {
        this.actioning = actioning;
    }

    @Override
    public boolean isActioning() {
        return this.actioning;
    }

    @Override
    public void setCrawling(boolean crawling) {
        this.crawling = crawling;
    }

    @Override
    public boolean isCrawling() {
        return this.crawling;
    }

    @Override
    public boolean isSliding() {
        return isSprinting() && isCrawling();
    }

    @Override
    public void setClimbing(boolean climbing) {
        this.climbing = climbing;
    }

    public boolean isClimbing() {
        return this.climbing;
    }

}
