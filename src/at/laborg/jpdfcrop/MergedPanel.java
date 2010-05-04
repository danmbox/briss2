package at.laborg.jpdfcrop;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

@SuppressWarnings("serial")
public class MergedPanel extends JPanel implements MouseMotionListener,
		MouseListener {

	private static final int MINIMUM_WIDTH = 30;
	private static final int MINIMUM_HEIGHT = 40;
	private BufferedImage img;
	private int zoomXStart, zoomXEnd, zoomYStart, zoomYEnd;
	private static Composite composite = AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, .2f);
	private MergedPanel connectedMerge;

	public MergedPanel() {
		super();
		try {
			img = ImageIO.read(new File("res/nullImage.png"));
			this
					.setPreferredSize(new Dimension(img.getWidth(), img
							.getHeight()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		addMouseMotionListener(this);
		addMouseListener(this);
	}

	@Override
	public void paint(Graphics g) {
		update(g);
	}

	@Override
	public void update(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(img, null, 0, 0);
		if (isEnabled()) {
			g2.setComposite(composite);
			g2.setColor(Color.BLUE);
			if (zoomXStart < zoomXEnd) {
				if (zoomYStart < zoomYEnd) {
					g2.fillRect(zoomXStart, zoomYStart, zoomXEnd - zoomXStart,
							zoomYEnd - zoomYStart);
				} else {
					g2.fillRect(zoomXStart, zoomYEnd, zoomXEnd - zoomXStart,
							zoomYStart - zoomYEnd);
				}
			} else {
				if (zoomYStart < zoomYEnd) {
					g2.fillRect(zoomXEnd, zoomYStart, zoomXStart - zoomXEnd,
							zoomYEnd - zoomYStart);
				} else {
					g2.fillRect(zoomXEnd, zoomYEnd, zoomXStart - zoomXEnd,
							zoomYStart - zoomYEnd);
				}
			}
		}
		g2.dispose();
	}

	@Override
	public void mouseDragged(MouseEvent mE) {
		zoomXEnd = mE.getX();
		zoomYEnd = mE.getY();
		if (connectedMerge != null) {
			connectedMerge.drawResizeWindow(zoomXStart, zoomXEnd, zoomYStart,
					zoomYEnd);
		}
		repaint();
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
		zoomXStart = mE.getX();
		zoomYStart = mE.getY();

	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}

	public void setConnectedMerge(MergedPanel connectedMerge) {
		this.connectedMerge = connectedMerge;
	}

	public void drawResizeWindow(int zoomXStart, int zoomXEnd, int zoomYStart,
			int zoomYEnd) {
		this.zoomXStart = zoomXStart;
		this.zoomXEnd = zoomXEnd;
		this.zoomYStart = zoomYStart;
		this.zoomYEnd = zoomYEnd;
		repaint();
	}

	public void setImage(BufferedImage img) {
		this.img = img;
		setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
		repaint();
	}

	/**
	 * creates the crop ratios from the user selection. 0 = xStart 1 = xEnd 2 =
	 * yStart 3 = yEnd
	 * 
	 * @return the cropped ratios or null if to small
	 */
	public float[] getCutRatios() {
		int x1,x2,y1,y2;
		if (zoomXStart > zoomXEnd) {
			x1 = zoomXEnd;
			x2 = zoomXStart;
		} else {
			x1 = zoomXStart;
			x2 = zoomXEnd;			
		}
		if (zoomYStart > zoomYEnd) {
			y1 = zoomYEnd;
			y2 = zoomYStart;
		} else {
			y1 = zoomYStart;
			y2 = zoomYEnd;			
		}
		if ((x2-x1) < MINIMUM_WIDTH) {
			return null;
		}
		if ((y2-y1) < MINIMUM_HEIGHT) {
			return null;
		}
		
		float[] ratios = new float[4];
		ratios[0] = img.getWidth()/(float)x1;
		ratios[1] = img.getWidth()/(float)x2;
		ratios[2] = img.getHeight()/(float)y1;
		ratios[3] = img.getHeight()/(float)y2;
		
		return ratios;
	}

}
