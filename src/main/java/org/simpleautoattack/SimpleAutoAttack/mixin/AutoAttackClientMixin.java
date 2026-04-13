package org.simpleautoattack.SimpleAutoAttack.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.simpleautoattack.SimpleAutoAttack.config.AutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;

@Environment(EnvType.CLIENT)
@Mixin(Minecraft.class)
public abstract class AutoAttackClientMixin {
    @Shadow
    public MultiPlayerGameMode gameMode;
    @Shadow
    public LocalPlayer player;

    // Prevents breaking blocks when holding a weapon
    @Inject(method = "continueAttack(Z)V", at = @At("HEAD"), cancellable = true)
    public void onHandleBlockBreaking(boolean isBreakPressed, CallbackInfo info) {
        // Get config instance
        AutoAttackConfig config = AutoConfig.getConfigHolder(AutoAttackConfig.class).getConfig();
        
        // Only prevent block breaking if the config option is enabled
        if (config.preventBlockBreaking.enabled && isBreakPressed && player != null) {
            // ItemStack mainHandItem = player.getInventory().getMainHandStack();
            ItemStack mainHandItem = player.getMainHandItem();
            String itemName = mainHandItem.getItem().toString().toLowerCase();

            boolean shouldPrevent = false;
            if (mainHandItem.is(ItemTags.SWORDS) && config.preventBlockBreaking.sword) {
                shouldPrevent = true;
            } else if (itemName.contains("trident") && config.preventBlockBreaking.trident) {
                shouldPrevent = true;
            } else if (itemName.contains("mace") && config.preventBlockBreaking.mace) {
                shouldPrevent = true;
            } else if (mainHandItem.is(ItemTags.AXES) && config.preventBlockBreaking.axe) {
                shouldPrevent = true;
            } else if (mainHandItem.is(ItemTags.PICKAXES) && config.preventBlockBreaking.pickaxe) {
                shouldPrevent = true;
            } else if (mainHandItem.is(ItemTags.SHOVELS) && config.preventBlockBreaking.shovel) {
                shouldPrevent = true;
            } else if (mainHandItem.is(ItemTags.HOES) && config.preventBlockBreaking.hoe) {
                shouldPrevent = true;
            } else if (mainHandItem.is(ItemTags.SPEARS) && config.preventBlockBreaking.spear) {
                shouldPrevent = true;
            }

            if (shouldPrevent) {
                gameMode.stopDestroyBlock();
                info.cancel();
            }
        }
    }
}
