package com.troller2705.restartmod;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class Config {

    public static final Config CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    private Config(ModConfigSpec.Builder builder){
        builder.push("AMP Settings");

        AMP_HOST = builder
                .comment("AMP Host (e.g., http://127.0.0.1:8080/)")
                .define("amp_host", "http://127.0.0.1:8080/");

        AMP_USERNAME = builder
                .comment("AMP admin username")
                .define("amp_username", "admin");

        AMP_PASSWORD = builder
                .comment("AMP admin password or API key")
                .define("amp_password", "changeme");

        AMP_INSTANCE_ID = builder
                .comment("AMP instance ID (e.g., Minecraft01)")
                .define("amp_instance_id", "Minecraft01");

        builder.pop();
    }

    static {
        Pair<Config, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Config::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public static ModConfigSpec.ConfigValue<String> AMP_HOST;
    public static ModConfigSpec.ConfigValue<String> AMP_USERNAME;
    public static ModConfigSpec.ConfigValue<String> AMP_PASSWORD;
    public static ModConfigSpec.ConfigValue<String> AMP_INSTANCE_ID;

}
