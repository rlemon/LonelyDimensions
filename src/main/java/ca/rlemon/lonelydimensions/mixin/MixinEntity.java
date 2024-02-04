package ca.rlemon.lonelydimensions.mixin;

import ca.rlemon.lonelydimensions.common.LonelyDimensions;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(value = {Entity.class})
public abstract class MixinEntity {
    @Shadow
    public Level level;
    @Shadow
    protected int portalTime;
    @Shadow
    protected boolean isInsidePortal;
    @Shadow
    abstract int getPortalWaitTime();
    @Shadow
    abstract boolean isPassenger();
    @Shadow
    abstract void setPortalCooldown();
    @Shadow
    abstract Entity changeDimension(ServerLevel level);
    @Shadow
    abstract void processPortalCooldown();
    @Shadow
    abstract UUID getUUID();

    /**
     * @author
     * @reason
     */
    @Overwrite
    protected void handleNetherPortal() {
        if (this.level instanceof ServerLevel) {
            int i = this.getPortalWaitTime();
            ServerLevel serverlevel = (ServerLevel)this.level;
            if (this.isInsidePortal) {
                MinecraftServer minecraftserver = serverlevel.getServer();
                ResourceKey<Level> levelKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(LonelyDimensions.MOD_ID, this.getUUID().toString()));
                ResourceKey<Level> resourcekey = this.level.dimension() == Level.NETHER ? levelKey : Level.NETHER;
                ServerLevel serverlevel1 = minecraftserver.getLevel(resourcekey);
                if (serverlevel1 != null && minecraftserver.isNetherEnabled() && !this.isPassenger() && this.portalTime++ >= i) {
                    this.level.getProfiler().push("portal");
                    this.portalTime = i;
                    this.setPortalCooldown();
                    this.changeDimension(serverlevel1);
                    this.level.getProfiler().pop();
                }

                this.isInsidePortal = false;
            } else {
                if (this.portalTime > 0) {
                    this.portalTime -= 4;
                }

                if (this.portalTime < 0) {
                    this.portalTime = 0;
                }
            }

            this.processPortalCooldown();
        }
    }
}
