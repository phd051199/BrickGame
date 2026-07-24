package brickgame;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/** Entry point for the MIDP 2.0 Brick Game port. */
public final class Midlet extends MIDlet {

    private BrickCanvas canvas;

    public void startApp() {
        if (canvas == null) {
            canvas = new BrickCanvas();
        }

        canvas.setSystemPaused(false);
        Display.getDisplay(this).setCurrent(canvas);
        canvas.start();
    }

    public void pauseApp() {
        if (canvas != null) {
            canvas.setSystemPaused(true);
        }
    }

    public void destroyApp(boolean unconditional) {
        if (canvas != null) {
            canvas.stop();
        }
    }
}
