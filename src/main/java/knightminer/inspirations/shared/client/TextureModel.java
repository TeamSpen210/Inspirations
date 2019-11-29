package knightminer.inspirations.shared.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import knightminer.inspirations.library.Util;
import knightminer.inspirations.library.util.TagUtil;
import knightminer.inspirations.library.util.TextureBlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.BakedQuadRetextured;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import slimeknights.mantle.client.ModelHelper;

public class TextureModel extends BakedModelWrapper<IBakedModel> {
	private final Map<CacheKey, List<BakedQuad>> cache = new HashMap<>();
	private boolean item;

	// For the textured blocks, we need a valid sprite which we will replace with the correct texture.
	protected static TextureAtlasSprite textureKey;

	public TextureModel(IBakedModel originalModel, boolean item) {
		super(originalModel);
		this.item = item;
		textureKey = getSprite(Util.getResource("retexture"));
	}

	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
		IBakedModel bakedModel = this.originalModel;
		String texture = extraData.getData(TextureBlockUtil.TEXTURE_PROP);
		if(texture != null) {
			return cache.computeIfAbsent(new CacheKey(texture, side), (spr) -> {
				List<BakedQuad> quads = bakedModel.getQuads(state, side, rand);
				return retexture(quads, new ResourceLocation(texture));
			});
		}
		return bakedModel.getQuads(state, side, rand);
	}

	@Nonnull
	@Override
	public TextureAtlasSprite getParticleTexture(@Nonnull IModelData data) {
		String texture = data.getData(TextureBlockUtil.TEXTURE_PROP);
		if (texture != null) {
			return getSprite(new ResourceLocation(texture));
		}
		return originalModel.getParticleTexture();
	}

	protected TextureAtlasSprite getSprite(ResourceLocation location) {
		return ModelLoader.defaultTextureGetter().apply(location);
	}

	protected List<BakedQuad> retexture(List<BakedQuad> original, ResourceLocation replacement) {
		TextureAtlasSprite sprite = getSprite(replacement);
		ArrayList<BakedQuad>newQuads = new ArrayList<>(original.size());

		for(BakedQuad quad: original) {
			if (quad.getSprite() == textureKey) {
				newQuads.add(new BakedQuadRetextured(quad, sprite));
			} else {
				newQuads.add(quad);
			}
		}

		newQuads.trimToSize();
		return newQuads;
	}

	@Nonnull
	@Override
	public ItemOverrideList getOverrides() {
		return item ? ItemTextureOverride.INSTANCE : super.getOverrides();
	}

	private static class ItemTextureOverride extends ItemOverrideList {

		static ItemTextureOverride INSTANCE = new ItemTextureOverride();
		private final Map<TextureAtlasSprite, IBakedModel> cache = new HashMap<>();

		private ItemTextureOverride() {
			super();
		}

		@Nullable
		@Override
		public IBakedModel getModelWithOverrides(@Nonnull IBakedModel originalModel, @Nonnull ItemStack stack, @Nullable World world, @Nullable LivingEntity entity) {
			if(originalModel instanceof TextureModel) {
				// read out the data on the itemstack
				ItemStack blockStack = ItemStack.read(TagUtil.getTagSafe(stack).getCompound(TextureBlockUtil.TAG_TEXTURE));
				if(!blockStack.isEmpty()) {
					// get model from data
					Item item = blockStack.getItem();
					Block block = Block.getBlockFromItem(item);
					TextureAtlasSprite sprite = ModelHelper.getTextureFromBlockstate(block.getDefaultState());
					TextureModel textureModel = (TextureModel) originalModel;
					return originalModel;
				}
			}

			return originalModel;
		}
	}


	private static class CacheKey {
		@Nullable
		private String texture;
		@Nullable
		private Direction direction;
		CacheKey(@Nullable String texture, @Nullable Direction direction) {
			this.texture = texture;
			this.direction = direction;
		}

		@Override
		public boolean equals(Object o) {
			if(this == o) {
				return true;
			}
			if(o == null || getClass() != o.getClass()) {
				return false;
			}

			CacheKey that = (CacheKey) o;
			return Objects.equals(this.texture, that.texture) && Objects.equals(this.direction, that.direction);
		}

		@Override
		public int hashCode() {
			return (texture == null ? 0 : 31 * texture.hashCode()) + (direction == null ? 0 : direction.hashCode());
		}
	}

	// Add a special sprite which will be replaced by the real sprite whenever the model is used.
	@SubscribeEvent
	public static void onTextureStitchPre(TextureStitchEvent.Pre event) {
		if (event.getMap().getBasePath().equals("texture")) {
			event.addSprite(Util.getResource("retexture"));
		}
	}
	@SubscribeEvent
	public static void onTextureStitchPost(TextureStitchEvent.Pre event) {
		if(event.getMap().getBasePath().equals("texture")) {
			textureKey = event.getMap().getSprite(Util.getResource("retexture"));
		}
	}
}
