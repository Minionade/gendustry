package thedarkcolour.gendustry.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;


import forestry.core.platform.fluids.ITankManager;
import forestry.core.platform.fluids.TankManager;
import forestry.core.platform.tile.ILiquidTankTile;
import forestry.core.platform.tile.TilePowered;


public abstract class PoweredTankBlockEntity extends TilePowered implements ILiquidTankTile {
	protected final TankManager tankManager;

	public PoweredTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int maxTransfer, int capacity) {
		super(type, pos, state, maxTransfer, capacity);

		this.tankManager = new TankManager(this);
	}

	@Override
	public void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.saveAdditional(nbt, registries);
		this.tankManager.write(nbt, registries);
	}

	@Override
	public void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
		super.loadAdditional(nbt, registries);
		this.tankManager.read(nbt, registries);
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		super.writeData(data);
		this.tankManager.writeData(data);
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		super.readData(data);
		this.tankManager.readData(data);
	}

	@Override
	public void writeGuiData(RegistryFriendlyByteBuf data) {
		super.writeGuiData(data);
		this.tankManager.writeData(data);
	}

	@Override
	public void readGuiData(RegistryFriendlyByteBuf data) {
		super.readGuiData(data);
		this.tankManager.readData(data);
	}

	@Override
	public ITankManager getTankManager() {
		return this.tankManager;
	}
}
