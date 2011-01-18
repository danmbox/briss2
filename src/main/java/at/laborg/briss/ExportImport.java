package at.laborg.briss;

import java.io.File;
import java.util.List;
import java.util.Set;

public class ExportImport {

	public static String export(ClusterJobData clusterJobData) {

		StringBuffer result = new StringBuffer();
		result.append(exportExcludePages(clusterJobData.getExcludedPageSet())
				+ "\n");
		for (PageCluster cluster : clusterJobData.getClusterAsList()) {
			result.append(exportRatios(cluster.getRatiosList()) + "\n");
		}
		return result.toString();
	}

	private static String exportRatios(List<Float[]> ratiosList) {
		StringBuffer result = new StringBuffer();
		for (Float[] ratios : ratiosList) {
			result.append(ratios[0] + " " + ratios[1] + " " + ratios[2] + " "
					+ ratios[3] + " ");
		}
		return result.toString();
	}

	private static String exportExcludePages(Set<Integer> excludedPages) {
		if (excludedPages != null) {
			StringBuffer result = new StringBuffer();
			for (Integer page : excludedPages) {
				result.append(page + " ");
			}
			return result.toString();
		}
		return null;
	}

	public static ClusterJobData importClusterJobData(File pdf, File inFileText) {
		// create clusterjob
		// add excluded Pages
		// run merging
		// set rectangles
		// return data
		return null;
	}
}
