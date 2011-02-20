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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
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
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jpedal.exception.PdfException;

import at.laborg.briss.gui.HelpDialog;
import at.laborg.briss.gui.MergedPanel;
import at.laborg.briss.gui.WrapLayout;
import at.laborg.briss.model.ClusterJob;
import at.laborg.briss.model.CropJob;
import at.laborg.briss.model.SingleCluster;
import at.laborg.briss.utils.PDFFileFilter;
import at.laborg.briss.utils.PageNumberParser;

import com.itextpdf.text.DocumentException;

/**
 * 
 * @author gerhard
 * 
 */
@SuppressWarnings("serial")
public class BrissGUI extends JFrame implements ActionListener,
		PropertyChangeListener {

	private static final String EXCLUDE_PAGES_DESCRIPTION = "Enter pages to be excluded from merging (e.g.: \"1-4;6;9\").\n"
			+ "First page has number: 1\n"
			+ "If you don't know what you should do just press \"Cancel\"";
	private static final String LOAD = "Load File";
	private static final String CROP = "Crop PDF";
	private static final String EXIT = "Exit";
	private static final String MAXIMIZE_WIDTH = "Maximize to width";
	private static final String MAXIMIZE_HEIGHT = "Maximize to height";
	private static final String EXCLUDE_OTHER_PAGES = "Exclude other pages";
	private static final String PREVIEW = "Preview";
	private static final String DONATE = "Donate";
	private static final String HELP = "Show help";

	private static final String DONATION_URI = "http://sourceforge.net/project/project_donations.php?group_id=320676";
	private static final String RES_ICON_PATH = "/Briss_icon_032x032.gif";

	private JMenuBar menuBar;
	private JPanel previewPanel;
	private JProgressBar progressBar;
	private JMenuItem loadButton, cropButton, maximizeWidthButton,
			maximizeHeightButton, showPreviewButton, showHelpButton,
			openDonationLinkButton, excludePagesButton;
	private List<MergedPanel> mergedPanels = null;

	private File lastOpenDir;

	private ClusterJob curClusterJob;

	public BrissGUI() {
		super("BRISS - BRight Snippet Sire");
		init();
	}

	private void init() {

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		this.setTransferHandler(new BrissTransferHandler());

		setUILook();

		loadIcon();

		// Create the menu bar.
		menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");
		JMenu actionMenu = new JMenu("Action");

		menuBar.add(fileMenu);
		menuBar.add(actionMenu);

		loadButton = new JMenuItem(LOAD, KeyEvent.VK_L);
		loadButton.addActionListener(this);
		loadButton.setEnabled(true);
		fileMenu.add(loadButton);

		fileMenu.addSeparator();

		openDonationLinkButton = new JMenuItem(DONATE);
		openDonationLinkButton.addActionListener(this);
		fileMenu.add(openDonationLinkButton);

		excludePagesButton = new JMenuItem(EXCLUDE_OTHER_PAGES);
		excludePagesButton.addActionListener(this);
		excludePagesButton.setEnabled(false);
		fileMenu.add(excludePagesButton);

		showHelpButton = new JMenuItem(HELP);
		showHelpButton.addActionListener(this);
		fileMenu.add(showHelpButton);

		fileMenu.addSeparator();

		JMenuItem menuItem = new JMenuItem(EXIT, KeyEvent.VK_E);
		menuItem.addActionListener(this);
		fileMenu.add(menuItem);

		cropButton = new JMenuItem(CROP, KeyEvent.VK_C);
		cropButton.addActionListener(this);
		cropButton.setEnabled(false);
		actionMenu.add(cropButton);

		showPreviewButton = new JMenuItem(PREVIEW, KeyEvent.VK_P);
		showPreviewButton.addActionListener(this);
		showPreviewButton.setEnabled(false);
		actionMenu.add(showPreviewButton);

		maximizeWidthButton = new JMenuItem(MAXIMIZE_WIDTH, KeyEvent.VK_W);
		maximizeWidthButton.addActionListener(this);
		maximizeWidthButton.setEnabled(false);
		actionMenu.add(maximizeWidthButton);

		maximizeHeightButton = new JMenuItem(MAXIMIZE_HEIGHT, KeyEvent.VK_H);
		maximizeHeightButton.addActionListener(this);
		maximizeHeightButton.setEnabled(false);
		actionMenu.add(maximizeHeightButton);

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

	private void setUILook() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException ex) {
			System.out.println("Unable to load native look and feel");
		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		}
	}

	private void loadIcon() {
		InputStream is = getClass().getResourceAsStream(RES_ICON_PATH);
		byte[] buf = new byte[1024 * 100];
		try {
			int cnt = is.read(buf);
			byte[] imgBuf = Arrays.copyOf(buf, cnt);
			setIconImage(new ImageIcon(imgBuf).getImage());
		} catch (IOException e) {
		}
	}

	private static Set<Integer> getExcludedPages() {
		boolean inputIsValid = false;
		String previousInput = "";

		// repeat show_dialog until valid input or canceled
		while (!inputIsValid) {
			String input = JOptionPane.showInputDialog(
					EXCLUDE_PAGES_DESCRIPTION, previousInput);
			previousInput = input;

			if (input == null || input.equals(""))
				return null;

			try {
				return PageNumberParser.parsePageNumber(input);
			} catch (ParseException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(),
						"Input Error", JOptionPane.ERROR_MESSAGE);
			}

		}
		return null;
	}

	private File getCropFileDestination(File recommendation) {

		JFileChooser fc = new JFileChooser(lastOpenDir);
		fc.setSelectedFile(recommendation);
		fc.setFileFilter(new PDFFileFilter());
		int retval = fc.showSaveDialog(this);

		if (retval == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFile();

		return null;
	}

	private File getNewFileToCrop() {

		JFileChooser fc = new JFileChooser(lastOpenDir);
		fc.setFileFilter(new PDFFileFilter());
		int retval = fc.showOpenDialog(this);

		if (retval == JFileChooser.APPROVE_OPTION)
			return fc.getSelectedFile();

		return null;
	}

	public void actionPerformed(ActionEvent action) {
		if (action.getActionCommand().equals(DONATE)) {
			openDonationLink();
		} else if (action.getActionCommand().equals(EXIT)) {
			System.exit(0);
		} else if (action.getActionCommand().equals(HELP)) {
			new HelpDialog(this, "Briss Help", Dialog.ModalityType.MODELESS);
		} else if (action.getActionCommand().equals(MAXIMIZE_HEIGHT)) {
			maximizeHeightInSelectedRects();
		} else if (action.getActionCommand().equals(MAXIMIZE_WIDTH)) {
			maximizeWidthInSelectedRects();
		} else if (action.getActionCommand().equals(EXCLUDE_OTHER_PAGES)) {
			excludeOtherPages();
		} else if (action.getActionCommand().equals(LOAD)) {
			File inputFile = getNewFileToCrop();
			importNewPdfFile(inputFile);
		} else if (action.getActionCommand().equals(CROP)) {
			cropFile();
		} else if (action.getActionCommand().equals(PREVIEW)) {
			createAndShowPreview();
		}
	}

	private void createAndShowPreview() {
		setLongRunningState("Creating and showing preview...");
		// create temp file and show
		CropJob cropJob;
		File tmpCropFileDestination = null;
		try {
			tmpCropFileDestination = File.createTempFile("briss", ".pdf");
			if (tmpCropFileDestination == null)
				return;
			if (!tmpCropFileDestination.exists()) {
				tmpCropFileDestination.createNewFile();
			}
			cropJob = CropManager.createCropJob(curClusterJob.getSource());
			cropJob.setDestinationFile(tmpCropFileDestination);
			cropJob.setClusters(curClusterJob.getClusters());

			CropManager.crop(cropJob);
		} catch (DocumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(tmpCropFileDestination);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		setActiveState("");
	}

	private void cropFile() {
		setLongRunningState("loading PDF");
		CropJob cropJob = null;
		try {
			cropJob = CropManager.createCropJob(curClusterJob.getSource());
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		File recommendedDestinationFile = cropJob.getRecommendedDestination();
		File cropDestinationFile = getCropFileDestination(recommendedDestinationFile);
		if (cropDestinationFile == null)
			return;
		lastOpenDir = cropDestinationFile.getParentFile();
		try {
			if (!cropDestinationFile.exists()) {
				cropDestinationFile.createNewFile();
			}
			cropJob.setDestinationFile(cropDestinationFile);
			cropJob.setClusters(curClusterJob.getClusters());
			CropManager.crop(cropJob);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (DocumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().open(cropDestinationFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		setActiveState("");

	}

	private void setActiveState(String stateMessage) {
		progressBar.setValue(0);
		progressBar.setString(stateMessage);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void setLongRunningState(String stateMessage) {
		progressBar.setString(stateMessage);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	}

	private void importNewPdfFile(File loadFile) {
		if (loadFile == null)
			return;
		setLongRunningState("Importing File");
		lastOpenDir = loadFile.getParentFile();
		setTitle("BRISS - " + loadFile.getName());
		try {
			curClusterJob = ClusterManager.createClusterJob(loadFile);
			curClusterJob.setExcludedPageSet(getExcludedPages());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PdfException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		previewPanel.removeAll();
		ClusterPagesTask clusterTask = new ClusterPagesTask(curClusterJob);
		clusterTask.addPropertyChangeListener(this);
		clusterTask.execute();
	}

	private void excludeOtherPages() {
		// reloadPDF with new excluded Pages
		if (curClusterJob.getSource() == null)
			return; // nothing todo
		setLongRunningState("Exclude other pages");

		setTitle("BRISS - " + curClusterJob.getSource().getName());
		try {
			ClusterJob newClusterJob = ClusterManager
					.createClusterJob(curClusterJob.getSource());
			newClusterJob.setExcludedPageSet(getExcludedPages());
			previewPanel.removeAll();
			ClusterPagesTask clusterTask = new ClusterPagesTask(newClusterJob);
			clusterTask.addPropertyChangeListener(this);
			clusterTask.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (PdfException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void maximizeWidthInSelectedRects() {
		// maximize to width
		// search for maximum width
		int maxWidth = -1;
		for (MergedPanel panel : mergedPanels) {
			int panelMaxWidth = panel.getWidestSelectedRect();
			if (maxWidth < panelMaxWidth) {
				maxWidth = panelMaxWidth;
			}
		}
		// set maximum widt to all rectangles
		if (maxWidth == -1)
			return;
		for (MergedPanel mp : mergedPanels) {
			mp.setSelCropWidth(maxWidth);
		}
	}

	private void maximizeHeightInSelectedRects() {
		// maximize to height
		// search for maximum height
		int maxHeight = -1;
		for (MergedPanel panel : mergedPanels) {
			int panelMaxHeight = panel.getHeighestSelectedRect();
			if (maxHeight < panelMaxHeight) {
				maxHeight = panelMaxHeight;
			}
		}
		// set maximum height to all rectangles
		if (maxHeight == -1)
			return;
		for (MergedPanel mp : mergedPanels) {
			mp.setSelCropHeight(maxHeight);
		}
	}

	private void openDonationLink() {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			URI donationURI;
			try {
				donationURI = new URI(DONATION_URI);
				desktop.browse(donationURI);
			} catch (URISyntaxException e) {
			} catch (IOException e) {
			}
		}
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			progressBar.setValue((Integer) evt.getNewValue());
		}
	}

	private static double sd(int[][][] v, int xs, int w, int ys, int h) {
		double sum = 0;
		for (int i = xs; i < xs + w; i++) {
			for (int j = ys; j < ys + h; j++) {
				for (int k = 0; k < v[0][0].length; k++) {
					sum += v[i][j][k];
				}
			}
		}
		double mean = sum / (w * h * v[0][0].length);

		sum = 0;
		for (int i = xs; i < xs + w; i++) {
			for (int j = ys; j < ys + h; j++) {
				for (int k = 0; k < v[0][0].length; k++) {
					sum += (v[i][j][k] - mean) * (v[i][j][k] - mean);
				}
			}
		}
		double sd = Math.sqrt(sum / w * h * v[0][0].length);
		return sd;
	}

	private void setStateAfterClusteringFinished(ClusterJob newClusterJob) {

		// remove old stuff
		previewPanel.removeAll();

		// create merged panels
		mergedPanels = new ArrayList<MergedPanel>();

		List<SingleCluster> allClusters = newClusterJob.getClusters()
				.getAsList();

		for (SingleCluster cluster : allClusters) {
			// check if the previous cluster was just reloaded
			if (curClusterJob != null
					&& curClusterJob.getSource().equals(
							newClusterJob.getSource())) {

				for (Integer pageNumber : cluster.getAllPages()) {
					SingleCluster p = curClusterJob.getClusters().getSingleCluster(pageNumber);
					for (Float[] ratios : p.getRatiosList()) {
						cluster.addRatios(ratios);
					}
				}
			}
			MergedPanel p = new MergedPanel(cluster);
			previewPanel.add(p);
			mergedPanels.add(p);

			BufferedImage bI = cluster.getPreviewImage();
			WritableRaster r = bI.getRaster();
			int[] tmp = null;
			int[][] data = new int[r.getWidth()][r.getHeight()];
			for (int i = 0; i < r.getWidth(); i++) {
				for (int j = 0; j < r.getHeight(); j++) {
					data[i][j] = r.getPixel(i, j, tmp)[0];
				}
			}

			int[] putter = new int[100];
			for (int i = 0; i < putter.length; i++) {
				putter[i] = 233;
			}
			int[][][] hihi = TMPC.imgdata;

			// TODO !!!!
			// ausreisser entfernen... ... .. .mmmmhhhmmm

			// reduce

			// for (int i = 0; i < hihi.length; i++) {
			// for (int j = 0; j < hihi[0].length; j++) {
			// double[] sdval = new double[1];
			// sdval[0] = sd(hihi[i][j]);
			// r.setPixel(i, j, sdval);
			// }
			// }

			// int max = 0;
			// for (int i = 0; i < r.getWidth(); i++) {
			// for (int j = 0; j < r.getHeight(); j++) {
			// if (r.getPixel(i, j, tmp)[0] > max) {
			// max = r.getPixel(i, j, tmp)[0];
			// }
			// }
			// }
			// float ratio = 255/max;
			// for (int i = 0; i < r.getWidth(); i++) {
			// for (int j = 0; j < r.getHeight(); j++) {
			// int[] vali = r.getPixel(i, j, tmp);
			// vali[0]=(int) (vali[0]*ratio);
			// // sdval[0] = sd(hihi[i][j]);
			// r.setPixel(i, j, vali);
			// }
			// }
			//			 
			// for (int i = 0; i < wi; i++) {
			// for (int j = 0; j < hi; j++) {
			// // double sdblock = sd(r
			// // .getPixels(i * 10, j * 10, 10, 10, tmp));
			// // double sdblock = sd()
			//					
			//
			// // for (int k = 0; k < putter.length; k++) {
			// // putter[i] = (int) sdblock;
			// // }
			// for (int k = i * 10; k < (i + 1) * 10; k++) {
			// for (int l = j * 10; l < (j + 1) * 10; l++) {
			// data[k][l] = (int) sdblock;
			// }
			// }
			// // System.out.printf("%4.1f ",);
			// // if (sdblock > 3) {
			// // r.setPixels(i*10, j*10, 10, 10, putter);
			// // }
			// }
			// System.out.println("");
			// }

			for (int i = 0; i < (r.getWidth() / 10) - 1; i++) {
				for (int j = 0; j < (r.getHeight() / 10) - 1; j++) {
					int[] vali = new int[1];
					// vali[0] = data[i][j];
					vali = new int[100];
					int ssssd = (int) sd(hihi, i * 10, 10, j * 10, 10);
					for (int s = 0; s < 100; s++) {
						vali[s] = ssssd;
					}
					// vali[0] = (int) sd(hihi,i*10,10,j*10,10);
					// vali[0]=(int) (vali[0]*ratio);
					// sdval[0] = sd(hihi[i][j]);
					r.setPixels(i * 10, j * 10, 10, 10, vali);
				}
			}
			int max = 1;
			for (int i = 0; i < r.getWidth(); i++) {
				for (int j = 0; j < r.getHeight(); j++) {
					if (r.getPixel(i, j, tmp)[0] > max) {
						max = r.getPixel(i, j, tmp)[0];
					}
				}
			}
			float ratio = 255 / max;
			for (int i = 0; i < r.getWidth(); i++) {
				for (int j = 0; j < r.getHeight(); j++) {
					int[] vali = r.getPixel(i, j, tmp);
					vali[0] = (int) (vali[0] * ratio);
					r.setPixel(i, j, vali);
				}
			}
		}
		progressBar.setString("Clustering and Rendering finished");
		cropButton.setEnabled(true);
		maximizeWidthButton.setEnabled(true);
		maximizeHeightButton.setEnabled(true);
		excludePagesButton.setEnabled(true);
		showPreviewButton.setEnabled(true);
		setActiveState("");
		pack();
		setExtendedState(Frame.MAXIMIZED_BOTH);
		curClusterJob = newClusterJob;
	}

	private final class BrissTransferHandler extends TransferHandler {

		@Override
		public boolean canImport(TransferSupport support) {
			if (!support.isDataFlavorSupported(DataFlavor.stringFlavor))
				return false;
			return true;

		}

		@Override
		public boolean importData(TransferSupport support) {
			if (!canImport(support))
				return false;

			// Fetch the Transferable and its data
			Transferable t = support.getTransferable();
			try {
				String dropInput = (String) t
						.getTransferData(DataFlavor.stringFlavor);

				String[] filenames = dropInput.split("\n");

				for (String filename : filenames) {
					if (filename.trim().endsWith(".pdf")) {
						File loadFile = new File(filename);
						importNewPdfFile(loadFile);
						break;
					}
				}

			} catch (UnsupportedFlavorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return true;
		}
	}

	private class ClusterPagesTask extends SwingWorker<Void, Void> {

		private final ClusterJob cropJob;

		public ClusterPagesTask(ClusterJob cropJob) {
			super();
			this.cropJob = cropJob;
			progressBar.setString("Analysing PDF pages");
		}

		@Override
		protected void done() {
			setStateAfterClusteringFinished(cropJob);
		}

		@Override
		protected Void doInBackground() {

			try {
				ClusterManager.clusterPages(cropJob);

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			int totalWorkUnits = cropJob.getTotalWorkUnits();
			ClusterManager.ClusterRenderWorker renderWorker = new ClusterManager.ClusterRenderWorker(
					cropJob);
			renderWorker.start();

			progressBar.setString("PDF analysed - creating merged previews");

			while (renderWorker.isAlive()) {
				int percent = (int) ((renderWorker.workerUnitCounter / (float) totalWorkUnits) * 100);
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
