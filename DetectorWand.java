package ED.item;

import ED.entities.DetectorArrow;
import ED.entities.EntitiesRood;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetectorWand extends Item {

    // 创建一个专属的日志记录器，方便在控制台搜索
    private static final Logger LOGGER = LoggerFactory.getLogger("DetectorWand");

    public DetectorWand(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 1. 防连点机制
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        // 2. 核心逻辑：仅在服务端执行 NBT 读写和实体生成
        if (!level.isClientSide) {
            CompoundTag tag = stack.getOrCreateTag();
            boolean isCharged = tag.getBoolean("IsCharged");

            if (!isCharged) {
                // 【第一次右键】：拉弓蓄力
                tag.putBoolean("IsCharged", true);


                // 控制台日志：确认状态已切换
                LOGGER.info("✅ 第一次右键：状态已切换为【蓄力中】");

            } else {
                // 【第二次右键】：发射实体
                tag.putBoolean("IsCharged", false);

                // 控制台日志：确认进入发射分支
                LOGGER.info("🚀 第二次右键：状态已重置，准备生成实体...");

                try {
                    DetectorArrow arrow = new DetectorArrow(level, player);
                    arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
                    level.addFreshEntity(arrow);

                    // 控制台日志：确认实体成功生成
                    LOGGER.info("🎯 实体生成成功！实体ID: {}", arrow.getId());
                } catch (Exception e) {
                    // 如果实体生成出错，捕获异常并打印，防止游戏崩溃
                    LOGGER.error("❌ 生成实体时发生错误：", e);
                }

                // 给物品加 1 秒冷却
                player.getCooldowns().addCooldown(this, 0);
            }
        }

        return InteractionResultHolder.success(stack);
    }
}