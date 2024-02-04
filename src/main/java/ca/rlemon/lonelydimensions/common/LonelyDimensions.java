package ca.rlemon.lonelydimensions.common;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;


@Mod(LonelyDimensions.MOD_ID)
public class LonelyDimensions
{
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "lonelydimensions";

    public LonelyDimensions()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC, "lonelydimensions-common.toml");

    }
}
