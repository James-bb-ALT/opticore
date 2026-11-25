package dev.opticore.mixin.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents NullPointerException in EMI's EmiAnvilEnchantRecipe.getBook() when a modded
 * item/enchantment leaves ItemStack#getTag() null or without an Enchantments list.
 */
@Pseudo
@Mixin(targets = "dev.emi.emi.recipe.special.EmiAnvilEnchantRecipe", remap = false)
public abstract class EmiAnvilEnchantRecipeMixin {
    @Shadow
    private Enchantment enchantment;

    @Shadow
    private int level;

    @Inject(method = "getTool", at = @At("RETURN"), require = 0)
    private void opticore$ensureEnchantmentTag(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = cir.getReturnValue();
        if (stack == null || this.enchantment == null) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Enchantments", 9) || tag.getList("Enchantments", 10).isEmpty()) {
            ResourceLocation key = BuiltInRegistries.ENCHANTMENT.getKey(this.enchantment);
            if (key != null) {
                ListTag list = new ListTag();
                CompoundTag entry = new CompoundTag();
                entry.putString("id", key.toString());
                entry.putShort("lvl", (short) this.level);
                list.add(entry);
                tag.put("Enchantments", list);
            }
        }
    }
}
