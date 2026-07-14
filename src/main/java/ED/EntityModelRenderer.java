package ED;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * 专门负责渲染实体模型的独立工具类（2D纯文本称号版）
 */
public class EntityModelRenderer {

    private static final float TARGET_SIZE = 2.0F;
    private static final float MAX_SCALE_FACTOR = 0.8F;
    private static final float MIN_SCALE_FACTOR = 1.5F;
    private static final float MIN_BBOX_SIZE = 0.1F;

    public static void render(GuiGraphics guiGraphics, EntityType<?> type, int x, int y, String title) {
        Minecraft mc = Minecraft.getInstance();
        LivingEntity entity = (LivingEntity) type.create(mc.level);
        if (entity == null) return;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();

        // 1. 先平移到指定的屏幕坐标
        poseStack.translate(x - 80, y + 90, 50);

        // 2. 计算等比例缩放
        float originalWidth = Math.max((float) entity.getBbWidth(), MIN_BBOX_SIZE);
        float originalHeight = Math.max((float) entity.getBbHeight(), MIN_BBOX_SIZE);
        float maxOriginalDim = Math.max(originalWidth, originalHeight);
        float equalScaleFactor = TARGET_SIZE / maxOriginalDim;
        equalScaleFactor = Math.max(MIN_SCALE_FACTOR, Math.min(MAX_SCALE_FACTOR, equalScaleFactor));

        float baseUiScale = 45.0F;
        float finalScale = equalScaleFactor * baseUiScale;

        // 3. 脚底对齐逻辑：把原点往下挪动“半个怪物的高度”
        poseStack.translate(0, originalHeight / 2.0F, 0);

        // 4. 应用缩放（Y轴为负数以正确翻转）
        poseStack.scale(finalScale, -finalScale, finalScale);

        // 5. 设置光照
        RenderSystem.setShaderLights(new Vector3f(0.5F, 0.5F, 0.5F), new Vector3f(0.5F, 0.5F, 0.5F));

        // 6. 渲染 3D 实体
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, poseStack, guiGraphics.bufferSource(), 15728880);

        poseStack.popPose(); // 【关键】彻底结束 3D 渲染，恢复正常的 2D 屏幕坐标系

        // 7. 【纯 2D 文字渲染法】直接在屏幕上画字，彻底告别 3D 矩阵翻车
        if (title != null && !title.isEmpty()) {
            Font font = mc.font;
            int textWidth = font.width(title);

            // X 轴：以传入的渲染中心点 (x - 80) 为基准，减去文字宽度的一半，实现完美居中
            float textX = (x - 80) - (textWidth / 2.0F);

            // Y 轴：以传入的渲染中心点 (y + 90) 为基准，固定往上抬 70 个像素。
            // 这个 70 可以根据你的实际效果微调，保证文字刚好悬浮在怪物头顶上方
            float textY = (y + 170) - 70;

            // 使用标准的 GUI 文本渲染，颜色为亮黄色 0xFFFF55，带阴影
            guiGraphics.drawString(font, title, (int) textX, (int) textY, 0xFF5555, true);
        }
    }
}