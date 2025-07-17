package dev.smootheez.elytracontrol.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.Holder;


import java.util.function.Predicate;

public class ElytraEquipmentUtil {
    
    public static int findBestElytraSlot(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) return -1;
        
        AbstractContainerMenu container = player.containerMenu;

        Predicate<ItemStack> filter = (s) -> s.getItem().equals(Items.ELYTRA) &&
                s.getDamageValue() < s.getMaxDamage() - 10;

        int targetSlot = -1;
        int bestScore = -1;
        
        for (int i = 0; i < container.slots.size(); i++) {
            int score = 0;
            ItemStack stack = container.getSlot(i).getItem();
            if (!filter.test(stack)) continue;
            
            // 檢查附魔

            ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(stack);
            if (!enchantments.isEmpty()) {
                for (Holder<Enchantment> enchantmentHolder : enchantments.keySet()) {
                    int level = enchantments.getLevel(enchantmentHolder);
                    
                    if (enchantmentHolder.is(Enchantments.UNBREAKING)) {
                        score += level * 2;
                    }
                    if (enchantmentHolder.is(Enchantments.MENDING)) {
                        score += level;
                    }
                    if (enchantmentHolder.is(Enchantments.VANISHING_CURSE)) {
                        score = -1;
                    }
                    if (enchantmentHolder.is(Enchantments.BINDING_CURSE)) {
                        score = -1;
                    }
                }
            }
            
            if (score > bestScore) {
                bestScore = score;
                targetSlot = i;
            }
        }
        
        return targetSlot;
    }
    
    public static void equipBestElytra(Minecraft client, int targetSlot) {
        LocalPlayer player = client.player;
        if (player == null) return;
        
        AbstractContainerMenu container = player.containerMenu;
        if (targetSlot >= 0 && client.gameMode != null) {
            // 使用點擊模擬來移動物品
            client.gameMode.handleInventoryMouseClick(container.containerId, targetSlot, 0, ClickType.PICKUP, player);
            client.gameMode.handleInventoryMouseClick(container.containerId, 6, 0, ClickType.PICKUP, player); // 胸甲槽位
            client.gameMode.handleInventoryMouseClick(container.containerId, targetSlot, 0, ClickType.PICKUP, player);
        }
    }
}
