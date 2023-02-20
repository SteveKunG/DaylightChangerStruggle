package jugglestruggle.timechangerstruggle.client.util.render;

import jugglestruggle.timechangerstruggle.TimeChangerStruggle;
import jugglestruggle.timechangerstruggle.client.TimeChangerStruggleClient;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import java.io.IOException;
import java.util.Optional;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import com.mojang.blaze3d.vertex.VertexFormatElement.Type;
import com.mojang.blaze3d.vertex.VertexFormatElement.Usage;

/**
 * 
 *
 * @author JuggleStruggle
 * @implNote Created on 20-Feb-2022, Sunday
 */
public class RainbowShader extends ShaderInstance
{
	public static final VertexFormat RAINBOW_SHADER_FORMAT;
	public static final VertexFormatElement FLOAT_GENERIC;
	
	static
	{
		FLOAT_GENERIC = new VertexFormatElement(0, Type.FLOAT, Usage.GENERIC, 1);
		
		ImmutableMap.Builder<String, VertexFormatElement> builder = ImmutableMap.builderWithExpectedSize(2);
		
		builder.put("aPosition", DefaultVertexFormat.ELEMENT_POSITION);
		builder.put("aOffset", DefaultVertexFormat.ELEMENT_POSITION);
		builder.put("aProgress", RainbowShader.FLOAT_GENERIC);
		
		RAINBOW_SHADER_FORMAT = new VertexFormat(builder.build());
	}
	
	
	
	
	
	
	public final Uniform strokeWidth;
	public final Uniform stripeScale;
	public final Uniform timeOffset;

	public RainbowShader() throws IOException
	{
		super(new ShaderResourceFactory(), "rainbow_shader", RainbowShader.RAINBOW_SHADER_FORMAT);
		
		this.timeOffset  = super.getUniform("uTimeOffset");
		this.strokeWidth = super.getUniform("uStrokeWidth");
		this.stripeScale = super.getUniform("uDashCount");
	}

	static class ShaderResourceFactory implements ResourceProvider
	{
		static final String BASE_LOCATION = "/assets/"+TimeChangerStruggle.MOD_ID+"/";
		
		@Override
		public Optional<Resource> getResource(ResourceLocation id)
		{
			if (id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE))
			{
				if (id.getPath().contains("shaders/core")) 
				{
				    return Optional.of(new Resource(TimeChangerStruggle.MOD_ID, 
				        () -> TimeChangerStruggleClient.class.getResourceAsStream(BASE_LOCATION + id.getPath())));
				}
			}
			
			return Optional.empty();
		}
	}
}
