package kiev.gui.swt;

import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Draw2DTest {

  public static void main(String[] args) {
    final Renderer renderer = new Renderer();
    Shell shell = new Shell();
    shell.setSize(350, 350);

    shell.open();
    shell.setText("Draw2d Hello World");
    LightweightSystem lws = new LightweightSystem(shell);
    IFigure figure = new Figure() {
      protected void paintClientArea(org.eclipse.draw2d.Graphics graphics) {
        Dimension controlSize = getSize();

        renderer.prepareRendering(graphics);
        // prepares the Graphics2D renderer

        // gets the Graphics2D context and switch on the antialiasing
        Graphics2D g2d = renderer.getGraphics2D();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // paints the background with a color gradient
        g2d.setPaint(new GradientPaint(0.0f, 0.0f,
            java.awt.Color.yellow, (float) controlSize.width,
            (float) controlSize.width, java.awt.Color.white));
        g2d.fillRect(0, 0, controlSize.width, controlSize.width);

        // draws rotated text
        g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD,
            16));
        g2d.setColor(java.awt.Color.blue);

        g2d.translate(controlSize.width / 2, controlSize.width / 2);
        int nbOfSlices = 18;
        for (int i = 0; i < nbOfSlices; i++) {
          g2d.drawString("Angle = " + (i * 360 / nbOfSlices)
              + "\u00B0", 30, 0);
          g2d.rotate(-2 * Math.PI / nbOfSlices);
        }

        // now that we are done with Java2D, renders Graphics2D
        // operation
        // on the SWT graphics context
        renderer.render(graphics);

        // now we can continue with pure SWT paint operations
        graphics.drawOval(0, 0, controlSize.width, controlSize.width);
      }
    };
    lws.setContents(figure);
    Display display = Display.getDefault();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    renderer.dispose();
  }
}
