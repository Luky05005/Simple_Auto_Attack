package org.simpleautoattack.SimpleAutoAttack.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
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
@Mixin(MinecraftClient.class)
public abstract class AutoAttackClientMixin {
    @Shadow
    public ClientPlayerInteractionManager interactionManager;
    @Shadow
    public ClientPlayerEntity player;

    // Prevents breaking blocks when holding a weapon
    @Inject(method = "handleBlockBreaking(Z)V", at = @At("HEAD"), cancellable = true)
    public void onHandleBlockBreaking(boolean isBreakPressed, CallbackInfo info) {
        AutoAttackConfig config = AutoConfig.getConfigHolder(AutoAttackConfig.class).getConfig();

        // Only prevent block breaking if the config option is enabled
        if (config.preventBlockBreaking.enabled && isBreakPressed && player != null) {
            ItemStack mainHandItem = player.getMainHandStack();

            if (shouldPrevent(mainHandItem, config)) {
                interactionManager.cancelBlockBreaking();
                info.cancel();
            }
        }
    }

    @Unique
    private boolean shouldPrevent(ItemStack stack, AutoAttackConfig config) {
        String itemName = stack.getItem().toString().toLowerCase();

        if (stack.isIn(ItemTags.SWORDS) && config.preventBlockBreaking.sword) {
            return true;
        } else if (itemName.contains("trident") && config.preventBlockBreaking.trident) {
            return true;
        } else if (itemName.contains("mace") && config.preventBlockBreaking.mace) {
            return true;
        } else if (stack.isIn(ItemTags.AXES) && config.preventBlockBreaking.axe) {
            return true;
        } else if (stack.isIn(ItemTags.PICKAXES) && config.preventBlockBreaking.pickaxe) {
            return true;
        } else if (stack.isIn(ItemTags.SHOVELS) && config.preventBlockBreaking.shovel) {
            return true;
        } else if (stack.isIn(ItemTags.HOES) && config.preventBlockBreaking.hoe) {
            return true;
        } else {
            return false;
        }
    }

    // Prevent first attack
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    public void doAttack(CallbackInfoReturnable<Boolean> cir) {
        AutoAttackConfig config = AutoConfig.getConfigHolder(AutoAttackConfig.class).getConfig();

        if (config.enabled && config.preventFirstAttack && player != null) {
            ItemStack mainHandItem = player.getMainHandStack();

            if (shouldPrevent(mainHandItem, config)) {
                cir.setReturnValue(false);
            }
        }
    }
}
