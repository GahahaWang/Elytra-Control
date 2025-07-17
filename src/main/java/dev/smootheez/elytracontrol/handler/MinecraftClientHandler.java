package dev.smootheez.elytracontrol.handler;

import dev.smootheez.elytracontrol.*;
import dev.smootheez.elytracontrol.config.*;
import dev.smootheez.elytracontrol.registry.*;
import dev.smootheez.elytracontrol.util.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.player.*;
import net.minecraft.network.chat.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

import java.util.*;

public class MinecraftClientHandler {
    private int elytraTime = 0;
    private final Minecraft client;
    private static boolean shouldDisableFlying = false;
    private static boolean shouldEasyFly = true;
    private ActionStep actionStep = ActionStep.START_MOMENTUM;
    private boolean startEasyFly = false;
    private final Random random = new Random();

    public MinecraftClientHandler(Minecraft client) {
        this.client = client;
    }

    public void handleKeyBinds() {
        var player = client.player;
        var gameMode = client.gameMode;

        if (player == null || gameMode == null) return;

        var startFlyingKey = KeyBindsRegistry.START_FLYING;
        var stopFlyingKey = KeyBindsRegistry.STOP_FLYING;

        if (startFlyingKey.same(stopFlyingKey)) {
            while (startFlyingKey.consumeClick()) {
                if (player.isFallFlying())
                    player.stopFallFlying();
                sendStartFlyingPacket(player);
            }
        } else {
            while (startFlyingKey.consumeClick()) {
                if (player.isFallFlying()) continue;
                sendStartFlyingPacket(player);
            }

            while (stopFlyingKey.consumeClick()) {
                if (!player.isFallFlying()) continue;
                player.stopFallFlying();
                sendStartFlyingPacket(player);
            }
        }

        while (KeyBindsRegistry.DISABLE_FLYING_TOGGLE.consumeClick() && ElytraControlConfig.ALLOW_FLYING.getValue()) {
            shouldDisableFlying = !shouldDisableFlying;

            if (ElytraControlConfig.DISABLE_FLY_NOTIFICATION.getValue())
                player.displayClientMessage(
                        CommonComponents.optionStatus(Component.translatable("notification.elytracontrol.disableFlying"), shouldDisableFlying), true
                );
        }
        while (KeyBindsRegistry.EASY_FLY_TOGGLE.consumeClick() && ElytraControlConfig.EASY_FLY.getValue()) {
            shouldEasyFly = !shouldEasyFly;

            if (ElytraControlConfig.EASY_FLY_NOTIFICATION.getValue())
                player.displayClientMessage(
                        CommonComponents.optionStatus(Component.translatable("notification.elytracontrol.easyFly"), shouldEasyFly), true
                );
        }
    }

    public void onEndClientTick() {
        if (client.screen != null) return;

        var player = client.player;
        var options = client.options;
        var gameMode = client.gameMode;

        if (player == null || gameMode == null) return;

        var isPlayerFlying = player.isFallFlying();

        int randomNumber = random.nextInt(3) + 1;

        if ((options.keyJump.consumeClick() && elytraTime > randomNumber && ElytraControlConfig.DEFAULT_ELTRA_CONTROL.getValue() && isPlayerFlying) || shouldDisableFlying) {
            player.stopFallFlying();
            sendStartFlyingPacket(player);
        }

        if (isPlayerFlying && !options.keyJump.isDown())
            elytraTime = (elytraTime + 1) % 1000;
        else elytraTime = 0;

        if (canInitiateEasyFly()) {
            for (InteractionHand hand : InteractionHand.values()) {
                if (player.getItemInHand(hand).getItem() instanceof FireworkRocketItem) {
                    startEasyFly = true;
                    break;
                }
            }
        }

        if (startEasyFly) {
            switch (actionStep) {
                case EQUIP_ELYTRA -> handleEquipElytra(client);
                case START_MOMENTUM -> handleMomentumAction(player);
                case START_FLYING -> handleFlyingAction(player);
                case USE_FIREWORK -> handleFireworkAction(player, gameMode);
            }
        }
    }

    private void handleEquipElytra(Minecraft client) {
        // 如果 EASY_SWITCH 為 true，則自動裝備最佳鞘翅
        Constants.LOGGER.info("Check Equip Elytra");
        if (!isWearingElytra()) {
            if (ElytraControlConfig.EASY_SWITCH.getValue()) {
                int bestElytraSlot = ElytraEquipmentUtil.findBestElytraSlot(client);
                if (bestElytraSlot != -1) {
                    ElytraEquipmentUtil.equipBestElytra(client, bestElytraSlot);
                }
            }
        }
        actionStep = ActionStep.START_MOMENTUM;
    }

    private void handleMomentumAction(LocalPlayer player) {
        Constants.LOGGER.info("Starting momentum");
        Vec3 vec3 = player.getDeltaMovement();
        player.setDeltaMovement(vec3.x, 0.1F, vec3.z);
        actionStep = ActionStep.START_FLYING;
    }

    public void handleFlyingAction(LocalPlayer player) {
        Constants.LOGGER.info("Start flying using elytra");
        sendStartFlyingPacket(player);
        actionStep = ActionStep.USE_FIREWORK;
    }

    public void handleFireworkAction(LocalPlayer player, MultiPlayerGameMode gameMode) {
        Constants.LOGGER.info("Start using firework");
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof FireworkRocketItem) {
                player.swing(hand);
                gameMode.useItem(player, hand);
                break;
            }
        }
        startEasyFly = false;
        actionStep = ActionStep.EQUIP_ELYTRA;
    }

    private boolean canInitiateEasyFly() {
        var player = client.player;

        return player != null
                && ((client.options.keyUse.isDown() && KeyBindsRegistry.EASY_FLY.isUnbound()) || KeyBindsRegistry.EASY_FLY.consumeClick())
                //&& player.onGround()
                && (isWearingElytra() || (hasElytraInPlayerContainer() && ElytraControlConfig.EASY_SWITCH.getValue()))
                && isCorsshairClear()
                //&& !player.isInWater()
                && !player.isUsingItem()
                && !player.swinging
                && ElytraControlConfig.EASY_FLY.getValue()
                && shouldEasyFly
                && !shouldDisableFlying;
    }

    private boolean hasElytraInPlayerContainer(){
        int BestElytraSlot = ElytraEquipmentUtil.findBestElytraSlot(client);
        return BestElytraSlot != -1;
    }

    private boolean isWearingElytra() {
        if (client.player != null) {
            return client.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        }
        return false;
    }

    private boolean isCorsshairClear() {
        HitResult hitResult = client.hitResult;
        return hitResult == null || hitResult.getType() == HitResult.Type.MISS;
    }

    private void sendStartFlyingPacket(LocalPlayer player) {
        player.connection.send(new ServerboundPlayerCommandPacket(player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
    }

    public static boolean isShouldDisableFlying() {
        return shouldDisableFlying;
    }
}
