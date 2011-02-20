package at.laborg.briss.model;

import java.io.File;
import java.util.Set;

public class ClusterJob {

	private Set<Integer> excludedPageSet;
	private final File source;
	private final ClusterSet clusters;

	public ClusterJob(int pageCount, File inFile) {
		clusters = new ClusterSet(pageCount);
		this.source = inFile;
	}

	public ClusterSet getClusters() {
		return clusters;
	}

	public Set<Integer> getExcludedPageSet() {
		return excludedPageSet;
	}

	public void setExcludedPageSet(Set<Integer> excludedPageSet) {
		this.excludedPageSet = excludedPageSet;
	}

	public File getSource() {
		return source;
	}

	public int getTotalWorkUnits() {
		int size = 0;
		for (SingleCluster cluster : clusters.getAsList()) {
			size += cluster.getPagesToMerge().size();
		}
		return size;
	}

}