// $Id$
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
package at.laborg.briss;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import org.jpedal.exception.PdfException;

import at.laborg.briss.gui.JHelpDialog;
import at.laborg.briss.gui.MergedPanel;
import at.laborg.briss.gui.WrapLayout;
import at.laborg.briss.utils.PageNumberParser;

/**
 * 
 * @author gerhard
 * 
 */
@SuppressWarnings("serial")
public class Briss extends JFrame implements ActionListener,
		PropertyChangeListener {

	private static final String LOAD = "Load File";
	private static final String CROP = "Crop PDF";
	private static final String EXIT = "Exit";
	private static final String MAXIMIZE_WIDTH = "Maximize to width";
	private static final String MAXIMIZE_HEIGHT = "Maximize to height";
	private static final String RELOAD = "Reload PDF";
	private static final String PREVIEW = "Preview";
	private static final String DONATE = "Donate";
	private static final String HELP = "Show help";

	private static final String DONATION_URI_STRING = "http://sourceforge.net/project/project_donations.php?group_id=320676";
	private static final String RES_ICON_PATH = "/Briss_icon_032x032.gif";

	private JMenuBar menuBar;
	private JPanel previewPanel;
	private JProgressBar progressBar;
	private JMenuItem loadItem, cropItem, maxWItem, maxHItem, previewItem,
			helpItem, donateItem, reloadItem;
	private List<MergedPanel> mergedPanels = null;

	private File lastOpenDir;
	private File origFile = null;
	private File croppedFile = null;

	private ClusterJobData currentClusterJobData;

	public Briss() {
		super("BRISS - BRigt Snippet Sire");
		init();
	}

	public static void main(String args[]) {
		new Briss();
	}

	private File loadPDF(String recommendation, boolean saveDialog) {

		JFileChooser fc = lastOpenDir == null ? new JFileChooser()
				: new JFileChooser(lastOpenDir);

		if (recommendation != null) {
			File recommendedFile = new File(recommendation);
			fc.setSelectedFile(recommendedFile);
		}
		fc.setFileFilter(new FileFilter() {
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
		});

		int retval;
		if (saveDialog) {
			retval = fc.showSaveDialog(this);
		} else {
			retval = fc.showOpenDialog(this);
		}

		if (retval == JFileChooser.APPROVE_OPTION) {
			if (fc.getSelectedFile() != null) {
				lastOpenDir = fc.getSelectedFile().getParentFile();
			}
			return fc.getSelectedFile();
		}

		return null;
	}

	private void init() {

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException ex) {
			System.out.println("Unable to load native look and feel");
		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		}

		InputStream is = getClass().getResourceAsStream(RES_ICON_PATH);
		byte[] buf = new byte[1024 * 100];
		try {
			int cnt = is.read(buf);
			byte[] imgBuf = Arrays.copyOf(buf, cnt);
			setIconImage(new ImageIcon(imgBuf).getImage());
		} catch (IOException e) {
		}

		// Create the menu bar.
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu actionMenu = new JMenu("Action");

		menuBar.add(fileMenu);
		menuBar.add(actionMenu);

		loadItem = new JMenuItem(LOAD, KeyEvent.VK_L);
		loadItem.addActionListener(this);
		loadItem.setEnabled(true);
		fileMenu.add(loadItem);

		fileMenu.addSeparator();

		donateItem = new JMenuItem(DONATE);
		donateItem.addActionListener(this);
		fileMenu.add(donateItem);

		reloadItem = new JMenuItem(RELOAD);
		reloadItem.addActionListener(this);
		reloadItem.setEnabled(false);
		fileMenu.add(reloadItem);

		helpItem = new JMenuItem(HELP);
		helpItem.addActionListener(this);
		fileMenu.add(helpItem);

		fileMenu.addSeparator();

		JMenuItem menuItem = new JMenuItem(EXIT, KeyEvent.VK_E);
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);

		cropItem = new JMenuItem(CROP, KeyEvent.VK_C);
		cropItem.addActionListener(this);
		cropItem.setEnabled(false);
		actionMenu.add(cropItem);

		previewItem = new JMenuItem(PREVIEW, KeyEvent.VK_P);
		previewItem.addActionListener(this);
		previewItem.setEnabled(false);
		actionMenu.add(previewItem);

		maxWItem = new JMenuItem(MAXIMIZE_WIDTH, KeyEvent.VK_W);
		maxWItem.addActionListener(this);
		maxWItem.setEnabled(false);
		actionMenu.add(maxWItem);

		maxHItem = new JMenuItem(MAXIMIZE_HEIGHT, KeyEvent.VK_H);
		maxHItem.addActionListener(this);
		maxHItem.setEnabled(false);
		actionMenu.add(maxHItem);

		setJMenuBar(menuBar);

		previewPanel = new JPanel();
		previewPanel.setLayout(new WrapLayout(FlowLayout.LEFT));
		previewPanel.setEnabled(true);
		previewPanel.setBackground(Color.BLACK);

		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(400, 30));
		progressBar.setEnabled(true);

		JScrollPane scrollPane = new JScrollPane(previewPanel,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(30);
		add(scrollPane, BorderLayout.CENTER);
		add(progressBar, BorderLayout.PAGE_END);
		pack();
		setVisible(true);
	}

	private static Set<Integer> getExcludedPages() {
		boolean inputIsValid = false;
		String rememberInputString = "";
		// show dialog
		while (!inputIsValid) {
			String inputString = JOptionPane
					.showInputDialog(
							"Enter pages to be excluded from merging (e.g.: \"1-4;6;9\").\n"
									+ "First page has number: 1\n"
									+ "If you don't know what you should do just press \"Cancel\"",
							rememberInputString);
			rememberInputString = inputString;

			if (inputString == null || inputString.equals(""))
				return null;

			try {
				return PageNumberParser.parsePageNumber(inputString);
			} catch (ParseException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(),
						"Input Error", JOptionPane.ERROR_MESSAGE);
			}

		}
		return null;
	}

	public void actionPerformed(ActionEvent aE) {
		if (aE.getActionCommand().equals(DONATE)) {
			if (Desktop.isDesktopSupported()) {
				Desktop desktop = Desktop.getDesktop();
				URI donationURI;
				try {
					donationURI = new URI(DONATION_URI_STRING);
					desktop.browse(donationURI);
				} catch (URISyntaxException e) {
				} catch (IOException e) {
				}
			}
		} else if (aE.getActionCommand().equals(EXIT)) {
			System.exit(0);
		} else if (aE.getActionCommand().equals(HELP)) {
			new JHelpDialog(this, "Briss Help", Dialog.ModalityType.MODELESS);
		} else if (aE.getActionCommand().equals(MAXIMIZE_HEIGHT)) {
			// maximize to height
			// search for maximum height
			int maxHeight = -1;
			for (MergedPanel mp : mergedPanels) {
				int mpMax = mp.getHeighestRect();
				if (maxHeight < mpMax) {
					maxHeight = mpMax;
				}
			}
			// set maximum height to all rectangles
			if (maxHeight == -1)
				return;
			for (MergedPanel mp : mergedPanels) {
				mp.setSelCropHeight(maxHeight);
			}

		} else if (aE.getActionCommand().equals(MAXIMIZE_WIDTH)) {
			// maximize to width
			// search for maximum width
			int maxWidth = -1;
			for (MergedPanel mp : mergedPanels) {
				int mpMax = mp.getWidestRect();
				if (maxWidth < mpMax) {
					maxWidth = mpMax;
				}
			}
			// set maximum widt to all rectangles
			if (maxWidth == -1)
				return;
			for (MergedPanel mp : mergedPanels) {
				mp.setSelCropWidth(maxWidth);
			}
		} else if (aE.getActionCommand().equals(RELOAD)) {
			// reloadPDF with new excluded Pages
			// save the original crop rectangles
			Map<Integer, List<Float[]>> backupCropRectangle = ClusterManager
					.getCropRectangles(currentClusterJobData);

			// distribute the original crop rectangles
			if (origFile != null) {
				setTitle("BRISS - " + origFile.getName());
				try {
					currentClusterJobData = ClusterManager.createClusterJob(
							origFile, backupCropRectangle);
					currentClusterJobData
							.setExcludedPageSet(getExcludedPages());

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (PdfException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				previewPanel.removeAll();
				progressBar.setString("loading PDF");
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				ClusterPagesTask clusterTask = new ClusterPagesTask(
						currentClusterJobData);
				clusterTask.addPropertyChangeListener(this);
				clusterTask.execute();
			}
		} else if (aE.getActionCommand().equals(LOAD)) {
			File loadFile = loadPDF(null, false);
			if (loadFile != null) {
				setTitle("BRISS - " + loadFile.getName());
				origFile = loadFile;
				try {
					currentClusterJobData = ClusterManager
							.createClusterJob(origFile);
					currentClusterJobData
							.setExcludedPageSet(getExcludedPages());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (PdfException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				previewPanel.removeAll();
				progressBar.setString("loading PDF");
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				ClusterPagesTask clusterTask = new ClusterPagesTask(
						currentClusterJobData);
				clusterTask.addPropertyChangeListener(this);
				clusterTask.execute();
			}
		} else if (aE.getActionCommand().equals(CROP)) {
			// create file recommendation
			String origName = origFile.getAbsolutePath();
			String recommendedName = origName.substring(0,
					origName.length() - 4)
					+ "_cropped.pdf";
			croppedFile = loadPDF(recommendedName, true);
			if (croppedFile == null)
				return;
			if (!croppedFile.exists()) {
				try {
					croppedFile.createNewFile();
				} catch (IOException e) {
					// TODO show dialog
				}
			}
			progressBar.setString("loading PDF");
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			CropManager.crop(origFile, croppedFile, currentClusterJobData);
			progressBar.setValue(0);
			progressBar.setString("");
			if (Desktop.isDesktopSupported()) {
				try {
					Desktop.getDesktop().open(croppedFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

		} else if (aE.getActionCommand().equals(PREVIEW)) {
			try {
				// create temp file and show
				croppedFile = File.createTempFile("briss", ".pdf");
				if (croppedFile == null)
					return;
				if (!croppedFile.exists()) {
					try {
						croppedFile.createNewFile();
					} catch (IOException e) {
						// TODO show dialog
					}
				}
				progressBar.setString("loading PDF");
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				CropManager.crop(origFile, croppedFile, currentClusterJobData);
				progressBar.setValue(0);
				progressBar.setString("");
				if (Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().open(croppedFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			progressBar.setValue((Integer) evt.getNewValue());
		}
	}

	private class ClusterPagesTask extends SwingWorker<Void, Void> {

		private final ClusterJobData pdfCluster;

		public ClusterPagesTask(ClusterJobData pdfCluster) {
			super();
			this.pdfCluster = pdfCluster;
			progressBar.setString("Analysing PDF pages");
		}

		@Override
		protected void done() {
			setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			mergedPanels = new ArrayList<MergedPanel>();

			List<PageCluster> tmpClusterList = pdfCluster.getClusterAsList();

			for (PageCluster cluster : tmpClusterList) {
				MergedPanel p = new MergedPanel(cluster);
				previewPanel.add(p);
				mergedPanels.add(p);
			}
			progressBar
					.setString("PDF loaded - Select crop size and press crop");
			cropItem.setEnabled(true);
			maxWItem.setEnabled(true);
			maxHItem.setEnabled(true);
			reloadItem.setEnabled(true);
			previewItem.setEnabled(true);
			setProgress(0);
			pack();
			setExtendedState(Frame.MAXIMIZED_BOTH);
		}

		@Override
		protected Void doInBackground() {

			try {
				ClusterManager.clusterPages(pdfCluster);

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			int totWorkUnits = pdfCluster.getTotWorkUnits();
			ClusterManager.WorkerThread wT = new ClusterManager.WorkerThread(
					pdfCluster);
			wT.start();

			progressBar.setString("PDF analysed - creating merged previews");

			while (wT.isAlive()) {
				int percent = (int) ((wT.workerUnitCounter / (float) totWorkUnits) * 100);
				setProgress(percent);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}

			return null;
		}
	}

}
