package lib.costs;

public class CostInfo {
    private int money;
    private int exp;
    private int level;
    private String itemsPlugin, itemsType;
    private int itemAmount;

    public CostInfo(int money, int exp, int level, String itemsPlugin, String itemsType, int itemAmount) {
        this.money = money;
        this.exp = exp;
        this.level = level;
        this.itemsPlugin = itemsPlugin;
        this.itemsType = itemsType;
        this.itemAmount = itemAmount;
    }

}
