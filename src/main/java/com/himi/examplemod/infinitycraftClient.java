package com.himi.examplemod;

import com.himi.examplemod.client.ModKeyMappings;
import com.himi.examplemod.client.model.SnifferHatModel;
import com.himi.examplemod.client.renderer.BlockProjectileRenderer;
import com.himi.examplemod.client.renderer.ShadowCloneRenderer;
import com.himi.examplemod.client.renderer.WanderingSnifferMerchantRenderer;
import com.himi.examplemod.network.OpenEnderChestPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.PacketDistributor;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = infinitycraft.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = infinitycraft.MODID, value = Dist.CLIENT)
public class infinitycraftClient {
    public infinitycraftClient(IEventBus modEventBus, ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        // 实体渲染器与模型层定义属于 MOD 总线事件，显式注册到 modEventBus
        modEventBus.addListener(infinitycraftClient::registerRenderers);
        modEventBus.addListener(infinitycraftClient::registerLayerDefinitions);
        // 为反之弓注册 pulling / pull 自定义物品属性（原版仅对 Items.BOW 注册，自定义弓类需自行注册才能触发蓄力贴图切换）
        modEventBus.addListener(infinitycraftClient::registerItemProperties);
    }

    /** 为「反之弓」注册 pulling / pull 属性，逻辑与原版弓一致，使模型 overrides 随蓄力切换贴图。 */
    static void registerItemProperties(FMLClientSetupEvent event) {
        ResourceLocation pulling = ResourceLocation.withDefaultNamespace("pulling");
        ResourceLocation pull = ResourceLocation.withDefaultNamespace("pull");
        event.enqueueWork(() -> ItemProperties.register(
                infinitycraft.REVERSE_BOW.get(), pulling,
                (stack, level, entity, seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F));
        event.enqueueWork(() -> ItemProperties.register(
                infinitycraft.REVERSE_BOW.get(), pull,
                (stack, level, entity, seed) -> entity == null ? 0.0F
                        : (entity.getUseItem() != stack ? 0.0F
                        : (float) (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20.0F)));
    }

    /** 注册流浪嗅探兽商人渲染器与影流分身渲染器。 */
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(infinitycraft.WANDERING_SNIFFER_MERCHANT.get(), WanderingSnifferMerchantRenderer::new);
        event.registerEntityRenderer(infinitycraft.SHADOW_CLONE.get(), ShadowCloneRenderer::new);
        event.registerEntityRenderer(infinitycraft.BLOCK_PROJECTILE.get(), BlockProjectileRenderer::new);
    }

    /** 注册帽子模型层定义（嗅探兽本体模型 ModelLayers.SNIFFER 由原版提供，无需重复注册）。 */
    static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SnifferHatModel.HAT_LAYER, SnifferHatModel::createBodyLayer);
    }

    /**
     * 客户端 Tick：装备贝质素时按 C 键发送「打开末影箱」包，由服务端校验后打开界面。
     */
    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        while (ModKeyMappings.OPEN_ENDER_CHEST.consumeClick()) {
            if (Minecraft.getInstance().player != null) {
                PacketDistributor.sendToServer(new OpenEnderChestPayload());
            }
        }
    }
}
