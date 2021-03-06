package twopiradians.blockArmor.common.seteffect;

import net.minecraft.block.Block;
import net.minecraft.init.Enchantments;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.text.TextFormatting;

public class SetEffectFrosty extends SetEffect {

	protected SetEffectFrosty() {
		this.color = TextFormatting.AQUA;
		this.description = "Provides Frost Walking II";
		this.enchantments.add(new EnchantmentData(Enchantments.FROST_WALKER, (short) 2, EntityEquipmentSlot.FEET));
	}

	/**Should block be given this set effect*/
	@Override
	protected boolean isValid(Block block, int meta) {		
		if (SetEffect.registryNameContains(block, meta, new String[] {"ice", "snow", "frozen", "frost"}))
			return true;		
		return false;
	}
}