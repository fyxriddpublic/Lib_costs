package com.fyxridd.lib.costs;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

public class CostInfo {
    private int money;
    private int exp;
    private int level;
    private HashMap<ItemStack, Integer> items;

    public CostInfo(int money, int exp, int level, HashMap<ItemStack, Integer> items) {
        this.money = money;
        this.exp = exp;
        this.level = level;
        this.items = items;
    }

    public int getMoney() {
        return money;
    }

    public int getExp() {
        return exp;
    }

    public int getLevel() {
        return level;
    }

    public HashMap<ItemStack, Integer> getItems() {
        return items;
    }
}
