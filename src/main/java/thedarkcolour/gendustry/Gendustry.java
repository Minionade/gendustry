package thedarkcolour.gendustry;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.fml.common.Mod;

@Mod(Gendustry.ID)
public class Gendustry {
	public static final String ID = "gendustry";

	public static ResourceLocation loc(String path) {
		return ResourceLocation.fromNamespaceAndPath(ID, path);
	}
}
