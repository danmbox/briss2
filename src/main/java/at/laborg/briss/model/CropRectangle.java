package at.laborg.briss.model;

public class CropRectangle {
	/**
	 * returns the ratio to crop the page x1,y1,x2,y2, origin = bottom left x1:
	 * from left edge to left edge of crop rectange y1: from lower edge to lower
	 * edge of crop rectange x2: from right edge to right edge of crop rectange
	 * y2: from top edge to top edge of crop rectange
	 * 
	 * @return
	 */
	private float xToLeft, yToBottom, xToRight, yToTop;

	public CropRectangle(float xToLeft, float yToBottom, float xToRight,
			float yToTop) {
		super();
		this.xToLeft = xToLeft;
		this.yToBottom = yToBottom;
		this.xToRight = xToRight;
		this.yToTop = yToTop;
	}

	public float getxToLeft() {
		return xToLeft;
	}

	public void setxToLeft(float xToLeft) {
		this.xToLeft = xToLeft;
	}

	public float getyToBottom() {
		return yToBottom;
	}

	public void setyToBottom(float yToBottom) {
		this.yToBottom = yToBottom;
	}

	public float getxToRight() {
		return xToRight;
	}

	public void setxToRight(float xToRight) {
		this.xToRight = xToRight;
	}

	public float getyToTop() {
		return yToTop;
	}

	public void setyToTop(float yToTop) {
		this.yToTop = yToTop;
	}

}
