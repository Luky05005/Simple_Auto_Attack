package org.simpleautoattack.SimpleAutoAttack;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagEntry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.simpleautoattack.SimpleAutoAttack.config.AutoAttackConfig;

public class AutoAttack implements ClientModInitializer {
    private static AutoAttackConfig config;
    private static KeyMapping toggleKeyBinding;
    private static KeyMapping togglePreventBlockBreakingKeyBinding;

    @Override
    public void onInitializeClient() {
        // Register config
        AutoConfig.register(AutoAttackConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(AutoAttackConfig.class).getConfig();
       
        final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath("simple_auto_attack", "main"));

        // Register keybinding 
        toggleKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.simple_auto_attack.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_COMMA,
            CATEGORY
        ));

        togglePreventBlockBreakingKeyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.simple_auto_attack.prevent_block_breaking",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_PERIOD,
            CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKeyBinding.consumeClick() && client.player != null && client.screen == null) {
                config.enabled = !config.enabled;
                AutoConfig.getConfigHolder(AutoAttackConfig.class).save();
                if (config.toggleNotification) {
                    String msgKey = config.enabled ? "simple_auto_attack.actionbar.on" : "simple_auto_attack.actionbar.off";
                    client.player.sendOverlayMessage(Component.translatable(msgKey));
                }
            }
            if (togglePreventBlockBreakingKeyBinding.consumeClick() && client.player != null && client.screen == null) {
                config.preventBlockBreaking.enabled = !config.preventBlockBreaking.enabled;
                AutoConfig.getConfigHolder(AutoAttackConfig.class).save();
                if (config.toggleNotification) {
                    String msgKey = config.preventBlockBreaking.enabled
                            ? "simple_auto_attack.actionbar.blockbreaking.on"
                            : "simple_auto_attack.actionbar.blockbreaking.off";

                    client.player.sendOverlayMessage(Component.translatable(msgKey));
                }
            }
            if (config.enabled) {  // Only run if enabled in config
                AutoMeleeTick(client);
            }
        });
    }

    // Add a static getter for the config
    public static AutoAttackConfig getConfig() {
        return config;
    }

    private void AutoMeleeTick(Minecraft mc) {
        if (!mc.options.keyAttack.isDown() || mc.player == null || mc.level == null || mc.gameMode == null
                || !(mc.player.getAttackStrengthScale(0) >= 1)) {
            return;
        }

        ItemStack mainHandItem = mc.player.getMainHandItem();
        PiercingWeapon spear = mainHandItem.get(DataComponents.PIERCING_WEAPON);

        if (mc.hitResult.getType() == HitResult.Type.MISS) {
            if (config.alwaysAttack) {
                // mc.player.resetLastAttackedTicks();
                if (spear != null) {
                    mc.gameMode.piercingAttack(spear);
                }
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } else if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.hitResult;
            BlockPos blockPos = blockHit.getBlockPos();
            BlockState blockState = mc.level.getBlockState(blockPos);

            if (blockState.getCollisionShape(mc.level, blockPos).isEmpty() || blockState.getDestroySpeed(mc.level, blockPos) == 0.0F) {
                float reach = (float) (mc.player.getAttackRangeWith(mainHandItem).maxReach());
                Vec3 camera = mc.player.getEyePosition(1.0F);
                Vec3 rotation = mc.player.getViewVector(1.0F);
                Vec3 end = camera.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);
                EntityHitResult result = ProjectileUtil.getEntityHitResult(mc.player, camera, end, new AABB(camera, end),
                        e -> !e.isSpectator() && e.isAttackable(), reach * reach);
                if (result != null && result.getEntity().isAlive()) {
                    // Spear exclusive attack
                    if (spear != null) {
                        if (PiercingWeapon.canHitEntity(mc.player, result.getEntity())) {
                            mc.gameMode.piercingAttack(spear);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                        }
                    } else {
                        mc.gameMode.attack(mc.player, result.getEntity());
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                }
            }
        } else if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) mc.hitResult).getEntity();
            if (entity.isAlive() && entity.isAttackable()) {
                // Spear exclusive attack
                if (spear != null) {
                    if (PiercingWeapon.canHitEntity(mc.player, entity)) {
                        mc.gameMode.piercingAttack(spear);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                    }
                } else {
                    mc.gameMode.attack(mc.player, entity);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        }
    }
}
