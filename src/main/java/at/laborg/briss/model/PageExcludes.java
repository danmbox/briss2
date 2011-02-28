package at.laborg.briss.model;

import java.util.Set;

public class PageExcludes {
	private Set<Integer> excludedPageSet;
	public PageExcludes(Set<Integer> excludedPageSet) {
		this.excludedPageSet = excludedPageSet;
	}
	public Set<Integer> getExcludedPageSet() {
		return excludedPageSet;
	}

}
