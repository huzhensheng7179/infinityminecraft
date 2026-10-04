package com.himi.examplemod.event;

import com.himi.examplemod.infinityminecraft;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 贝质素（腰带）事件处理器。装备在 Curios 腰带栏位时：
 * <ul>
 *   <li>免疫摔落伤害（取消 {@link LivingFallEvent}）；</li>
 *   <li>跳跃高度提升到约 2 格（{@link LivingEvent.LivingJumpEvent} 中重设竖直起跳速度）；</li>
 *   <li>可直接跨越 1 格高的方块（{@link Attributes#STEP_HEIGHT} 台阶高度 +0.5，0.6→1.1）。</li>
 * </ul>
 *
 * <p>跳跃与台阶在客户端与服务端两侧一致生效，避免移动预测回弹。</p>
 */
@EventBusSubscriber(modid = infinityminecraft.MODID)
public class BeiZhiSuHandler {

    // 起跳竖直速度：原版 0.42（约 1.25 格），0.55 可达约 2 格
    private static final double JUMP_VELOCITY = 0.55D;

    // 台阶高度属性修饰符：基础 0.6，+0.5 → 1.1，可跨 1 格
    private static final double STEP_HEIGHT_BONUS = 0.5D;
    private static final ResourceLocation STEP_HEIGHT_ID =
            ResourceLocation.fromNamespaceAndPath(infinityminecraft.MODID, "bei_zhi_su.step_height");

    /** 摔落事件：装备贝质素则免疫摔落伤害。 */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!hasBeiZhiSu(player)) {
            return;
        }
        event.setCanceled(true);
    }

    /** 跳跃事件：装备贝质素时把起跳竖直速度重设为约 2 格高度（两侧一致）。 */
    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!hasBeiZhiSu(player)) {
            return;
        }
        Vec3 delta = player.getDeltaMovement();
        player.setDeltaMovement(delta.x, JUMP_VELOCITY, delta.z);
    }

    /** 玩家 Tick：装备时维持台阶高度 +0.5，卸下时移除修饰符（服务端属性会自动同步到客户端）。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight == null) {
            return;
        }
        if (hasBeiZhiSu(player)) {
            stepHeight.addOrUpdateTransientModifier(new AttributeModifier(
                    STEP_HEIGHT_ID, STEP_HEIGHT_BONUS, AttributeModifier.Operation.ADD_VALUE));
        } else {
            stepHeight.removeModifier(STEP_HEIGHT_ID);
        }
    }

    /** 检查玩家是否在 Curios 腰带栏位装备了贝质素。 */
    public static boolean hasBeiZhiSu(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinityminecraft.BEI_ZHI_SU.get())))
                .isPresent();
    }
}
