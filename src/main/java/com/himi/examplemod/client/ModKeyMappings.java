package com.himi.examplemod.client;

import com.himi.examplemod.infinitycraft;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键注册（MOD 事件总线，仅客户端）。
 *
 * <p>「打开末影箱」默认绑定到 C 键：装备贝质素时按下即在服务端校验后打开末影箱。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModKeyMappings {

    public static final String KEY_CATEGORY = "key.categories.infinitycraft";

    /** 打开末影箱（装备贝质素时生效），默认 C 键。 */
    public static final KeyMapping OPEN_ENDER_CHEST = new KeyMapping(
            "key.infinitycraft.open_ender_chest", GLFW.GLFW_KEY_C, KEY_CATEGORY);

    @SubscribeEvent
    public static void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        event.register(OPEN_ENDER_CHEST);
    }
}
