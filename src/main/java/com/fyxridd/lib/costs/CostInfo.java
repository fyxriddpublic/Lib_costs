package com.fyxridd.lib.costs;

import java.util.List;

public class CostInfo {
    private int money;
    private int exp;
    private int level;
    private List<ItemInfo> items;

    public CostInfo(int money, int exp, int level, List<ItemInfo> items) {
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

    public List<ItemInfo> getItems() {
        return items;
    }
}
