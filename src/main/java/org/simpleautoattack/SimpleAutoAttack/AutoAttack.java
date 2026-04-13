package org.simpleautoattack.SimpleAutoAttack;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.simpleautoattack.SimpleAutoAttack.config.AutoAttackConfig;

import net.minecraft.block.BlockState;

import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class AutoAttack implements ClientModInitializer {
    private static AutoAttackConfig config;
    private static KeyBinding toggleKeyBinding;
    private static KeyBinding togglePreventBlockBreakingKeyBinding;

    @Override
    public void onInitializeClient() {
        // Register config
        AutoConfig.register(AutoAttackConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(AutoAttackConfig.class).getConfig();

        final KeyBinding.Category CATEGORY =
                KeyBinding.Category.create(Identifier.of("simple_auto_attack", "main"));

        // Register keybinding
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_auto_attack.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_COMMA,
                CATEGORY
        ));

        togglePreventBlockBreakingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.simple_auto_attack.prevent_block_breaking",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_PERIOD,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleKeyBinding.wasPressed() && client.player != null && client.currentScreen == null) {
                config.enabled = !config.enabled;
                AutoConfig.getConfigHolder(AutoAttackConfig.class).save();
                if (config.toggleNotification) {
                    String msgKey = config.enabled ? "simple_auto_attack.actionbar.on" : "simple_auto_attack.actionbar.off";
                    client.player.sendMessage(Text.translatable(msgKey), true);
                }
            }
            if (togglePreventBlockBreakingKeyBinding.wasPressed() && client.player != null && client.currentScreen == null) {
                config.preventBlockBreaking.enabled = !config.preventBlockBreaking.enabled;
                AutoConfig.getConfigHolder(AutoAttackConfig.class).save();
                if (config.toggleNotification) {
                    String msgKey = config.preventBlockBreaking.enabled
                            ? "simple_auto_attack.actionbar.blockbreaking.on"
                            : "simple_auto_attack.actionbar.blockbreaking.off";

                    client.player.sendMessage(Text.translatable(msgKey), true);
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

    private void AutoMeleeTick(MinecraftClient mc) {
        if (!mc.options.attackKey.isPressed()
                || mc.player == null
                || mc.world == null
                || mc.interactionManager == null) {
            return;
        }

        ItemStack mainHandItem = mc.player.getMainHandStack();

        // Mace Ignore cooldown
        boolean ignoreCooldown = getConfig().ignoreMaceCooldown && mainHandItem.getItem().toString().toLowerCase().contains("mace");
        if (!ignoreCooldown && mc.player.getAttackCooldownProgress(0) < 1) {
            return;
        }

        if (mc.crosshairTarget.getType() == HitResult.Type.MISS) {
            if (config.alwaysAttack) {
                mc.player.resetLastAttackedTicks();
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) mc.crosshairTarget;
            BlockPos blockPos = blockHit.getBlockPos();
            BlockState blockState = mc.world.getBlockState(blockPos);

            if (blockState.getCollisionShape(mc.world, blockPos).isEmpty() || blockState.getHardness(mc.world, blockPos) == 0.0F) {
                float reach = (float) (mc.player.isInCreativeMode() ? 4.5 : 3.0);
                Vec3d camera = mc.player.getCameraPosVec(1.0F);
                Vec3d rotation = mc.player.getRotationVec(1.0F);
                Vec3d end = camera.add(rotation.x * reach, rotation.y * reach, rotation.z * reach);
                EntityHitResult result = ProjectileUtil.raycast(mc.player, camera, end, new Box(camera, end),
                        e -> !e.isSpectator() && e.isAttackable(), reach * reach);
                if (result != null && result.getEntity().isAlive()) {
                    mc.interactionManager.attackEntity(mc.player, result.getEntity());
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
            }
        } else if (mc.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity entity = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (entity.isAlive() && entity.isAttackable()) {
                mc.interactionManager.attackEntity(mc.player, entity);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
