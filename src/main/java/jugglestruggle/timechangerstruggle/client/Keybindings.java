package jugglestruggle.timechangerstruggle.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/**
 *
 * @author JuggleStruggle
 * @implNote
 * Created on 26-Jan-2022 Wednesday
 */
public class Keybindings
{
    public static KeyMapping timeChangerMenuKey;
    public static KeyMapping toggleWorldTimeKey;

    public static void registerKeybindings()
    {
        if (Keybindings.timeChangerMenuKey != null)
            return;

        Keybindings.timeChangerMenuKey = Keybindings.registerBinding("timechangermenu");
        Keybindings.toggleWorldTimeKey = Keybindings.registerBinding("toggleworldtime");
    }

    private static KeyMapping registerBinding(String keyName)
    {
        return Keybindings.registerBinding(keyName, "timechanger");
    }

    private static KeyMapping registerBinding(String keyName, String category)
    {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("jugglestruggle.tcs.key." + keyName, InputConstants.Type.KEYSYM, -1, "jugglestruggle.tcs.keycategory." + category));
    }
}
