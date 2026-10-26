package thedarkcolour.gendustry.item;

import com.google.common.collect.ImmutableList;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;

import org.jetbrains.annotations.Nullable;
import thedarkcolour.gendustry.data.TranslationKeys;

public class GeneticTemplateItem extends SpeciesTypeItem {
	public static final String NBT_ALLELES = "alleles";

	public GeneticTemplateItem() {
		super(new Properties().stacksTo(1).rarity(Rarity.RARE));
	}

	public static void addAlleles(ItemStack template, Map<IChromosome<?>, Allele<?>> samples) {
		CompoundTag tag = ItemStackData.read(template);
		CompoundTag alleles = new CompoundTag();
		samples.forEach((chromosome, allele) -> {
			CompoundTag encoded = new CompoundTag();
			encoded.putString("value", GeneSampleItem.encodeValue(chromosome, allele));
			encoded.putBoolean("dominant", allele.dominant());
			alleles.put(chromosome.id().toString(), encoded);
		});
		tag.put(NBT_ALLELES, alleles);
		ItemStackData.write(template, tag);
	}

	public static Map<IChromosome<?>, Allele<?>> getAlleles(ItemStack template) {
		CompoundTag tag = ItemStackData.read(template);
		if (!tag.contains(NBT_ALLELES)) return Map.of();
		CompoundTag encodedAlleles = tag.getCompound(NBT_ALLELES);
		ISpeciesType<?, ?> type = getSpeciesType(template);
		if (type == null) return Map.of();

		IdentityHashMap<IChromosome<?>, Allele<?>> alleles = new IdentityHashMap<>(encodedAlleles.size());
		for (String key : encodedAlleles.getAllKeys()) {
			ResourceLocation chromosomeId = ResourceLocation.tryParse(key);
			if (chromosomeId == null) continue;
			IChromosome<?> chromosome = type.getKaryotype().getChromosome(chromosomeId);
			if (chromosome == null) continue;
			CompoundTag encoded = encodedAlleles.getCompound(key);
			Allele<?> allele = GeneSampleItem.decodeValue(chromosome, encoded.getString("value"), encoded.getBoolean("dominant"));
			if (allele != null) alleles.put(chromosome, allele);
		}
		return alleles;
	}

	public static boolean isComplete(ItemStack stack) {
		ISpeciesType<?, ?> speciesType = getSpeciesType(stack);
		Map<IChromosome<?>, Allele<?>> alleles = getAlleles(stack);
		return speciesType != null && speciesType.getKaryotype().size() == alleles.size();
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
		ISpeciesType<?, ?> speciesType = getSpeciesType(stack);
		if (speciesType == null) return;

		ImmutableList<IChromosome<?>> chromosomes = speciesType.getKaryotype().getChromosomes();
		Map<IChromosome<?>, Allele<?>> alleles = getAlleles(stack);
		int countIndex = tooltip.size();
		int foundAlleles = 0;

		for (IChromosome<?> chromosome : chromosomes) {
			Allele<?> allele = alleles.get(chromosome);
			Component chromosomeName = Component.translatable(chromosome.chromosomeTranslationKey());
			if (allele == null) {
				tooltip.add(Component.translatable(TranslationKeys.TEMPLATE_ALLELE_ENTRY, chromosomeName, Component.translatable(TranslationKeys.TEMPLATE_MISSING_ALLELE).withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
			} else {
				Component alleleName = GeneSampleItem.displayAllele(chromosome, allele).withStyle(style -> style.withColor(GeneSampleItem.getColorCoding(allele.dominant())));
				tooltip.add(Component.translatable(TranslationKeys.TEMPLATE_ALLELE_ENTRY, chromosomeName, alleleName).withStyle(ChatFormatting.GRAY));
				++foundAlleles;
			}
		}
		tooltip.add(countIndex, Component.translatable(TranslationKeys.TEMPLATE_ALLELE_COUNT, foundAlleles, chromosomes.size()).withStyle(ChatFormatting.GRAY));
	}
}
