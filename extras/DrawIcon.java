import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Draws the IndianGold app icon (a gold coin stamped "IG") as a 1024x1024 PNG.
 * Run with: java extras/DrawIcon.java extras/IndianGold-icon.png
 */
public class DrawIcon
{
	public static void main(String[] args) throws Exception
	{
		int size = 1024;
		BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		Color darkGold = new Color(0x8A5A00);
		Color gold = new Color(0xD4A017);
		Color lightGold = new Color(0xFFE27A);
		Color paleGold = new Color(0xFFF4C2);

		// Soft drop shadow
		for (int i = 0; i < 24; i++) {
			g.setColor(new Color(0, 0, 0, 4));
			g.fill(new Ellipse2D.Double(52 + i * 0.5, 72 + i * 0.6, 920 - i, 920 - i));
		}

		// Coin edge: dark-to-light ring
		Ellipse2D outer = new Ellipse2D.Double(48, 40, 928, 928);
		g.setPaint(new GradientPaint(0, 40, lightGold, 0, 968, darkGold));
		g.fill(outer);

		// Coin face: radial gradient with the light from the top left
		Ellipse2D face = new Ellipse2D.Double(104, 96, 816, 816);
		g.setPaint(new RadialGradientPaint(new Point2D.Double(380, 330), 640,
				new float[] { 0f, 0.45f, 0.85f, 1f },
				new Color[] { paleGold, lightGold, gold, new Color(0xB07D0A) }));
		g.fill(face);

		// Raised rim line and dotted border, as on a minted coin
		g.setStroke(new BasicStroke(10f));
		g.setPaint(new GradientPaint(0, 96, darkGold, 0, 912, lightGold));
		g.draw(face);
		g.setColor(new Color(0x9C6B05));
		int dots = 72;
		for (int i = 0; i < dots; i++) {
			double a = 2 * Math.PI * i / dots;
			double x = 512 + Math.cos(a) * 360, y = 504 + Math.sin(a) * 360;
			g.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
		}

		// "IG" stamped in the middle: dark engraving offset by a light edge
		Font font = new Font(Font.SERIF, Font.BOLD, 400);
		GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), "IG");
		Rectangle2D b = gv.getVisualBounds();
		double tx = 512 - b.getCenterX(), ty = 504 - b.getCenterY();
		Shape letters = AffineTransform.getTranslateInstance(tx, ty).createTransformedShape(gv.getOutline());
		g.setColor(new Color(255, 248, 210, 230));
		g.fill(AffineTransform.getTranslateInstance(6, 6).createTransformedShape(letters));
		g.setPaint(new GradientPaint(0, (float) (ty + b.getY()), new Color(0x7A4E00), 0, (float) (ty + b.getMaxY()), new Color(0xA8740A)));
		g.fill(letters);

		// Glossy highlight across the top
		g.setClip(face);
		g.setPaint(new GradientPaint(0, 96, new Color(255, 255, 255, 110), 0, 520, new Color(255, 255, 255, 0)));
		g.fill(new Ellipse2D.Double(170, 60, 684, 460));
		g.dispose();

		ImageIO.write(img, "png", new File(args.length > 0 ? args[0] : "IndianGold-icon.png"));
	}
}
