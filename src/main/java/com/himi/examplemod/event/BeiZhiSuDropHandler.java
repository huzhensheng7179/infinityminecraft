package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * 贝质素掉落处理器：潜影贝死亡时有 6.4% 基础概率额外掉落一个「贝质素」（腰带饰品），
 * 击杀者武器的「抢夺」附魔每级使掉率相对提升 25%（+1.6 个百分点/级，抢夺 III 时 11.2%）。
 *
 * <p>这是贝质素在「神秘石球奖励」之外的新增获取途径，也呼应其成就「潜影贝的小礼物」。
 * 与铡刃头颅掉落（{@link GuillotineHandler}）同一套模式：在结算掉落物的 {@link LivingDropsEvent}
 * 中掷骰，命中后向 {@code event.getDrops()} 追加一个 {@link ItemEntity}；仅服务端结算。
 * 抢夺等级须自行从击杀者武器读取（本版 {@code LivingDropsEvent} 无 getLootingLevel）。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class BeiZhiSuDropHandler {

    // 潜影贝掉落贝质素的基础概率：6.4%
    private static final double DROP_CHANCE = 0.064D;
    // 每级抢夺的掉率增量：基础的 25% = +1.6 个百分点/级（抢夺 I/II/III → 8.0%/9.6%/11.2%）
    private static final double DROP_CHANCE_PER_LOOTING = DROP_CHANCE * 0.25D;

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;

        // 仅处理潜影贝
        if (victim.getType() != EntityType.SHULKER) return;

        // 抢夺等级：仅当击杀者为玩家时读取其武器上的抢夺（本版事件无 getLootingLevel，须自行解析）
        int lootingLevel = 0;
        DamageSource source = event.getSource();
        if (source.getEntity() instanceof Player killer) {
            Holder<Enchantment> looting = victim.level().registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.LOOTING);
            lootingLevel = EnchantmentHelper.getEnchantmentLevel(looting, killer);
        }

        // 掷骰：基础 6.4% + 每级抢夺 1.6%，命中则额外掉落一个贝质素
        double chance = DROP_CHANCE + DROP_CHANCE_PER_LOOTING * lootingLevel;
        RandomSource random = victim.getRandom();
        if (random.nextDouble() < chance) {
            event.getDrops().add(new ItemEntity(
                    victim.level(), victim.getX(), victim.getY(), victim.getZ(),
                    new ItemStack(infinitycraft.BEI_ZHI_SU.get())));
        }
    }
}
