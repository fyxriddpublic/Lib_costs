package com.fyxridd.lib.costs;

import org.bukkit.inventory.ItemStack;

public class ItemInfo {
    //数量
    private int amount;

    //物品模式,1表示exact,2表示kind
    private int mode;

    //模式1
    private ItemStack is;

    //模式2
    private String kind;

    public ItemInfo(ItemStack is, int amount) {
        this.mode = 1;
        this.is = is;
        this.amount = amount;
    }

    public ItemInfo(String kind, int amount) {
        this.mode = 2;
        this.kind = kind;
        this.amount = amount;
    }

    public int getAmount() {
        return amount;
    }

    public int getMode() {
        return mode;
    }

    public ItemStack getIs() {
        return is;
    }

    public String getKind() {
        return kind;
    }
}
