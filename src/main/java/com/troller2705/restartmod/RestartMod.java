package com.troller2705.restartmod;

import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(value = RestartMod.MODID, dist = Dist.DEDICATED_SERVER)
@EventBusSubscriber(modid = "restartmod")
public class RestartMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "restartmod";

    public RestartMod(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        System.out.println("RestartMod for AMP loaded.");
    }

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("restart")
                        .requires(source -> source.hasPermission(4))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            source.sendSuccess(() -> Component.literal("Requesting server restart..."), true);

                            new Thread(() -> {
                                try {
                                    String token = authenticateWithAMP();
                                    if (token != null) {
                                        restartAMPInstance(token);
                                        Thread.sleep(1000); // let AMP process the command
                                        source.getServer().halt(false);
                                    } else {
                                        source.sendFailure(Component.literal("Failed to authenticate with AMP."));
                                    }
                                } catch (Exception e) {
                                    source.sendFailure(Component.literal("AMP restart failed: " + e.getMessage()));
                                    e.printStackTrace();
                                }
                            }).start();

                            return 1;
                        })
        );
    }
    private static String authenticateWithAMP() throws Exception {
        URL url = new URL(Config.AMP_HOST.get() + "API/Core/Login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        String json = "{\"username\":\"" + Config.AMP_USERNAME.get() + "\",\"password\":\"" + Config.AMP_PASSWORD.get() + "\"}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        String response = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();
        JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
        return obj.has("token") ? obj.get("token").getAsString() : null;
    }

    private static void restartAMPInstance(String token) throws Exception {
        URL url = new URL(Config.AMP_HOST.get() + "API/ADSModule/RestartInstance");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("InstanceName", Config.AMP_INSTANCE_ID.get());
        conn.setRequestProperty("SESSIONID", "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");

        String json = "{\"InstanceID\":\"" + Config.AMP_INSTANCE_ID.get() + "\",\"Input\":\"" + "\"}";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("AMP rejected restart request: HTTP " + conn.getResponseCode());
        }
    }
}