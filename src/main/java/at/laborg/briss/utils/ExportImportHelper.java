package at.laborg.briss.utils;

import java.io.File;
import java.util.List;
import java.util.Set;

import at.laborg.briss.model.ClusterJob;
import at.laborg.briss.model.SingleCluster;

public class ExportImportHelper {

	public static String export(ClusterJob clusterJob) {

		StringBuffer result = new StringBuffer();
		result.append(exportExcludePages(clusterJob.getExcludedPageSet())
				+ "\n");
		for (SingleCluster cluster : clusterJob.getClusters().getAsList()) {
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

	public static ClusterJob importClusterJobData(File pdf, File inFileText) {
		// create clusterjob
		// add excluded Pages
		// run merging
		// set rectangles
		// return data
		return null;
	}
}
