package com.fyxridd.lib.costs.api;

import com.fyxridd.lib.costs.CostsMain;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class CostsApi {
    /**
     * 重新读取花费配置
     * @param plugin 注册的插件名,可为null(null时无效果)
     * @param config 配置,可为null(null时无效果)
     */
    public static void reloadCosts(String plugin, YamlConfiguration config) {
        CostsMain.reloadCosts(plugin, config);
    }

    /**
     */
    public static boolean cost(Player p, String plugin, String type, boolean force, boolean tip){
        return CostsMain.cost(p, plugin, type, force, tip);
    }
}
