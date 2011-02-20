package at.laborg.briss.utils;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class PDFFileFilter extends FileFilter {
	@Override
	public boolean accept(File pathname) {
		if (pathname.isDirectory())
			return true;
		return pathname.toString().toLowerCase().endsWith(".pdf");
	}

	@Override
	public String getDescription() {
		return null;
	}
}
