package com.himi.examplemod.entity;

import java.util.EnumSet;

import javax.annotation.Nullable;

import com.himi.examplemod.infinitycraft;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * 流浪嗅探兽商人：完整保留原版嗅探兽的生物行为（掘地、嗅探、漫步、受击恐慌等，掘地本就会挖出火把花种子/瓶子草荚），
 * 在此基础上叠加「流浪商人式」的交易能力（{@link Merchant}）。
 *
 * <p>与流浪商人一致的点：右键打开交易界面；一次性随机生成一组交易，无等级、不升级；存在一段时间后自动消失（despawnDelay）；
 * 不会因远离玩家而消失（由 despawnDelay 统一控制）。</p>
 *
 * <p>不同点：外观为头戴帽子的嗅探兽（帽子见 client 渲染层）；货币为火把花/瓶子草荚；禁止繁殖（商人不产生幼体）；
 * 交易条目售罄且未在交易时会自动重新生成（补货）。</p>
 */
public class WanderingSnifferMerchant extends Sniffer implements Merchant {

    /** 每完成多少笔交易解锁 1 条新交易（从初始 12 条成长到最多 16 条）。 */
    private static final int TRADES_PER_UNLOCK = 3;

    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    private int despawnDelay;
    /** 已完成的交易笔数，用于驱动交易条目成长。 */
    private int tradeCount;

    public WanderingSnifferMerchant(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    // ===================== 交易（Merchant 实现） =====================

    /** 重新生成一组随机交易（流浪商人式：一次性生成、无等级）。 */
    public void randomizeTrades() {
        this.offers = SnifferMerchantTrades.generate(this.random, this.registryAccess(), this.level().getRecipeManager());
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    public boolean isTrading() {
        return this.tradingPlayer != null;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.level().isClientSide) {
            throw new IllegalStateException("Cannot load sniffer merchant offers on the client");
        }
        if (this.offers == null) {
            this.randomizeTrades();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(@Nullable MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        if (offer.shouldRewardExp()) {
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5D, this.getZ(), offer.getXp()));
        }
        this.tradeCount++;
        this.maybeUnlockTrade();
    }

    /** 每完成 {@value #TRADES_PER_UNLOCK} 笔交易追加 1 条新交易，最多到 {@link SnifferMerchantTrades#MAX_TRADES} 条。 */
    private void maybeUnlockTrade() {
        if (this.level().isClientSide || this.offers == null) {
            return;
        }
        if (this.offers.size() >= SnifferMerchantTrades.MAX_TRADES) {
            return;
        }
        if (this.tradeCount % TRADES_PER_UNLOCK != 0) {
            return;
        }
        MerchantOffer extra = SnifferMerchantTrades.generateSingle(this.random, this.registryAccess(), this.level().getRecipeManager());
        if (extra != null) {
            this.offers.add(extra);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!this.level().isClientSide && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            this.playSound(this.getTradeUpdatedSound(!stack.isEmpty()), 1.0F, 1.0F);
        }
    }

    protected SoundEvent getTradeUpdatedSound(boolean success) {
        return success ? SoundEvents.SNIFFER_HAPPY : SoundEvents.SNIFFER_IDLE;
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
        // 商人不积累经验、不升级
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.SNIFFER_HAPPY;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    @Override
    public boolean canRestock() {
        return false;
    }

    // ===================== 交互：打开交易界面 =====================

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 刷怪蛋交给原版逻辑处理；其余情况（存活、非交易中、非幼体）打开交易界面
        if (!(stack.getItem() instanceof SpawnEggItem) && this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (!this.level().isClientSide) {
                if (this.getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    // ===================== 目标：交易时停下并注视玩家 =====================

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
    }

    /** 交易期间停止移动/跳跃（玩家打开交易界面时生效）。 */
    static class TradeWithPlayerGoal extends Goal {
        private final WanderingSnifferMerchant mob;

        TradeWithPlayerGoal(WanderingSnifferMerchant mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!this.mob.isAlive() || this.mob.isInWater() || !this.mob.onGround() || this.mob.hurtMarked) {
                return false;
            }
            Player player = this.mob.getTradingPlayer();
            if (player == null) {
                return false;
            }
            return this.mob.distanceToSqr(player) <= 16.0D && player.containerMenu != null;
        }

        @Override
        public void start() {
            this.mob.getNavigation().stop();
        }

        @Override
        public void stop() {
            this.mob.setTradingPlayer(null);
        }
    }

    /** 交易期间注视交易玩家。 */
    static class LookAtTradingPlayerGoal extends LookAtPlayerGoal {
        private final WanderingSnifferMerchant mob;

        LookAtTradingPlayerGoal(WanderingSnifferMerchant mob) {
            super(mob, Player.class, 8.0F);
            this.mob = mob;
        }

        @Override
        public boolean canUse() {
            if (this.mob.isTrading()) {
                this.lookAt = this.mob.getTradingPlayer();
                return true;
            }
            return false;
        }
    }

    // ===================== 禁止繁殖 / 恒为成体 =====================

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal other) {
        // 商人不可繁殖：不产出后代，也不掉落嗅探兽蛋
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
            @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, type, null);
        this.setAge(Math.max(0, this.getAge()));
        return data;
    }

    // ===================== 消失（despawn）与补货 =====================

    public void setDespawnDelay(int delay) {
        this.despawnDelay = delay;
    }

    public int getDespawnDelay() {
        return this.despawnDelay;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            this.maybeDespawn();
            this.maybeRestock();
        }
    }

    private void maybeDespawn() {
        if (this.despawnDelay > 0 && !this.isTrading() && --this.despawnDelay == 0) {
            this.discard();
        }
    }

    /** 交易条目全部售罄且未在交易时，重新生成一组交易（补货）。 */
    private void maybeRestock() {
        if (this.isTrading() || this.offers == null || this.offers.isEmpty()) {
            return;
        }
        for (MerchantOffer offer : this.offers) {
            if (!offer.isOutOfStock()) {
                return;
            }
        }
        this.randomizeTrades();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // ===================== 持久化 =====================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DespawnDelay", this.despawnDelay);
        tag.putInt("TradeCount", this.tradeCount);
        if (!this.level().isClientSide && this.offers != null && !this.offers.isEmpty()) {
            MerchantOffers.CODEC
                    .encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.offers)
                    .resultOrPartial(msg -> infinitycraft.LOGGER.warn("Failed to save sniffer merchant offers: {}", msg))
                    .ifPresent(encoded -> tag.put("Offers", encoded));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("DespawnDelay", 99)) {
            this.despawnDelay = tag.getInt("DespawnDelay");
        }
        if (tag.contains("TradeCount", 99)) {
            this.tradeCount = tag.getInt("TradeCount");
        }
        if (tag.contains("Offers")) {
            MerchantOffers.CODEC
                    .parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get("Offers"))
                    .resultOrPartial(msg -> infinitycraft.LOGGER.warn("Failed to load sniffer merchant offers: {}", msg))
                    .ifPresent(loaded -> this.offers = loaded);
        }
        this.setAge(Math.max(0, this.getAge()));
    }
}
