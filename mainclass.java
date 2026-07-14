package ED;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ED.item.ItemRood;
import ED.entities.EntitiesRood;
import ED.tab.TabRoot; // ✅ 只认识 TabRoot 注册中心
import org.slf4j.Logger;

@Mod(mainclass.MODID)
public class mainclass {
    public static final String MODID = "gooduseitem";
    private static final Logger LOGGER = LogUtils.getLogger();

    public mainclass() {
        init(FMLJavaModLoadingContext.get().getModEventBus());
    }

    private void init(IEventBus modEventBus) {
        // 第一步：最先注册实体！
        EntitiesRood.register(modEventBus);
        // 第二步：然后注册物品
        ItemRood.register(modEventBus);
        // 第三步：把创造模式标签页的注册，完全交给 TabRoot 处理！
        TabRoot.register(modEventBus);

        // 第四步：注册配置文件
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}