package ED;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 专门负责渲染错误占位图的类
 * 当实体模型无法渲染时，使用多语言图片进行替代
 */
public class ErrorRenderer {

    // 缓存已加载的图片资源，避免每帧都去查找
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();
    // 缓存图片的真实尺寸，避免每帧都去读文件
    private static final Map<String, int[]> SIZE_CACHE = new HashMap<>();

    // 基础图片路径
    private static final String BASE_PATH = "textures/gui/what.png";
    private static final String EN_PATH = "textures/gui/what.en_us.png";
    private static final String ZH_PATH = "textures/gui/what.zh_cn.png";

    /**
     * 渲染错误占位图
     */
    public static void render(GuiGraphics guiGraphics, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // 1. 根据当前游戏语言选择对应的图片
        String currentLang = mc.getLanguageManager().getSelected();
        ResourceLocation texture = getTexture(currentLang);

        // 2. 获取图片的真实原始尺寸，防止图片被强制拉伸或压缩变形
        int[] size = getImageSize(mc, texture);
        int imgWidth = size[0];
        int imgHeight = size[1];

        // 3. 设置缩放比例 (例如 0.8f 代表缩小到原来的 80%)
        float scale = 0.5f;

        // 计算缩放后的实际绘制宽高，用于精准居中计算
        int drawWidth = (int) (imgWidth * scale);
        int drawHeight = (int) (imgHeight * scale);

        // 4. 渲染图片 (保持居中，并应用缩放和位置偏移)
        RenderSystem.enableBlend();
        guiGraphics.pose().pushPose(); // 保存当前渲染状态

        // 移动到居中位置，并应用你之前调好的 -20 像素 X 轴偏移
        guiGraphics.pose().translate(x - drawWidth / 2f - 60, y - drawHeight / 2f, 0);

        guiGraphics.pose().scale(scale, scale, 1.0f); // 应用等比例缩放

        // 按原始尺寸绘制，由 pose 负责缩放，绝对不会变形
        guiGraphics.blit(texture, 0, 0, 0, 0, imgWidth, imgHeight, imgWidth, imgHeight);

        guiGraphics.pose().popPose(); // 恢复渲染状态，防止影响其他UI
        RenderSystem.disableBlend();
    }

    /**
     * 获取对应语言的图片资源位置
     */
    private static ResourceLocation getTexture(String langCode) {
        String path = BASE_PATH;
        if ("zh_cn".equals(langCode)) {
            path = ZH_PATH;
        } else if ("en_us".equals(langCode)) {
            path = EN_PATH;
        }

        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }

        // 注意：请确保你的模组ID是 "eliminationdetails"
        ResourceLocation location = new ResourceLocation("eliminationdetails", path);
        CACHE.put(path, location);
        return location;
    }

    /**
     * 通过读取 PNG 文件头获取图片真实宽高
     */
    private static int[] getImageSize(Minecraft mc, ResourceLocation location) {
        String path = location.getPath();
        if (SIZE_CACHE.containsKey(path)) {
            return SIZE_CACHE.get(path);
        }

        int width = 64;  // 默认兜底尺寸
        int height = 64; // 默认兜底尺寸

        try {
            Resource resource = mc.getResourceManager().getResource(location).orElse(null);
            if (resource != null) {
                try (InputStream inputStream = resource.open()) {
                    // 跳过 PNG 文件头，读取宽高数据
                    inputStream.skip(16);
                    byte[] buffer = new byte[8];
                    if (inputStream.read(buffer) == 8) {
                        width = ((buffer[0] & 0xFF) << 24) | ((buffer[1] & 0xFF) << 16) | ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
                        height = ((buffer[4] & 0xFF) << 24) | ((buffer[5] & 0xFF) << 16) | ((buffer[6] & 0xFF) << 8) | (buffer[7] & 0xFF);
                    }
                }
            }
        } catch (Exception e) {
            // 如果读取失败，静默使用默认的 64x64
        }

        int[] result = new int[]{width, height};
        SIZE_CACHE.put(path, result);
        return result;
    }
}