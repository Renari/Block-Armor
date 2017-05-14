package twopiradians.blockArmor.client.gui.config;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

public class BlockArmorGuiFactory implements IModGuiFactory 
{
	@Override
	public void initialize(Minecraft minecraftInstance) 
	{

	}

	@Override
	public Class<? extends GuiScreen> mainConfigGuiClass() 
	{
		return BlockArmorGuiConfig.class;
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() 
	{
		return null;
	}

	@SuppressWarnings("deprecation")
	@Override
	public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) 
	{
		return null;
	}

	@Override
	public boolean hasConfigGui() { //TODO these two methods were added for some reason, test that everything still works
		return false;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen) {
		return null;
	}
}
