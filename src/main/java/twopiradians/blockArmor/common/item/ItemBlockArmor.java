package twopiradians.blockArmor.common.item;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import net.minecraft.block.BlockLiquid;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import twopiradians.blockArmor.client.model.ModelBlockArmor;
import twopiradians.blockArmor.common.BlockArmor;
import twopiradians.blockArmor.common.block.ModBlocks;
import twopiradians.blockArmor.common.command.CommandDev;

public class ItemBlockArmor extends ItemArmor
{
	public static final UUID MOVEMENT_SPEED_UUID = UUID.fromString("308e48ee-a300-4846-9b56-05e53e35eb8f");
	public static final UUID ATTACK_SPEED_UUID = UUID.fromString("3094e67f-88f1-4d81-a59d-655d4e7e8065");
	public static final UUID ATTACK_STRENGTH_UUID = UUID.fromString("d7dfa4ea-1cdf-4dd9-8842-883d7448cb00");
	public static final UUID KNOCKBACK_RESISTANCE_UUID = UUID.fromString("c8bb1118-78be-4864-9de3-a718047d28bd");
	public static final UUID MAX_HEALTH_UUID = UUID.fromString("0fefa40c-fd5a-4019-a25e-7fffc8dcf621");
	public static final UUID LUCK_UUID = UUID.fromString("537fd0e2-78ef-4dd3-affb-959ff059b1bd");

	private static final String TEXT_FORMATTING_SET_EFFECT_HEADER = TextFormatting.ITALIC+""+TextFormatting.GOLD;
	private static final String TEXT_FORMATTING_SET_EFFECT_DESCRIPTION = TextFormatting.WHITE+"";
	private static final String TEXT_FORMATTING_SET_EFFECT_EXTRA = TextFormatting.GREEN+"";

	public ItemBlockArmor(ArmorMaterial material, int renderIndex, EntityEquipmentSlot equipmentSlot) 
	{
		super(material, renderIndex, equipmentSlot);
		this.setCreativeTab(null);
	}

	/**Change armor texture based on block*/
	@Override
	@SideOnly(Side.CLIENT)
	public String getArmorTexture(ItemStack stack, Entity entity, EntityEquipmentSlot slot, String type)
	{
		TextureAtlasSprite sprite = ArmorSet.getSprite(this);
		String texture = sprite.getIconName()+".png";
		int index = texture.indexOf(":");
		texture = texture.substring(0, index+1)+"textures/"+texture.substring(index+1);
		return texture;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ModelBiped getArmorModel(EntityLivingBase entity, ItemStack stack, EntityEquipmentSlot slot, ModelBiped oldModel)
	{
		TextureAtlasSprite sprite = ArmorSet.getSprite(this);
		int width = sprite.getIconWidth();
		int height = sprite.getIconHeight() * sprite.getFrameCount();
		boolean isTranslucent = ArmorSet.getSet(this).isTranslucent;
		int currentFrame = ArmorSet.getCurrentAnimationFrame(this);
		int nextFrame = ArmorSet.getNextAnimationFrame(this);
		int color = ArmorSet.getColor(this);
		float alpha = ArmorSet.getAlpha(this);
		//ModelBlockArmor model = new ModelBlockArmor(height, width, currentFrame, nextFrame, slot);
		ModelBlockArmor model =  (ModelBlockArmor) BlockArmor.proxy.getBlockArmorModel(height, width, currentFrame, nextFrame, slot);
		model.translucent = isTranslucent;
		model.color = color;
		model.alpha = alpha;
		return model;
	}

	/**Don't display item in creative tab/JEI if disabled*/
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item itemIn, CreativeTabs tab, List<ItemStack> subItems)
	{
		if (ArmorSet.disabledItems != null) {
			for (ItemStack stack : ArmorSet.disabledItems)
				if (stack.getItem() == itemIn)
					return;
		}

		subItems.add(new ItemStack(itemIn));
	}

	/**Change display name based on the block*/
	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		return ArmorSet.getItemStackDisplayName(stack, this.armorType);
	}

	/**Handles the attributes when wearing an armor set.*/
	@Override
	public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack)
	{
		Multimap<String, AttributeModifier> map = this.getItemAttributeModifiers(slot);
		if (slot != this.armorType)
			return map;

		ArmorSet set = ArmorSet.getSet(this);
		if (!stack.hasTagCompound())
			stack.setTagCompound(new NBTTagCompound());
		if (stack.getTagCompound().getBoolean("isWearing"))
		{
			if (set.block == Blocks.REEDS)
				map.put(SharedMonsterAttributes.MOVEMENT_SPEED.getAttributeUnlocalizedName(), 
						new AttributeModifier(MOVEMENT_SPEED_UUID, "Speed Boost", 0.1d, 0));
			else if (set.block == Blocks.BEDROCK)
			{
				map.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getAttributeUnlocalizedName(), 
						new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "Knockback Resistance", 2d, 0));
				map.put(SharedMonsterAttributes.MAX_HEALTH.getAttributeUnlocalizedName(), 
						new AttributeModifier(MAX_HEALTH_UUID, "Health Boost", 20d, 0));
			}
			else if (set.block == Blocks.OBSIDIAN)
			{
				map.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getAttributeUnlocalizedName(), 
						new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "Knockback Resistance", 2d, 0));
				map.put(SharedMonsterAttributes.MAX_HEALTH.getAttributeUnlocalizedName(), 
						new AttributeModifier(MAX_HEALTH_UUID, "Health Boost", 10d, 0));
			}
			else if (set.block == Blocks.BRICK_BLOCK)
			{
				map.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getAttributeUnlocalizedName(), 
						new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "Knockback Resistance", 0.5d, 0));
				map.put(SharedMonsterAttributes.MAX_HEALTH.getAttributeUnlocalizedName(), 
						new AttributeModifier(MAX_HEALTH_UUID, "Health Boost", 4d, 0));
			}
			else if (set.block == Blocks.QUARTZ_BLOCK)
			{
				map.put(SharedMonsterAttributes.ATTACK_SPEED.getAttributeUnlocalizedName(), 
						new AttributeModifier(ATTACK_SPEED_UUID, "Attack Speed Boost", 1d, 0));
				map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getAttributeUnlocalizedName(), 
						new AttributeModifier(ATTACK_STRENGTH_UUID, "Attack Strength Boost", 3d, 0));
			}
			else if (set.block == Blocks.EMERALD_BLOCK)
				map.put(SharedMonsterAttributes.LUCK.getAttributeUnlocalizedName(), 
						new AttributeModifier(LUCK_UUID, "Knockback Resistance", 1d, 0));
		}
		else 
		{
			if (set.block == Blocks.REEDS)
				map.put(SharedMonsterAttributes.MOVEMENT_SPEED.getAttributeUnlocalizedName(), 
						new AttributeModifier(MOVEMENT_SPEED_UUID, "Speed Boost", 0d, 0));
			else if (set.block == Blocks.BEDROCK)
			{
				map.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getAttributeUnlocalizedName(), 
						new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "Knockback Resistance", 0d, 0));
				map.put(SharedMonsterAttributes.MAX_HEALTH.getAttributeUnlocalizedName(), 
						new AttributeModifier(MAX_HEALTH_UUID, "Health Boost", 0d, 0));
			}
			else if (set.block == Blocks.OBSIDIAN)
			{
				map.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getAttributeUnlocalizedName(), 
						new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "Knockback Resistance", 0d, 0));
				map.put(SharedMonsterAttributes.MAX_HEALTH.getAttributeUnlocalizedName(), 
						new AttributeModifier(MAX_HEALTH_UUID, "Health Boost", 0d, 0));
			}
			else if (set.block == Blocks.BRICK_BLOCK)
			{
				map.put(SharedMonsterAttributes.KNOCKBACK_RESISTANCE.getAttributeUnlocalizedName(), 
						new AttributeModifier(KNOCKBACK_RESISTANCE_UUID, "Knockback Resistance", 0d, 0));
				map.put(SharedMonsterAttributes.MAX_HEALTH.getAttributeUnlocalizedName(), 
						new AttributeModifier(MAX_HEALTH_UUID, "Health Boost", 0d, 0));
			}
			else if (set.block == Blocks.QUARTZ_BLOCK)
			{
				map.put(SharedMonsterAttributes.ATTACK_SPEED.getAttributeUnlocalizedName(), 
						new AttributeModifier(ATTACK_SPEED_UUID, "Knockback Resistance", 0d, 0));
				map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getAttributeUnlocalizedName(), 
						new AttributeModifier(ATTACK_STRENGTH_UUID, "Health Boost", 0d, 0));
			}
			else if (set.block == Blocks.EMERALD_BLOCK)
				map.put(SharedMonsterAttributes.LUCK.getAttributeUnlocalizedName(), 
						new AttributeModifier(LUCK_UUID, "Knockback Resistance", 0d, 0));
		}
		return map;
	}

	/**Set to have tooltip color show if item has effect*/
	@Override
	public EnumRarity getRarity(ItemStack stack)
	{
		if (stack.isItemEnchanted())
			return EnumRarity.RARE;
		else if (ArmorSet.getSet(this).hasSetEffect)
			return EnumRarity.UNCOMMON;
		else
			return EnumRarity.COMMON;
	}

	/**Deals with armor tooltips*/
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced)
	{
		ArmorSet set = ArmorSet.getSet(this);

		if (stack.hasTagCompound() && stack.getTagCompound().hasKey("devSpawned"))
			tooltip.add(TextFormatting.DARK_PURPLE+""+TextFormatting.BOLD+"Dev Spawned");

		if (GuiScreen.isShiftKeyDown())
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_EXTRA+"Generated from: "+set.stack.getDisplayName());

		if (set.hasSetEffect)
			tooltip = this.addFullSetEffectTooltip(tooltip);
	}

	/**Deals with armor tooltips.*/
	@SideOnly(Side.CLIENT)
	public List<String> addFullSetEffectTooltip(List<String> tooltip) 
	{
		ArmorSet set = ArmorSet.getSet(this);
		if (!set.hasSetEffect)
			return tooltip;

		tooltip.add(TEXT_FORMATTING_SET_EFFECT_HEADER+"Full Set Effect:");
		if (!ArmorSet.isSetEffectEnabled(set))
		{
			tooltip.add(TextFormatting.RED+"Disabled.");
			if (GuiScreen.isShiftKeyDown())
			{
				tooltip.add(TextFormatting.RED+"SP: Disabled in your config.");
				tooltip.add(TextFormatting.RED+"MP: Disabled in server config.");
			}
		}
		else if (set.block == Blocks.END_STONE)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Teleports in the direction");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"you're looking upon sneaking!");
			if (GuiScreen.isShiftKeyDown())
				tooltip.add(TEXT_FORMATTING_SET_EFFECT_EXTRA+"Now you see me. Now you *poof*");
		}
		else if (set.block == Blocks.SLIME_BLOCK)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Bounces off walls!");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Bounces off the ground!");
			if (GuiScreen.isShiftKeyDown())
			{
				tooltip.add(TEXT_FORMATTING_SET_EFFECT_EXTRA+"Bouncy, trouncy, flouncy, pouncy,");
				tooltip.add(TEXT_FORMATTING_SET_EFFECT_EXTRA+"fun, fun, fun, fun, fun!");
			}
		}
		else if (set.block == Blocks.REEDS)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Increases movement speed!");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Can breathe near");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"the water surface.");
			if (GuiScreen.isShiftKeyDown())
				tooltip.add(TEXT_FORMATTING_SET_EFFECT_EXTRA+"Just like snorkeling!");
		}
		else if (set.block == Blocks.PRISMARINE)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Sinks faster in liquids!");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Respiration, night vision,");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"and depth strider in water!");
		}
		else if (set.block == Blocks.EMERALD_BLOCK)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Increases fortune level!");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Increases luck.");
			if (GuiScreen.isShiftKeyDown())
				tooltip.add(TEXT_FORMATTING_SET_EFFECT_EXTRA+"More Goodies!");
		}
		else if (set.block == Blocks.BEDROCK)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Knockback resistance.");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Greatly increases");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"max health.");
		}
		else if (set.block == Blocks.QUARTZ_BLOCK)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Increases attack strength!");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Increases attack speed!");
		}
		else if (set.block == Blocks.BRICK_BLOCK)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Minor knockback resistance.");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Increases max health.");
		}
		else if (set.block == Blocks.NETHERRACK) 
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Gives Fire Protection.");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Creates light");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"while sneaking.");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Lights target on fire");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"after attacking.");
		}
		else if (set.block == Blocks.OBSIDIAN)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Gives Fire Protection.");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Knockback resistance.");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Increases max health.");
		}
		else if (set.block == Blocks.REDSTONE_BLOCK)
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Creates light while sneaking.");
		else if (set.block == Blocks.SNOW)
		{
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Spawns snow and snowballs");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"while sneaking!");
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Gives Frost Walking 2.");
			if (GuiScreen.isShiftKeyDown())
				tooltip.add(TEXT_FORMATTING_SET_EFFECT_EXTRA+"Do you wanna build a snowman?");
		}
		else if (set.block == Blocks.LAPIS_BLOCK)
			tooltip.add(TEXT_FORMATTING_SET_EFFECT_DESCRIPTION+"Gives xp for wearing!");
		//TODO add tooltips for new sets

		return tooltip;
	}

	/**Handles enchanting armor when worn*/
	@Override
	public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) 
	{	
		if (!(entityIn instanceof EntityLivingBase))
			return;

		EntityLivingBase entity = (EntityLivingBase) entityIn;

		ArmorSet set = ArmorSet.getSet(this);

		if (ArmorSet.isSetEffectEnabled(set))
			this.doEnchantments(stack, entity);

		if (!stack.hasTagCompound())
			stack.setTagCompound(new NBTTagCompound());

		//delete dev spawned items if not in dev's inventory
		if (!entityIn.worldObj.isRemote && entityIn instanceof EntityPlayer &&
				stack.getTagCompound().hasKey("devSpawned") && !CommandDev.DEVS.contains(entityIn.getPersistentID()) &&
				((EntityPlayer)entityIn).inventory.getStackInSlot(itemSlot) == stack) {
			((EntityPlayer)entityIn).inventory.setInventorySlotContents(itemSlot, new ItemStack(Blocks.AIR));
			return;
		}

		if (!ArmorSet.isWearingFullSet(entity, set) || !ArmorSet.isSetEffectEnabled(set))
		{
			stack.getTagCompound().setBoolean("isWearing", false);
			return;
		}

		stack.getTagCompound().setBoolean("isWearing", true);

		int cooldown = stack.getTagCompound().hasKey("cooldown") ? stack.getTagCompound().getInteger("cooldown") : 0;
		--cooldown;
		stack.getTagCompound().setInteger("cooldown", cooldown);

		if (worldIn instanceof WorldServer)
			((WorldServer)worldIn).getEntityTracker().updateTrackedEntities();
	}

	/**Delete dev spawned dropped items*/
	@Override
	public boolean onEntityItemUpdate(EntityItem entityItem)
	{
		if (!entityItem.worldObj.isRemote && entityItem != null && entityItem.getEntityItem() != null && 
				entityItem.getEntityItem().hasTagCompound() && entityItem.getEntityItem().getTagCompound().hasKey("devSpawned")) {
			entityItem.setDead();
			return true;
		}

		return false;
	}

	/**Handles most of the armor set special effects and bonuses.*/
	@SuppressWarnings("deprecation")
	@Override
	public void onArmorTick(World world, EntityPlayer player, ItemStack itemStack)
	{		
		//delete dev spawned items if not worn by dev
		if (!world.isRemote && itemStack != null && itemStack.hasTagCompound() && itemStack.getTagCompound().hasKey("devSpawned") && 
				!CommandDev.DEVS.contains(player.getPersistentID()) && 
				player.getItemStackFromSlot(this.getEquipmentSlot()) == itemStack) {
			player.setItemStackToSlot(this.getEquipmentSlot(), new ItemStack(Blocks.AIR));
			return;
		}

		ArmorSet set = ArmorSet.getSet(this);		
		if (!ArmorSet.isSetEffectEnabled(set) || !ArmorSet.isWearingFullSet(player, set))
			return;

		//dark pris
		//sink faster in water; respiration, night vision, depth strider in water
		if (set.block == Blocks.PRISMARINE)	{
			if (player.isInWater() 
					&& world.getBlockState(new BlockPos(player.posX, player.posY+1.7, player.posZ)).getBlock() instanceof BlockLiquid)
			{ 
				if (!player.isPotionActive(Potion.getPotionById(16)) 
						|| (player.isPotionActive(Potion.getPotionById(16))
								&& player.getActivePotionEffect(Potion.getPotionById(16)).getDuration() < 205))
					player.addPotionEffect(new PotionEffect(MobEffects.NIGHT_VISION, 210, 0, true, true));
				try {
					if (world.isRemote && !((EntityPlayerSP)player).movementInput.jump  && !player.onGround 
							&& world.getBlockState(new BlockPos(player.posX, player.posY+2, player.posZ)).getBlock() 
							instanceof BlockLiquid && player.motionY < 0.0D)
					{	
						player.motionY = Math.max(-0.3D, player.motionY * 1.2D);
					}
				} catch (Exception e) { }
			}
		}

		//only allow boots past this point
		if (this.armorType != EntityEquipmentSlot.FEET)
			return;

		//Wet Sponge
		if (set.block == Blocks.SPONGE && set.meta == 1) {
			if (itemStack.getTagCompound().getInteger("cooldown") <= 0)
			{
				//TODO redo by only changing item AND do it by iterating through "slots"
				int headDamage = player.getItemStackFromSlot((EntityEquipmentSlot.HEAD)).getItemDamage();
				int chestDamage = player.getItemStackFromSlot((EntityEquipmentSlot.CHEST)).getItemDamage();
				int legDamage = player.getItemStackFromSlot((EntityEquipmentSlot.LEGS)).getItemDamage();
				int feetDamage = player.getItemStackFromSlot((EntityEquipmentSlot.FEET)).getItemDamage();
				ArmorSet set2 = ArmorSet.getSet(Blocks.SPONGE, 0);

				//HINT (needs tweaking still):
				EntityEquipmentSlot[] slots = new EntityEquipmentSlot[] {EntityEquipmentSlot.HEAD,
						EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
				ItemStack oldStack = player.getItemStackFromSlot(slots[0]);
				NBTTagCompound nbt = new NBTTagCompound();
				oldStack.writeToNBT(nbt);
				ResourceLocation resourcelocation = (ResourceLocation)Item.REGISTRY.getNameForObject(set2.getArmorForSlot(slots[0]));
				nbt.setString("id", resourcelocation == null ? "minecraft:air" : resourcelocation.toString());
				ItemStack newStack = new ItemStack(nbt);

				player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(set2.getArmorForSlot(EntityEquipmentSlot.HEAD)));
				player.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(set2.getArmorForSlot(EntityEquipmentSlot.CHEST)));
				player.setItemStackToSlot(EntityEquipmentSlot.LEGS, new ItemStack(set2.getArmorForSlot(EntityEquipmentSlot.LEGS)));
				player.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(set2.getArmorForSlot(EntityEquipmentSlot.FEET)));
				player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).damageItem(headDamage, player);
				player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).damageItem(chestDamage, player);
				player.getItemStackFromSlot(EntityEquipmentSlot.LEGS).damageItem(legDamage, player);
				player.getItemStackFromSlot(EntityEquipmentSlot.FEET).damageItem(feetDamage, player);
			}
		}

		//Sponge
		else if (set.block == Blocks.SPONGE && set.meta == 0) {//TODO check that player can edit each block removed
			if (!world.isRemote && player.isSneaking()/* && player.isAllowEdit()*/
					&& player.worldObj.getBlockState(new BlockPos(player.posX, player.posY, player.posZ)).getBlock() instanceof BlockLiquid) {
				Queue<Tuple<BlockPos, Integer>> queue = Lists.<Tuple<BlockPos, Integer>>newLinkedList();
				List<BlockPos> list = Lists.<BlockPos>newArrayList();
				queue.add(new Tuple(player.getPosition(), Integer.valueOf(0)));
				int i = 0;

				while (!((Queue)queue).isEmpty()) {
					Tuple<BlockPos, Integer> tuple = (Tuple)queue.poll();
					BlockPos blockpos = (BlockPos)tuple.getFirst();
					int j = ((Integer)tuple.getSecond()).intValue();

					for (EnumFacing enumfacing : EnumFacing.values()) {
						BlockPos blockpos1 = blockpos.offset(enumfacing);

						if (world.getBlockState(blockpos1).getBlock() instanceof BlockLiquid) {
							world.setBlockState(blockpos1, Blocks.AIR.getDefaultState(), 2);
							list.add(blockpos1);
							++i;

							if (j < 6) //~diameter
								queue.add(new Tuple(blockpos1, Integer.valueOf(j + 1)));
						}
					}
					if (i > 64) { break;} //max number of blocks
				}

				for (BlockPos blockpos2 : list) 
					world.notifyNeighborsOfStateChange(blockpos2, Blocks.AIR, false);

				world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BUCKET_FILL, 
						SoundCategory.PLAYERS, 1.0F, world.rand.nextFloat() + 0.5f);

				//TODO redo by only changing item AND do it by iterating through slots
				int headDamage = player.getItemStackFromSlot((EntityEquipmentSlot.HEAD)).getItemDamage();
				int chestDamage = player.getItemStackFromSlot((EntityEquipmentSlot.CHEST)).getItemDamage();
				int legDamage = player.getItemStackFromSlot((EntityEquipmentSlot.LEGS)).getItemDamage();
				int feetDamage = player.getItemStackFromSlot((EntityEquipmentSlot.FEET)).getItemDamage();
				ArmorSet set2 = ArmorSet.getSet(Blocks.SPONGE, 1);

				//HINT (needs tweaking still):
				EntityEquipmentSlot[] slots = new EntityEquipmentSlot[] {EntityEquipmentSlot.HEAD,
						EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET};
				ItemStack oldStack = player.getItemStackFromSlot(slots[0]);
				NBTTagCompound nbt = new NBTTagCompound();
				oldStack.writeToNBT(nbt);
				ResourceLocation resourcelocation = (ResourceLocation)Item.REGISTRY.getNameForObject(set2.getArmorForSlot(slots[0]));
				nbt.setString("id", resourcelocation == null ? "minecraft:air" : resourcelocation.toString());
				ItemStack newStack = new ItemStack(nbt);

				player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(set2.getArmorForSlot(EntityEquipmentSlot.HEAD)));
				player.setItemStackToSlot(EntityEquipmentSlot.CHEST, new ItemStack(set2.getArmorForSlot(EntityEquipmentSlot.CHEST)));
				player.setItemStackToSlot(EntityEquipmentSlot.LEGS, new ItemStack(set2.getArmorForSlot(EntityEquipmentSlot.LEGS)));
				player.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(set2.getArmorForSlot(EntityEquipmentSlot.FEET)));
				player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).damageItem(headDamage, player);
				player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).damageItem(chestDamage, player);
				player.getItemStackFromSlot(EntityEquipmentSlot.LEGS).damageItem(legDamage, player);
				player.getItemStackFromSlot(EntityEquipmentSlot.FEET).damageItem(feetDamage, player);
				itemStack = player.getItemStackFromSlot(EntityEquipmentSlot.FEET);
				if (!itemStack.hasTagCompound())
					itemStack.setTagCompound(new NBTTagCompound());
				itemStack.getTagCompound().setInteger("cooldown", 60);
			}
		}

		//Emerald
		else if (set.block == Blocks.EMERALD_BLOCK)	{
			if (!world.isRemote)
				player.addPotionEffect(new PotionEffect(Potion.getPotionById(3), 5, 1, true, true)); //haste
		}

		//TNT
		else if (set.block == Blocks.TNT) { //TODO check each block before destroying
			if (player.isSneaking()/* && player.isAllowEdit() */&& itemStack.getTagCompound().getInteger("cooldown") <= 0) {

				itemStack.getTagCompound().setInteger("cooldown", 10);

				if (!world.isRemote)
				{
					Explosion explosion = new Explosion(world, player, player.posX, player.posY + 0.49D, player.posZ, 6.0f, false, true);
					if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(world, explosion)) 
						return;
					explosion.doExplosionA();
					explosion.doExplosionB(true); 
					player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).damageItem(player.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getMaxDamage()/9, player);
					player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).damageItem(player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).getMaxDamage()/9, player);
					player.getItemStackFromSlot(EntityEquipmentSlot.LEGS).damageItem(player.getItemStackFromSlot(EntityEquipmentSlot.LEGS).getMaxDamage()/9, player);
					player.getItemStackFromSlot(EntityEquipmentSlot.FEET).damageItem(player.getItemStackFromSlot(EntityEquipmentSlot.FEET).getMaxDamage()/9, player);
				}
			}
		}

		//Repeating Command Block
		else if (set.block == Blocks.REPEATING_COMMAND_BLOCK) {
			if (player.isSneaking())
				world.setWorldTime(world.getWorldTime() - 11);
		}

		//Chain Command Block
		else if (set.block == Blocks.CHAIN_COMMAND_BLOCK) {
			if (player.isSneaking())
				world.setWorldTime(world.getWorldTime() - 1);
		}

		//Command Block
		else if (set.block == Blocks.COMMAND_BLOCK)	{
			if (player.isSneaking())
				world.setWorldTime(world.getWorldTime() + 9);
		}

		//Piston
		else if (set.block == Blocks.PISTON) {
			if (player.isSneaking() && itemStack.getTagCompound().getInteger("cooldown") <= 0) {
				itemStack.getTagCompound().setInteger("cooldown", 40);

				AxisAlignedBB aabb = player.getEntityBoundingBox().expand(5, 5, 5);
				List<Entity> list = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, aabb);//player.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, axisAlignedBB);
				/*list.remove(player);
				for (int i=0; i<list.size(); i++)
					if (list.get(i) instanceof EntityArmorStand)
						list.remove(i--);*/

				if (!list.isEmpty()) {
					Iterator<Entity> iterator = list.iterator();            
					while (iterator.hasNext())
					{
						Entity entityCollided = iterator.next();
						if (!entityCollided.isImmuneToExplosions()) {
							double xVel = entityCollided.posX - player.posX;
							double yVel = entityCollided.posY - player.posY;
							double zVel = entityCollided.posZ - player.posZ;
							double velScale = 5 / Math.sqrt(xVel * xVel + yVel * yVel + zVel * zVel);
							entityCollided.addVelocity(velScale*xVel, velScale*yVel, velScale*zVel); 
						}
					}
					world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.PLAYERS, 1.0F, world.rand.nextFloat() + 0.5f);
				}
			}
		}

		//Sticky Piston
		else if (set.block == Blocks.STICKY_PISTON)	{
			if (player.isSneaking() && itemStack.getTagCompound().getInteger("cooldown") <= 0) {
				itemStack.getTagCompound().setInteger("cooldown", 40);

				AxisAlignedBB aabb = player.getEntityBoundingBox().expand(5, 5, 5);
				List<Entity> list = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, aabb);//player.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, axisAlignedBB);
				/*list.remove(player);
				for (int i=0; i<list.size(); i++)
					if (list.get(i) instanceof EntityArmorStand)
						list.remove(i--);*/

				if (!list.isEmpty()) {
					Iterator<Entity> iterator = list.iterator();            
					while (iterator.hasNext())
					{
						Entity entityCollided = iterator.next();
						if (!entityCollided.isImmuneToExplosions()) {
							double xVel = entityCollided.posX - player.posX;
							double yVel = entityCollided.posY - player.posY;
							double zVel = entityCollided.posZ - player.posZ;
							double velScale = 2 / Math.sqrt(xVel * xVel + yVel * yVel + zVel * zVel);
							entityCollided.addVelocity(-velScale*xVel, -velScale*yVel, -velScale*zVel);  
						}
					}
					world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.PLAYERS, 1.0F, world.rand.nextFloat() + 0.5f);
				}
			}
		}

		//Beacon
		else if (set.block == Blocks.BEACON && !world.isRemote)
		{
			//FIXME use MobEffects fields instead of potion ids
			player.addPotionEffect(new PotionEffect(Potion.getPotionById(1), 5, 1, true, true)); //speed
			player.addPotionEffect(new PotionEffect(Potion.getPotionById(3), 5, 1, true, true)); //haste
			player.addPotionEffect(new PotionEffect(Potion.getPotionById(5), 5, 1, true, true)); //strength
			player.addPotionEffect(new PotionEffect(Potion.getPotionById(8), 5, 1, true, true)); //jump boost
			player.addPotionEffect(new PotionEffect(Potion.getPotionById(11), 5, 1, true, true)); //resistance
			player.addPotionEffect(new PotionEffect(Potion.getPotionById(10), 5, 0, true, true)); //regen
		}

		//Dispenser
		else if (set.block == Blocks.DISPENSER)	{
			if (player.isSneaking() && itemStack.getTagCompound().getInteger("cooldown") <= 0 && !world.isRemote)
			{
				int numArrows = 16;
				for(int i = 0; i < numArrows; i++)
				{
					itemStack.getTagCompound().setInteger("cooldown", 40);
					ItemArrow itemArrow = (ItemArrow) Items.ARROW;
					EntityArrow entityArrow = itemArrow.createArrow(world, new ItemStack(itemArrow), player);
					entityArrow.setAim(player, 0.0F, player.rotationYaw + i*(360/numArrows), 0.0F, 2.0F, 0.0F);
					world.spawnEntityInWorld(entityArrow);
					entityArrow.pickupStatus = EntityArrow.PickupStatus.DISALLOWED;
				}
			}
			world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, world.rand.nextFloat() + 0.5f);
		}

		//Cactus
		//damages entities collided with; thorns enchant
		else if (set.block == Blocks.CACTUS) {
			AxisAlignedBB axisAlignedBB = player.getEntityBoundingBox();
			List<EntityLivingBase> list = player.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, axisAlignedBB);
			list.remove(player);

			if (!list.isEmpty()) {
				Iterator<EntityLivingBase> iterator = list.iterator();            
				while (iterator.hasNext())
				{
					EntityLivingBase entityCollided = iterator.next();
					entityCollided.attackEntityFrom(DamageSource.cactus, 1.0F);   
					//TODO add sound, maybe this one (same as baby guardian's):
					world.playSound((EntityPlayer)null, player.posX + 0.5D, player.posY + 0.5D, player.posZ + 0.5D, SoundEvents.ENTITY_BLAZE_HURT, SoundCategory.PLAYERS, 1.0F, world.rand.nextFloat() * 0.4F + 8F);
				}
			}
		}

		//Netherrack
		//gives fire protection; while sneaking gives off particles and light; ignites target when attacked
		else if (set.block == Blocks.NETHERRACK) {
			int radius = 3;
			if (player.isSneaking() && !player.isInWater())
			{
				if (!world.isRemote && player.ticksExisted % 2 == 0)
					((WorldServer)world).spawnParticle(EnumParticleTypes.LAVA, player.posX+(world.rand.nextDouble()-0.5D)*radius, 
							player.posY+world.rand.nextDouble()+1.0D, player.posZ+(world.rand.nextDouble()-0.5D)*radius, 
							1, 0, 0, 0, 0, new int[0]);
				if (world.rand.nextInt(20) == 0)
					world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_FIRECHARGE_USE, 
							player.getSoundCategory(), 0.5f, world.rand.nextFloat());
				if (world.getBlockState(new BlockPos(player.posX, player.posY+1, player.posZ)) 
						!= ModBlocks.movinglightsource.getDefaultState()
						&& world.getBlockState(new BlockPos(player.posX, player.posY+1, player.posZ)) == Blocks.AIR.getDefaultState()
						&& !world.isRemote)
					world.setBlockState(new BlockPos(player.posX, player.posY+1, player.posZ), 
							ModBlocks.movinglightsource.getDefaultState());
			}
		}

		//Redstone
		//gives light while sneaking
		else if (set.block == Blocks.REDSTONE_BLOCK) {
			if (player.isSneaking()
					&& world.getBlockState(new BlockPos(player.posX, player.posY+1, player.posZ)) != 
					ModBlocks.movinglightsource.getDefaultState()
					&& world.getBlockState(new BlockPos(player.posX, player.posY+1, player.posZ)) == Blocks.AIR.getDefaultState())
				world.setBlockState(new BlockPos(player.posX, player.posY+1, player.posZ), ModBlocks.movinglightsource.getDefaultState());
		}

		//Snow
		//spawns snow, snowballs, and particles while sneaking; frost walking 2
		else if (player.isSneaking() && set.block == Blocks.SNOW) {
			int radius = 3;
			if (!world.isRemote && player.ticksExisted % 2 == 0)
				((WorldServer)world).spawnParticle(EnumParticleTypes.SNOW_SHOVEL, player.posX+(world.rand.nextDouble()-0.5D)*radius, 
						player.posY+world.rand.nextDouble()+1.0D, player.posZ+(world.rand.nextDouble()-0.5D)*radius, 
						1, 0, 0, 0, 0, new int[0]);
			if (world.rand.nextInt(5) == 0)
				world.playSound(player, player.posX, player.posY, player.posZ, SoundEvents.WEATHER_RAIN, 
						player.getSoundCategory(), 1.0f, world.rand.nextFloat());
			if (!world.isRemote) {
				for (int x=-radius/2; x<=radius/2; x++)
					for (int z=-radius/2; z<=radius/2; z++)
						for (int y=0; y<=2; y++)//FIXME check if player can edit each block
							if (/*player.capabilities.allowEdit && */world.rand.nextInt(100) == 0 
							&& world.isAirBlock(new BlockPos(player.posX+x, player.posY+y, player.posZ+z)))
							{
								if (world.getBlockState(new BlockPos(player.posX+x, player.posY+y-1, 
										player.posZ+z)).getBlock().isVisuallyOpaque(world.getBlockState(new BlockPos(player.posX+x, player.posY+y-1, 
												player.posZ+z))))
									world.setBlockState(new BlockPos(player.posX+x, player.posY+y, 
											player.posZ+z), Blocks.SNOW_LAYER.getDefaultState());
								else if (world.getBlockState(new BlockPos(player.posX+x, player.posY+y-1, 
										player.posZ+z)).getBlock() == Blocks.WATER)
									world.setBlockState(new BlockPos(player.posX+x, player.posY+y-1, 
											player.posZ+z), Blocks.FROSTED_ICE.getDefaultState());
								else if (world.getBlockState(new BlockPos(player.posX+x, player.posY+y-1, 
										player.posZ+z)).getBlock() == Blocks.FROSTED_ICE)
									world.setBlockState(new BlockPos(player.posX+x, player.posY+y-1, 
											player.posZ+z), Blocks.FROSTED_ICE.getDefaultState());
							}
				//spawn snowballs
				if (world.rand.nextInt(100) == 0)
				{
					EntityItem item = new EntityItem(world, player.posX+(world.rand.nextDouble()-0.5D)*radius, 
							player.posY+world.rand.nextDouble()+1.5D, player.posZ+(world.rand.nextDouble()-0.5D)*radius,
							new ItemStack(Items.SNOWBALL));
					item.setPickupDelay(40);
					world.spawnEntityInWorld(item);
					world.playSound(player, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_SNOW_STEP, 
							player.getSoundCategory(), 1.0f, world.rand.nextFloat());
				}
			}
		}

		//Lapis
		//gives xp for wearing. From lvl 0 to lvl 30 in one real hour (about)
		else if (!world.isRemote && set.block == Blocks.LAPIS_BLOCK) {
			if (itemStack.getTagCompound().getInteger("cooldown") <= 0)
			{
				itemStack.getTagCompound().setInteger("cooldown", 50);
				player.addExperience(1);
			}
		}

		//Endstone   
		//teleports in the direction looking when sneaking
		else if (!world.isRemote && set.block == Blocks.END_STONE) {
			if (itemStack.getTagCompound().getInteger("cooldown") <= 0 && player.isSneaking() && !player.capabilities.isFlying)
			{    
				itemStack.getTagCompound().setInteger("cooldown", 50);
				int distance = player.getRNG().nextInt(10) + 16;
				double rotX = - Math.sin(player.rotationYaw*Math.PI/180);
				double rotY = - Math.sin(player.rotationPitch*Math.PI/180);
				double rotZ = Math.cos(player.rotationYaw*Math.PI/180);
				double viewVectorLength = Math.sqrt(Math.pow(rotX, 2)+Math.pow(rotY, 2)+Math.pow(rotZ, 2));
				double x = player.posX + distance/viewVectorLength * rotX;
				double y = player.posY + distance/viewVectorLength * rotY;
				double z = player.posZ + distance/viewVectorLength * rotZ;

				BlockPos pos = new BlockPos(x, y, z);
				boolean posFound = false;
				for (int i = 0; i < 128; ++i)
				{
					double newX = 8*(world.rand.nextDouble()-0.5D);
					double newY = 8*(world.rand.nextDouble()-0.5D);
					double newZ = 8*(world.rand.nextDouble()-0.5D);
					if (!posFound && player.worldObj.isAirBlock(pos.add(newX, newY, newZ)) 
							&& player.worldObj.isAirBlock(pos.add(newX, newY+1, newZ)) 
							&& !player.worldObj.isAirBlock(pos.add(newX, newY-1, newZ)) 
							&& !(player.worldObj.getBlockState(pos.add(newX, newY-1, newZ)).getBlock() instanceof BlockLiquid)) 
					{
						pos = pos.add(newX, newY, newZ);
						posFound = true;
						break;
					}
				}
				if (posFound && player.attemptTeleport(pos.getX()+0.5d, pos.getY(), pos.getZ()+0.5d)) //if pos found and can tp
				{
					if (player.isRiding())
						player.dismountRidingEntity();
					world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
					world.playSound((EntityPlayer)null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
					player.playSound(SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT, 1.0F, 1.0F);
					for (int j = 0; j < 64; ++j) {
						((WorldServer)world).spawnParticle(EnumParticleTypes.PORTAL, player.posX+2*world.rand.nextDouble(), player.posY+world.rand.nextDouble()+1, player.posZ+2*world.rand.nextDouble(), 1, 0, 0, 0, 1, new int[0]);
						((WorldServer)world).spawnParticle(EnumParticleTypes.PORTAL, player.posX+2*world.rand.nextDouble(), player.posY+world.rand.nextDouble()+1.0D, player.posZ+2*world.rand.nextDouble(), 1, 0, 0, 0, 1, new int[0]);
					}
				}
				else { //no valid pos found
					world.playSound((EntityPlayer)null, player.posX, player.posY, player.posZ, SoundEvents.BLOCK_NOTE_BASS, 
							SoundCategory.PLAYERS, 1.0F, world.rand.nextFloat() + 0.5F);	
					itemStack.getTagCompound().setInteger("cooldown", 10);
				}
			}
		}

		//Slime
		//bounces on landing and off walls at high enough speed
		//--uses StopFallDamageEvent--
		else if (!player.isSneaking() && set.block == Blocks.SLIME_BLOCK) {	
			if (world.isRemote)
			{
				if (itemStack.getTagCompound().getInteger("cooldown") <= 0 && player.isCollidedHorizontally 
						&& Math.sqrt(Math.pow(player.posX - player.prevChasingPosX, 2) + 
								Math.pow(player.posZ - player.prevChasingPosZ, 2)) >= 0.9D)
				{	
					itemStack.getTagCompound().setInteger("cooldown", 10);
					if (player.motionX == 0)
					{
						player.motionX = -(player.posX - player.prevChasingPosX)*1.5D;
						player.motionZ = (player.posZ - player.prevChasingPosZ)*1.5D;
					}
					else if (player.motionZ == 0)
					{
						player.motionX = (player.posX - player.prevChasingPosX)*1.5D;
						player.motionZ = -(player.posZ - player.prevChasingPosZ)*1.5D;
					}
					player.motionY += 0.1;
					world.playSound(player, player.posX, player.posY, player.posZ, 
							SoundEvents.BLOCK_SLIME_FALL, SoundCategory.BLOCKS, 1.0F, 1.0F);
				}
			}
		}

		//sugarcane
		//can breath under water if less than two blocks from surface; speed boost
		else if (!world.isRemote && set.block == Blocks.REEDS) {
			if (player.isInWater() && player.ticksExisted % 10 == 0 
					&& world.getBlockState(new BlockPos(player.posX, player.posY+3, player.posZ)).getBlock() == Blocks.AIR)
			{
				player.setAir(300);
			}
		}
	}

	/**Does the (dis)enchanting of the armor*/
	private void doEnchantments(ItemStack stack, EntityLivingBase entity) 
	{
		ArmorSet set = ArmorSet.getSet(this);

		//Depth Strider
		if (set.block == Blocks.PRISMARINE && this.armorType == EntityEquipmentSlot.FEET)
		{
			NBTTagList list = stack.getEnchantmentTagList();	
			int targetId = 8;
			int id = 0;
			int lvl = 0;
			boolean wasEnchanted = false; 
			Enchantment e;
			if (list != null)
			{
				for (int i = 0; i < list.tagCount(); i++) 
				{
					NBTTagCompound compound = list.getCompoundTagAt(i);
					id = compound.getShort("id");
					lvl = compound.getShort("lvl");
					if (id == 8 && lvl >= 2)
						wasEnchanted = true;
				}
			}
			if (entity instanceof EntityLivingBase && ArmorSet.isWearingFullSet((EntityLivingBase) entity, set) 
					&& ((EntityLivingBase) entity).getItemStackFromSlot(EntityEquipmentSlot.FEET) == stack 
					&& entity.isInWater() && !wasEnchanted)
				stack.addEnchantment(Enchantments.DEPTH_STRIDER, 2);
			else if (stack.isItemEnchanted() && (!entity.isInWater() 
					||	!ArmorSet.isWearingFullSet((EntityLivingBase) entity, set)
					|| !(((EntityLivingBase) entity).getItemStackFromSlot(EntityEquipmentSlot.FEET) == stack)))
			{
				if (list != null)
				{
					for (int i = 0; i < list.tagCount(); i++) 
					{
						NBTTagCompound compound = list.getCompoundTagAt(i);
						id = compound.getShort("id");
						lvl = compound.getShort("lvl");
						e = Enchantment.getEnchantmentByID(id);
						if (e == null || id != targetId)
							continue;
						NBTTagCompound stackCompound = stack.getTagCompound();
						if (stackCompound == null)
							return;
						if (lvl <= 2)
							list.removeTag(i);
						if (list.tagCount() <= 0)
							stackCompound.removeTag("ench");
						return;
					}
				}
			}
		}
		//Respiration
		if (set.block == Blocks.PRISMARINE && this.armorType == EntityEquipmentSlot.HEAD)
		{
			NBTTagList list = stack.getEnchantmentTagList();	
			int targetId = 5;
			int id = 0;
			int lvl = 0;
			boolean wasEnchanted = false; 
			Enchantment e;
			if (list != null)
			{
				for (int i = 0; i < list.tagCount(); i++) 
				{
					NBTTagCompound compound = list.getCompoundTagAt(i);
					id = compound.getShort("id");
					lvl = compound.getShort("lvl");
					if (id == 5 && lvl == 3)
						wasEnchanted = true;
				}
			}
			if (entity instanceof EntityLivingBase && ArmorSet.isWearingFullSet((EntityLivingBase) entity, set) 
					&& ((EntityLivingBase) entity).getItemStackFromSlot(EntityEquipmentSlot.HEAD) == stack 
					&& entity.worldObj.getBlockState(new BlockPos(entity.posX, entity.posY+1.7D, entity.posZ)).getBlock() 
					instanceof BlockLiquid && !wasEnchanted)
				stack.addEnchantment(Enchantments.RESPIRATION, 3);
			else if (stack.isItemEnchanted() && 
					(!(entity.worldObj.getBlockState(new BlockPos(entity.posX, entity.posY+1.7D, entity.posZ)).getBlock() 
							instanceof BlockLiquid) || !ArmorSet.isWearingFullSet((EntityLivingBase) entity, set)))
			{
				if (list != null)
				{
					for (int i = 0; i < list.tagCount(); i++) 
					{
						NBTTagCompound compound = list.getCompoundTagAt(i);
						id = compound.getShort("id");
						lvl = compound.getShort("lvl");
						e = Enchantment.getEnchantmentByID(id);
						if (e == null || id != targetId)
							continue;
						NBTTagCompound stackCompound = stack.getTagCompound();
						if (stackCompound == null)
							return;
						list.removeTag(i);
						if (list.tagCount() <= 0)
							stackCompound.removeTag("ench");
						return;
					}
				}
			}
		}
		//Frost Walker
		if (set.block == Blocks.SNOW && this.armorType == EntityEquipmentSlot.FEET)
		{
			NBTTagList list = stack.getEnchantmentTagList();	
			int targetId = 9;
			int id = 0;
			boolean wasEnchanted = false; 
			Enchantment e;
			if (list != null)
			{
				for (int i = 0; i < list.tagCount(); i++) 
				{
					NBTTagCompound compound = list.getCompoundTagAt(i);
					id = compound.getShort("id");
					if (id == 9)
						wasEnchanted = true;
				}
			}
			if (entity instanceof EntityLivingBase && ArmorSet.isWearingFullSet((EntityLivingBase) entity, set) 
					&& ((EntityLivingBase) entity).getItemStackFromSlot(EntityEquipmentSlot.FEET) == stack 
					&& !wasEnchanted)
				stack.addEnchantment(Enchantments.FROST_WALKER, 2);
			else if (stack.isItemEnchanted() && !ArmorSet.isWearingFullSet((EntityLivingBase) entity, set))
			{
				if (list != null)
				{
					for (int i = 0; i < list.tagCount(); i++) 
					{
						NBTTagCompound compound = list.getCompoundTagAt(i);
						id = compound.getShort("id");
						e = Enchantment.getEnchantmentByID(id);
						if (e == null || id != targetId)
							continue;
						NBTTagCompound stackCompound = stack.getTagCompound();
						if (stackCompound == null)
							return;
						list.removeTag(i);
						if (list.tagCount() <= 0)
							stackCompound.removeTag("ench");
						return;
					}
				}
			}
		}
		//Fire Protection
		if (set.block == Blocks.NETHERRACK || set.block == Blocks.OBSIDIAN)
		{
			NBTTagList list = stack.getEnchantmentTagList();	
			int targetId = 1;
			int id = 0;
			boolean wasEnchanted = false; 
			Enchantment e;
			if (list != null)
			{
				for (int i = 0; i < list.tagCount(); i++) 
				{
					NBTTagCompound compound = list.getCompoundTagAt(i);
					id = compound.getShort("id");
					if (id == targetId)
						wasEnchanted = true;
				}
			}
			if (entity instanceof EntityLivingBase && ArmorSet.isWearingFullSet((EntityLivingBase) entity, set) 
					&& ((EntityLivingBase) entity).getItemStackFromSlot(EntityEquipmentSlot.FEET) == stack
					&& !wasEnchanted)
				stack.addEnchantment(Enchantments.FIRE_PROTECTION, 4);
			else if (stack.isItemEnchanted() && !ArmorSet.isWearingFullSet((EntityLivingBase) entity, set))
			{
				if (list != null)
				{
					for (int i = 0; i < list.tagCount(); i++) 
					{
						NBTTagCompound compound = list.getCompoundTagAt(i);
						id = compound.getShort("id");
						e = Enchantment.getEnchantmentByID(id);
						if (e == null || id != targetId)
							continue;
						NBTTagCompound stackCompound = stack.getTagCompound();
						if (stackCompound == null)
							return;
						list.removeTag(i);
						if (list.tagCount() <= 0)
							stackCompound.removeTag("ench");
						return;
					}
				}
			}
		}
		//Thorns
		if (set.block == Blocks.CACTUS)
		{
			NBTTagList list = stack.getEnchantmentTagList();	
			int targetId = 7;
			int id = 0;
			boolean wasEnchanted = false; 
			Enchantment e;
			if (list != null)
			{
				for (int i = 0; i < list.tagCount(); i++) 
				{
					NBTTagCompound compound = list.getCompoundTagAt(i);
					id = compound.getShort("id");
					if (id == 7)
						wasEnchanted = true;
				}
			}
			if (entity instanceof EntityLivingBase && ArmorSet.isWearingFullSet((EntityLivingBase) entity, set) && !wasEnchanted)
				stack.addEnchantment(Enchantments.THORNS, 1);
			else if (stack.isItemEnchanted() && !ArmorSet.isWearingFullSet((EntityLivingBase) entity, set))
			{
				if (list != null)
				{
					for (int i = 0; i < list.tagCount(); i++) 
					{
						NBTTagCompound compound = list.getCompoundTagAt(i);
						id = compound.getShort("id");
						e = Enchantment.getEnchantmentByID(id);
						if (e == null || id != targetId)
							continue;
						NBTTagCompound stackCompound = stack.getTagCompound();
						if (stackCompound == null)
							return;
						list.removeTag(i);
						if (list.tagCount() <= 0)
							stackCompound.removeTag("ench");
						return;
					}
				}
			}
		}
	}
}
