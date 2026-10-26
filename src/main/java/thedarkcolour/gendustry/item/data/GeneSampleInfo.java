package thedarkcolour.gendustry.item.data;

import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;

/** Runtime view of a gene sample's species, chromosome, and inline allele value. */
public record GeneSampleInfo(ISpeciesType<?, ?> type, IChromosome<?> chromosome, Allele<?> allele) {}
