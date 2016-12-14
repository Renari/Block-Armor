package twopiradians.blockArmor.client;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.common.collect.Maps;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import twopiradians.blockArmor.client.gui.armorDisplay.OpenGuiEvent;
import twopiradians.blockArmor.client.model.ModelBlockArmor;
import twopiradians.blockArmor.client.model.ModelDynBlockArmor;
import twopiradians.blockArmor.common.BlockArmor;
import twopiradians.blockArmor.common.CommonProxy;
import twopiradians.blockArmor.common.block.ModBlocks;
import twopiradians.blockArmor.common.item.ArmorSet;
import twopiradians.blockArmor.common.item.ModItems;
import twopiradians.blockArmor.packets.DisableSetsPacket;

public class ClientProxy extends CommonProxy
{
	/**Map of models to their constructor fields - generated as needed*/
	private HashMap<String, ModelBlockArmor> modelMaps = Maps.newHashMap();
	/**Should textures and icons be remapped next client tick*/
	public boolean remapTextures;
	/**Is this the first time the world was loaded since restarting client*/
	private boolean firstWorldLoad = true;
	/**Allow ArmorSet to retry finding textures once if it can't find one*/
	public boolean shouldRetryFindingTextures = true;

	@Override
	public void preInit() {
		ModelLoaderRegistry.registerLoader(ModelDynBlockArmor.LoaderDynBlockArmor.INSTANCE);
	}

	@Override
	public void init() {
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new OpenGuiEvent());
	}

	@Override
	public void postInit() {
		ModBlocks.registerRenders();
		ModItems.registerRenders();
	}

	@Override
	public Object getBlockArmorModel(int height, int width, boolean isTranslucent, int frame, EntityEquipmentSlot slot) {
		String key = height+""+width+""+isTranslucent+""+frame+""+slot.getName();
		ModelBlockArmor model = modelMaps.get(key);
		if (model == null) {
			model = new ModelBlockArmor(height, width, isTranslucent, frame, slot);
			modelMaps.put(key, model);
		}
		return model;
	}

	@SubscribeEvent
	public void worldLoad(WorldEvent.Load event)
	{
		if (firstWorldLoad) { //used to map textures immediately if needed - sometimes ClientTickEvent doesn't get to it in time
			firstWorldLoad = false;
			this.mapTextures();
		}
	}

	@SubscribeEvent
	public void clientTick(TickEvent.ClientTickEvent event)
	{
		if (remapTextures && Minecraft.getMinecraft().thePlayer != null)
			this.mapTextures();
	}

	/**Resets model and item quads and maps block textures (called when client joins world or resource pack loaded)*/
	private void mapTextures() {
		//reset model and item quad maps
		modelMaps = Maps.newHashMap();

		//find block textures
		remapTextures = false;
		int numTextures = 0;
		ArmorSet.disabledItems = new ArrayList<Item>();
		for (ArmorSet set : ArmorSet.allSets) {
			int ret = set.initTextures();
			if (ret == -1) {
				BlockArmor.logger.warn("Unable to find texture for item: "+set.stack.getDisplayName());
				this.shouldRetryFindingTextures = false;
				BlockArmor.logger.warn("Searching for textures again...");
				this.remapTextures = true;
				break;
			}
			else
				numTextures += set.initTextures();
		}
		BlockArmor.logger.info("Found "+numTextures+" block textures for Block Armor");
		
		//send disabled sets to server and disable them
		BlockArmor.network.sendToServer(new DisableSetsPacket(ArmorSet.disabledItems));
		ArmorSet.disableItems();

		//create inventory icons
		if (!remapTextures) { //if all block textures were found
			this.shouldRetryFindingTextures = true;
			int numIcons = ModelDynBlockArmor.BakedDynBlockArmorOverrideHandler.createInventoryIcons();
			BlockArmor.logger.info("Created "+numIcons+" inventory icons for Block Armor");
		}
	}

	/**Used to register block textures to override inventory textures and for inventory icons*/
	@SubscribeEvent
	public void textureStitch(TextureStitchEvent.Pre event)
	{
		//textures for overriding
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/reeds"));

		//textures for inventory icons
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet_base"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet_cover"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet1_template"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_helmet2_template"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_chestplate_base"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_chestplate_cover"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_chestplate1_template"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_chestplate2_template"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_leggings_base"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_leggings_cover"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_leggings1_template"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_leggings2_template"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_boots_base"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_boots_cover"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_boots1_template"));
		event.getMap().registerSprite(new ResourceLocation(BlockArmor.MODID, "items/icons/block_armor_boots2_template"));
	}

	@SubscribeEvent
	public void textureStitch(TextureStitchEvent.Post event)
	{
		this.remapTextures = true;
	}
}
