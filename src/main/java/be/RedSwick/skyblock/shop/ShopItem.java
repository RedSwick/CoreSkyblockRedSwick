package be.RedSwick.skyblock.shop;

import org.bukkit.Material;

/**
 * ShopItem — record immuable représentant un item du shop.
 *
 * Modes :
 *  of(mat, name, buy, sell)       → achat + vente en coins
 *  sellOnly(mat, name, sell)      → vente seulement en coins
 *  gem(mat, name, gems)           → achat en gemmes, pas de revente
 *  gemSell(mat, name, gems, sell) → achat en gemmes + revente en coins (spawners)
 */
public record ShopItem(
        Material material,
        String   displayName,
        long     buyPrice,
        long     sellPrice,
        int      gemPrice
) {
    /** Achat + vente en coins */
    public static ShopItem of(Material mat, String name, long buy, long sell) {
        return new ShopItem(mat, name, buy, sell, 0);
    }
    /** Vente seulement en coins */
    public static ShopItem sellOnly(Material mat, String name, long sell) {
        return new ShopItem(mat, name, 0, sell, 0);
    }
    /** Achat en gemmes, pas de revente */
    public static ShopItem gem(Material mat, String name, int gems) {
        return new ShopItem(mat, name, 0, 0, gems);
    }
    /** Achat en gemmes + revente en coins (ex: spawners) */
    public static ShopItem gemSell(Material mat, String name, int gems, long sell) {
        return new ShopItem(mat, name, 0, sell, gems);
    }

    public boolean isBuyable()    { return buyPrice > 0; }
    public boolean isSellable()   { return sellPrice > 0; }
    public boolean isGemBuy()     { return gemPrice > 0; }
    /** Vrai si l'item s'achète en gemmes ET peut être revendu en coins */
    public boolean isGemWithSell(){ return gemPrice > 0 && sellPrice > 0; }
}