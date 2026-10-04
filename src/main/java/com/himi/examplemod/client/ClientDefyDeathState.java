package com.himi.examplemod.client;

import net.minecraft.core.BlockPos;

/**
 * 客户端侧的「撼动死亡！」记录点状态（由服务端同步包更新）。
 *
 * <p>纯数据持有，不依赖任何客户端专有 API，故双端皆可安全加载；
 * 供物品 tooltip「已记录」与死亡界面「死亡回归」按钮读取。</p>
 */
public final class ClientDefyDeathState {

    /** 是否已记录坐标。 */
    public static volatile boolean has = false;
    /** 已记录的坐标（未记录时为 null）。 */
    public static volatile BlockPos point = null;

    private ClientDefyDeathState() {
    }

    public static void apply(boolean has, BlockPos point) {
        ClientDefyDeathState.has = has;
        ClientDefyDeathState.point = point;
    }
}
