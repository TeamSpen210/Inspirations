package knightminer.inspirations.common.item;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import knightminer.inspirations.common.IHidable;
import knightminer.inspirations.library.util.TextureBlockUtil;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class ItemBlockTexture extends HidableBlockItem {

	public ItemBlockTexture(Block block, BlockItem.Properties props) {
		super(block, props);
	}

	@Override
	public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<ITextComponent> tooltip, @Nonnull ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);
		if(!stack.hasTag()) {
			return;
		}

		ItemStack texture = TextureBlockUtil.getStackTexture(stack);
		if(!texture.isEmpty()) {
			tooltip.add(texture.getDisplayName());
		}
	}
}
