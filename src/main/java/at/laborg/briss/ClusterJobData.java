package at.laborg.briss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClusterJobData {

	private final HashMap<Integer, PageCluster> pagesToClusters;
	private final HashMap<PageCluster, List<Integer>> clustersToPages;
	private final int pageCount;
	private Set<Integer> excludedPageSet;
	private final String fullFilePath;
	private Map<Integer, List<Float[]>> preFilledCropRectangles;
	private boolean dirty;

	public ClusterJobData(int pageCount, String fullFilePath) {
		this.dirty = true;
		this.pageCount = pageCount;
		this.pagesToClusters = new HashMap<Integer, PageCluster>();
		this.clustersToPages = new HashMap<PageCluster, List<Integer>>();
		this.fullFilePath = fullFilePath;
	}

	public ClusterJobData(int pageCount, String fullFilePath,
			Map<Integer, List<Float[]>> preFilledCropRectangles) {
		this(pageCount, fullFilePath);
		this.preFilledCropRectangles = preFilledCropRectangles;
	}

	public Map<Integer, List<Float[]>> getPreFilledCropRectangles() {
		return preFilledCropRectangles;
	}

	public HashMap<Integer, PageCluster> getPagesToClusters() {
		return pagesToClusters;
	}

	public HashMap<PageCluster, List<Integer>> getClustersToPages() {
		return clustersToPages;
	}

	public int getPageCount() {
		return pageCount;
	}

	public Set<Integer> getExcludedPageSet() {
		return excludedPageSet;
	}

	public void setExcludedPageSet(Set<Integer> excludedPageSet) {
		this.excludedPageSet = excludedPageSet;
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public String getFullFilePath() {
		return fullFilePath;
	}

	private <T extends Comparable<? super T>> List<T> asSortedList(
			Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	public List<PageCluster> getClusterAsList() {
		return asSortedList(clustersToPages.keySet());
	}

	public Set<PageCluster> getClusters() {
		return clustersToPages.keySet();
	}

	public int getTotWorkUnits() {
		int size = 0;
		for (PageCluster cluster : getClustersToPages().keySet()) {
			size += cluster.getPagesToMerge().size();
		}
		return size;
	}

}