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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.simpleautoattack.SimpleAutoAttack.config.AutoAttackConfig;
import me.shedaniel.autoconfig.AutoConfig;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

            if (shouldPrevent(mainHandItem, config)) {
                gameMode.stopDestroyBlock();
                info.cancel();
            }
        }
    }

    @Unique
    private boolean shouldPrevent(ItemStack stack, AutoAttackConfig config) {
        String itemName = stack.getItem().toString().toLowerCase();

        if (stack.is(ItemTags.SWORDS) && config.preventBlockBreaking.sword) {
            return true;
        } else if (itemName.contains("trident") && config.preventBlockBreaking.trident) {
            return true;
        } else if (itemName.contains("mace") && config.preventBlockBreaking.mace) {
            return true;
        } else if (stack.is(ItemTags.SPEARS) && config.preventBlockBreaking.spear) {
            return true;
        } else if (stack.is(ItemTags.AXES) && config.preventBlockBreaking.axe) {
            return true;
        } else if (stack.is(ItemTags.PICKAXES) && config.preventBlockBreaking.pickaxe) {
            return true;
        } else if (stack.is(ItemTags.SHOVELS) && config.preventBlockBreaking.shovel) {
            return true;
        } else if (stack.is(ItemTags.HOES) && config.preventBlockBreaking.hoe) {
            return true;
        } else {
            return false;
        }
    }

    // Prevent first attack
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    public void doAttack(CallbackInfoReturnable<Boolean> cir) {
        AutoAttackConfig config = AutoConfig.getConfigHolder(AutoAttackConfig.class).getConfig();

        if (config.enabled && config.preventFirstAttack && player != null) {
            ItemStack mainHandItem = player.getMainHandItem();

            if (shouldPrevent(mainHandItem, config)) {
                cir.setReturnValue(false);
            }
        }
    }
}
