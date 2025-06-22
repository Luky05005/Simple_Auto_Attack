package org.simpleautoattack.SimpleAutoAttack.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "simple_auto_attack")
public class AutoAttackConfig implements ConfigData {
    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean enabled = true;

    @ConfigEntry.Gui.Tooltip
    public boolean alwaysAttack = false;

    @ConfigEntry.Gui.Tooltip(count = 2)
    public boolean toggleNotification = true;

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public PreventBlockBreakingGroup preventBlockBreaking = new PreventBlockBreakingGroup();

    public static class PreventBlockBreakingGroup {
        @ConfigEntry.Gui.Tooltip(count = 2)
        public boolean enabled = true;

        @ConfigEntry.Gui.NoTooltip
        public boolean sword = true;

        @ConfigEntry.Gui.NoTooltip
        public boolean trident = true;

        @ConfigEntry.Gui.NoTooltip
        public boolean mace = true;

        @ConfigEntry.Gui.NoTooltip
        public boolean axe = false;

        @ConfigEntry.Gui.NoTooltip
        public boolean pickaxe = false;

        @ConfigEntry.Gui.NoTooltip
        public boolean shovel = false;

        @ConfigEntry.Gui.NoTooltip
        public boolean hoe = false;
    }
}
