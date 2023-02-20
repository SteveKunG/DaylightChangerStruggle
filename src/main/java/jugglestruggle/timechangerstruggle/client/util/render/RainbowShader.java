package jugglestruggle.timechangerstruggle.client.util.render;

import jugglestruggle.timechangerstruggle.TimeChangerStruggle;
import jugglestruggle.timechangerstruggle.client.TimeChangerStruggleClient;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleResource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
		super(new ShaderResourceManager(), "rainbow_shader", RainbowShader.RAINBOW_SHADER_FORMAT);
		
		this.timeOffset  = super.getUniform("uTimeOffset");
		this.strokeWidth = super.getUniform("uStrokeWidth");
		this.stripeScale = super.getUniform("uDashCount");
	}

	static class ShaderResourceManager implements ResourceManager
	{
		static final String BASE_LOCATION = "/assets/"+TimeChangerStruggle.MOD_ID+"/";
		
		@Override
		public Resource getResource(ResourceLocation id) throws IOException
		{
			if (id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE))
			{
				if (id.getPath().contains("shaders/core"))
				{
					InputStream resource = TimeChangerStruggleClient.class
						.getResourceAsStream(BASE_LOCATION + id.getPath());
					
					if (resource != null) 
					{
						ResourceLocation newId = new ResourceLocation(TimeChangerStruggle.MOD_ID, id.getPath());
						return new SimpleResource(TimeChangerStruggle.MOD_ID, newId, resource, null);
					}
				}
			}
			
			return null;
		}

		@Override
		public Set<String> getNamespaces() {
			return ImmutableSet.of(TimeChangerStruggle.MOD_ID);
		}

		@Override
		public boolean hasResource(ResourceLocation id) {
			return true;
		}

		@Override
		public List<Resource> getResources(ResourceLocation id) throws IOException {
			return null;
		}

		@Override
		public Collection<ResourceLocation> listResources(String startingPath, Predicate<String> pathPredicate) {
			return null;
		}

		@Override
		public Stream<PackResources> listPacks() {
			return null;
		}
	}
}
