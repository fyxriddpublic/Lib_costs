package com.fyxridd.lib.costs;

import com.fyxridd.item.kind.api.model.KindInfo;
import com.fyxridd.lib.core.api.*;
import com.fyxridd.lib.core.api.event.ReloadConfigEvent;
import com.fyxridd.lib.core.api.inter.FancyMessage;
import com.fyxridd.lib.costs.api.CostsPlugin;
import com.fyxridd.lib.items.api.ItemsApi;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CostsMain implements Listener{
    public static CostsMain instance;
    private String savePath;

    //插件名 类型名 花费信息
    private HashMap<String, HashMap<String, CostInfo>> costsHash = new HashMap<>();

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

        List<FancyMessage> tips = new ArrayList<>();

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
        if (tip) tips.add(get(1300));
        boolean result = true;
        //金钱
        int hasMoney = (int) EcoApi.get(p.getName());
        if (costInfo.getMoney() > 0) {
            boolean checkMoney = hasMoney >= costInfo.getMoney();
            if (tip) tips.add(get(2000, costInfo.getMoney(), get(checkMoney ? 1330 : 1340)));
            if (!checkMoney) {
                if (!force) {
                    //提示结果
                    tips.add(get(2520));
                    ShowApi.tip(p, tips, true);
                    return false;
                }
                else result = false;
            }
        }
        //经验
        int hasExp = p.getTotalExperience();
        if (costInfo.getExp() > 0) {
            boolean checkExp = hasExp >= costInfo.getExp();
            if (tip) tips.add(get(2010, costInfo.getExp(), get(checkExp ? 1330 : 1340)));
            if (!checkExp) {
                if (!force) {
                    //提示结果
                    tips.add(get(2520));
                    ShowApi.tip(p, tips, true);
                    return false;
                }
                else result = false;
            }
        }
        //等级
        int hasLevel = p.getLevel();
        if (costInfo.getLevel() > 0) {
            boolean checkLevel = hasLevel >= costInfo.getLevel();
            if (tip) tips.add(get(2020, costInfo.getLevel(), get(checkLevel ? 1330 : 1340)));
            if (!checkLevel) {
                if (!force) {
                    //提示结果
                    tips.add(get(2520));
                    ShowApi.tip(p, tips, true);
                    return false;
                }
                else result = false;
            }
        }
        //物品
        Inventory inv = p.getInventory();
        if (costInfo.getItems() != null && !costInfo.getItems().isEmpty()) {
            for (ItemInfo itemInfo:costInfo.getItems()) {
                boolean checkItem = false;
                switch (itemInfo.getMode()) {
                    case 1:
                        checkItem = ItemApi.hasExactItem(inv, itemInfo.getIs(), itemInfo.getAmount(), true);
                        break;
                    case 2:
                        checkItem = hasKindAmount(inv, itemInfo.getKind(), itemInfo.getAmount());
                        break;
                }
                if (tip) {
                    String itemName = "";
                    switch (itemInfo.getMode()) {
                        case 1:
                            itemName = NamesApi.getItemName(itemInfo.getIs());
                            break;
                        case 2:
                            if (CostsPlugin.itemKindHook) {
                                KindInfo kindInfo = com.fyxridd.item.kind.api.ItemApi.getKindInfo(itemInfo.getKind());
                                if (kindInfo != null) itemName = kindInfo.getShow();
                                else itemName = itemInfo.getKind();
                            }
                            break;
                    }
                    tips.add(get(2030, itemName, itemInfo.getAmount(), get(checkItem ? 1330 : 1340)));
                }
                if (!checkItem) {
                    if (!force) {
                        //提示结果
                        tips.add(get(2520));
                        ShowApi.tip(p, tips, true);
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
        if (costExp > 0) p.setTotalExperience(hasExp-costExp);
        //等级
        hasLevel = p.getLevel();
        int costLevel = Math.min(hasLevel, costInfo.getLevel());
        if (costLevel > 0) p.setLevel(hasLevel-costLevel);
        //物品
        if (costInfo.getItems() != null && !costInfo.getItems().isEmpty()) {
            for (ItemInfo itemInfo:costInfo.getItems()) {
                switch (itemInfo.getMode()) {
                    case 1:
                        ItemApi.removeExactItem(inv, itemInfo.getIs(), itemInfo.getAmount(), true, true);
                        break;
                    case 2:
                        removeKindAmount(inv, itemInfo.getKind(), itemInfo.getAmount());
                        break;
                }
            }
        }

        //提示结果
        if (result) tips.add(get(2500));
        else tips.add(get(2510));
        ShowApi.tip(p, tips, true);

        //返回
        return result;
    }

    public void reloadCosts(String plugin) {
        if (plugin == null) return;
        reloadCosts(plugin, CoreApi.loadConfigByUTF8(new File(CoreApi.pluginPath, plugin+"/costs.yml")));
    }

    public void reloadCosts(String plugin, File file) {
        if (plugin == null || file == null) return;
        reloadCosts(plugin, CoreApi.loadConfigByUTF8(file));
    }

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
     * 移除指定种类的指定数量物品
     */
    private void removeKindAmount(Inventory inv, String kind, int amount) {
        if (!CostsPlugin.itemKindHook || amount <= 0) return;
        //需要减少的数量
        int need = amount;
        for (int i=0;i<inv.getSize();i++) {
            ItemStack is2 = inv.getItem(i);
            if (is2 != null && is2.getType() != Material.AIR) {
                if (kind.equals(com.fyxridd.item.kind.api.ItemApi.getKind(is2))) {//检测相同成功,减少物品
                    int has = is2.getAmount();
                    if (need <= has) {//结束
                        if (has == need) inv.setItem(i, null);
                        else is2.setAmount(has-need);
                        break;
                    }else {
                        need -= has;
                        inv.setItem(i, null);
                    }
                }
            }
        }
    }

    /**
     * 检测是否有指定数量的指定种类物品
     * @return 如果未挂钩ItemKind插件则返回false
     */
    private boolean hasKindAmount(Inventory inv, String kind, int amount) {
        if (!CostsPlugin.itemKindHook) return false;

        int sum = 0;
        for (int i=0;i<inv.getSize();i++) {
            ItemStack check = inv.getItem(i);
            if (check != null && check.getType() != Material.AIR) {
                if (kind.equals(com.fyxridd.item.kind.api.ItemApi.getKind(check))) {
                    sum += inv.getItem(i).getAmount();
                    if (sum >= amount) return true;
                }
            }
        }
        return false;
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
        List<ItemInfo> items = new ArrayList<>();
        MemorySection ms = (MemorySection)config.get(type+".items");
        if (ms != null) {
            for (String key:ms.getValues(false).keySet()) {
                MemorySection itemMs = (MemorySection) ms.get(key);
                ItemInfo itemInfo = null;
                if (itemMs.contains("exact")) {
                    itemInfo = new ItemInfo(ItemsApi.loadItemStack((MemorySection) itemMs.get("exact")), itemMs.getInt("amount", 1));
                }else if (itemMs.contains("kind")) {
                    itemInfo = new ItemInfo(itemMs.getString("kind"), itemMs.getInt("amount", 1));
                }
                if (itemInfo != null) items.add(itemInfo);
            }
        }
        //添加
        costsHash.get(plugin).put(type, new CostInfo(money, exp, level, items));
    }

    private void initConfig() {
        ConfigApi.register(CostsPlugin.file, CostsPlugin.dataPath, CostsPlugin.pn);
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
