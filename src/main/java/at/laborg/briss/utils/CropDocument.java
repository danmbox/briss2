// $Id: SingleCluster.java 55 2011-02-22 21:45:59Z laborg $
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
package at.laborg.briss.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import at.laborg.briss.model.ClusterCollection;
import at.laborg.briss.model.PageExcludes;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.SimpleBookmark;

public class CropDocument {

	private final File sourceFile;
	private final int sourcePageCount;
	private final HashMap<String, String> sourceMetaInfo;
	private final List<HashMap<String, Object>> sourceBookmarks;
	private final ClusterCollection clusterCollection;

	private PageExcludes pageExcludes;

	private CropDocument(File source, int pageCount,
			HashMap<String, String> metaInfo,
			List<HashMap<String, Object>> bookmarks,
			ClusterCollection clusterCollection) {
		this.sourceFile = source;
		this.sourcePageCount = pageCount;
		this.sourceMetaInfo = metaInfo;
		this.sourceBookmarks = bookmarks;
		this.clusterCollection = clusterCollection;
	}

	public static CropDocument createCropDoc(File source) throws IOException {
		if (source == null)
			throw new IllegalArgumentException("Source must be provided");
		if (!source.exists())
			throw new IllegalArgumentException("Source("
					+ source.getAbsolutePath() + ") file doesn't exist");

		PdfReader reader = new PdfReader(source.getAbsolutePath());
		CropDocument result = new CropDocument(source, reader
				.getNumberOfPages(), reader.getInfo(), SimpleBookmark
				.getBookmark(reader), new ClusterCollection());
		reader.close();
		return result;
	}

	public File getSourceFile() {
		return sourceFile;
	}

	public ClusterCollection getClusterCollection() {
		return clusterCollection;
	}

	public void setPageExcludes(PageExcludes pageExcludes) {
		this.pageExcludes = pageExcludes;
	}

	public PageExcludes getPageExcludes() {
		return pageExcludes;
	}

	public int getSourcePageCount() {
		return sourcePageCount;
	}

	public HashMap<String, String> getSourceMetaInfo() {
		return sourceMetaInfo;
	}

	public List<HashMap<String, Object>> getSourceBookmarks() {
		return sourceBookmarks;
	}

}
