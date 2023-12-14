package jugglestruggle.timechangerstruggle.client.util.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.renderer.GameRenderer;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 11-Feb-2022, Friday
 */
public final class RenderUtils
{
    public static void fillPointedGradient(PoseStack matrices, int startX, int startY, int endX, int endY, int z, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor)
    {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        final var tess = Tesselator.getInstance();
        final var bb = tess.getBuilder();
        final var mat = matrices.last().pose();

        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        RenderUtils.fillPoint(mat, bb, endX, startY, z, topRightColor);
        RenderUtils.fillPoint(mat, bb, startX, startY, z, topLeftColor);
        RenderUtils.fillPoint(mat, bb, startX, endY, z, bottomLeftColor);
        RenderUtils.fillPoint(mat, bb, endX, endY, z, bottomRightColor);

        tess.end();

        RenderSystem.disableBlend();
    }

    public static void fillPoint(Matrix4f mat, BufferBuilder bb, int x, int y, int z, int color)
    {
        var a = (color >> 24 & 0xFF) / 255.0f;
        var r = (color >> 16 & 0xFF) / 255.0f;
        var g = (color >> 8 & 0xFF) / 255.0f;
        var b = (color & 0xFF) / 255.0f;

        bb.vertex(mat, x, y, z).color(r, g, b, a).endVertex();
    }
}
