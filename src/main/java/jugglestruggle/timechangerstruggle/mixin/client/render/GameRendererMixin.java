package jugglestruggle.timechangerstruggle.mixin.client.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import jugglestruggle.timechangerstruggle.client.TimeChangerStruggleClient;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;

/**
 * @author JuggleStruggle
 * @implNote Created on 08-Mar-2022, Tuesday
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin
{
    @Inject(method = "getNightVisionScale", cancellable = true, at = @At(value = "HEAD"))
    private static void disableNightVision(LivingEntity livingEntity, float nanoTime, CallbackInfoReturnable<Float> info)
    {
        if (TimeChangerStruggleClient.disableNightVisionEffect)
        {
            info.setReturnValue(0.0f);
        }
    }
}