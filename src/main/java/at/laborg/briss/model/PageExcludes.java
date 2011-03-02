package at.laborg.briss.model;

import java.util.Set;

public class PageExcludes {
	private final Set<Integer> excludedPageSet;

	public PageExcludes(Set<Integer> excludedPageSet) {
		this.excludedPageSet = excludedPageSet;
	}

	public Set<Integer> getExcludedPageSet() {
		return excludedPageSet;
	}

	public boolean containsPage(int page) {
		return excludedPageSet.contains(new Integer(page));
	}
}
