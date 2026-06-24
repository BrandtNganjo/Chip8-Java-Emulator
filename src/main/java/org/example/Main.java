package org.example;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Chip8 chip8 = new Chip8();
        chip8.initialize();
        chip8.loadRom("roms/4-flags.ch8");

        Display display = new Display(chip8);
        display.createWindow();

        long lastTimerUpdate = System.nanoTime();
        final double timerInterval = 1_000_000_000.0 / 60.0; // 60Hz interval in nanoseconds

        while(true) {
            //Execute the current CPU cycle
            chip8.cycle();

            // Checks if it is time to update the timers and display. If the difference b
            long now = System.nanoTime();
            if(now - lastTimerUpdate >= timerInterval) {
                chip8.updateTimers();
                display.repaint();
                lastTimerUpdate = now;
            }

            // Sleep for 2ms to limit the CPU to ~500 instructions per second.
            Thread.sleep(3);
        }
    }
}