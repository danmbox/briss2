package at.laborg.briss.utils;

import java.util.ArrayList;
import java.util.List;

public class CropParser {
	public static List<List<Float[]>> parse (String string) {
		List<List<Float[]>> crop = new ArrayList<List<Float[]>>();
		for(String page : string.split(":")) {
			List<Float[]> pageratios = new ArrayList<Float[]>();
			crop.add(pageratios);
			for(String part : page.split(",")) {
				String[] parts = part.split("/");
				Float[] ratios = new Float[parts.length];
				for(int i = 0; i < ratios.length; i++) {
					ratios[i] = Float.parseFloat(parts[i]);
				}
				pageratios.add(ratios);
			}
		}
		return crop;
	}
	public static String cropToString (List<List<Float[]>> clusters) {
		StringBuilder crop = new StringBuilder();

		int i = -1;
		for(List<Float[]> cluster : clusters) {
			++i;
			if (i > 0) crop.append(":");
			int j = -1;
			for(Float[] parts : cluster) {
				++j;
				if (j > 0) crop.append(',');
				for(int k = 0; k < parts.length; k++) {
					if (k > 0) crop.append('/');
					// cut away those huge decimals
					if (0.0 == parts[k]) crop.append('0');
					else crop.append(String.valueOf(parts[k].floatValue()));
				}
			}
		}
		return crop.toString ();
	}
}
