package be.RedSwick.skyblock.shop;

import org.bukkit.Material;

public record ShopItem(
        Material material,
        String   displayName,
        long     buyPrice,
        long     sellPrice,
        int      gemPrice
) {
    public static ShopItem of(Material mat, String name, long buy, long sell) {
        return new ShopItem(mat, name, buy, sell, 0);
    }
    public static ShopItem gem(Material mat, String name, int gems) {
        return new ShopItem(mat, name, 0, 0, gems);
    }
    public static ShopItem sellOnly(Material mat, String name, long sell) {
        return new ShopItem(mat, name, 0, sell, 0);
    }

    public boolean isBuyable()  { return buyPrice  > 0; }
    public boolean isSellable() { return sellPrice > 0; }
    public boolean isGemBuy()   { return gemPrice  > 0; }
}