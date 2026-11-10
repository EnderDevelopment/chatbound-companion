package com.enderdevelopment.plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private final Map<UUID, UUID> companions = new HashMap<>();
    private final Map<UUID, Mode> modes = new HashMap<>();
    private BukkitTask behaviorTask;

    private enum Mode { FOLLOW, STAY, WANDER }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("friend").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("This command is for players.");
                return true;
            }
            if (args.length == 0) {
                tell(player, "Try /friend summon, follow, stay, wander, come, or remove.");
                return true;
            }
            switch (args[0].toLowerCase()) {
                case "summon" -> summon(player);
                case "follow" -> setMode(player, Mode.FOLLOW, "I'll follow you.");
                case "stay" -> setMode(player, Mode.STAY, "I'll stay right here.");
                case "wander" -> setMode(player, Mode.WANDER, "I'll explore nearby.");
                case "come" -> bringToPlayer(player);
                case "remove" -> removeFriend(player);
                default -> tell(player, "I don't know that command. Try /friend.");
            }
            return true;
        });
        behaviorTask = Bukkit.getScheduler().runTaskTimer(this, this::tickFriends, 20L, 20L);
    }

    @Override
    public void onDisable() {
        if (behaviorTask != null) behaviorTask.cancel();
        for (UUID entityId : companions.values()) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) entity.remove();
        }
        companions.clear();
    }

    private void summon(org.bukkit.entity.Player player) {
        if (findFriend(player) != null) {
            tell(player, "I'm already here!");
            return;
        }
        Location spawn = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        spawn.setY(player.getLocation().getY());
        Villager friend = (Villager) player.getWorld().spawnEntity(spawn, EntityType.VILLAGER);
        friend.customName(Component.text(player.getName() + "'s Friend", NamedTextColor.GREEN));
        friend.setCustomNameVisible(true);
        friend.setAI(false);
        friend.setInvulnerable(true);
        friend.setPersistent(true);
        friend.setProfession(Villager.Profession.NITWIT);
        companions.put(player.getUniqueId(), friend.getUniqueId());
        modes.put(player.getUniqueId(), Mode.FOLLOW);
        tell(player, "Hey! I'm here. Talk to me in chat, or use /friend stay or /friend wander.");
    }

    private void setMode(org.bukkit.entity.Player player, Mode mode, String response) {
        if (findFriend(player) == null) {
            tell(player, "I can't see you yet. Use /friend summon first.");
            return;
        }
        modes.put(player.getUniqueId(), mode);
        tell(player, response);
    }

    private void bringToPlayer(org.bukkit.entity.Player player) {
        Entity friend = findFriend(player);
        if (friend == null) { summon(player); return; }
        friend.teleport(player.getLocation().add(player.getLocation().getDirection().multiply(2)));
        modes.put(player.getUniqueId(), Mode.FOLLOW);
        tell(player, "I'm coming!");
    }

    private void removeFriend(org.bukkit.entity.Player player) {
        Entity friend = findFriend(player);
        if (friend != null) friend.remove();
        companions.remove(player.getUniqueId());
        modes.remove(player.getUniqueId());
        tell(player, "See you later!");
    }

    private Entity findFriend(org.bukkit.entity.Player player) {
        UUID id = companions.get(player.getUniqueId());
        return id == null ? null : Bukkit.getEntity(id);
    }

    private void tickFriends() {
        for (Map.Entry<UUID, UUID> entry : companions.entrySet()) {
            org.bukkit.entity.Player owner = Bukkit.getPlayer(entry.getKey());
            Entity entity = Bukkit.getEntity(entry.getValue());
            if (entity == null || !(entity instanceof Villager friend)) continue;
            if (owner == null || !owner.isOnline() || !owner.getWorld().equals(friend.getWorld())) continue;
            Mode mode = modes.getOrDefault(entry.getKey(), Mode.FOLLOW);
            if (mode == Mode.FOLLOW && friend.getLocation().distanceSquared(owner.getLocation()) > 9) {
                Location target = owner.getLocation().clone().add(owner.getLocation().getDirection().multiply(-1.5));
                target.setY(owner.getLocation().getY());
                friend.teleport(target);
            } else if (mode == Mode.WANDER && Math.random() < 0.35) {
                Location target = friend.getLocation().clone().add(Math.random() * 8 - 4, 0, Math.random() * 8 - 4);
                target.setY(friend.getWorld().getHighestBlockYAt(target) + 1);
                if (target.getBlock().getType().isAir() && target.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) friend.teleport(target);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage().toLowerCase();
        org.bukkit.entity.Player player = event.getPlayer();
        if (!message.contains("friend") && !companions.containsKey(player.getUniqueId())) return;
        Bukkit.getScheduler().runTask(this, () -> respond(player, message));
    }

    private void respond(org.bukkit.entity.Player player, String message) {
        if (!companions.containsKey(player.getUniqueId())) return;
        String reply;
        if (message.contains("follow")) { modes.put(player.getUniqueId(), Mode.FOLLOW); reply = "Okay, I'll follow you."; }
        else if (message.contains("stay") || message.contains("wait")) { modes.put(player.getUniqueId(), Mode.STAY); reply = "I'll wait here."; }
        else if (message.contains("wander") || message.contains("explore")) { modes.put(player.getUniqueId(), Mode.WANDER); reply = "I'm going to explore nearby."; }
        else if (message.contains("come") || message.contains("here")) { bringToPlayer(player); return; }
        else if (message.contains("hello") || message.contains("hi ") || message.startsWith("hi")) reply = "Hello, " + player.getName() + "! What should we do?";
        else if (message.contains("what") && message.contains("doing")) reply = "I'm keeping an eye on things. Ask me to follow, stay, or explore.";
        else if (message.contains("mine") || message.contains("wood") || message.contains("build")) reply = "I can't break blocks yet, but I can keep you company while you work.";
        else reply = new String[] {"Sounds good!", "Interesting. Tell me more.", "I'm here with you.", "What should we do next?"}[(int) (Math.random() * 4)];
        tell(player, reply);
    }

    @EventHandler
    public void onFriendDamage(EntityDamageEvent event) {
        if (companions.containsValue(event.getEntity().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Entity friend = findFriend(event.getPlayer());
        if (friend != null) friend.remove();
        companions.remove(event.getPlayer().getUniqueId());
        modes.remove(event.getPlayer().getUniqueId());
    }

    private void tell(org.bukkit.entity.Player player, String text) {
        player.sendMessage(Component.text("Friend ", NamedTextColor.GREEN).append(Component.text(text, NamedTextColor.WHITE)));
    }
}
