package ca.rlemon.lonelydimensions.common;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Integer> SPAWN_Y;
    public static final ForgeConfigSpec.ConfigValue<Boolean> USE_SPAWN_STRUCTURE;

    static {
        BUILDER.push("Configs for LonelyDimensions");
        USE_SPAWN_STRUCTURE = BUILDER.comment("Should the player spawn on a spawn structure? if false the player spawns at the same location they would have in the overworld.")
                .define("Use Spawn Platform", true);
        SPAWN_Y = BUILDER.comment("What Y level the spawning structure should spawn at.")
                .defineInRange("Spawn Y", 136, -64, 320);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
