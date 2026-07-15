package ED;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = mainclass.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientDamageDisplay {
    private static final Logger LOGGER = LogManager.getLogger("DamageDisplay");
    // 防刷屏开关
    private static boolean hasLoggedThisDeath = false;

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        Minecraft mc = Minecraft.getInstance();
        // 只要不是死亡界面，就重置开关并返回
        if (!(event.getScreen() instanceof DeathScreen)) {
            hasLoggedThisDeath = false;
            return;
        }

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int width = event.getScreen().width;
        int height = event.getScreen().height;

        // --- 1. 调度实体渲染 ---
        EntityType<?> killerType = findLastKillerType();

        if (killerType != null) {
            // 使用国际化键值替换硬编码字符串
            String killerTitle = Component.translatable("eliminationdetails.title.killer").getString();

            // 尝试渲染实体模型
            try {
                EntityModelRenderer.render(guiGraphics, killerType, width / 4, height / 2 + 20, killerTitle);
            } catch (Exception e) {
                // 如果实体渲染失败，显示占位图
                //LOGGER.warn("实体渲染失败，显示占位图", e);
                ErrorRenderer.render(guiGraphics, width / 4, height / 2 - 40);
            }
        } else {
            // 【关键修复】如果找不到击杀者实体类型，直接显示占位图
            //LOGGER.info("未找到击杀者实体类型，显示占位图");
            ErrorRenderer.render(guiGraphics, width / 4, height / 2 + 20);
        }

        // --- 2. 执行文字渲染 ---
        renderKillLog(guiGraphics);
    }

    /**
     * 渲染右侧的战报文字列表
     */
    private static void renderKillLog(GuiGraphics guiGraphics) {
        // 组装排版数据
        List<String[]> lines = new ArrayList<>();

        // 使用国际化键值替换硬编码字符串
        lines.add(new String[]{Component.translatable("eliminationdetails.title.detail").getString(), String.valueOf(0xFF5555)});

        DamageTracker.DamageRecord fatal = DamageTracker.GLOBAL_FATAL_BLOW;
        if (fatal != null) {
            // 使用国际化键值和占位符
            String fatalBlowText = Component.translatable("eliminationdetails.title.fatal_blow", fatal.attackerName).getString();
            lines.add(new String[]{fatalBlowText, String.valueOf(0xFF5555)});

            lines.add(new String[]{String.format("%s (%.1f/%.1f)", fatal.playerName, fatal.playerHealth, fatal.playerMaxHealth), String.valueOf(0xFFFFFF)});
            lines.add(new String[]{String.format("%s (%.1f/%.1f)", fatal.attackerName, fatal.attackerHealth, fatal.attackerMaxHealth), String.valueOf(0xFFFF55)});
        }

        for (DamageTracker.DamageRecord r : DamageTracker.GLOBAL_RECORDS) {
            lines.add(new String[]{String.format("%s - %.1f", r.attackerName, r.damage), String.valueOf(0xAAAAAA)});
        }

        if (lines.size() <= 1) {
            // 使用国际化键值替换硬编码字符串
            lines.add(new String[]{Component.translatable("eliminationdetails.title.one_shot").getString(), String.valueOf(0xAAAAAA)});
        }

        // 只在刚打开死亡界面的第一帧打印一次日志
        if (!hasLoggedThisDeath) {
            hasLoggedThisDeath = true;
            LOGGER.info("========== [DamageDisplay] 完整淘汰详情 ==========");
            for (String[] line : lines) {
                LOGGER.info("[DamageDisplay] {}", line[0]);
            }
            LOGGER.info("==================================================");
        }

        // 开始渲染
        Font font = Minecraft.getInstance().font;
        int x = 370;
        int baseY = 90;
        int lineHeight = font.lineHeight + 2;
        for (int i = 0; i < lines.size(); i++) {
            String[] lineData = lines.get(i);
            String text = lineData[0];
            int color = Integer.parseInt(lineData[1]);
            int drawY = baseY + (i * lineHeight);
            guiGraphics.drawString(font, text, x, drawY, color, true);
        }
    }

    /**
     * 【核心修复】查找最后的击杀者类型
     * 逻辑：直接返回 GLOBAL_FATAL_BLOW 中的攻击者类型，保证左右显示一致
     */
    private static EntityType<?> findLastKillerType() {
        DamageTracker.DamageRecord fatal = DamageTracker.GLOBAL_FATAL_BLOW;
        if (fatal != null) {
            return getEntityTypeByName(fatal.attackerName);
        }
        return null;
    }

    /**
     * 通过攻击者的名字（如“尸壳”）查找对应的实体类型
     */
    private static EntityType<?> getEntityTypeByName(String name) {
        if (name == null) return null;
        // 1. 尝试直接通过资源位置查找 (例如 "minecraft:husk")
        Optional<EntityType<?>> optional = BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.tryParse(name.toLowerCase()));
        if (optional.isPresent()) return optional.get();
        // 2. 尝试通过显示名称查找 (例如 "尸壳")
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getDescription().getString().equals(name)) {
                return type;
            }
        }
        return null;
    }
}