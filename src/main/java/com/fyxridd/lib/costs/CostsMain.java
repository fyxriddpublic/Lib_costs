package com.fyxridd.lib.costs;

import com.fyxridd.lib.core.api.ConfigApi;
import com.fyxridd.lib.core.api.CoreApi;
import com.fyxridd.lib.core.api.FormatApi;
import com.fyxridd.lib.core.api.ShowApi;
import com.fyxridd.lib.core.api.event.ReloadConfigEvent;
import com.fyxridd.lib.core.api.inter.FancyMessage;
import com.fyxridd.lib.costs.api.CostsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CostsMain implements Listener{
    private static String savePath;

    //插件名 类型名 花费信息
    private static HashMap<String, HashMap<String, CostInfo>> costsHash = new HashMap<String, HashMap<String, CostInfo>>();

	public CostsMain() {
        savePath = CostsPlugin.dataPath+File.separator+"costs.yml";
        //初始化配置
        initConfig();
		//读取配置文件
		loadConfig();
		//注册事件
		Bukkit.getPluginManager().registerEvents(this, CostsPlugin.instance);
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void onReloadConfig(ReloadConfigEvent e) {
		if (e.getPlugin().equals(CostsPlugin.pn)) loadConfig();
	}

    /**
     * 花费
     * @param p 玩家,不为null
     * @param plugin 插件,null表示Lib插件名
     * @param type 花费的类型,不为null
     * @param force 表示在花费不满足的情况下是否强制花费
     * @param tip 是否提示
     * @return 花费是否完全成功
     */
    public static boolean cost(Player p, String plugin, String type, boolean force, boolean tip){
        //修正
        if (plugin == null) plugin = CostsPlugin.pn;
        //类型不存在
        if (!costsHash.containsKey(plugin) || costsHash.get(plugin).get(type) == null) {
            if (tip) ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "fail", get(1320)), true);
            return false;
        }
        //开始检测
        if (tip) ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "tip", get(1300)), true);
        CostInfo costInfo = costsHash.get(plugin).get(type);
        boolean result = true;
        //金钱
        int hasMoney = (int) UtilEco.get(p.getName());
        if (hasMoney < costInfo.money) {
            if (tip) ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "costMoney", costInfo.money, hasMoney), true);
            if (!force) return false;
            else result = false;
        }
        //经验
        int hasExp = Util.getTotalExperience(p);
        if (hasExp < costInfo.exp) {
            if (tip) ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "costExp", costInfo.exp, hasExp), true);
            if (!force) return false;
            else result = false;
        }
        //等级
        int hasLevel = p.getLevel();
        if (hasLevel < costInfo.level) {
            if (tip) ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "costLevel", costInfo.level, hasLevel), true);
            if (!force) return false;
            else result = false;
        }
        //物品
        if (costInfo.itemsPlugin != null && costInfo.itemsType != null && costInfo.itemAmount > 0) {
            Inventory inv = p.getInventory();
            int count = 0;
            boolean has = false;
            for (int index = 0; index < inv.getSize(); index++) {
                ItemStack is = inv.getItem(index);
                if (is != null && !is.getType().equals(Material.AIR) && is.getAmount() > 0) {
                    try {
                        if (UtilTypes.checkItem(costInfo.itemsPlugin, costInfo.itemsType, is)) {
                            count += is.getAmount();
                            if (count >= costInfo.itemAmount) {
                                has = true;
                                break;
                            }
                        }
                    } catch (MsgException e) {
                        ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "fail", get(1325)), true);
                        return false;
                    }
                }
            }
            if (!has) {
                if (tip) ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "fail", get(1330)), true);
                if (!force) return false;
                else result = false;
            }
        }

        //花费
        //money
        int costMoney = Math.min(hasMoney, costInfo.money);
        if (costMoney > 0) {
            UtilEco.del(p.getName(), (double)costMoney);
            if (tip) ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "costMoney2", costMoney), true);
        }
        //exp
        int costExp = Math.min(hasExp, costInfo.exp);
        if (costExp > 0) {
            Util.setTotalExperience(p, hasExp-costInfo.exp);
            if (tip) ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "costExp2", costExp), true);
        }
        //等级修正
        hasLevel = p.getLevel();
        //level
        int costLevel = Math.min(hasLevel, costInfo.level);
        if (costLevel > 0) {
            p.setLevel(hasLevel-costInfo.level);
            if (tip) ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "costLevel2", costLevel), true);
        }
        //item
        if (costInfo.itemsPlugin != null && costInfo.itemsType != null && costInfo.itemAmount > 0) {
            Inventory inv = p.getInventory();
            int need = costInfo.itemAmount;
            for (int index = 0; index < inv.getSize(); index++) {
                ItemStack is = inv.getItem(index);
                if (is != null && !is.getType().equals(Material.AIR) && is.getAmount() > 0) {
                    if (Types.checkItem(costInfo.itemsPlugin, costInfo.itemsType, is)) {
                        if (is.getAmount() > need) {//已经满足
                            ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "costItem5", UtilNames.getItemName(is), is.getTypeId(), need), true);
                            is.setAmount(is.getAmount()-need);
                            break;
                        }else if (is.getAmount() == need) {//已经满足
                            ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "costItem5", UtilNames.getItemName(is), is.getTypeId(), need), true);
                            inv.setItem(index, null);
                            break;
                        }else {//未满足
                            ShowApi.tip(p, UtilFormat.format(CostsPlugin.pn, "costItem5", UtilNames.getItemName(is), is.getTypeId(), is.getAmount()), true);
                            need -= is.getAmount();
                            inv.setItem(index, null);
                        }
                    }
                }
            }
        }

        //返回
        return result;
    }

    /**
     * @see lib.costs.api.CostsApi#reloadCosts(String, org.bukkit.configuration.file.YamlConfiguration)
     */
    public static void reloadCosts(String plugin, YamlConfiguration config) {
        if (plugin == null || config == null) return;

        //重置
        costsHash.put(plugin, new HashMap<String, CostInfo>());
        //重新读取
        Map<String, Object> map = config.getValues(false);
        if (map == null) {
            ConfigApi.log(CostsPlugin.pn, "load costs error");
            return;
        }
        for (String key:map.keySet()) loadCost(plugin, config, key);
    }

    /**
     * 读取指定的花费配置
     * @param plugin 插件名,不为null
     * @param config 配置,不为null
     * @param type 类型,不为null
     */
    private static void loadCost(String plugin, YamlConfiguration config, String type) {
        //money
        int money = config.getInt(type+".money");
        if (money < 0) {
            ConfigApi.log(CostsPlugin.pn, "load costs type '"+type+"' money error");
            money = 0;
        }
        //exp
        int exp = config.getInt(type+".exp");
        if (exp < 0) {
            ConfigApi.log(CostsPlugin.pn, "load costs type '"+type+"' exp error");
            exp = 0;
        }
        //level
        int level = config.getInt(type+".level");
        if (level < 0) {
            ConfigApi.log(CostsPlugin.pn, "load costs type '"+type+"' level error");
            level = 0;
        }
        //items
        String itemsPlugin = null;
        String itemsType = null;
        int itemAmount = 0;
        if (config.contains(type+".item")) {
            itemsPlugin = config.getString(type+".item.plugin");
            itemsType = config.getString(type+".item.type");
            itemAmount = config.getInt(type+".item.amount");
            if (itemAmount < 0) {
                ConfigApi.log(CostsPlugin.pn, "load costs type '"+type+"' itemAmount error");
                itemAmount = 0;
            }
        }
        //添加
        CostInfo costInfo = new CostInfo(money, exp, level, itemsPlugin, itemsType, itemAmount);
        costsHash.get(plugin).put(type, costInfo);
    }

    private void initConfig() {
        List<String> filter = ConfigApi.getDefaultFilter();
        filter.add("costs.yml");
        ConfigApi.register(CostsPlugin.file, CostsPlugin.dataPath, filter, CostsPlugin.pn, null);
        ConfigApi.loadConfig(CostsPlugin.pn);
    }

	private static void loadConfig() {
		YamlConfiguration typesConfig = CoreApi.loadConfigByUTF8(new File(savePath));
        if (typesConfig == null) {
            ConfigApi.log(CostsPlugin.pn, "typesConfig load error");
            return;
        }
		reloadCosts(CostsPlugin.pn, typesConfig);
	}

    private static FancyMessage get(int id, Object... args) {
        return FormatApi.get(CostsPlugin.pn, id, args);
    }
}
