package io.github.gustav9797.CustomArrows;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CustomArrows extends JavaPlugin implements Listener
{
	public static Economy econ = null;

	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
		if (!setupEconomy())
		{
			getLogger().log(Level.SEVERE, String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
	}

	public void onDisable()
	{

	}

	private boolean setupEconomy()
	{
		if (getServer().getPluginManager().getPlugin("Vault") == null)
		{
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null)
		{
			return false;
		}
		econ = rsp.getProvider();
		return econ != null;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("poisonarrows"))
		{
			if (sender instanceof Player)
			{
				Player player = (Player) sender;
				if (player.hasPermission("customarrows.poison"))
				{
					if (econ.has(player.getName(), 400))
					{
						EconomyResponse response = econ.withdrawPlayer(player.getName(), 400);
						if (response.transactionSuccess())
						{
							ItemStack arrows = new ItemStack(Material.ARROW, 64);
							ItemMeta temp = arrows.getItemMeta();
							ArrayList<String> description = new ArrayList<String>()
							{
								{
									add("Poison arrow");
								}
							};
							temp.setLore(description);
							arrows.setItemMeta(temp);
							player.getInventory().addItem(arrows);
							player.sendMessage("You bought 1 stack of poison arrows.");
						}
						else
							player.sendMessage("Failed to buy arrows. " + response.errorMessage);
					}
					else
						player.sendMessage("Failed to buy arrows. You need atleast $400");
				}
			}
			return true;
		}
		return false;
	}

	@EventHandler
	public void onEntityShootBow(EntityShootBowEvent event)
	{
		if (event.getEntity() instanceof Player)
		{
			// Now check if the arrow that will be used is a poison arrow
			Player player = (Player) event.getEntity();
			Inventory inventory = player.getInventory();
			for (int i = 0; i <= 35; i++) // 35 magic value for player inventory
											// size, must fix
			{
				ItemStack current = inventory.getItem(i);
				if (current != null && current.getType() == Material.ARROW)
				{
					if (current.getItemMeta().getLore() != null && !current.getItemMeta().getLore().isEmpty())
					{
						if (current.getItemMeta().getLore().get(0).equalsIgnoreCase("Poison arrow"))
						{
							// Player is going to shoot a poison arrow
							if (event.getProjectile() instanceof Arrow)
							{
								Arrow projectile = (Arrow) event.getProjectile();
								projectile.setMetadata("parrow", new FixedMetadataValue(this, true));
								return;
							}
						}
					}
					else
						return;
				}
			}
		}
	}

	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
	{
		if (event.getDamager() instanceof Arrow)
		{
			Arrow damager = (Arrow) event.getDamager();
			List<MetadataValue> temp = damager.getMetadata("parrow");
			if (!temp.isEmpty())
			{
				MetadataValue isPoisonArrow = temp.get(0);
				if (isPoisonArrow.asBoolean())
				{
					if (event.getEntity() instanceof Creature)
					{
						Creature damaged = (Creature) event.getEntity();
						PotionEffect potionEffect = new PotionEffect(PotionEffectType.POISON, 3580, -2);
						damaged.addPotionEffect(potionEffect);
					}
					else if (event.getEntity() instanceof Player)
					{
						Player damaged = (Player) event.getEntity();
						PotionEffect potionEffect = new PotionEffect(PotionEffectType.POISON, 3580, -2);
						damaged.addPotionEffect(potionEffect);
					}
				}
			}
		}
	}
}
