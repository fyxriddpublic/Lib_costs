package com.fyxridd.lib.costs;

import com.fyxridd.lib.core.api.*;
import com.fyxridd.lib.core.api.event.ReloadConfigEvent;
import com.fyxridd.lib.core.api.inter.FancyMessage;
import com.fyxridd.lib.costs.api.CostsPlugin;
import com.fyxridd.lib.items.api.ItemsApi;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CostsMain implements Listener{
    public static CostsMain instance;
    private String savePath;

    //插件名 类型名 花费信息
    private HashMap<String, HashMap<String, CostInfo>> costsHash = new HashMap<String, HashMap<String, CostInfo>>();

	public CostsMain() {
        instance = this;
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
     * @see com.fyxridd.lib.costs.api.CostsApi#cost(org.bukkit.entity.Player, String, String, boolean, boolean)
     */
    public boolean cost(Player p, String plugin, String type, boolean force, boolean tip){
        if (p == null || plugin == null || type == null) return false;

        //类型不存在
        HashMap<String, CostInfo> hash = costsHash.get(plugin);
        if (hash == null) {
            if (tip) ShowApi.tip(p, get(1320), true);
            return false;
        }
        CostInfo costInfo = hash.get(type);
        if (costInfo == null) {
            if (tip) ShowApi.tip(p, get(1320), true);
            return false;
        }

        //检测
        if (tip) ShowApi.tip(p, get(1300), true);
        boolean result = true;
        //金钱
        int hasMoney = (int) EcoApi.get(p.getName());
        if (costInfo.getMoney() > 0) {
            boolean checkMoney = hasMoney >= costInfo.getMoney();
            if (tip) ShowApi.tip(p, get(2000, costInfo.getMoney(), get(checkMoney ? 1330 : 1340)), true);
            if (!checkMoney) {
                if (!force) {
                    //提示结果
                    ShowApi.tip(p, get(2520), true);
                    return false;
                }
                else result = false;
            }
        }
        //经验
        int hasExp = CoreApi.getTotalExperience(p);
        if (costInfo.getExp() > 0) {
            boolean checkExp = hasExp >= costInfo.getExp();
            if (tip) ShowApi.tip(p, get(2010, costInfo.getExp(), get(checkExp ? 1330 : 1340)), true);
            if (!checkExp) {
                if (!force) {
                    //提示结果
                    ShowApi.tip(p, get(2520), true);
                    return false;
                }
                else result = false;
            }
        }
        //等级
        int hasLevel = p.getLevel();
        if (costInfo.getLevel() > 0) {
            boolean checkLevel = hasLevel >= costInfo.getLevel();
            if (tip) ShowApi.tip(p, get(2020, costInfo.getExp(), get(checkLevel ? 1330 : 1340)), true);
            if (!checkLevel) {
                if (!force) {
                    //提示结果
                    ShowApi.tip(p, get(2520), true);
                    return false;
                }
                else result = false;
            }
        }
        //物品
        Inventory inv = p.getInventory();
        if (costInfo.getItems() != null && !costInfo.getItems().isEmpty()) {
            for (Map.Entry<ItemStack, Integer> entry:costInfo.getItems().entrySet()) {
                boolean checkItem = ItemApi.hasExactItem(inv, entry.getKey(), entry.getValue(), true);
                if (tip) ShowApi.tip(p, get(2030, NamesApi.getItemName(entry.getKey()), entry.getValue(), get(checkItem?1330:1340)), true);
                if (!checkItem) {
                    if (!force) {
                        //提示结果
                        ShowApi.tip(p, get(2520), true);
                        return false;
                    }
                    else result = false;
                }
            }
        }

        //花费
        //金钱
        int costMoney = Math.min(hasMoney, costInfo.getMoney());
        if (costMoney > 0) EcoApi.del(p.getName(), costMoney);
        //经验
        int costExp = Math.min(hasExp, costInfo.getExp());
        if (costExp > 0) CoreApi.setTotalExperience(p, hasExp-costExp);
        //等级
        hasLevel = p.getLevel();
        int costLevel = Math.min(hasLevel, costInfo.getLevel());
        if (costLevel > 0) p.setLevel(hasLevel-costLevel);
        //物品
        if (costInfo.getItems() != null && !costInfo.getItems().isEmpty()) {
            for (Map.Entry<ItemStack, Integer> entry:costInfo.getItems().entrySet()) {
                ItemApi.removeExactItem(inv, entry.getKey(), entry.getValue(), true, true);
            }
        }

        //提示结果
        if (result) ShowApi.tip(p, get(2500), true);
        else ShowApi.tip(p, get(2510), true);

        //返回
        return result;
    }

    /**
     * @see com.fyxridd.lib.costs.api.CostsApi#reloadCosts(String, File)
     */
    public void reloadCosts(String plugin, File file) {
        if (plugin == null || file == null) return;
        reloadCosts(plugin, CoreApi.loadConfigByUTF8(file));
    }

    /**
     * @see com.fyxridd.lib.costs.api.CostsApi#reloadCosts(String, org.bukkit.configuration.file.YamlConfiguration)
     */
    public void reloadCosts(String plugin, YamlConfiguration config) {
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
    private void loadCost(String plugin, YamlConfiguration config, String type) {
        //money
        int money = config.getInt(type+".money", 0);
        if (money < 0) {
            ConfigApi.log(CostsPlugin.pn, "load costs type '"+type+"' money error");
            money = 0;
        }
        //exp
        int exp = config.getInt(type+".exp", 0);
        if (exp < 0) {
            ConfigApi.log(CostsPlugin.pn, "load costs type '"+type+"' exp error");
            exp = 0;
        }
        //level
        int level = config.getInt(type+".level", 0);
        if (level < 0) {
            ConfigApi.log(CostsPlugin.pn, "load costs type '"+type+"' level error");
            level = 0;
        }
        //items
        HashMap<ItemStack, Integer> items = new HashMap<ItemStack, Integer>();
        for (Object obj:config.getList(type+".items")) {
            LinkedHashMap<String, Object> map = (LinkedHashMap<String, Object>) obj;
            MemorySection ms = (MemorySection) map.get("exact");
            ItemStack is = ItemsApi.loadItemStack(ms);
            int amount = (Integer) map.get("amount");
            items.put(is, amount);
        }
        //添加
        costsHash.get(plugin).put(type, new CostInfo(money, exp, level, items));
    }

    private void initConfig() {
        ConfigApi.register(CostsPlugin.file, CostsPlugin.dataPath, CostsPlugin.pn, null);
        ConfigApi.loadConfig(CostsPlugin.pn);
    }

	private void loadConfig() {
        //重新读取花费配置
		reloadCosts(CostsPlugin.pn, new File(savePath));
	}

    private static FancyMessage get(int id, Object... args) {
        return FormatApi.get(CostsPlugin.pn, id, args);
    }
}
