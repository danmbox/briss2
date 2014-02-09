package at.laborg.briss.utils;

import java.util.ArrayList;
import java.util.List;

public class CropParser {
	public static List<List<Float[]>> parse (String string) {
		ArrayList<List<Float[]>> crop = new ArrayList<List<Float[]>>();
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
}
