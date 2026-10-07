package com.himi.examplemod.worldgen.portal;

import java.util.Comparator;
import java.util.Optional;
import javax.annotation.Nullable;

import com.himi.examplemod.block.HotSandPortalBlock;
import com.himi.examplemod.infinitycraft;

import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;

/**
 * 热砂世界传送门定位/生成器：负责在目标维度寻找已存在的返程门，或在缺失时自动生成一座。
 *
 * <p>结构参照 {@code net.minecraft.world.level.portal.PortalForcer}，
 * 但使用本模组的 POI（{@link infinitycraft#HOT_SAND_PORTAL_POI}）、
 * 「哭泣的黑曜石」框架与本模组传送门方块，实现主世界 ↔ 热砂世界的双向链接。</p>
 */
public class HotSandPortalForcer {
    private static final int PORTAL_RADIUS = 128;
    protected final ServerLevel level;

    public HotSandPortalForcer(ServerLevel level) {
        this.level = level;
    }

    public Optional<BlockPos> findClosestPortalPosition(BlockPos exitPos, WorldBorder worldBorder) {
        PoiManager poimanager = this.level.getPoiManager();
        poimanager.ensureLoadedAndValid(this.level, exitPos, PORTAL_RADIUS);
        return poimanager.getInSquare(poi -> poi.is(infinitycraft.HOT_SAND_PORTAL_POI.getKey()), exitPos, PORTAL_RADIUS, PoiManager.Occupancy.ANY)
                .map(PoiRecord::getPos)
                .filter(worldBorder::isWithinBounds)
                .filter(p -> this.level.getBlockState(p).hasProperty(BlockStateProperties.HORIZONTAL_AXIS))
                .min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(exitPos)).thenComparingInt(Vec3i::getY));
    }

    public Optional<BlockUtil.FoundRectangle> createPortal(BlockPos pos, Direction.Axis axis) {
        Direction direction = Direction.get(Direction.AxisDirection.POSITIVE, axis);
        double bestDist = -1.0;
        BlockPos bestPos = null;
        double fallbackDist = -1.0;
        BlockPos fallbackPos = null;
        WorldBorder worldborder = this.level.getWorldBorder();
        int maxY = Math.min(this.level.getMaxBuildHeight(), this.level.getMinBuildHeight() + this.level.getLogicalHeight()) - 1;
        BlockPos.MutableBlockPos cursor = pos.mutable();

        for (BlockPos.MutableBlockPos spiral : BlockPos.spiralAround(pos, 16, Direction.EAST, Direction.SOUTH)) {
            int k = Math.min(maxY, this.level.getHeight(Heightmap.Types.MOTION_BLOCKING, spiral.getX(), spiral.getZ()));
            if (worldborder.isWithinBounds(spiral) && worldborder.isWithinBounds(spiral.move(direction, 1))) {
                spiral.move(direction.getOpposite(), 1);
                for (int l = k; l >= this.level.getMinBuildHeight(); l--) {
                    spiral.setY(l);
                    if (this.canPortalReplaceBlock(spiral)) {
                        int top = l;
                        while (l > this.level.getMinBuildHeight() && this.canPortalReplaceBlock(spiral.move(Direction.DOWN))) {
                            l--;
                        }
                        if (l + 4 <= maxY) {
                            int gap = top - l;
                            if (gap <= 0 || gap >= 3) {
                                spiral.setY(l);
                                if (this.canHostFrame(spiral, cursor, direction, 0)) {
                                    double d2 = pos.distSqr(spiral);
                                    if (this.canHostFrame(spiral, cursor, direction, -1)
                                            && this.canHostFrame(spiral, cursor, direction, 1)
                                            && (bestDist == -1.0 || bestDist > d2)) {
                                        bestDist = d2;
                                        bestPos = spiral.immutable();
                                    }
                                    if (bestDist == -1.0 && (fallbackDist == -1.0 || fallbackDist > d2)) {
                                        fallbackDist = d2;
                                        fallbackPos = spiral.immutable();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (bestDist == -1.0 && fallbackDist != -1.0) {
            bestPos = fallbackPos;
            bestDist = fallbackDist;
        }

        if (bestDist == -1.0) {
            // 未找到合适空地：在目标 Y 强制清出一块并搭建框架
            int minY = Math.max(this.level.getMinBuildHeight() - -1, 70);
            int ceiling = maxY - 9;
            if (ceiling < minY) {
                return Optional.empty();
            }
            bestPos = new BlockPos(pos.getX() - direction.getStepX(), Mth.clamp(pos.getY(), minY, ceiling), pos.getZ() - direction.getStepZ()).immutable();
            bestPos = worldborder.clampToBounds(bestPos);
            Direction side = direction.getClockWise();
            for (int i = -1; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    for (int k2 = -1; k2 < 3; k2++) {
                        BlockState state = k2 < 0 ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
                        cursor.setWithOffset(bestPos, j * direction.getStepX() + i * side.getStepX(), k2, j * direction.getStepZ() + i * side.getStepZ());
                        this.level.setBlockAndUpdate(cursor, state);
                    }
                }
            }
        }

        // 搭建哭泣的黑曜石框架（外圈 4x5，含转角）
        for (int l1 = -1; l1 < 3; l1++) {
            for (int j2 = -1; j2 < 4; j2++) {
                if (l1 == -1 || l1 == 2 || j2 == -1 || j2 == 3) {
                    cursor.setWithOffset(bestPos, l1 * direction.getStepX(), j2, l1 * direction.getStepZ());
                    this.level.setBlock(cursor, Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
                }
            }
        }

        // 填充内部 2x3 传送门方块
        BlockState portal = infinitycraft.HOT_SAND_PORTAL.get().defaultBlockState().setValue(HotSandPortalBlock.AXIS, axis);
        for (int k2 = 0; k2 < 2; k2++) {
            for (int l2 = 0; l2 < 3; l2++) {
                cursor.setWithOffset(bestPos, k2 * direction.getStepX(), l2, k2 * direction.getStepZ());
                this.level.setBlock(cursor, portal, 18);
            }
        }

        return Optional.of(new BlockUtil.FoundRectangle(bestPos.immutable(), 2, 3));
    }

    private boolean canPortalReplaceBlock(BlockPos.MutableBlockPos pos) {
        BlockState blockstate = this.level.getBlockState(pos);
        return blockstate.canBeReplaced() && blockstate.getFluidState().isEmpty();
    }

    private boolean canHostFrame(BlockPos originalPos, BlockPos.MutableBlockPos offsetPos, Direction direction, int offsetScale) {
        Direction side = direction.getClockWise();
        for (int i = -1; i < 3; i++) {
            for (int j = -1; j < 4; j++) {
                offsetPos.setWithOffset(originalPos,
                        direction.getStepX() * i + side.getStepX() * offsetScale, j,
                        direction.getStepZ() * i + side.getStepZ() * offsetScale);
                if (j < 0 && !this.level.getBlockState(offsetPos).isSolid()) {
                    return false;
                }
                if (j >= 0 && !this.canPortalReplaceBlock(offsetPos)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 计算实体穿越后的落点：优先复用目标维度已存在的返程门，否则就地新建一座。
     */
    @Nullable
    public static DimensionTransition getExitPortal(ServerLevel target, Entity entity, BlockPos portalPos, BlockPos exitPos, WorldBorder border) {
        HotSandPortalForcer forcer = new HotSandPortalForcer(target);
        Optional<BlockPos> existing = forcer.findClosestPortalPosition(exitPos, border);
        BlockUtil.FoundRectangle rectangle;
        DimensionTransition.PostDimensionTransition post;
        if (existing.isPresent()) {
            BlockPos found = existing.get();
            BlockState state = target.getBlockState(found);
            rectangle = BlockUtil.getLargestRectangleAround(found, state.getValue(BlockStateProperties.HORIZONTAL_AXIS), 21, Direction.Axis.Y, 21,
                    p -> target.getBlockState(p) == state);
            post = DimensionTransition.PLAY_PORTAL_SOUND.then(e -> e.placePortalTicket(found));
        } else {
            Direction.Axis axis = entity.level().getBlockState(portalPos).getOptionalValue(HotSandPortalBlock.AXIS).orElse(Direction.Axis.X);
            Optional<BlockUtil.FoundRectangle> created = forcer.createPortal(exitPos, axis);
            if (created.isEmpty()) {
                return null;
            }
            rectangle = created.get();
            post = DimensionTransition.PLAY_PORTAL_SOUND.then(DimensionTransition.PLACE_PORTAL_TICKET);
        }
        return createDimensionTransition(entity, portalPos, rectangle, target, post);
    }

    private static DimensionTransition createDimensionTransition(Entity entity, BlockPos portalPos, BlockUtil.FoundRectangle rectangle,
                                                                 ServerLevel target, DimensionTransition.PostDimensionTransition post) {
        BlockState blockstate = entity.level().getBlockState(portalPos);
        Direction.Axis axis;
        Vec3 offset;
        if (blockstate.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            axis = blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            BlockUtil.FoundRectangle source = BlockUtil.getLargestRectangleAround(portalPos, axis, 21, Direction.Axis.Y, 21,
                    p -> entity.level().getBlockState(p) == blockstate);
            offset = entity.getRelativePortalPosition(axis, source);
        } else {
            axis = Direction.Axis.X;
            offset = new Vec3(0.5, 0.0, 0.0);
        }

        BlockPos minCorner = rectangle.minCorner;
        BlockState destState = target.getBlockState(minCorner);
        Direction.Axis destAxis = destState.getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
        double d0 = rectangle.axis1Size;
        double d1 = rectangle.axis2Size;
        EntityDimensions dims = entity.getDimensions(entity.getPose());
        int yRotOffset = axis == destAxis ? 0 : 90;
        Vec3 motion = entity.getDeltaMovement();
        Vec3 speed = axis == destAxis ? motion : new Vec3(motion.z, motion.y, -motion.x);
        double dx = dims.width() / 2.0 + (d0 - dims.width()) * offset.x();
        double dy = (d1 - dims.height()) * offset.y();
        double dz = 0.5 + offset.z();
        boolean xAxis = destAxis == Direction.Axis.X;
        Vec3 destPos = new Vec3(minCorner.getX() + (xAxis ? dx : dz), minCorner.getY() + dy, minCorner.getZ() + (xAxis ? dz : dx));
        Vec3 safe = PortalShape.findCollisionFreePosition(destPos, target, entity, dims);
        return new DimensionTransition(target, safe, speed, entity.getYRot() + yRotOffset, entity.getXRot(), post);
    }
}
