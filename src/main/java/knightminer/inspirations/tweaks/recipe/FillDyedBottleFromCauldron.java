package knightminer.inspirations.tweaks.recipe;

import knightminer.inspirations.library.recipe.cauldron.ICauldronRecipe;
import knightminer.inspirations.tweaks.InspirationsTweaks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;

public class FillDyedBottleFromCauldron implements ICauldronRecipe {

	public static final FillDyedBottleFromCauldron INSTANCE = new FillDyedBottleFromCauldron();
	private FillDyedBottleFromCauldron() {}

	@Override
	public boolean matches(ItemStack stack, boolean boiling, int level, CauldronState state) {
		return level != 0 && state.getType() == CauldronContents.DYE && stack.getItem() == Items.GLASS_BOTTLE;
	}

	@Override
	public ItemStack getResult(ItemStack stack, boolean boiling, int level, CauldronState state) {
		return InspirationsTweaks.dyedWaterBottle.getStackWithColor(state.getColor());
	}

	@Override
	public int getLevel(int level) {
		return level - 1;
	}

	@Override
	public SoundEvent getSound(ItemStack stack, boolean boiling, int level, CauldronState state) {
		return SoundEvents.ITEM_BOTTLE_FILL;
	}

	@Override
	public float getVolume() {
		return 1.0f;
	}
}
