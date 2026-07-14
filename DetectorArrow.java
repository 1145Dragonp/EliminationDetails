package ED.entities;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.BlockPos;
import java.util.List;

public class DetectorArrow extends Arrow {

    // ================= 配置文件区域 =================
    // 雷达扫过时的发光时间（单位：秒），可通过修改此值调整
    public static final int RADAR_GLOW_DURATION_SECONDS = 5;

    // 被箭矢直接命中时的发光时间（单位：秒），通常设置得更长
    public static final int HIT_GLOW_DURATION_SECONDS = 15;
    // ================================================

    public DetectorArrow(EntityType<? extends Arrow> type, Level level) {
        super(type, level);
    }

    public DetectorArrow(Level level, LivingEntity shooter) {
        super(level, shooter);
    }

    // 核心逻辑：击中实体后让其发光（时间更长）
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        applyGlowing(result.getEntity(), HIT_GLOW_DURATION_SECONDS);
    }

    // 核心逻辑：高空雷达动态扫描
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // 1. 获取箭矢正下方地面的Y坐标
            BlockPos floorPos = this.level().getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING,
                    this.blockPosition()
            );
            double groundY = floorPos.getY();
            double heightAboveGround = this.getY() - groundY;

            // 2. 动态计算探测范围
            double horizontalRange = heightAboveGround;
            double verticalDepth = heightAboveGround + 15;

            // 3. 构建不对称检测箱并获取实体
            AABB detectionBox = this.getBoundingBox().inflate(horizontalRange, verticalDepth, 5);
            List<Entity> nearbyEntities = this.level().getEntities(this, detectionBox);

            // 4. 给检测到的实体施加发光效果（雷达时间）
            for (Entity entity : nearbyEntities) {
                if (entity != this.getOwner()) {
                    applyGlowing(entity, RADAR_GLOW_DURATION_SECONDS);
                }
            }
        }
    }

    /**
     * 封装的发光方法：使用发光药水效果代替 setGlowingTag
     * @param entity 目标实体
     * @param durationSeconds 发光持续时间（秒）
     */
    private void applyGlowing(Entity entity, int durationSeconds) {
        if (entity instanceof LivingEntity livingEntity) {
            // 将秒转换为游戏刻（1秒 = 20刻）
            int durationTicks = durationSeconds * 20;
            // 参数：效果类型, 持续时间(刻), 等级, 是否隐藏粒子, 是否隐藏图标
            livingEntity.addEffect(new MobEffectInstance(
                    MobEffects.GLOWING,
                    durationTicks,
                    0,
                    false,
                    false
            ));
        }
    }

    public static EntityType.Builder<DetectorArrow> createBuilder() {
        return EntityType.Builder.<DetectorArrow>of(DetectorArrow::new, MobCategory.MISC)
                .sized(0.5F, 0.5F)
                .clientTrackingRange(4)
                .updateInterval(20);
    }
}