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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

@SuppressWarnings("serial")
public class MergedPanel extends JPanel implements MouseMotionListener,
		MouseListener {

	private static final int MINIMUM_WIDTH = 20;
	private static final int MINIMUM_HEIGHT = 20;
	private BufferedImage img;
	private int tmpCropXStart, tmpCropXEnd, tmpCropYStart, tmpCropYEnd;
	private static Composite composite = AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, .2f);
	private List<CropRectangle> crops = new ArrayList<CropRectangle>();
	private PDFPageCluster cluster;

	public MergedPanel(PDFPageCluster cluster) {
		super();
		this.cluster = cluster;
		this.img = cluster.getPreviewImage();
		setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
		addMouseMotionListener(this);
		addMouseListener(this);

		setToolTipText(createInfoString(cluster));
	}

	private String createInfoString(PDFPageCluster cluster) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(cluster.isEvenPage() ? "Even " : "Odd ").append("page<br>");
		sb.append(cluster.getAllPages().size() + " pages: ");
		int pagecounter = 0;
		for (Integer pageNumber : cluster.getAllPages()) {
			sb.append(pageNumber + " ");
			if (pagecounter++ > 10) {
				pagecounter = 0;
				sb.append("<br>");
			}
		}
		sb.append("</html>");
		return sb.toString();
	}

	@Override
	public void paint(Graphics g) {
		update(g);
	}

	@Override
	public void update(Graphics g) {
		if (!isEnabled())
			return;

		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(img, null, 0, 0);

		// draw previously created rectangles
		int cropCnt = 0;
		Font currentFont = g2.getFont();
		g2.setComposite(composite);
		for (CropRectangle crop : crops) {
			g2.setColor(Color.BLUE);
			g2.fillRect(crop.xStart, crop.yStart, crop.width, crop.height);

			g2.setColor(Color.BLACK);
			Font scaledFont = scaleFont(String.valueOf(cropCnt++), crop
					.getRectangle(), g2, currentFont);
			g2.setFont(scaledFont);
			g2.drawString(String.valueOf(cropCnt), crop.xStart, crop.yStart
					+ crop.height);
		}
		g2.setComposite(composite);
		g2.setColor(Color.BLUE);

		if (tmpCropXStart < tmpCropXEnd) {
			if (tmpCropYStart < tmpCropYEnd) {
				g2.fillRect(tmpCropXStart, tmpCropYStart, tmpCropXEnd
						- tmpCropXStart, tmpCropYEnd - tmpCropYStart);
			} else {
				g2.fillRect(tmpCropXStart, tmpCropYEnd, tmpCropXEnd
						- tmpCropXStart, tmpCropYStart - tmpCropYEnd);
			}
		} else {
			if (tmpCropYStart < tmpCropYEnd) {
				g2.fillRect(tmpCropXEnd, tmpCropYStart, tmpCropXStart
						- tmpCropXEnd, tmpCropYEnd - tmpCropYStart);
			} else {
				g2.fillRect(tmpCropXEnd, tmpCropYEnd, tmpCropXStart
						- tmpCropXEnd, tmpCropYStart - tmpCropYEnd);
			}
		}
		g2.dispose();

	}

	@Override
	public void mouseDragged(MouseEvent mE) {
		if ((tmpCropXStart != -1) && (tmpCropYStart != -1)) {
			tmpCropXEnd = mE.getX();
			tmpCropYEnd = mE.getY();
			repaint();
		}
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {

	}

	@Override
	public void mouseClicked(MouseEvent mE) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent mE) {
		if (mE.getButton() == MouseEvent.BUTTON1) {
			tmpCropXStart = mE.getX();
			tmpCropYStart = mE.getY();
		} else {
			tmpCropXEnd = -1;
			tmpCropXStart = -1;
			tmpCropYEnd = -1;
			tmpCropYStart = -1;

			crops.clear();
			cluster.clearRatios();
			repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// add croprectangle to list
		if (tmpCropXStart != -1 && tmpCropYStart != -1) {

			crops.add(new CropRectangle(tmpCropXStart, tmpCropYStart,
					tmpCropXEnd, tmpCropYEnd));
			cluster.addRatios(getCutRatiosForPdf());
		}
		tmpCropXEnd = -1;
		tmpCropXStart = -1;
		tmpCropYEnd = -1;
		tmpCropYStart = -1;

		repaint();
	}

	/**
	 * creates the crop ratios from the user selection. 0 = left 1 = bottom 2 =
	 * right 3 = top
	 * 
	 * @return the cropped ratios or null if to small
	 */
	private Float[] getCutRatiosForPdf() {
		int x1, x2, y1, y2;
		// x1 should always be smaller than x2
		if (tmpCropXStart > tmpCropXEnd) {
			x1 = tmpCropXEnd;
			x2 = tmpCropXStart;
		} else {
			x1 = tmpCropXStart;
			x2 = tmpCropXEnd;
		}
		// y1 should always be smaller than y2
		if (tmpCropYStart > tmpCropYEnd) {
			y1 = tmpCropYEnd;
			y2 = tmpCropYStart;
		} else {
			y1 = tmpCropYStart;
			y2 = tmpCropYEnd;
		}

		// check for maximum and minimum
		if (x1 < 0) {
			x1 = 0;
		}
		if (x2 > img.getWidth()) {
			x2 = img.getWidth();
		}
		if (y1 < 0) {
			y1 = 0;
		}
		if (y2 > img.getHeight()) {
			y2 = img.getHeight();
		}

		if ((x2 - x1) < MINIMUM_WIDTH) {
			return null;
		}
		if ((y2 - y1) < MINIMUM_HEIGHT) {
			return null;
		}

		Float[] ratios = new Float[4];
		// left
		ratios[0] = (float) x1 / img.getWidth();
		// bottom
		ratios[1] = (float) (img.getHeight() - y2) / img.getHeight();
		// right
		ratios[2] = 1 - ((float) x2 / img.getWidth());
		// top
		ratios[3] = 1 - ((float) (img.getHeight() - y1) / img.getHeight());

		return ratios;
	}

	private Font scaleFont(String text, Rectangle rect, Graphics g, Font pFont) {

		Font font = pFont;
		float fontSize = (float) font.getSize();
		font = g.getFont().deriveFont(fontSize);
		int width = g.getFontMetrics(font).stringWidth(text);
		int height = g.getFontMetrics(font).getHeight();
		float scaleFactorWidth = rect.width / width;
		float scaleFactorHeight = rect.height / height;
		float scaledWidth = (scaleFactorWidth * fontSize) * 0.7f;
		float scaledHeight = (scaleFactorHeight * fontSize) * 0.7f;
		return g.getFont().deriveFont(
				(scaleFactorHeight > scaleFactorWidth) ? scaledWidth
						: scaledHeight);
	}

	private class CropRectangle {
		private int xStart, yStart, width, height;

		/**
		 * Creates a rectangle defined by top left and bottom right corner with
		 * the origin in top left corner of the component
		 * 
		 * @param x1
		 * @param y1
		 * @param x2
		 * @param y2
		 */
		public CropRectangle(int x1, int y1, int x2, int y2) {
			super();

			// x1 should always be smaller than x2
			if (x1 > x2) {
				this.xStart = x2;
				this.width = x1 - x2;
			} else {
				this.xStart = x1;
				this.width = x2 - x1;
			}
			// y1 should always be smaller than y2
			if (y1 > y2) {
				this.yStart = y2;
				this.height = y1 - y2;
			} else {
				this.yStart = y1;
				this.height = y2 - y1;
			}
		}

		private Rectangle getRectangle() {
			return new Rectangle(xStart, yStart, width, height);
		}

	}
}
