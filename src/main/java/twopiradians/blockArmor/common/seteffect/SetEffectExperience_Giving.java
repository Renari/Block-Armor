package twopiradians.blockArmor.common.seteffect;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import twopiradians.blockArmor.common.item.ArmorSet;

public class SetEffectExperience_Giving extends SetEffect {

	protected SetEffectExperience_Giving() {
		this.color = TextFormatting.GREEN;
		this.description = "Gives experience over time";
	}

	/**Only called when player wearing full, enabled set*/
	public void onArmorTick(World world, EntityPlayer player, ItemStack stack) {
		super.onArmorTick(world, player, stack);

		if (ArmorSet.getFirstSetItem(player, this) == stack && !world.isRemote && 
				!player.getCooldownTracker().hasCooldown(stack.getItem())) {
			this.setCooldown(player, 50);
			player.addExperience(1);
			this.damageArmor(player, 1, false);
		}
	}

	/**Should block be given this set effect*/
	@Override
	protected boolean isValid(Block block, int meta) {	
		if (SetEffect.registryNameContains(block, meta, new String[] {"lapis", "enchant", "experience"}))
			return true;		
		return false;
	}
}