package io.github.gustav9797.CustomArrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Material;
import org.bukkit.block.Dispenser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

public class CustomArrows extends JavaPlugin implements Listener
{
	public static Economy econ = null;
	public static HashMap<String, CustomArrow> arrowTypes = null;

	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
		if (!setupEconomy())
		{
			getLogger().log(Level.SEVERE, String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Initialize all types of arrows
		arrowTypes = new HashMap<String, CustomArrow>();
		for (PotionType potionType : PotionType.values())
		{
			String potionName = potionType.name().toLowerCase();
			// getLogger().log(Level.INFO, "Trying: " + potionName);
			if (getConfig().contains("customarrows." + potionName))
			{
				arrowTypes.put(
						potionName,
						new CustomArrow(potionName, new PotionImpactEffect(potionType.getEffectType(), getConfig().getInt("customarrows." + potionName + ".duration"), getConfig().getInt(
								"customarrows." + potionName + ".amplifier")), getConfig().getInt("customarrows." + potionName + ".cost")));
				// getLogger().log(Level.INFO, "Added arrow: " + potionName);
			}
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
		if (cmd.getName().equalsIgnoreCase("buyarrows") && args.length >= 1)
		{
			if (sender instanceof Player)
			{
				Player player = (Player) sender;
				if (arrowTypes.containsKey(args[0]))
				{
					if (player.hasPermission("customarrows." + args[0]))
					{
						// Get the amount of arrows the player wants to buy
						int amount = 1;
						try
						{
							if (args.length > 1)
								amount = Integer.parseInt(args[1]);
						}
						catch (NumberFormatException e)
						{
							amount = 1;
						}

						int totalCost = amount * arrowTypes.get(args[0]).cost;

						if (econ.has(player.getName(), totalCost))
						{
							EconomyResponse response = econ.withdrawPlayer(player.getName(), totalCost);
							if (response.transactionSuccess())
							{
								ItemStack arrows = new ItemStack(Material.ARROW, amount);
								ItemMeta temp = arrows.getItemMeta();
								List<String> description = new ArrayList<String>();
								description.add(args[0]);
								temp.setLore(description);
								arrows.setItemMeta(temp);
								player.getInventory().addItem(arrows);
								if (amount >= 1)
									player.sendMessage("You bought " + amount + " " + args[0] + " arrow.");
								else
									player.sendMessage("You bought " + amount + " " + args[0] + " arrows.");
							}
							else
								player.sendMessage("Failed to buy arrows. " + response.errorMessage);
						}
						else
							player.sendMessage("Failed to buy arrows. You need atleast $" + totalCost);
					}
					else
						player.sendMessage("You don't have permission to buy that arrow.");
				}
				else
					player.sendMessage("That arrow type does not exist.");
			}
			return true;
		}
		return false;
	}

	@EventHandler
	public void onEntityShootBow(EntityShootBowEvent event)
	{
		// getLogger().log(Level.INFO, "11");
		if (event.getEntity() instanceof Player)
		{
			// getLogger().log(Level.INFO, "22");
			// Now check if the arrow that will be used is a poison arrow
			Player player = (Player) event.getEntity();
			Inventory inventory = player.getInventory();
			for (int i = 0; i <= 35; i++) // 35 magic value for player inventory
											// size, must fix
			{
				ItemStack current = inventory.getItem(i);
				if (current != null && current.getType() == Material.ARROW)
				{
					// getLogger().log(Level.INFO, "33");
					if (current.getItemMeta().getLore() != null && !current.getItemMeta().getLore().isEmpty())
					{
						String arrowType = current.getItemMeta().getLore().get(0);
						// getLogger().log(Level.INFO, "44 " + arrowType);
						if (arrowTypes.containsKey(arrowType))
						{
							// getLogger().log(Level.INFO, "55");
							// Player is going to shoot a custom arrow
							if (event.getProjectile() instanceof Arrow)
							{
								// getLogger().log(Level.INFO, "66");
								Arrow projectile = (Arrow) event.getProjectile();
								projectile.setMetadata("cusarr", new FixedMetadataValue(this, arrowTypes.get(arrowType)));
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
		// getLogger().log(Level.INFO, "1");
		if (event.getDamager() instanceof Arrow)
		{
			// getLogger().log(Level.INFO, "2");
			Arrow damager = (Arrow) event.getDamager();
			List<MetadataValue> temp = damager.getMetadata("cusarr");
			if (!temp.isEmpty())
			{
				// getLogger().log(Level.INFO, "3");
				MetadataValue isArrow = temp.get(0);
				if (isArrow.value() instanceof CustomArrow)
				{
					// getLogger().log(Level.INFO, "4");
					CustomArrow arrow = (CustomArrow) isArrow.value();
					PotionImpactEffect potionImpactEffect = ((PotionImpactEffect) arrow.impactEffect);
					if (potionImpactEffect == null)
						return;
					PotionEffect potionEffect = new PotionEffect(potionImpactEffect.type, arrow.impactEffect.duration, potionImpactEffect.amplifier);

					if (event.getEntity() instanceof Creature)
					{
						// getLogger().log(Level.INFO, "5");
						Creature damaged = (Creature) event.getEntity();
						damaged.addPotionEffect(potionEffect);
					}
					else if (event.getEntity() instanceof Player)
					{
						// getLogger().log(Level.INFO, "5");
						Player damaged = (Player) event.getEntity();
						damaged.addPotionEffect(potionEffect);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent event)
	{
		if (event.getItem().getItemStack().getType() == Material.ARROW)
		{
			//Player is going to pick up an arrow
			List<MetadataValue> temp = event.getItem().getMetadata("cusarr");
			if (temp != null && !temp.isEmpty())
			{
				//It is a custom arrow
				for (MetadataValue v : temp)
				{
					if(v.value() instanceof CustomArrow)
					{
						CustomArrow customArrow = ((CustomArrow) v.value());
						ItemStack result = event.getItem().getItemStack();
						ItemMeta itemMeta = result.getItemMeta();
						List<String> lore = new ArrayList<String>();
						lore.add(customArrow.getType());
						itemMeta.setLore(lore);
						result.setItemMeta(itemMeta);
						event.getPlayer().getInventory().addItem(result);
						event.getItem().remove();
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onBlockDispense(BlockDispenseEvent event)
	{
		//event.getBlock().
		//Dispenser dispenser = (Dispenser)event.getBlock();
		//dispenser.
		
	}
	
	@EventHandler
	public void onProjectileLaunch(ProjectileLaunchEvent event)
	{
		//event.
	}
}
