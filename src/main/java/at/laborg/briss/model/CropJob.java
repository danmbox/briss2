package at.laborg.briss.model;

import java.io.File;
import java.util.HashMap;
import java.util.List;

public class CropJob {
	private final File source;
	private final int sourcePageCount;
	private final HashMap<String, String> sourceMetaInfo;
	private final List<HashMap<String, Object>> sourceBookmarks;
	private File destinationFile;

	private ClusterSet clusters;

	private static final String RECOMMENDED_ENDING = "_cropped.pdf";

	public CropJob(File source, int pageCount,
			HashMap<String, String> metaInfo,
			List<HashMap<String, Object>> bookmarks) {
		super();
		this.source = source;
		this.sourcePageCount = pageCount;
		this.sourceMetaInfo = metaInfo;
		this.sourceBookmarks = bookmarks;
	}

	public HashMap<String, String> getSourceMetaInfo() {
		return sourceMetaInfo;
	}

	public List<HashMap<String, Object>> getSourceBookmarks() {
		return sourceBookmarks;
	}

	public File getSource() {
		return source;
	}

	public int getPageCount() {
		return sourcePageCount;
	}

	public File getDestinationFile() {
		return destinationFile;
	}

	public void setDestinationFile(File destinationFile) {
		this.destinationFile = destinationFile;
	}

	public File getRecommendedDestination() {
		// create file recommendation
		String origName = getSource().getAbsolutePath();
		String recommendedName = origName.substring(0, origName.length() - 4)
				+ RECOMMENDED_ENDING;
		return new File(recommendedName);
	}

	public ClusterSet getClusters() {
		return clusters;
	}

	public void setClusters(ClusterSet clusters) {
		this.clusters = clusters;
	}

}
