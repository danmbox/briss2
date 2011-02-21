/**
 * Copyright 2010 Gerhard Aigner
 * 
 * This file is part of BRISS.
 * 
 * BRISS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * BRISS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * BRISS. If not, see http://www.gnu.org/licenses/.
 */
package at.laborg.briss.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ClusterSet {
	private HashMap<Integer, SingleCluster> pagesToClusters;
	private HashMap<SingleCluster, List<Integer>> clustersToPages;
	private int pageCount;
	private boolean dirty;

	public ClusterSet(int pageCount) {
		this.dirty = true;
		this.pageCount = pageCount;
		this.pagesToClusters = new HashMap<Integer, SingleCluster>();
		this.clustersToPages = new HashMap<SingleCluster, List<Integer>>();
	}

	private <T extends Comparable<? super T>> List<T> asSortedList(
			Collection<T> c) {
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	public List<SingleCluster> getAsList() {
		return asSortedList(getClustersToPages().keySet());
	}

	public HashMap<Integer, SingleCluster> getPagesToClusters() {
		return pagesToClusters;
	}

	public HashMap<SingleCluster, List<Integer>> getClustersToPages() {
		return clustersToPages;
	}

	public int getPageCount() {
		return pageCount;
	}

	public SingleCluster getSingleCluster(int pageNumber) {
		if (dirty) {
			for (SingleCluster cluster : getClustersToPages().keySet()) {
				for (Integer page : getClustersToPages().get(cluster)) {
					getPagesToClusters().put(page - 1, cluster);
				}
			}
			dirty = false;
		}
		return getPagesToClusters().get(pageNumber - 1);
	}

	public void addPageToCluster(SingleCluster tmpCluster, int pageNumber) {
		if (getClustersToPages().containsKey(tmpCluster)) {
			// cluster exists
			List<Integer> pageNumbers = getClustersToPages().get(tmpCluster);
			pageNumbers.add(pageNumber);

		} else {
			// new Cluster
			List<Integer> pageNumbers = new ArrayList<Integer>();
			pageNumbers.add(pageNumber);
			getClustersToPages().put(tmpCluster, pageNumbers);
		}
		// whenever a page was added the pagesToClustersMapping isn't useful
		// anymore. This musst be handled when reading the pages
		dirty = true;
	}
}