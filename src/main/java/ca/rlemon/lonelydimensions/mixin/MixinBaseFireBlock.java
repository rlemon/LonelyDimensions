package ca.rlemon.lonelydimensions.mixin;

import ca.rlemon.lonelydimensions.common.LonelyDimensions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = {BaseFireBlock.class})
public abstract class MixinBaseFireBlock {

    /**
     * @author rlemon
     * @reason need to be able to generate portals in custom dimensions.
     */
    @Overwrite
    private static boolean inPortalDimension(Level level) {
        return level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER || level.dimension().location().getNamespace().equals(LonelyDimensions.MOD_ID);
    }

}
