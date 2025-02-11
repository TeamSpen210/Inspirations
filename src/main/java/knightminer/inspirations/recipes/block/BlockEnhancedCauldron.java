package knightminer.inspirations.recipes.block;

import com.google.common.collect.Lists;
import knightminer.inspirations.common.Config;
import knightminer.inspirations.library.InspirationsRegistry;
import knightminer.inspirations.library.Util;
import knightminer.inspirations.library.util.TextureBlockUtil;
import knightminer.inspirations.recipes.client.BoilingParticle;
import knightminer.inspirations.recipes.tileentity.TileCauldron;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import slimeknights.mantle.property.PropertyString;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BlockEnhancedCauldron extends BlockCauldron implements ITileEntityProvider {

	public static final PropertyEnum<CauldronContents> CONTENTS = PropertyEnum.create("contents", CauldronContents.class);
	public static final PropertyInteger LEVELS = PropertyInteger.create("levels", 0, 4);
	public static final PropertyBool BOILING = PropertyBool.create("boiling");
	public static final PropertyString TEXTURE = TextureBlockUtil.TEXTURE_PROP;

	public BlockEnhancedCauldron() {
		IBlockState state = this.blockState.getBaseState()
				.withProperty(LEVEL, 0)
				.withProperty(BOILING, false)
				.withProperty(CONTENTS, CauldronContents.FLUID);
		if(Config.enableBiggerCauldron) {
			state = state.withProperty(LEVELS, 0);
		}
		this.setDefaultState(state);

		this.setHardness(2.0F);
		this.setUnlocalizedName("cauldron");
		this.hasTileEntity = true;
	}

	/* TE behavior */

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TileCauldron();
	}

	@Override
	public void fillWithRain(World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		// do not fill unless the current contents are water
		if(te instanceof TileCauldron && !((TileCauldron) te).isWater()) {
			return;
		}

		// allow disabling the random 1/20 chance
		if((Config.fasterCauldronRain || world.rand.nextInt(20) == 0)
				&& world.getBiomeProvider().getTemperatureAtHeight(world.getBiome(pos).getTemperature(pos), pos.getY()) >= 0.15F) {
			IBlockState state = world.getBlockState(pos);
			int level = getLevel(state);
			if(level < (Config.enableBiggerCauldron ? 4 : 3)) {
				setWaterLevel(world, pos, state, level+1);
			}
		}
	}

	/**
	 * Called When an Entity Collided with the Block
	 */
	@Override
	public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
		TileEntity te = world.getTileEntity(pos);
		// do not estinguish unless the current contents are water
		if(!(te instanceof TileCauldron)) {
			return;
		}
		if(world.isRemote) {
			return;
		}

		// ensure the entity is touching the fluid inside
		int level = getLevel(state);
		float f = pos.getY() + ((Config.enableBiggerCauldron ? 2.5F : 5.5F) + 3 * Math.max(level, 1)) / 16.0F;
		if (entity.getEntityBoundingBox().minY <= f) {
			// if so, have the TE handle it
			int newLevel = ((TileCauldron)te).onEntityCollide(entity, level, state);
			// if the level changed, update it
			if(level != newLevel) {
				this.setWaterLevel(world, pos, state, newLevel);
			}

		}
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		int level = getLevel(state);
		if(!Config.dropCauldronContents || level == 0) {
			return;
		}

		TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileCauldron) {
			((TileCauldron)te).onBreak(pos, level);
		}

		super.breakBlock(world, pos, state);
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
		this.onBlockHarvested(world, pos, state, player);
		world.setBlockState(pos, Blocks.AIR.getDefaultState(), world.isRemote ? 11 : 3);
		return world.getBlockState(pos).getBlock() != Blocks.CAULDRON;
	}

	/* Content texture */

	@Override
	protected ExtendedBlockState createBlockState() {
		if(Config.enableBiggerCauldron) {
			return new ExtendedBlockState(this, new IProperty[]{LEVEL, LEVELS, CONTENTS, BOILING}, new IUnlistedProperty[]{TEXTURE});
		}
		return new ExtendedBlockState(this, new IProperty[]{LEVEL, CONTENTS, BOILING}, new IUnlistedProperty[]{TEXTURE});
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileCauldron) {
			state = state.withProperty(CONTENTS, ((TileCauldron)te).getContentType());
		}
		return state;
	}

	@Nonnull
	@Override
	public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
		IExtendedBlockState extendedState = (IExtendedBlockState) state;

		TileEntity te = world.getTileEntity(pos);
		if(te instanceof TileCauldron) {
			return ((TileCauldron)te).writeExtendedBlockState(extendedState);
		}

		return super.getExtendedState(state, world, pos);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		return true; // all moved to the cauldron registry
	}


	/* boiling */
	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
		setBoiling(world, pos, state);
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		setBoiling(world, pos, state);
	}

	private static void setBoiling(World world, BlockPos pos, IBlockState state) {
		world.setBlockState(pos, state.withProperty(BOILING, InspirationsRegistry.isCauldronFire(world.getBlockState(pos.down()))));
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
		if(!state.getValue(BOILING)) {
			return;
		}

		int level = getLevel(state);
		if(level == 0) {
			return;
		}

		ParticleManager manager = Minecraft.getMinecraft().effectRenderer;
		for(int i = 0; i < 2; i++) {
			double x = pos.getX() + 0.1875D + (rand.nextFloat() * 0.625D);
			double y = pos.getY() + (Config.enableBiggerCauldron ? 0.1875 : 0.375D) + (level * 0.1875D);
			double z = pos.getZ() + 0.1875D + (rand.nextFloat() * 0.625D);
			manager.addEffect(new BoilingParticle(world, x, y, z, 0, 0, 0));
		}
	}


	/* 4 bottle support */
	@Override
	public void setWaterLevel(World worldIn, BlockPos pos, IBlockState state, int level) {
		// if 4, set 4 prop
		if(Config.enableBiggerCauldron) {
			state = state.withProperty(LEVELS, MathHelper.clamp(level, 0, 4));
		}
		worldIn.setBlockState(pos, state.withProperty(LEVEL, MathHelper.clamp(level, 0, 3)), 2);
		worldIn.updateComparatorOutputLevel(pos, this);
	}

	/**
	 * Gets the level of a cauldron from the given state
	 * @param state  Block state
	 * @return  Cauldron level
	 */
	public static int getCauldronLevel(IBlockState state) {
		Block block = state.getBlock();
		if(state.getBlock() instanceof BlockEnhancedCauldron) {
			return ((BlockEnhancedCauldron)block).getLevel(state);
		}
		if(state.getPropertyKeys().contains(LEVEL)) {
			return state.getValue(LEVEL);
		}
		return InspirationsRegistry.getCauldronMax();
	}

	public int getLevel(IBlockState state) {
		if(Config.enableBiggerCauldron) {
			return state.getValue(LEVELS);
		}
		return state.getValue(LEVEL);
	}

	@Override
	public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
		return getLevel(state);
	}

	/**
	 * Convert the given metadata into a BlockState for this Block
	 */
	@Override
	public IBlockState getStateFromMeta(int meta) {
		IBlockState state = this.getDefaultState().withProperty(LEVEL, MathHelper.clamp(meta & 7, 0, 3));
		// if 4, set 4 prop
		if(Config.enableBiggerCauldron) {
			state = state.withProperty(LEVELS, MathHelper.clamp(meta & 7, 0, 4));
		}
		state = state.withProperty(BOILING, (meta & 8) > 0);
		return state;
	}

	/**
	 * Convert the BlockState into the correct metadata value
	 */
	@Override
	public int getMetaFromState(IBlockState state) {
		return getLevel(state) | (state.getValue(BOILING) ? 8 : 0);
	}

	/* bounds */
	public static final AxisAlignedBB[] BOUNDS = {
			new AxisAlignedBB(0, 0.1875, 0, 1, 1, 1),
			new AxisAlignedBB(0,     0, 0,     0.25,  0.1875, 0.125),
			new AxisAlignedBB(0,     0, 0.125, 0.125, 0.1875, 0.25 ),
			new AxisAlignedBB(0.75,  0, 0,     1,     0.1875, 0.125),
			new AxisAlignedBB(0.875, 0, 0.125, 1,     0.1875, 0.25 ),
			new AxisAlignedBB(0,     0, 0.875, 0.25,  0.1875, 1    ),
			new AxisAlignedBB(0,     0, 0.75,  0.125, 0.1875, 0.875),
			new AxisAlignedBB(0.75,  0, 0.875, 1,     0.1875, 1    ),
			new AxisAlignedBB(0.875, 0, 0.75,  1,     0.1875, 0.875)
	};

	@Deprecated
	@Override
	public RayTraceResult collisionRayTrace(IBlockState state, @Nonnull World world, @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end) {
		// basically the same BlockStairs does
		// Raytrace through all AABBs (plate, legs) and return the nearest one
		List<RayTraceResult> list = Lists.<RayTraceResult>newArrayList();
		for(AxisAlignedBB axisalignedbb : BOUNDS) {
			list.add(rayTrace(pos, start, end, axisalignedbb));
		}

		return Util.closestResult(list, end);
	}

	public static enum CauldronContents implements IStringSerializable {
		FLUID,
		DYE,
		POTION;

		private int meta;
		CauldronContents() {
			this.meta = ordinal();
		}

		@Override
		public String getName() {
			return name().toLowerCase(Locale.US);
		}

		public int getMeta() {
			return meta;
		}

		public static CauldronContents fromMeta(int meta) {
			if(meta > values().length) {
				meta = 0;
			}

			return values()[meta];
		}
	}
}
