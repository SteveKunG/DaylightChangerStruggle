package jugglestruggle.timechangerstruggle.client.util.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/**
 *
 * @author JuggleStruggle
 * @implNote Created on 11-Feb-2022, Friday
 */
public final class RenderUtils
{
	public static RainbowShader rainbowAllTheWay;
	
	public static void fillPointedGradient(PoseStack matrices, int startX, int startY, int endX, int endY,
		int z, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor)
	{
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		
		final Tesselator tess = Tesselator.getInstance();
		final BufferBuilder bb = tess.getBuilder();
		final Matrix4f mat = matrices.last().pose();
		
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
		float a = (float)(color >> 24 & 0xFF) / 255.0f;
		float r = (float)(color >> 16 & 0xFF) / 255.0f;
		float g = (float)(color >> 8 & 0xFF) / 255.0f;
		float b = (float)(color & 0xFF) / 255.0f;
		
		bb.vertex(mat, x, y, z).color(r, g, b, a).endVertex();
	}
	
	public static void fillRainbow
	(
	        GuiGraphics graphics, int startX, int startY, int endX, int endY, int z,
		float offsetX, float offsetY, float offsetZ, float progress, boolean adv
	)
	{
		if (!adv)
		{
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
		}
		
		
		
		RenderSystem.setShader(() -> RenderUtils.rainbowAllTheWay);
		
		final Tesselator tess = Tesselator.getInstance();
		final BufferBuilder bb = tess.getBuilder();
		final Matrix4f mat = graphics.pose().last().pose();
		
		bb.begin(VertexFormat.Mode.QUADS, RainbowShader.RAINBOW_SHADER_FORMAT);
		
		float width  = endX - startX;
		float height = endY - startY;
		// Width over Height
		float ratioW = width / height;
		// Height over Width
		float ratioH = height / width;
		
		float topLeftProgress = 0.0f;
		float topRghtProgress = 0.5f;
		float btmLeftProgress = 0.5f;
		float btmRghtProgress = 1.0f;
		
		// Width is higher than height
		if (ratioW > 1.0f) 
		{
			topRghtProgress = 0.5f * ratioW;
			btmLeftProgress = 0.5f;
			btmRghtProgress = topRghtProgress + 0.5f;
		}
		// Height is higher than width
		else if (ratioW < 1.0f)
		{
			topRghtProgress = 0.5f;
			btmLeftProgress = 0.5f * ratioH;
			btmRghtProgress = btmLeftProgress + 0.5f;
		}
		
		RenderUtils.fillRainbowPoint(mat, bb,   endX, startY, z, offsetX, offsetY, offsetZ, progress + topRghtProgress);
		RenderUtils.fillRainbowPoint(mat, bb, startX, startY, z, offsetX, offsetY, offsetZ, progress + topLeftProgress);
		RenderUtils.fillRainbowPoint(mat, bb, startX,   endY, z, offsetX, offsetY, offsetZ, progress + btmLeftProgress);
		RenderUtils.fillRainbowPoint(mat, bb,   endX,   endY, z, offsetX, offsetY, offsetZ, progress + btmRghtProgress);
		
		tess.end();
		
		
		
		if (!adv)
		{
			RenderSystem.disableBlend();
		}
	}
	
	public static void fillRainbowPoint(Matrix4f mat, BufferBuilder bb, int x, int y, int z, 
		float offsetX, float offsetY, float offsetZ, float progress) 
	{
		bb.vertex(mat, x, y, z);
		bb.vertex(mat, offsetX, offsetY, offsetZ);
		
		bb.putFloat(0, progress); bb.nextElement();
		
		bb.endVertex();
	}
}
