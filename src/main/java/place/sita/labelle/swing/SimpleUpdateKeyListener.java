package place.sita.labelle.swing;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public abstract class SimpleUpdateKeyListener implements KeyListener {

    @Override
    public void keyTyped(KeyEvent e) {
        sync();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        sync();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        sync();
    }

    public abstract void sync();
}
