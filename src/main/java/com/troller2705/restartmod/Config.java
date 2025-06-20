package com.troller2705.restartmod;

import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> AMP_HOST;
    public static final ModConfigSpec.ConfigValue<String> AMP_USERNAME;
    public static final ModConfigSpec.ConfigValue<String> AMP_PASSWORD;
    public static final ModConfigSpec.ConfigValue<String> AMP_INSTANCE_ID;

    static {
        BUILDER.push("AMP Settings");

        AMP_HOST = BUILDER
                .comment("AMP Host (e.g., http://127.0.0.1:8080/)")
                .define("amp_host", "http://127.0.0.1:8080/");

        AMP_USERNAME = BUILDER
                .comment("AMP admin username")
                .define("amp_username", "admin");

        AMP_PASSWORD = BUILDER
                .comment("AMP admin password or API key")
                .define("amp_password", "changeme");

        AMP_INSTANCE_ID = BUILDER
                .comment("AMP instance ID (e.g., Minecraft01)")
                .define("amp_instance_id", "Minecraft01");

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
