package knightminer.inspirations.library.recipe;

import com.google.gson.JsonObject;

import knightminer.inspirations.Inspirations;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ShapelessNoContainerRecipe extends ShapelessRecipe {

	public ShapelessNoContainerRecipe(ResourceLocation id, String group, ItemStack result, NonNullList<Ingredient> input) {
		super(id, group, result, input);
	}

	private ShapelessNoContainerRecipe(ShapelessRecipe orig) {
		super(orig.getId(), orig.getGroup(), orig.getRecipeOutput(), orig.getIngredients());
	}

	@Nonnull
	@Override
	public NonNullList<ItemStack> getRemainingItems(CraftingInventory inv) {
		return NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
	}

	@Nonnull
	@Override
	public IRecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

	public static final IRecipeSerializer<?> SERIALIZER = new Serializer().setRegistryName(new ResourceLocation(Inspirations.modID, "shapeless_no_container"));

	private static class Serializer extends net.minecraftforge.registries.ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<ShapelessNoContainerRecipe> {
		// This recipe has the exact same options as the parent type, redirect to that code.
		@Nonnull
		@Override
		public ShapelessNoContainerRecipe read(ResourceLocation recipeID, PacketBuffer buffer) {
			return new ShapelessNoContainerRecipe(CRAFTING_SHAPELESS.read(recipeID, buffer));
		}

		@Nonnull
		@Override
		public ShapelessNoContainerRecipe read(ResourceLocation recipeID, JsonObject json) {
			return new ShapelessNoContainerRecipe(CRAFTING_SHAPELESS.read(recipeID, json));
		}

		@Override
		public void write(PacketBuffer buffer, ShapelessNoContainerRecipe recipe) {
			Serializer.CRAFTING_SHAPELESS.write(buffer, recipe);
		}
	}
}
