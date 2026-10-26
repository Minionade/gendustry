package thedarkcolour.gendustry.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

final class ItemStackData {
	private ItemStackData() {}

	static CompoundTag read(ItemStack stack) {
		return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
	}

	static void write(ItemStack stack, CompoundTag tag) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}
}
