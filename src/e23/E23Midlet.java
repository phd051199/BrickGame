package e23;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public final class E23Midlet extends MIDlet {
    private E23Canvas screen;

    protected void startApp() {
        if (screen == null) {
            screen = new E23Canvas(this);
        }
        Display.getDisplay(this).setCurrent(screen);
        screen.start();
    }

    protected void pauseApp() {
        if (screen != null) {
            screen.stop();
        }
    }

    protected void destroyApp(boolean unconditional) {
        if (screen != null) {
            screen.stop();
        }
    }

    void exit() {
        if (screen != null) {
            screen.stop();
        }
        notifyDestroyed();
    }
}
