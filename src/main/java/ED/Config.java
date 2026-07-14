package ED;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// 订阅 MOD 总线，以便监听配置加载事件
@Mod.EventBusSubscriber(modid = mainclass.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // 1. 定义配置项：发光持续时间 (秒)
    public static final ForgeConfigSpec.IntValue GLOW_DURATION;

    static {
        BUILDER.comment("MrMagicDragon 模组配置").push("Detector Arrow Settings");

        GLOW_DURATION = BUILDER
                .comment("雷达箭命中或被探测到的实体，发光效果持续的时间（单位：秒）")
                .defineInRange("glowDurationSeconds", 15, 1, 3600);

        BUILDER.pop();
    }

    // 2. 构建配置规范（这是 mainclass 注册时需要的核心对象）
    public static final ForgeConfigSpec SPEC = BUILDER.build();

    // 3. 运行时变量（代码里实际读取的值）
    public static int glowDurationSeconds;

    // 4. 监听配置加载事件，把配置文件的值同步给运行时变量
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        glowDurationSeconds = GLOW_DURATION.get();
    }
}