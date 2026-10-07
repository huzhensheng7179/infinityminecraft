package com.himi.examplemod.event;

import com.himi.examplemod.infinitycraft;
import com.himi.examplemod.network.DefyDeathSyncPayload;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.himi.examplemod.network.ReviveEffectPayload;

/**
 * 「撼动死亡！」记录点的存取、跨重生/重登保留、客户端同步与死亡回归复活逻辑。
 *
 * <p>记录点以 long（{@link BlockPos#asLong()}）存于玩家持久化数据键 {@value #KEY_POINT}，
 * 键存在即视为已记录。死亡→重生时经 {@link PlayerEvent.Clone} 复制到新玩家；
 * 重生/登入后经同步包告知客户端，用于 tooltip 与死亡界面按钮显示。</p>
 */
@EventBusSubscriber(modid = infinitycraft.MODID)
public class DefyDeathHandler {

    /** 玩家持久化数据中记录点的键（long，BlockPos.asLong）。 */
    private static final String KEY_POINT = "DefyDeathPoint";

    /** 已请求「死亡回归」的玩家 UUID：死亡界面点按钮时加入，重生时消费以传送到标记点（区分普通重生）。 */
    private static final Set<UUID> PENDING_RETURN = ConcurrentHashMap.newKeySet();

    /** 返回玩家已记录的坐标；未记录返回 null。 */
    public static BlockPos getRecordedPoint(net.minecraft.world.entity.player.Player player) {
        if (!player.getPersistentData().contains(KEY_POINT)) {
            return null;
        }
        return BlockPos.of(player.getPersistentData().getLong(KEY_POINT));
    }

    public static boolean hasRecord(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData().contains(KEY_POINT);
    }

    public static void setRecordedPoint(net.minecraft.world.entity.player.Player player, BlockPos point) {
        player.getPersistentData().putLong(KEY_POINT, point.asLong());
    }

    /** 清除记录点（「撼动死亡！」为一次性，死亡回归用掉后清除）。 */
    public static void clearRecord(net.minecraft.world.entity.player.Player player) {
        player.getPersistentData().remove(KEY_POINT);
    }

    /** 把当前记录点同步给客户端（用于 tooltip「已记录」与死亡界面按钮）。 */
    public static void syncToClient(ServerPlayer player) {
        BlockPos point = getRecordedPoint(player);
        PacketDistributor.sendToPlayer(player, new DefyDeathSyncPayload(point != null, point));
    }

    /**
     * 死亡界面点击「死亡回归」时调用：仅登记本次重生要回到标记点。
     * 真正的复活交给原版重生流程（客户端随后发送 PERFORM_RESPAWN），
     * 保证复活出来的玩家是完全正常、可操作的实体；重生时见 {@link #onPlayerRespawn}。
     */
    public static void requestDeathReturn(ServerPlayer player) {
        if (getRecordedPoint(player) != null) {
            PENDING_RETURN.add(player.getUUID());
        }
    }

    /** 复活特效：不死图腾弹窗（沙漏贴图）+ 敲钟声（音量调高）+ 玩家周围白/紫粒子。 */
    private static void playReviveEffect(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ReviveEffectPayload());
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + 1.0D;
        double z = player.getZ();
        // 敲钟：叠加 BELL_BLOCK + BELL_RESONATE，音量 6.0（响度/传播距离显著高于默认）
        level.playSound(null, x, y, z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 6.0F, 1.0F);
        level.playSound(null, x, y, z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 6.0F, 1.0F);
        // 白色（末地烛）+ 紫色（传送门）粒子，环绕玩家上升（服务端广播，本人及周围玩家均可见）
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 80, 1.0D, 1.5D, 1.0D, 0.05D);
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 150, 1.2D, 1.8D, 1.2D, 0.4D);
    }

    /** 死亡/重生创建新玩家时，把记录点从旧玩家复制到新玩家。 */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        var originalTag = event.getOriginal().getPersistentData();
        if (originalTag.contains(KEY_POINT)) {
            event.getEntity().getPersistentData().putLong(KEY_POINT, originalTag.getLong(KEY_POINT));
        }
    }

    /**
     * 持有记录点的玩家死亡时，保留全部 Curios 饰品（不掉落、仍穿戴）。
     *
     * <p>Curios 在 {@code LivingDropsEvent}（HIGHEST 优先级）里会直接清空饰品槽，
     * 因此“取消掉落事件”会导致饰品丢失；正确做法是通过 {@link DropRulesEvent}
     * 添加 {@link ICurio.DropRule#ALWAYS_KEEP} 覆盖，使饰品直接跳过掉落与清槽。</p>
     */
    @SubscribeEvent
    public static void onCurioDropRules(DropRulesEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && hasRecord(sp)) {
            event.addOverride(stack -> true, ICurio.DropRule.ALWAYS_KEEP);
        }
    }

    /**
     * 消耗「撼动死亡！」：清除记录点 + 从饰品槽移除该饰品本身（死亡回归后消失，一次性）。
     * 直接操作我们自己的饰品槽，不触碰 Curios 掉落规则（DropRulesEvent），避免与其他模组冲突。
     */
    private static void consumeDefyDeath(ServerPlayer player) {
        clearRecord(player);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            for (ICurioStacksHandler stacksHandler : handler.getCurios().values()) {
                IDynamicStackHandler stacks = stacksHandler.getStacks();
                for (int i = 0; i < stacks.getSlots(); i++) {
                    if (stacks.getStackInSlot(i).is(infinitycraft.DEFY_DEATH.get())) {
                        stacks.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
            }
        });
    }

    /** 重生完成后：若本次是「死亡回归」，则传送回标记点（极限模式切回生存）、播放复活特效并消耗饰品；随后同步客户端。 */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (PENDING_RETURN.remove(sp.getUUID())) {
            BlockPos point = getRecordedPoint(sp);
            if (point != null) {
                MinecraftServer server = sp.getServer();
                // 延到下一 tick 执行：原版极限模式会在重生事件之后强制把玩家设为旁观，
                // 必须在其之后再切回生存，否则 setGameMode 会被覆盖（表现为“死归后仍是旁观”）。
                server.tell(new TickTask(server.getTickCount() + 1, () -> {
                    if (sp.level().getLevelData().isHardcore()) {
                        sp.setGameMode(GameType.SURVIVAL);
                    }
                    sp.teleportTo(sp.serverLevel(), point.getX() + 0.5D, point.getY(), point.getZ() + 0.5D,
                            sp.getYRot(), sp.getXRot());
                    playReviveEffect(sp);
                    grantDeathReturnAdvancement(sp);
                }));
                // 一次性消耗：移除「撼动死亡！」饰品并清除记录点（同步移除，随后的 entityJoinWorld 会把最新饰品状态同步给客户端）
                consumeDefyDeath(sp);
            }
        }
        syncToClient(sp);
    }

    /** 授予「死亡回归」成就（该成就为 impossible 触发器，仅在复活时手动授予）。 */
    private static void grantDeathReturnAdvancement(ServerPlayer player) {
        AdvancementHolder advancement = player.server.getAdvancements()
                .get(ResourceLocation.fromNamespaceAndPath(infinitycraft.MODID, "death_return"));
        if (advancement != null) {
            player.getAdvancements().award(advancement, "code");
        }
    }

    /** 登入时同步记录点（重登后 tooltip/按钮仍正确）。 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }
}
