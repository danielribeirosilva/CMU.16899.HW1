package tetris.simulator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

import javax.swing.*;

public class TLabel{
	private static final long serialVersionUID = 1L;
	
	public JLabel draw;
	
	// pre-defined colors
	public static final Color BLACK      = Color.BLACK;
	public static final Color BLUE       = Color.BLUE;
	public static final Color CYAN       = Color.CYAN;
	public static final Color DARK_GRAY  = Color.DARK_GRAY;
	public static final Color GRAY       = Color.GRAY;
	public static final Color GREEN      = Color.GREEN;
	public static final Color LIGHT_GRAY = Color.LIGHT_GRAY;
	public static final Color MAGENTA    = Color.MAGENTA;
	public static final Color ORANGE     = Color.ORANGE;
	public static final Color PINK       = Color.PINK;
	public static final Color RED        = Color.RED;
	public static final Color WHITE      = Color.WHITE;
	public static final Color YELLOW     = Color.YELLOW;
	public static final Color NICEGREEN = new Color(0,153,0);
	// default colors
	public static final Color DEFAULT_PEN_COLOR   = BLACK;
	public static final Color DEFAULT_CLEAR_COLOR = WHITE;

	// current pen color
	private static Color penColor;

	// default canvas size is SIZE-by-SIZE
	static final int SIZE = 512;
	public int width  = SIZE;
	private int height = SIZE;

	// default pen radius
	private static final double DEFAULT_PEN_RADIUS = 0.002;

	// current pen radius
	private static double penRadius;

	// boundary of drawing canvas, 0% border
	public double BORDER = 0.00;
	private static final double DEFAULT_XMIN = 0.0;
	private static final double DEFAULT_XMAX = 1.0;
	private static final double DEFAULT_YMIN = 0.0;
	private static final double DEFAULT_YMAX = 1.0;
	public double xmin, ymin, xmax, ymax;

	// default font
	private final Font DEFAULT_FONT = new Font("Serif", Font.PLAIN, 16);

	// current font
	private static Font font;

	// double buffered graphics
	private BufferedImage offscreenImage, onscreenImage;
	protected Graphics2D offscreen, onscreen;
	
	// change the user coordinate system
	public void setXscale() { setXscale(DEFAULT_XMIN, DEFAULT_XMAX); }
	public void setYscale() { setYscale(DEFAULT_YMIN, DEFAULT_YMAX); }
	public void setXscale(double min, double max) {
		double size = max - min;
		xmin = min - BORDER * size;
		xmax = max + BORDER * size;
	}

	public void setYscale(double min, double max) {
		double size = max - min;
		ymin = min - BORDER * size;
		ymax = max + BORDER * size;
	}


	// helper functions that scale from user coordinates to screen coordinates and back
	protected double scaleX (double x) { return width  * (x - xmin) / (xmax - xmin); }
	protected double scaleY (double y) { return height * (ymax - y) / (ymax - ymin); }
	public double factorX(double w) { return w * width  / Math.abs(xmax - xmin);  }
	public double factorY(double h) { return h * height / Math.abs(ymax - ymin);  }
	public double userX  (double x) { return xmin + x * (xmax - xmin) / width;    }
	public double userY  (double y) { return ymax - y * (ymax - ymin) / height;   }


	//create a frame, insert self in frame, then show self
	public void showInFrame() {
		JFrame j = new JFrame();
		j.setTitle("Configuration");
		j.setContentPane(this.draw);
		j.setVisible(true);
		j.pack();
		show();
	}
	
	
	// clear the screen with given color
	public void clear() { clear(DEFAULT_CLEAR_COLOR); }
	public void clear(Color color) {
		offscreen.setColor(color);
		offscreen.fillRect(0, 0, width, height);
		offscreen.setColor(penColor);
	}

	public void clear(double x1, double x2, double y1, double y2)
	{ clear(DEFAULT_CLEAR_COLOR, x1, x2, y1, y2);  }
	public void clear(Color color, double x1, double x2, double y1, double y2) {

		int ix1, ix2, iy1, iy2;
		ix1 = (int) scaleX(x1);
		ix2 = (int) scaleX(x2);
		iy1 = (int) scaleY(y1);
		iy2 = (int) scaleY(y2);

		offscreen.setColor(color);
		offscreen.fillRect(ix1, iy1, ix2, iy2);
		offscreen.setColor(penColor);
		//show();
	}

	// set the pen size
	public void setPenRadius() { setPenRadius(DEFAULT_PEN_RADIUS); }
	public void setPenRadius(double r) {
		penRadius = r * SIZE;
		BasicStroke stroke = new BasicStroke((float) penRadius);
		offscreen.setStroke(stroke);
	}



	// set the pen color
	public void setPenColor() { setPenColor(DEFAULT_PEN_COLOR); }
	public void setPenColor(Color color) {
		penColor = color;
		offscreen.setColor(penColor);
	}

	// write the given string in the current font
	public void setFont() { setFont(DEFAULT_FONT); }
	public void setFont(Font f) { 
		Toolkit toolkit = java . awt . Toolkit . getDefaultToolkit ();
		double x = toolkit.getScreenSize().getWidth();
		double y = toolkit.getScreenSize().getHeight();
		double xscale = x/1400.0;
		double yscale = y/1050.0;
		double scale = Math.sqrt((xscale*xscale+yscale*yscale)/2);

		font = f.deriveFont((float) (f.getSize()*scale));
	}
	
	public TLabel(int w, int h){
		width = w;
		height = h;
		offscreenImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		onscreenImage  = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		offscreen = offscreenImage.createGraphics();
		onscreen  = onscreenImage.createGraphics();
		setXscale();
		setYscale();
		offscreen.setColor(DEFAULT_CLEAR_COLOR);
		offscreen.fillRect(0, 0, width, height);
		setPenColor();
		setPenRadius();
		setFont();
		clear();

		// add antialiasing
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		offscreen.addRenderingHints(hints);

		// frame stuff
		ImageIcon icon = new ImageIcon(onscreenImage);
		draw = new JLabel(icon);
	}
	
	public void add(Container frame, String spot){
		frame.add(draw, spot);
	}
	

	
	public void addML(MouseListener frame){
		draw.addMouseListener(frame);
	}
	
	public void addMML(MouseMotionListener frame){
		draw.addMouseMotionListener(frame);
	}
	
	public void addKL(KeyListener frame){
		draw.addKeyListener(frame);
	}
	
	public void addMWL(MouseWheelListener frame) {
		draw.addMouseWheelListener(frame);
	}
	
	public void remML(MouseListener frame){
		draw.removeMouseListener(frame);
	}
	
	public void remMML(MouseMotionListener frame){
		draw.removeMouseMotionListener(frame);
	}
	
	public void remKL(KeyListener frame){
		draw.removeKeyListener(frame);
	}
	
	public void remMWL(MouseWheelListener frame){
		draw.removeMouseWheelListener(frame);
	}
	
	// draw a line from (x0, y0) to (x1, y1)
	public void line(double x0, double y0, double x1, double y1) {
//		System.out.println("drawing a line from " + new Point(x0, y0).toString()+ " to " + new Point(x1,y1).toString());
		offscreen.draw(new Line2D.Double(scaleX(x0), scaleY(y0), scaleX(x1), scaleY(y1)));
	}



	// draw one pixel at (x, y)
	public void pixel(double x, double y) {
		offscreen.fillRect((int) Math.round(scaleX(x)), (int) Math.round(scaleY(y)), 1, 1);
	}
	
	// draw one pixel at (x, y)
	public void pixelP(double x, double y) {
		offscreen.fillRect((int) Math.round(x), (int) Math.round(y), 1, 1);
	}
	
	// draw one pixel at (x, y)
	public void pixelP(double x, double y, Color c) {
		setPenColor(c);
		offscreen.fillRect((int) Math.round(x), (int) Math.round(y), 1, 1);
	}

	// draw point at (x, y)
	public void point(double x, double y) {
		double xs = scaleX(x);
		double ys = scaleY(y);
		double r = penRadius;
		// double ws = factorX(2*r);
		// double hs = factorY(2*r);
		// if (ws <= 1 && hs <= 1) pixel(x, y);
		if (r <= 1) pixel(x, y);
		else offscreen.fill(new Ellipse2D.Double(xs - r/2, ys - r/2, r, r));
	}

	public void arc(double x, double y, double r, double startAngle, double arcRange) {
		double xs = scaleX(x);
		double ys = scaleY(y);
		double ws = factorX(2*r);
		double hs = factorY(2*r);
		if (ws <= 1 && hs <= 1) pixel(x, y);
		else offscreen.draw(new Arc2D.Double(xs - ws/2, ys - hs/2, ws, hs,startAngle, arcRange, Arc2D.OPEN));

	}
	
	// draw circle of radius r, centered on (x, y); degenerate to pixel if small
	public void circle(double x, double y, double r) {
		double xs = scaleX(x);
		double ys = scaleY(y);
		double ws = factorX(2*r);
		double hs = factorY(2*r);
		if (ws <= 1 && hs <= 1) pixel(x, y);
		else offscreen.draw(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
	}
	
	public void circleP(double x, double y, double r, Color col) {
		setPenColor(col);
		circleP(x,y,r);
	}
	
	public void circleP(double x, double y, double r) {
		double ws = 2*r;
		double hs = 2*r;
		double xs = scaleX(x);
		double ys = scaleY(y);
		if (ws <= 1 && hs <= 1) pixel(x, y);
		else offscreen.draw(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
	}


	
	public void circle(double x, double y, double r, Color color) {
		setPenColor(color);
		circle(x,y,r);
	}
	
	
	// draw filled circle of radius r, centered on (x, y); degenerate to pixel if small
	public void filledCircle(double x, double y, double r) {
		double xs = scaleX(x);
		double ys = scaleY(y);
		double ws = factorX(2*r);
		double hs = factorY(2*r);
		if (ws <= 1 && hs <= 1) pixel(x, y);
		else offscreen.fill(new Ellipse2D.Double(xs - ws/2, ys - hs/2, ws, hs));
	}


	
	public void filledCircleP(double x, double y, double r) {
		double ws = 2*r;
		double hs = 2*r;
		if (ws <= 1 && hs <= 1) pixel(x, y);
		else offscreen.fill(new Ellipse2D.Double(x - ws/2, y - hs/2, ws, hs));
	}

	// draw squared of side length 2r, centered on (x, y); degenerate to pixel if small
	public void square(double x, double y, double r) {
		// screen coordinates
		double xs = scaleX(x);
		double ys = scaleY(y);
		double ws = factorX(2*r);
		double hs = factorY(2*r);
		if (ws <= 1 && hs <= 1) pixel(x, y);
		else offscreen.draw(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
	}

	// draw squared of side length 2r, centered on (x, y); degenerate to pixel if small
	public void filledSquare(double x, double y, double r) {
		// screen coordinates
		double xs = scaleX(x);
		double ys = scaleY(y);
		double ws = factorX(2*r);
		double hs = factorY(2*r);
		if (ws <= 1 && hs <= 1) pixel(x, y);
		else offscreen.fill(new Rectangle2D.Double(xs - ws/2, ys - hs/2, ws, hs));
	}


	//Draw an arrow of the appropriate scale, color, position
	public void Arrow(double x, double y, double w, double h, double Scale, Color Color) {
		rectangle(x, y, w, h, Color);
		double[] xarray = {x+w/2-w/10+Scale*1/10*Math.sqrt(h*h*25+w*w)*Math.sqrt(3)/3,x+w/2-w/10,x+w/2-w/10};
		double[] yarray = {y,y+Scale*1/10*Math.sqrt(h*h*25+w*w)*Math.sqrt(2)/2,y-Scale*1/10*Math.sqrt(h*h*25+w*w)*Math.sqrt(2)/2};
		setPenColor(Color);
		filledPolygon(xarray, yarray);
		setPenColor();
	}

	// draw a polygon with the given (x[i], y[i]) coordinates
	public void polygon(double[] x, double[] y) {
		int N = x.length;
		GeneralPath path = new GeneralPath();
		path.moveTo((float) scaleX(x[0]), (float) scaleY(y[0]));
		for (int i = 0; i < N; i++)
			path.lineTo((float) scaleX(x[i]), (float) scaleY(y[i]));
		path.closePath();
		offscreen.draw(path);
	}



//	draw a polygon with the given (x[i], y[i]) coordinates
	public void polygonP(double[] x, double[] y) {
		int N = x.length;
		GeneralPath path = new GeneralPath();
		path.moveTo((float) x[0], (float) y[0]);
		for (int i = 0; i < N; i++)
			path.lineTo((float) x[i], (float) y[i]);
		path.closePath();
		offscreen.draw(path);
	}

	// draw a filled polygon with the given (x[i], y[i]) coordinates
	public void filledPolygon(double[] x, double[] y) {
		int N = x.length;
		GeneralPath path = new GeneralPath();
		path.moveTo((float) scaleX(x[0]), (float) scaleY(y[0]));
		for (int i = 0; i < N; i++)
			path.lineTo((float) scaleX(x[i]), (float) scaleY(y[i]));
		path.closePath();
		offscreen.fill(path);
	}

//	draw a filled polygon with the given (x[i], y[i]) coordinates
	public void filledPolygonP(double[] x, double[] y) {
		int N = x.length;
		GeneralPath path = new GeneralPath();
		path.moveTo((float) x[0], (float) y[0]);
		for (int i = 0; i < N; i++)
			path.lineTo((float) x[i], (float) y[i]);
		path.closePath();
		offscreen.fill(path);
	}

	//Draw rectangle at the given coordinates
	public void rectangle(double x, double y, double w, double h) {
		double[] xarray = {x-w/2,x-w/2,x+w/2,x+w/2};
		double[] yarray = {y-h/2,y+h/2,y+h/2,y-h/2};
		polygon(xarray,yarray);
	}
	
	public void rectangleLL(double x, double y, double w, double h) {
		double[] xarray = {x,x,x+w,x+w};
		double[] yarray = {y,y+h,y+h,y};
		polygon(xarray,yarray);
	}
	
	public void rectangleP(double x, double y, double w, double h) {
		double[] xarray = {x,x,x+w,x+w};
		double[] yarray = {y,y+h,y+h,y};
		polygonP(xarray,yarray);
	}

	public void rectangle(double x, double y, double w, double h, Color c) {
		double[] xarray = {x-w/2,x-w/2,x+w/2,x+w/2};
		double[] yarray = {y-h/2,y+h/2,y+h/2,y-h/2};
		setPenColor(c);
		filledPolygon(xarray,yarray);
		setPenColor(DEFAULT_PEN_COLOR);
	}
	
	public void rectangleC(double x, double y, double w, double h, Color c) {
		double[] xarray = {x,x,x+w,x+w};
		double[] yarray = {y,y+h,y+h,y};
		setPenColor(c);
		filledPolygon(xarray,yarray);
		setPenColor(DEFAULT_PEN_COLOR);
	}

	public void filledRectangleP(double x, double y, double w, double h, Color c) {
		double[] xarray = {x,x,x+w,x+w};
		double[] yarray = {y,y+h,y+h,y};
		setPenColor(c);
		filledPolygonP(xarray,yarray);
		setPenColor(DEFAULT_PEN_COLOR);
	}
	
	public void filledRectangleLL(double x, double y, double w, double h, Color c) {
		double[] xarray = {x,x,x+w,x+w};
		double[] yarray = {y,y+h,y+h,y};
		setPenColor(c);
		filledPolygon(xarray,yarray);
		setPenColor(DEFAULT_PEN_COLOR);
	}

	public void rectangle(double x, double y, double w, double h, Color c,boolean Border, Color BorderColor) {

		double[] xarray = {x-w/2,x-w/2,x+w/2,x+w/2};
		double[] yarray = {y-h/2,y+h/2,y+h/2,y-h/2};
		if (c!=null) {
			setPenColor(c);
			filledPolygon(xarray,yarray);
		}
		setPenColor(BorderColor);
		if (Border) polygon(xarray, yarray);
		setPenColor();
	}
	

	
	// draw picture (gif, jpg, or png) upperLeft on (x, y), rescaled to w-by-h
	public void image(double x, double y, Image image, double w, double h) {

		double xs = scaleX(x);
		double ys = scaleY(y);
		double ws = factorX(w);
		double hs = factorY(h);
		if (ws <= 1 && hs <= 1) pixel(x, y);
		else {
			offscreen.drawImage(image, (int) Math.round(xs),
					(int) Math.round(ys),
					(int) Math.round(ws),
					(int) Math.round(hs), null);
		}
	}
	
	// draw picture (gif, jpg, or png) upperLeft on (x, y), rescaled to w-by-h
	public void imageP(double x, double y, Image image) {

		//if (ws <= 1 && hs <= 1) pixel(x, y);

		offscreen.drawImage(image, (int) Math.round(x),(int) Math.round(y), null);
		
	}
	


	//Invert an image
	public BufferedImage invert(Image image) {
		BufferedImage b1 = new BufferedImage(image.getWidth(null),image.getHeight(null),BufferedImage.TYPE_INT_RGB);
		Graphics bg = b1.getGraphics();
		bg.drawImage(image, 0, 0, null);
		bg.dispose();

		BufferedImage b2 = new BufferedImage(image.getWidth(null),image.getHeight(null),BufferedImage.TYPE_INT_RGB);
		DataBuffer db1 = b1.getRaster().getDataBuffer();
		DataBuffer db2 = b2.getRaster().getDataBuffer();

		for (int i = db1.getSize() - 1, j = 0; i >= 0; --i, j++) {
			db2.setElem(j, db1.getElem(i));

		}
		for (int i = db1.getSize() - 1, j = 0; i >= 0; --i, j++) {
			db1.setElem(i, db1.getElem(i));

		}
		return b2;	  
	}




	// write the given text string in the current font, center on (x, y)
	public void text(double x, double y, String s) {
		offscreen.setFont(font);
		FontMetrics metrics = offscreen.getFontMetrics();
		double xs = scaleX(x);
		double ys = scaleY(y);
		int ws = metrics.stringWidth(s);
		int hs = metrics.getDescent();
		offscreen.drawString(s, (float) (xs - ws/2.0), (float) (ys + hs));
	}
	
	public void textTop(double x, double y, String s) {
		offscreen.setFont(font);
		FontMetrics metrics = offscreen.getFontMetrics();
		double xs = scaleX(x);
		double ys = scaleY(y);
		int ws = metrics.stringWidth(s);
		int hs = metrics.getDescent();
		offscreen.drawString(s, (float) (xs - ws/2.0), (float) (ys + hs*3));
	}

	public void textLeft(double x, double y, String s,Color c) {
		setPenColor(c);
		offscreen.setFont(font);
		FontMetrics metrics = offscreen.getFontMetrics();
		double xs = scaleX(x);
		double ys = scaleY(y);
		//int ws = metrics.stringWidth(s);
		int hs = metrics.getDescent();
		offscreen.drawString(s, (float) (xs), (float) (ys + hs));
		setPenColor();
	}


	//Draw text at the appropriate point and color
	public void text(double x, double y, String s,Color c) {
		setPenColor(c);
		offscreen.setFont(font);
		FontMetrics metrics = offscreen.getFontMetrics();
		double xs = scaleX(x);
		double ys = scaleY(y);
		int ws = metrics.stringWidth(s);
		int hs = metrics.getDescent();
		offscreen.drawString(s, (float) (xs - ws/2.0), (float) (ys + hs));
		setPenColor();
	}

//	write the given text string in the current font, center on (x, y) sized to w, h
	public void text(double x, double y, String s, double w, double h) {
		offscreen.setFont(font);
		//FontMetrics metrics = offscreen.getFontMetrics();
		double xs = scaleX(x);
		double ys = scaleY(y);
		double ws = factorX(w);
		double hs = factorY(h);
		offscreen.drawString(s, (float) (xs - ws/2.0), (float) (ys + hs));
	}

	public void absText(String s, int x, int y) {
		offscreen.drawString(s, (float) (x), (float) (y));
	}
	
	public void textinvert(double x, double y, String s) {
		offscreen.setFont(font);
		FontMetrics metrics = offscreen.getFontMetrics();
		double xs = scaleX(x);
		double ys = scaleY(y);
		int ws = metrics.stringWidth(s);
		int hs = metrics.getDescent();
		BufferedImage bimage = new BufferedImage(ws, hs,BufferedImage.TYPE_INT_ARGB);
		Graphics2D bimagegraphics = bimage.createGraphics();
		bimagegraphics.drawString(s, (float) (xs - ws/2.0), (float) (ys + hs));
		BufferedImage bimage2 = invert(bimage);
		offscreen.drawImage(bimage2, (int) Math.round(xs - ws/2.0),
				(int) Math.round(ys + hs), null);
	}
	
	

	
	// view on-screen, creating new frame if necessary
	public void show() {
		onscreen.drawImage(offscreenImage, 0, 0, null);
		try{
			draw.repaint();
			//frame.paint(frame.getGraphics());
		}
		catch(NullPointerException e){
			System.out.println("Null Pointer Exception in showatonce");
		}

	}

	


	

	
	

	
}
