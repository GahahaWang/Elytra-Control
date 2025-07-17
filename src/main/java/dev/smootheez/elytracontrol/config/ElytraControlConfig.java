package dev.smootheez.elytracontrol.config;

import dev.smootheez.elytracontrol.*;
import dev.smootheez.elytracontrol.config.option.*;
import dev.smootheez.scl.api.*;
import dev.smootheez.scl.config.*;

@Config(name = Constants.MOD_ID, gui = true)
public class ElytraControlConfig {
    public static final ConfigOption<Boolean> DEFAULT_ELTRA_CONTROL = ConfigOption.create("defaultElytraControl", true);

    public static final ConfigOption<Boolean> ALLOW_FLYING = ConfigOption.create("allowFlying", true);
    public static final ConfigOption<Boolean> DISABLE_FLY_NOTIFICATION = ConfigOption.create("disableFlyNotification", true);

    public static final ConfigOption<Boolean> EASY_FLY = ConfigOption.create("easyFly", false);
    public static final ConfigOption<Boolean> EASY_FLY_NOTIFICATION = ConfigOption.create("easyFlyNotification", true);
    public static final ConfigOption<Boolean> EASY_SWITCH = ConfigOption.create("easySwitch", false);

    public static final ConfigOption<LockIconMode> LOCK_ICON_MODE = ConfigOption.create("lockIconMode", LockIconMode.ICON_TEXT);
    public static final ConfigOption<OverlayPosition> OVERLAY_POSITION = ConfigOption.create("overlayPosition", OverlayPosition.TOP_LEFT);
}
