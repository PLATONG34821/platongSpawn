package org.platong;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.util.HashMap;
import java.util.UUID;

public class PlatongSpawn extends JavaPlugin implements Listener {
    private final HashMap<UUID, Location> teleportingPlayers = new HashMap<>();
    private final int WAIT_TIME = 5; // Wait time in seconds

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("platongSpawn has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("platongSpawn has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spawn")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Check if player is already teleporting
                if (teleportingPlayers.containsKey(player.getUniqueId())) {
                    player.sendMessage("§cYou are already teleporting!");
                    return true;
                }

                // Store initial location
                teleportingPlayers.put(player.getUniqueId(), player.getLocation());
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 1,true));
                // Start teleport countdown with particles
                new BukkitRunnable() {
                    int secondsLeft = WAIT_TIME;

                    @Override
                    public void run() {
                        if (!teleportingPlayers.containsKey(player.getUniqueId())) {
                            this.cancel();
                            return;
                        }

                        if (secondsLeft <= 0) {
                            teleportToSpawn(player);
                            teleportingPlayers.remove(player.getUniqueId());
                            this.cancel();
                            return;
                        }

                        // Spawn particles in a circle around the player
                        Location loc = player.getLocation();
                        for (int i = 0; i < 20; i++) {
                            double angle = 2 * Math.PI * i / 20;
                            double x = Math.cos(angle) * 1;
                            double z = Math.sin(angle) * 1;
                            loc.getWorld().spawnParticle(
                                    Particle.PORTAL,
                                    loc.getX() + x,
                                    loc.getY() + 1,
                                    loc.getZ() + z,
                                    1, 0, 0, 0, 0
                            );
                        }
                        player.sendTitle("\uE81D","Teleporting in " + secondsLeft + "s",0,40,20);
                        if (secondsLeft <= 3) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1.0f, 1.0f);
                        }

                        secondsLeft--;
                    }
                }.runTaskTimer(this, 0L, 20L); // Run every second

                return true;
            } else {
                sender.sendMessage("§cThis command can only be used by players!");
                return false;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (teleportingPlayers.containsKey(player.getUniqueId())) {
            Location from = teleportingPlayers.get(player.getUniqueId());
            Location to = event.getTo();

            // Cancel only if player changes block position (allows looking around)
            if (from.getBlockX() != to.getBlockX() ||
                    from.getBlockY() != to.getBlockY() ||
                    from.getBlockZ() != to.getBlockZ()) {

                teleportingPlayers.remove(player.getUniqueId());
                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.sendTitle("\uE81D","§cTeleport cancelled due to movement!");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1.0f, 1.0f);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        teleportToSpawn(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(event.getPlayer().getWorld().getSpawnLocation());
    }

    private void teleportToSpawn(Player player) {
        Location spawnLocation = player.getWorld().getSpawnLocation();
        spawnLocation.setX(player.getWorld().getSpawnLocation().getX() + 0.5); // Ensure player is above ground when teleporting
        spawnLocation.setY(player.getWorld().getSpawnLocation().getY() + 2); // Ensure player is above ground when teleporting
        spawnLocation.setZ(player.getWorld().getSpawnLocation().getZ() + 0.5); // Ensure player is above ground when teleporting
        player.teleport(spawnLocation);
        player.sendTitle("\uE80B","Welcome to spawn!", 40,20,80);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        // Spawn success particles at destination
        player.getWorld().spawnParticle(
                Particle.PORTAL,
                spawnLocation.add(0, 1, 0),
                50, 0.5, 1, 0.5, 0.1
        );
    }
}