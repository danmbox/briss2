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

package at.laborg.briss.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import at.laborg.briss.model.SingleCluster;

@SuppressWarnings("serial")
public class MergedPanel extends JPanel implements MouseMotionListener,
		MouseListener {

	// last drawn rectangle. a "ghosting" rectangle will
	// help the user to create the two equally sized crop rectangles
	private static CropRect lastCrop;
	private static CropRect curCrop;
	private static Point lastDragPoint;
	private static Point cropStartPoint;
	private static int dragCropIndex = -1;

	private final static Composite compositeSmooth = AlphaComposite
			.getInstance(AlphaComposite.SRC_OVER, .2f);
	private final static Composite compositeXor = AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, .8f);

	private final List<CropRect> crops = new ArrayList<CropRect>();
	private final SingleCluster cluster;
	private final BufferedImage img;

	public MergedPanel(SingleCluster cluster) {
		super();
		this.cluster = cluster;
		this.img = cluster.getPreviewImage();
		setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
		if (cluster.isFunctional()) {
			addMouseMotionListener(this);
			addMouseListener(this);
		}
		addRatiosAsCrops(cluster.getRatiosList());
		setToolTipText(createInfoString(cluster));
	}

	private void addRatiosAsCrops(List<Float[]> ratiosList) {
		for (Float[] ratios : cluster.getRatiosList()) {
			CropRect rect = new CropRect();
			rect.x = (int) (img.getWidth() * ratios[0]);
			rect.y = (int) (img.getHeight() * ratios[3]);
			rect.width = (int) (img.getWidth() * (1 - (ratios[0] + ratios[2])));
			rect.height = (int) (img.getHeight() * (1 - (ratios[1] + ratios[3])));
			crops.add(rect);
		}
	}

	private String createInfoString(SingleCluster cluster) {
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
		g2.setComposite(compositeSmooth);
		for (CropRect crop : crops) {
			g2.setColor(Color.BLUE);
			g2.fill(crop);

			g2.setColor(Color.BLACK);
			Font scaledFont = scaleFont(String.valueOf(cropCnt++), crop, g2,
					currentFont);
			g2.setFont(scaledFont);
			g2
					.drawString(String.valueOf(cropCnt), crop.x, crop.y
							+ crop.height);

			if (crop.isSelected()) {
				g2.setComposite(compositeXor);
				g2.setColor(Color.BLACK);

				scaledFont = scaleFont("Selected", crop, g2, currentFont);
				g2.setFont(scaledFont);
				g2.fill(crop);
				g2.setColor(Color.YELLOW);
				g2.setComposite(compositeSmooth);
				g2.drawString("Selected", crop.x, crop.y + crop.height);
			}
		}

		g2.setColor(Color.BLUE);
		if (curCrop != null) {
			g2.fill(curCrop);
			if (lastCrop != null) {
				// draw "ghost"-rectangle to show the dimension of the last crop
				g2.setColor(Color.GREEN);
				g2.setStroke(new BasicStroke(3.0f));
				g2.setComposite(compositeXor);
				g2.drawRect(curCrop.x, curCrop.y, lastCrop.width,
						lastCrop.height);
			}
		}

		g2.dispose();

	}

	public void mouseDragged(MouseEvent mE) {
		Point curPoint = mE.getPoint();

		if (dragCropIndex == -1) {
			if (cropStartPoint == null) {
				cropStartPoint = curPoint;
			}
			// create the rectangle to draw
			curCrop = new CropRect();
			curCrop.x = (curPoint.x < cropStartPoint.x) ? curPoint.x
					: cropStartPoint.x;
			curCrop.width = Math.abs(curPoint.x - cropStartPoint.x);
			curCrop.y = (curPoint.y < cropStartPoint.y) ? curPoint.y
					: cropStartPoint.y;
			curCrop.height = Math.abs(curPoint.y - cropStartPoint.y);
		} else {
			if (lastDragPoint == null) {
				lastDragPoint = curPoint;
			}
			// drag the rectangle around
			crops.get(dragCropIndex).translate(curPoint.x - lastDragPoint.x,
					curPoint.y - lastDragPoint.y);
			lastDragPoint = curPoint;
		}

		repaint();
	}

	public void mouseMoved(MouseEvent arg0) {
	}

	public void mouseClicked(MouseEvent mE) {
	}

	public void mouseEntered(MouseEvent arg0) {
	}

	public void mouseExited(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent mE) {
		Point p = mE.getPoint();

		if (mE.isControlDown()) {
			for (CropRect crop : crops) {
				if (crop.contains(p)) {
					crop.setSelected(!crop.isSelected());
				}
			}
			repaint();
			return;
		}
		if (SwingUtilities.isLeftMouseButton(mE)) {
			// check if the click was made in a crop rectangle
			for (Rectangle crop : crops) {
				if (crop.contains(p)) {
					dragCropIndex = crops.indexOf(crop);
				}
			}
			if (dragCropIndex == -1) {
				cropStartPoint = p;
			}
		} else {
			int deleteIndex = -1;
			for (Rectangle crop : crops) {
				if (crop.contains(p)) {
					deleteIndex = crops.indexOf(crop);
				}
			}
			if (deleteIndex != -1) {
				crops.remove(deleteIndex);
			}
			cluster.clearRatios();
			repaint();
		}
	}

	public int getWidestSelectedRect() {
		int max = -1;
		for (CropRect crop : crops) {
			if (crop.isSelected()) {
				if (crop.width > max) {
					max = crop.width;
				}
			}
		}
		return max;
	}

	public int getHeighestSelectedRect() {
		int max = -1;
		for (CropRect crop : crops) {
			if (crop.isSelected()) {
				if (crop.height > max) {
					max = crop.height;
				}
			}
		}
		return max;
	}

	public void setSelCropWidth(int width) {
		for (CropRect crop : crops) {
			if (crop.isSelected()) {
				int diffToMax = width - crop.width;
				crop.grow(diffToMax / 2, 0);
			}
		}
		updateClusterRatios(crops);
		repaint();
	}

	public void setSelCropHeight(int height) {
		for (CropRect crop : crops) {
			if (crop.isSelected()) {
				int diffToMax = height - crop.height;
				crop.grow(0, diffToMax / 2);
			}
		}
		updateClusterRatios(crops);
		repaint();
	}

	public void mouseReleased(MouseEvent arg0) {
		// add croprectangle to list
		if (curCrop != null) {
			crops.add(curCrop);
			lastCrop = curCrop;
		}
		// throw away all crops which are to small
		List<Rectangle> cropsToTrash = new ArrayList<Rectangle>();
		for (Rectangle crop : crops) {
			if (crop.getWidth() < 5 || crop.getHeight() < 5) {
				cropsToTrash.add(crop);
			}
		}
		crops.removeAll(cropsToTrash);
		updateClusterRatios(crops);
		cropStartPoint = null;
		lastDragPoint = null;
		dragCropIndex = -1;
		curCrop = null;
		repaint();
	}

	private void updateClusterRatios(List<CropRect> tmpCrops) {
		cluster.clearRatios();
		for (Rectangle crop : tmpCrops) {
			cluster.addRatios(getCutRatiosForPdf(crop, img.getWidth(), img
					.getHeight()));
		}
	}

	/**
	 * creates the crop ratios from the user selection. 0 = left 1 = bottom 2 =
	 * right 3 = top
	 * 
	 * @param crop
	 * 
	 * @return the cropped ratios or null if to small
	 */
	private static Float[] getCutRatiosForPdf(Rectangle crop, int imgWidth,
			int imgHeight) {
		int x1, x2, y1, y2;

		x1 = crop.x;
		x2 = x1 + crop.width;
		y1 = crop.y;
		y2 = y1 + crop.height;

		// check for maximum and minimum
		if (x1 < 0) {
			x1 = 0;
		}
		if (x2 > imgWidth) {
			x2 = imgWidth;
		}
		if (y1 < 0) {
			y1 = 0;
		}
		if (y2 > imgHeight) {
			y2 = imgHeight;
		}

		Float[] ratios = new Float[4];
		// left
		ratios[0] = (float) x1 / imgWidth;
		// bottom
		ratios[1] = (float) (imgHeight - y2) / imgHeight;
		// right
		ratios[2] = 1 - ((float) x2 / imgWidth);
		// top
		ratios[3] = 1 - ((float) (imgHeight - y1) / imgHeight);

		return ratios;
	}

	private static Font scaleFont(String text, Rectangle rect, Graphics g,
			Font pFont) {

		Font font = pFont;
		float fontSize = font.getSize();
		font = g.getFont().deriveFont(fontSize);
		int width = g.getFontMetrics(font).stringWidth(text);
		int height = g.getFontMetrics(font).getHeight();
		float scaleFactorWidth = rect.width / width;
		float scaleFactorHeight = rect.height / height;
		float scaledWidth = (scaleFactorWidth * fontSize);
		float scaledHeight = (scaleFactorHeight * fontSize);
		return g.getFont().deriveFont(
				(scaleFactorHeight > scaleFactorWidth) ? scaledWidth
						: scaledHeight);
	}

	private class CropRect extends Rectangle {
		private boolean selected = false;

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}

	}
}
