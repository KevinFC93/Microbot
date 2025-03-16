package net.runelite.client.plugins.microbot.runecrafting.LunarAltar;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.Getter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemID;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.breakhandler.BreakHandlerPlugin;
import net.runelite.client.plugins.microbot.runecrafting.LunarAltar.enums.Essence;
import net.runelite.client.plugins.microbot.runecrafting.LunarAltar.enums.Path;
import net.runelite.client.plugins.microbot.util.grandexchange.Rs2GrandExchange;
import net.runelite.client.plugins.microbot.util.inventory.Rs2Inventory;
import net.runelite.client.plugins.microbot.util.misc.Rs2Food;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;
import java.time.Instant;

@PluginDescriptor(
        name = PluginDescriptor.GMason + "Lunar Altar",
        description = "Microbot Lunar Altar plugin",
        tags = {"runecrafting", "microbot", "skilling"},
        enabledByDefault = false
)
public class LunarAltarPlugin extends Plugin {
    
    @Inject
    private LunarAltarConfig config;
    
    @Provides
    LunarAltarConfig provideConfig(ConfigManager configManager) { return configManager.getConfig(LunarAltarConfig.class); }
    
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private LunarAltarOverlay ouraniaOverlay;
    
    @Inject
    private LunarAltarScript ouraniaScript;

    public static String version = "1.2.0";

    @Getter
    public Instant startTime;
    
    @Getter
    private boolean ranOutOfAutoPay = false;
    @Getter
    private Essence essence;
    @Getter
    private int eatAtPercent;
    @Getter
    private Rs2Food rs2Food;
    @Getter
    private boolean useEnergyRestorePotions;
    @Getter
    private int drinkAtPercent;
    @Getter
    private Path path;
    @Getter
    private boolean toggleOverlay;
    @Getter
    private int profit;
    @Getter
    private boolean useDepositAll;
    @Getter
    private boolean useMassWorld;
    @Getter
    private boolean toggleProfitCalculator;
    
    @Override
    protected void startUp() throws AWTException {
        essence = config.essence();
        eatAtPercent = config.eatAtPercent();
        rs2Food = config.food();
        useEnergyRestorePotions = config.useEnergyRestorePotions();
        drinkAtPercent = config.drinkAtPercent();
        path = config.path();
        toggleOverlay = config.toggleOverlay();
        useDepositAll = config.useDepositAll();
        useMassWorld = config.useMassWorld();
        toggleProfitCalculator = config.toggleProfitCalculator();
        startTime = Instant.now();
        
        if(overlayManager != null) {
            overlayManager.add(ouraniaOverlay);
        }
        
        ouraniaScript.run();
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(ouraniaOverlay);
        ouraniaScript.shutdown();
        ranOutOfAutoPay = false;
    }
    
    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE) return;
        
        if (event.getMessage().toLowerCase().contains("you could not afford to pay with your quick payment")) {
            ranOutOfAutoPay = true;
        }
    }
    
    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!event.getGroup().equals(LunarAltarConfig.configGroup)) return;
        
        if (event.getKey().equals(LunarAltarConfig.essence)) {
            essence = config.essence();
        }
        
        if (event.getKey().equals(LunarAltarConfig.food)) {
            rs2Food = config.food();
        }
        
        if (event.getKey().equals(LunarAltarConfig.eatAtPercent)) {
            eatAtPercent = config.eatAtPercent();
        }

        if (event.getKey().equals(LunarAltarConfig.useEnergyRestorePotions)){
            useEnergyRestorePotions = config.useEnergyRestorePotions();
        }

        if (event.getKey().equals(LunarAltarConfig.drinkAtPercent)){
            drinkAtPercent = config.drinkAtPercent();
        }

        if (event.getKey().equals(LunarAltarConfig.path)){
            path = config.path();
        }

        if (event.getKey().equals(LunarAltarConfig.useDepositAll)){
            useDepositAll = config.useDepositAll();
        }
        if (event.getKey().equals(LunarAltarConfig.useMassWorld)){
            useMassWorld = config.useMassWorld();
        }

        if (event.getKey().equals(LunarAltarConfig.toggleOverlay)){
            toggleOverlay = config.toggleOverlay();
            toggleOverlay(toggleOverlay);
        }

        if (event.getKey().equals(LunarAltarConfig.toggleProfitCalculator)){
            toggleProfitCalculator = config.toggleProfitCalculator();
        }
    }

    public boolean isBreakHandlerEnabled() {
        return Microbot.isPluginEnabled(BreakHandlerPlugin.class);
    }
    
    public void calcuateProfit() {
        int teleportCost = Rs2GrandExchange.getPrice(ItemID.LAW_RUNE) + (Rs2GrandExchange.getPrice(ItemID.ASTRAL_RUNE) * 2);
        int runesCrafted = Rs2Inventory.items().stream()
                .filter(rs2Item -> rs2Item.getName().toLowerCase().contains("rune") && !rs2Item.getName().toLowerCase().contains("rune pouch"))
                .mapToInt(rs2Item -> Rs2GrandExchange.getPrice(rs2Item.getId()))
                .sum();
        int profitFromRun = teleportCost - runesCrafted;
        
        profit += profitFromRun;
    }

    private void toggleOverlay(boolean hideOverlay) {
        if (overlayManager != null) {
            boolean hasOverlay = overlayManager.anyMatch(ov -> ov.getName().equalsIgnoreCase(LunarAltarOverlay.class.getSimpleName()));

            if (hideOverlay) {
                if(!hasOverlay) return;

                overlayManager.remove(ouraniaOverlay);
            } else {
                if (hasOverlay) return;

                overlayManager.add(ouraniaOverlay);
            }
        }
    }
}
