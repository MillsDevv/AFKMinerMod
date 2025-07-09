package com.mills.afkminermod.client;

import com.mills.afkminermod.client.mixin.ActionBarAccessorMixin;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;

public class HudAccess {
    public static Text getOverlayMessage(InGameHud hud) {
        return ((ActionBarAccessorMixin) (Object) hud).getOverlayMessage();
    }
}
