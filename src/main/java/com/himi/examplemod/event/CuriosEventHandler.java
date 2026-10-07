package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 处理避箭之戒的弹射物免疫逻辑。
 * 当玩家在 Curios 戒指栏位装备避箭之戒时，免疫所有弹射物伤害，
 * 每次抵挡扣除1点耐久。
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class CuriosEventHandler {

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        DamageSource source = event.getSource();
        // 仅处理弹射物伤害
        if (!source.is(DamageTypeTags.IS_PROJECTILE)) return;

        // 在 Curios 戒指栏位中查找避箭之戒
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            handler.findFirstCurio(stack -> stack.is(infinitycraft.ARROW_DEFLECTION_RING.get()))
                    .ifPresent(result -> {
                        ItemStack ring = result.stack();
                        // 取消弹射物伤害
                        event.setCanceled(true);
                        // 扣除1点耐久（耐久附魔会自动生效）
                        if (player.level() instanceof ServerLevel serverLevel) {
                            ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
                            ring.hurtAndBreak(1, serverLevel, serverPlayer, (item) -> {
                                // 戒指耐久归零时销毁
                            });
                        }
                    });
        });
    }
}
