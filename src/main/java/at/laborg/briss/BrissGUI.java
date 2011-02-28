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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import org.jpedal.exception.PdfException;

import at.laborg.briss.gui.HelpDialog;
import at.laborg.briss.gui.MergedPanel;
import at.laborg.briss.gui.WrapLayout;
import at.laborg.briss.model.CropDocument;
import at.laborg.briss.model.PageExcludes;
import at.laborg.briss.model.SingleCluster;
import at.laborg.briss.utils.DesktopHelper;
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

	private CropDocument cropDocumenti;

	public BrissGUI() {
		super("BRISS - BRight Snippet Sire");
		init();
	}

	private void init() {

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		this.setTransferHandler(new BrissTransferHandler(this));

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

	private static PageExcludes getExcludedPages() {
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
				PageExcludes pageExcludes = new PageExcludes(PageNumberParser.parsePageNumber(input));
				return pageExcludes;
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
			DesktopHelper.openDonationLink(DONATION_URI);
		} else if (action.getActionCommand().equals(EXIT)) {
			System.exit(0);
		} else if (action.getActionCommand().equals(HELP)) {
			new HelpDialog(this, "Briss Help", Dialog.ModalityType.MODELESS);
		} else if (action.getActionCommand().equals(MAXIMIZE_HEIGHT)) {
			maximizeHeightInSelectedRects();
		} else if (action.getActionCommand().equals(MAXIMIZE_WIDTH)) {
			maximizeWidthInSelectedRects();
		} else if (action.getActionCommand().equals(EXCLUDE_OTHER_PAGES)) {
			if (cropDocumenti.getSourceFile() == null)
				return;
			setWorkingState("Exclude other pages");
			try {
				excludeOtherPagesAndCluster();
				setTitle("BRISS - " + cropDocumenti.getSourceFile().getName());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (PdfException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (action.getActionCommand().equals(LOAD)) {
			File inputFile = getNewFileToCrop();
			if (inputFile == null)
				return;
			try {
				setWorkingState("Importing File");
				importNewPdfFile(inputFile);
				setTitle("BRISS - " + inputFile.getName());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (PdfException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (action.getActionCommand().equals(CROP)) {
			try {
				setWorkingState("loading PDF");
				File result = createAndExecuteCropJob(cropDocumenti.getSourceFile());
				DesktopHelper.openFileWithDesktopApp(result);
				setIdleState("");
				lastOpenDir = result.getParentFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (action.getActionCommand().equals(PREVIEW)) {
			try {
				setWorkingState("Creating and showing preview...");
				File result = createAndExecuteCropJobForPreview();
				DesktopHelper.openFileWithDesktopApp(result);
				setIdleState("");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private File createAndExecuteCropJobForPreview() throws IOException,
			DocumentException {
		File tmpCropFileDestination = File.createTempFile("briss", ".pdf");
		File result =  cropDocumenti.crop(tmpCropFileDestination);
		return result;
	}

	private File createAndExecuteCropJob(File source) throws IOException,
			DocumentException {
		File cropDestinationFile = getCropFileDestination(cropDocumenti
				.getRecommendedDestination());
		File result = cropDocumenti.crop(cropDestinationFile);
		return result;
	}

	private void setIdleState(String stateMessage) {
		progressBar.setValue(0);
		progressBar.setString(stateMessage);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

	private void setWorkingState(String stateMessage) {
		progressBar.setString(stateMessage);
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	}

	void importNewPdfFile(File loadFile) throws IOException, PdfException {

		lastOpenDir = loadFile.getParentFile();
		cropDocumenti = CropDocument.createCropDoc(loadFile);
		cropDocumenti.setPageExcludes(getExcludedPages());
		previewPanel.removeAll();
		ClusterPagesTask clusterTask = new ClusterPagesTask(cropDocumenti);
		clusterTask.addPropertyChangeListener(this);
		clusterTask.execute();
	}

	private void excludeOtherPagesAndCluster() throws IOException, PdfException {

		CropDocument newCropDoc = CropDocument.createCropDoc(cropDocumenti.getSourceFile());
		newCropDoc.setPageExcludes(getExcludedPages());
		previewPanel.removeAll();
		ClusterPagesTask clusterTask = new ClusterPagesTask(newCropDoc);
		clusterTask.addPropertyChangeListener(this);
		clusterTask.execute();
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

	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
			progressBar.setValue((Integer) evt.getNewValue());
		}
	}

	private void setStateAfterClusteringFinished(CropDocument newCropDocument) {

		previewPanel.removeAll();

		mergedPanels = new ArrayList<MergedPanel>();

		List<SingleCluster> allClusters = newCropDocument.getClusterCollection()
				.getAsList();

		for (SingleCluster cluster : allClusters) {
			// check if the previous cluster was just reloaded
			if (cropDocumenti != null
					&& cropDocumenti.getSourceFile().equals(
							newCropDocument.getSourceFile())) {

				for (Integer pageNumber : cluster.getAllPages()) {
					SingleCluster p = cropDocumenti.getClusterCollection()
							.getSingleCluster(pageNumber);
					for (Float[] ratios : p.getRatiosList()) {
						cluster.addRatios(ratios);
					}
				}
			}
			MergedPanel p = new MergedPanel(cluster);
			previewPanel.add(p);
			mergedPanels.add(p);
		}
		progressBar.setString("Clustering and Rendering finished");
		cropButton.setEnabled(true);
		maximizeWidthButton.setEnabled(true);
		maximizeHeightButton.setEnabled(true);
		excludePagesButton.setEnabled(true);
		showPreviewButton.setEnabled(true);
		setIdleState("");
		pack();
		setExtendedState(Frame.MAXIMIZED_BOTH);
		cropDocumenti = newCropDocument;
	}

	private class ClusterPagesTask extends SwingWorker<Void, Void> {

		private CropDocument cropDoc;

		public ClusterPagesTask(CropDocument cropDoc) {
			super();
			this.cropDoc = cropDoc;
			progressBar.setString("Analysing PDF pages");
		}

		@Override
		protected void done() {
			setStateAfterClusteringFinished(cropDoc);
		}

		@Override
		protected Void doInBackground() {

			try {
				cropDoc.clusterPages();

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			int totalWorkUnits = cropDoc.getTotalWorkUnits();
			
			CropDocument.ClusterRenderWorker renderWorker = new CropDocument.ClusterRenderWorker(
					cropDoc);
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
