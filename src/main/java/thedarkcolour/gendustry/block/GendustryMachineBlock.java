package thedarkcolour.gendustry.block;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

import forestry.core.platform.block.BlockBase;

public class GendustryMachineBlock extends BlockBase<GendustryMachineType> {
	public GendustryMachineBlock(GendustryMachineType blockType) {
		super(Properties.of().sound(SoundType.METAL).mapColor(MapColor.SAND).instrument(NoteBlockInstrument.IRON_XYLOPHONE).requiresCorrectToolForDrops(), blockType);
	}
}
