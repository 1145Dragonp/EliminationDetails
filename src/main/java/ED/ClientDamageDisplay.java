package ED;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.network.chat.Component;
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
            // 把参数喂给 EntityModelRenderer 去画
            EntityModelRenderer.render(guiGraphics, killerType, width / 4, height / 2 + 20, Component.translatable("eliminationdetails.title.killer").getString());
        }

        // --- 2. 执行文字渲染 (你上传的文档逻辑) ---
        renderKillLog(guiGraphics);
    }

    /**
     * 渲染右侧的战报文字列表
     * (逻辑完全来自你上传的文档)
     */
    private static void renderKillLog(GuiGraphics guiGraphics) {
        // 组装排版数据
        List<String[]> lines = new ArrayList<>();
        // 【修改】颜色改为 0xFF5555 (红色)
        lines.add(new String[]{Component.translatable("eliminationdetails.title.detail").getString(), String.valueOf(0xFF5555)});

        DamageTracker.DamageRecord fatal = DamageTracker.GLOBAL_FATAL_BLOW;
        if (fatal != null) {
            lines.add(new String[]{Component.translatable("eliminationdetails.title.fatal_blow", fatal.attackerName).getString(), String.valueOf(0xFF5555)});
            lines.add(new String[]{String.format("%s (%.1f/%.1f)", fatal.playerName, fatal.playerHealth, fatal.playerMaxHealth), String.valueOf(0xFFFFFF)});
            lines.add(new String[]{String.format("%s (%.1f/%.1f)", fatal.attackerName, fatal.attackerHealth, fatal.attackerMaxHealth), String.valueOf(0xFFFF55)});
        }

        for (DamageTracker.DamageRecord r : DamageTracker.GLOBAL_RECORDS) {
            lines.add(new String[]{String.format("%s - %.1f", r.attackerName, r.damage), String.valueOf(0xAAAAAA)});
        }

        if (lines.size() <= 1) {
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
     * 核心逻辑：遍历伤害记录，找出导致玩家死亡的最后一个攻击者类型
     */
    private static EntityType<?> findLastKillerType() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return null;

        EntityType<?> lastAttackerType = null;

        // 倒序遍历，从最新的记录开始找
        for (int i = DamageTracker.GLOBAL_RECORDS.size() - 1; i >= 0; i--) {
            DamageTracker.DamageRecord record = DamageTracker.GLOBAL_RECORDS.get(i);

            // 只关心对当前玩家的伤害记录
            if (record.playerName.equals(player.getName().getString())) {
                // 如果这次伤害导致了死亡（伤害量 >= 受击前的血量），那攻击者就是击杀者
                if (record.damage >= record.playerHealth) {
                    return getEntityTypeByName(record.attackerName);
                }
                // 如果还没找到致命一击，就先记下这个攻击者类型，继续往前找
                if (lastAttackerType == null) {
                    lastAttackerType = getEntityTypeByName(record.attackerName);
                }
            }
        }
        return lastAttackerType;
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