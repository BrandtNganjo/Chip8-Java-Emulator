package org.example;
import  javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Display extends JPanel implements KeyListener {
    private final Chip8 chip8;
    private static final int SCALE = 10;

    public Display(Chip8 chip8) {
        this.chip8 = chip8;
        setPreferredSize(new Dimension(64 * SCALE, 32 * SCALE));
        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        boolean[] screen = chip8.getScreen();
        for(int y = 0; y < 32; y++) {
            for(int x = 0; x < 64; x++) {
                if(screen[y * 64 + x]) {
                    g.fillRect(
                            x*SCALE,
                            y*SCALE,
                            SCALE,
                            SCALE
                    );
                }
            }
        }
    }

    public void createWindow() {
        JFrame window = new JFrame("CHIP-8");
        window.add(this);
        window.pack();
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setVisible(true);
    }

    private int mapKey(int keyCode) {
        // Maps inputs. We're using the standard layout to mimic a hex pad (1234, QWER, ASDF, ZXCV)
        return switch (keyCode) {
            case KeyEvent.VK_1 -> 0x1;
            case KeyEvent.VK_2 -> 0x2;
            case KeyEvent.VK_3 -> 0x3;
            case KeyEvent.VK_4 -> 0xC;
            case KeyEvent.VK_Q -> 0x4;
            case KeyEvent.VK_W -> 0x5;
            case KeyEvent.VK_E -> 0x6;
            case KeyEvent.VK_R -> 0xD;
            case KeyEvent.VK_A -> 0x7;
            case KeyEvent.VK_S -> 0x8;
            case KeyEvent.VK_D -> 0x9;
            case KeyEvent.VK_F -> 0xE;
            case KeyEvent.VK_Z -> 0xA;
            case KeyEvent.VK_X -> 0x0;
            case KeyEvent.VK_C -> 0xB;
            case KeyEvent.VK_V -> 0xF;
            default -> -1;
        };
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyIndex = mapKey(e.getKeyCode());
        if (keyIndex != -1) {
            chip8.setKey(keyIndex, true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyIndex = mapKey(e.getKeyCode());
        if (keyIndex != -1) {
            chip8.setKey(keyIndex, false);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // The KeyListener interface needs this method. Nothing to see here :D
    }
}
