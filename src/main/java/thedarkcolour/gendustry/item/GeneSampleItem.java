package thedarkcolour.gendustry.item;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.core.platform.render.ColourProperties;

import org.jetbrains.annotations.Nullable;
import thedarkcolour.gendustry.registry.GItems;

public class GeneSampleItem extends SpeciesTypeItem {
	public static final String NBT_CHROMOSOME = "chromosome";
	public static final String NBT_ALLELE = "allele";
	private static final String NBT_DOMINANT = "dominant";

	public GeneSampleItem() {
		super(new Item.Properties().rarity(Rarity.UNCOMMON));
	}

	public static ItemStack createStack(ISpeciesType<?, ?> speciesType, IChromosome<?> chromosome, Allele<?> allele) {
		ItemStack stack = new ItemStack(GItems.GENE_SAMPLE);
		CompoundTag tag = ItemStackData.read(stack);
		tag.putString(NBT_SPECIES_TYPE, speciesType.id().toString());
		tag.putString(NBT_CHROMOSOME, chromosome.id().toString());
		tag.putString(NBT_ALLELE, encodeValue(chromosome, allele));
		tag.putBoolean(NBT_DOMINANT, allele.dominant());
		ItemStackData.write(stack, tag);
		return stack;
	}

	@Nullable
	public static IChromosome<?> getChromosome(ItemStack stack) {
		ISpeciesType<?, ?> type = getSpeciesType(stack);
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (type == null || data == null) return null;
		ResourceLocation id = ResourceLocation.tryParse(data.copyTag().getString(NBT_CHROMOSOME));
		return id == null ? null : type.getKaryotype().getChromosome(id);
	}

	@Nullable
	public static Allele<?> getAllele(ItemStack stack) {
		IChromosome<?> chromosome = getChromosome(stack);
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (chromosome == null || data == null) return null;
		CompoundTag tag = data.copyTag();
		return decodeValue(chromosome, tag.getString(NBT_ALLELE), tag.getBoolean(NBT_DOMINANT));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag advanced) {
		IChromosome<?> chromosome = getChromosome(stack);
		Allele<?> allele = getAllele(stack);
		if (chromosome != null && allele != null) {
			tooltip.add(Component.translatable(chromosome.chromosomeTranslationKey()).append(" - ").withStyle(ChatFormatting.GRAY)
					.append(displayAllele(chromosome, allele).withStyle(style -> style.withColor(getColorCoding(allele.dominant())))));
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static MutableComponent displayAllele(IChromosome chromosome, Allele allele) {
		return Component.translatable(chromosome.translationKey(allele.value()));
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static String encodeValue(IChromosome chromosome, Allele allele) {
		Object encoded = chromosome.valueCodec().encodeStart(JsonOps.INSTANCE, allele.value()).result().orElse(null);
		return encoded == null ? "" : encoded.toString();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static Allele<?> decodeValue(IChromosome chromosome, String json, boolean dominant) {
		try {
			Object value = chromosome.valueCodec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).result().orElse(null);
			return value == null ? null : new Allele(value, dominant);
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	// Copied from Forestry's analyzer color convention.
	public static int getColorCoding(boolean dominant) {
		return ColourProperties.INSTANCE.get(dominant ? "gui.beealyzer.dominant" : "gui.beealyzer.recessive");
	}
}
