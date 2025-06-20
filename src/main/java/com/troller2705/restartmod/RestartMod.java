package com.troller2705.restartmod;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RestartMod.MODID)
public class RestartMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "restartmod";

    public RestartMod(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (AntiItemLag) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        System.out.println("RestartMod for AMP loaded.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("restart")
                        .requires(source -> source.hasPermission(4))
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            MinecraftServer server = source.getServer();
                            source.sendSuccess(() -> Component.literal("Requesting server restart..."), true);

                            new Thread(() -> {
                                try {
                                    String token = authenticateWithAMP();
                                    if (token != null) {

                                        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());

                                        for (ServerPlayer player : players) {
                                            player.connection.disconnect(Component.literal("Server is restarting..."));
                                        }

                                        server.setMotd("Server is restarting...");

                                        server.saveEverything(false, true, true);

                                        restartAMPInstance(token);

                                        //Thread.sleep(Duration.ofSeconds(15));
                                        //server.halt(false);
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
    private String authenticateWithAMP() throws Exception {
        URL url = new URL(Config.AMP_HOST.get() + "API/Core/Login");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String json = String.format("{\"username\": \"%s\",\"password\": \"%s\", \"token\": \"\", \"rememberMe\": false}",
                Config.AMP_USERNAME.get(),
                Config.AMP_PASSWORD.get()
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        String response = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();
        JsonObject obj = JsonParser.parseString(response).getAsJsonObject();
        return obj.has("sessionID") ? obj.get("sessionID").getAsString() : null;
    }

    private void restartAMPInstance(String token) throws Exception {
        URL url = new URL(Config.AMP_HOST.get() + "API/ADSModule/RestartInstance");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");

        String json = String.format("{\"InstanceName\": \"%s\", \"SESSIONID\": \"%s\"}",
                Config.AMP_INSTANCE_ID.get(),
                token
        );

        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes());
        }

        //String response = new Scanner(conn.getInputStream()).useDelimiter("\\A").next();
        //System.out.println(response);

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("AMP rejected restart request: HTTP " + conn.getResponseCode());
        }
    }
}