package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 诺基亚事件处理器。
 * 装备在 Curios 戒指（ring）栏位时：玩家的每次近战攻击必定暴击。
 *
 * <p>实现要点（经反编译源码核验，NeoForge 21.1.252 / MC 1.21.1）：</p>
 * <ul>
 *   <li>{@link CriticalHitEvent} 在 {@code Player#attack(Entity)} 中触发，客户端与服务端都会 fire，
 *       故两端都需设置暴击状态，伤害与暴击粒子/音效表现才一致；</li>
 *   <li>{@code setCriticalHit(true)} 将本次攻击标记为暴击（触发暴击表现）；</li>
 *   <li>必须同时 {@code setDamageMultiplier(1.5F)}——原版判定为非暴击时，事件构造传入的倍率是 1.0，
 *       若只标记暴击而不改倍率，会出现「显示暴击但伤害无加成」；1.5 即原版暴击倍率。</li>
 * </ul>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class NokiaHandler {

    /** 暴击伤害倍率：与原版暴击一致（1.5 倍）。 */
    private static final float CRIT_MULTIPLIER = 1.5F;

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (!hasNokia(player)) {
            return;
        }
        // 强制本次近战攻击暴击，并确保使用暴击倍率（原版非暴击时传入的倍率为 1.0）
        event.setCriticalHit(true);
        event.setDamageMultiplier(CRIT_MULTIPLIER);
    }

    /** 判断玩家是否在 Curios 饰品栏装备了「诺基亚」。 */
    private static boolean hasNokia(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinitycraft.NOKIA.get())))
                .isPresent();
    }
}
