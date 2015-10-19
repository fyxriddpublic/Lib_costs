package com.fyxridd.lib.costs.api;

import com.fyxridd.lib.costs.CostsMain;
import org.bukkit.entity.Player;

public class CostsApi {
    /**
     * 重新读取花费配置
     * 会读取'插件名/costs.yml'文件
     */
    public static void reloadCosts(String plugin) {
        CostsMain.instance.reloadCosts(plugin);
    }

    /**
     * 花费
     * @param p 玩家,可为null(null时返回false)
     * @param plugin 插件,可为null(null时返回false)
     * @param type 花费的类型,可为null(null时返回false)
     * @param force 表示在花费不满足的情况下是否强制花费
     * @param tip 是否提示
     * @return 花费是否完全成功
     */
    public static boolean cost(Player p, String plugin, String type, boolean force, boolean tip){
        return CostsMain.instance.cost(p, plugin, type, force, tip);
    }
}
