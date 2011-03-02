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

import java.io.IOException;

import at.laborg.briss.model.PageExcludes;
import at.laborg.briss.model.SingleCluster;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;

public class ClusterCreator {

	public static void clusterPages(CropDocument cropDoc) throws IOException {
		PdfReader reader = new PdfReader(cropDoc.getSourceFile()
				.getAbsolutePath());

		for (int page = 1; page <= reader.getNumberOfPages(); page++) {

			Rectangle layoutBox = getLayoutBox(reader, page);

			// create Cluster
			// if the pagenumber should be excluded then use it as a
			// discriminating parameter, else use default value

			boolean excluded = checkExclusionAndGetPageNumber(cropDoc
					.getPageExcludes(), page);

			SingleCluster tmpCluster = new SingleCluster(page % 2 == 0,
					(int) layoutBox.getWidth(), (int) layoutBox.getHeight(),
					excluded, page);

			cropDoc.getClusterCollection().addOrMergeCluster(tmpCluster);
		}
		reader.close();
		cropDoc.getClusterCollection().selectAndSetPagesForMerging();
	}

	private static Rectangle getLayoutBox(PdfReader reader, int page) {
		Rectangle layoutBox = reader.getBoxSize(page, "crop");

		if (layoutBox == null) {
			layoutBox = reader.getBoxSize(page, "media");
		}
		return layoutBox;
	}

	private static boolean checkExclusionAndGetPageNumber(
			PageExcludes pageExcludes, int page) {
		return (pageExcludes != null && pageExcludes.containsPage(page));
	}
}