package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/**
 * 美袋团鼠（手镯）事件处理器。
 *
 * <p>装备在 Curios 护符栏位时：每当玩家<b>食用完一种食物</b>（{@link LivingEntityUseItemEvent.Finish}
 * 且物品带 {@link DataComponents#FOOD} 组件），随机获得 <b>1~3 个</b>正面效果，持续 <b>16 秒</b>。
 * 效果从「豪华池」中<b>不重复</b>地抽取，包含伤害吸收、力量 II、抗性提升 II 等较强增益。</p>
 *
 * <p>仅在服务端结算，效果与提示由原版机制自动同步到客户端。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class SugarGliderBraceletHandler {

    /** 16 秒 = 320 ticks。 */
    private static final int EFFECT_DURATION = 320;

    /** 一次最多随机到的效果个数。 */
    private static final int MAX_ROLL = 3;

    /**
     * 豪华随机池：（效果, 等级放大器）。力量与抗性提升为 II（amplifier 1），其余为 I。
     */
    private static final PoolEntry[] POOL = {
            new PoolEntry(MobEffects.REGENERATION, 0),        // 生命恢复 I
            new PoolEntry(MobEffects.ABSORPTION, 0),          // 伤害吸收 I
            new PoolEntry(MobEffects.DAMAGE_RESISTANCE, 1),   // 抗性提升 II
            new PoolEntry(MobEffects.DAMAGE_BOOST, 1),        // 力量 II
            new PoolEntry(MobEffects.MOVEMENT_SPEED, 0),      // 速度 I
            new PoolEntry(MobEffects.DIG_SPEED, 0),           // 效率 I
            new PoolEntry(MobEffects.JUMP, 0),                // 跳跃提升 I
            new PoolEntry(MobEffects.FIRE_RESISTANCE, 0),     // 抗火
            new PoolEntry(MobEffects.NIGHT_VISION, 0),        // 夜视
            new PoolEntry(MobEffects.WATER_BREATHING, 0),     // 水下呼吸
            new PoolEntry(MobEffects.LUCK, 0),                // 幸运 I
            new PoolEntry(MobEffects.SLOW_FALLING, 0),        // 缓慢下降
    };

    /** 池内一项：效果 Holder + 放大器。 */
    private record PoolEntry(Holder<MobEffect> effect, int amplifier) {
    }

    /**
     * 食用完成：佩戴饰品且吃的是食物时，随机施加 1~3 个正面效果。
     */
    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        ItemStack stack = event.getItem();
        if (!stack.has(DataComponents.FOOD)) return; // 仅食物/饮品触发
        if (!hasBracelet(player)) return;

        if (!(player.level() instanceof ServerLevel level)) return;

        // 不重复抽取 1~3 项：用服务端随机源洗牌后取前 n 个
        List<PoolEntry> shuffled = new ArrayList<>(List.of(POOL));
        for (int i = shuffled.size() - 1; i > 0; i--) {
            int j = level.random.nextInt(i + 1);
            PoolEntry tmp = shuffled.get(i);
            shuffled.set(i, shuffled.get(j));
            shuffled.set(j, tmp);
        }
        int count = 1 + level.random.nextInt(MAX_ROLL);

        for (int i = 0; i < count && i < shuffled.size(); i++) {
            PoolEntry entry = shuffled.get(i);
            player.addEffect(new MobEffectInstance(entry.effect(), EFFECT_DURATION, entry.amplifier()));
        }

        // 反馈：金光粒子 + 升级音效，让玩家知道「小家伙生效了」
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                player.getX(), player.getY() + player.getBbHeight() * 0.7D, player.getZ(),
                count * 8, player.getBbWidth() * 0.6D, 0.6D, player.getBbWidth() * 0.6D, 0.0D);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.6F);
    }

    /** 检查玩家是否在任意 Curios 栏位（本物品为护符槽）装备了美袋团鼠。 */
    private static boolean hasBracelet(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack ->
                        stack.is(infinitycraft.SUGAR_GLIDER_BRACELET.get())))
                .isPresent();
    }
}
