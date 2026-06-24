package org.example;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class Chip8 {
    // 4096 bytes of memory
    private final byte[] memory = new byte[4096];

    // 16 8-bit (1 byte) registers
    private final int[] V = new int[16];

    // The stack
    private final int[] stack = new int[16];
    private int sp;

    // 16 bit index register to point to memory addresses
    private int I;

    // 16-bit program counter  to track current instruction address
    private int pc;

    // Count down at 60hz when greater than 0
    private int delayTimer;
    private int soundTimer;

    // The screen. 64x32 (2048) pixels. true = black/on | false = white/off
    private final boolean[] framebuffer = new boolean[64 * 32];

    // The input keypad. 16 keys (0-F) true when pressed
    private final boolean[] keys = new boolean[16];

    private final byte[] fontSet = {
            (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0, // 0
            (byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70, // 1
            (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // 2
            (byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 3
            (byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10, // 4
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 5
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 6
            (byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40, // 7
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0, // 8
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0, // 9
            (byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90, // A
            (byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0, // B
            (byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0, // C
            (byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0, // D
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0, // E
            (byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80  // F
    };

    public void initialize() {
        Arrays.fill(memory, (byte)0);
        Arrays.fill(V, 0);
        Arrays.fill(framebuffer, false);
        this.I = 0;
        this.pc = 0x200; // Where the memory of rom data conventionally begins
        this.delayTimer = 0;
        this.soundTimer = 0;
        System.arraycopy(fontSet, 0, memory, 0x050, fontSet.length); //Placing fontset in memory, indexed 0x050-0x09F
        //System.out.print(Arrays.toString(memory)); // Print memory
    }

    public boolean[] getScreen() {
        return framebuffer;
    }

    public void loadRom(String filePath) throws IOException {
        byte[] romData = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath));
        System.arraycopy(romData, 0, memory, 512, romData.length);
    }

    public void cycle() {
        // Fetch the next two bytes and combines them into one 16-bit opcode
        int opcode = ((memory[pc] & 0xFF) << 8) | (memory[pc + 1] & 0xFF);

        // Increment PC forward by two bytes for the next cycle
        pc += 2;
        executeOpcode(opcode);
    }

    public void reset() {
        Main.romLoaded = false;
        initialize();
    }

    public void executeOpcode(int opcode) {
        //
        int firstNibble = opcode & 0xF000;
        int x = (opcode & 0x0F00) >> 8; //
        int y = (opcode & 0x00F0) >> 4; //
        int n = opcode & 0x000F;        //
        int nn = opcode & 0x00FF;       //
        int nnn = opcode & 0x0FFF;      // Memory address

        switch(firstNibble) {
            case 0x0000:
                handle0(opcode);
                break;
            case 0x1000: // jump. pc jumps to nnn
                pc = nnn;
                break;
            case 0x2000: // pushes pc to the stack, them jumps to nnn
                // System.out.println(sp + "Before increment");
                stack[sp] = pc;
                sp++;
                // System.out.println(sp + "After increment");
                pc = nnn;
                break;
            case 0x3000: // skip next instruction if Vx=nn
                if(V[x] == nn) pc += 2;
                break;
            case 0x4000: // skip next instruction if Vx!=nn
                if(V[x] != nn) pc += 2;
                break;
            case 0x5000: // skip next instruction if Vx=Vy
                if(V[x] == V[y]) pc += 2;
                break;
            case 0x9000: // skip next instruction if Vx!=Vy
                if(V[x] != V[y]) pc += 2;
                break;
            case 0x6000: // sets Vx to nn
                V[x] = nn;
                V[x] &= 0xFF;
                break;
            case 0x7000: // adds nn to Vx
                V[x] += nn;
                V[x] &= 0xFF;
                break;
            case 0x8000:
                handle8(opcode, x, y);
                break;
            case 0xA000: // Set index register I to nnn
                I = nnn;
                break;
            case 0xB000: // Jump with offset. Jumps pc to nnn plus the value of V0
                pc = nnn + V[0];
                break;
                /*
                case 0xB000: // Jump with offset new behavior. Make toggleable. Jumps pc to nnn plus the value of Vx
                pc = nnn + V[x];
                break;
                 */
            case 0xC000: // Randomizer. Generates a random number, bitwise ANDs it with the value NN, and puts the result in vx
                Random rand = new Random();
                V[x] = rand.nextInt() & nn;
                break;
            case 0xD000:
                // Getting the starting coordinates of the sprite. Modulo 64 and 32 (Same as bitwise AND 63 and 31) wraps the coordinates around the display
                int startX = V[x] & 63;
                int startY = V[y] & 31;

                // Setting the collision flag to 0. Set it to 1 if any pixel on screen is flipped from on to off (true to false)
                V[0xF] = 0;

                /* Loop through all n rows of the sprite, then the eight bits (pixels/columns).
                 * I is where the sprite data begins.
                 * The starting position wraps around but not actual sprites
                 */
                for(int row = 0; row < n; row++) {

                    int spriteByte = memory[I + row] & 0xFF;

                    for(int col = 0; col < 8; col++) {
                        int spritePixel = spriteByte & (0x80 >> col);
                        if(spritePixel != 0) {
                            int drawX = startX + col;
                            int drawY = startY + row;
                            if(drawX < 64 && drawY < 32) {
                                int index = screenIndex(drawX, drawY);
                                if(framebuffer[index]) V[0xF] = 1; // Collision checker
                                framebuffer[index] ^= true; // XORs the pixel
                            }
                        }
                    }
                }

                break;
            case 0xE000:
                switch (opcode & 0x00FF) {
                    case 0x009E:
                        if(keys[V[x]]) pc += 2;
                        break;
                    case 0x00A1:
                        if(!keys[V[x]]) pc += 2;
                        break;
                }
                break;
            case 0xF000:
                handleF(opcode, x);
                break;
            default:
                System.out.printf("""
                        Invalid opcode: %d
                        firstNibble: %d
                        x: %d
                        y: %d
                        n: %d
                        nn: %d
                        nnn: %d
                        """, opcode, firstNibble, x, y, n, nn, nnn);
                break;
        }

    }

    private int screenIndex(int x, int y) {
        return 64 * y + x;
    }

    private void handle0(int opcode) {
        switch (opcode) {
            case 0x0000: // Does nothing :D
                break;
            case 0x00E0: // Turns off screen
                Arrays.fill(framebuffer, false);
                break;
            case 0x00EE:
                sp--;
                pc = stack[sp];
                break;
            default:
                break;
        }
    }
    private void handle8(int opcode, int x, int y) {
        switch (opcode & 0x000F) {
            case 0x0000: // sets Vx to Vy
                V[x] = V[y];
                break;
            case 0x0001: // bitwise or
                V[x] = V[x] | V[y];
                break;
            case 0x0002: // bitwise and
                V[x] = V[x] & V[y];
                break;
            case 0x0003: // bitwise xor
                V[x] = V[x] ^ V[y];
                break;
            case 0x0004: { // adds Vy to Vx and stores in Vx with carry. Flag Vf set to 1 if a carry is triggered, 0 if not
                int vx = V[x];
                int vy = V[y];
                int sum = vx + vy;
                V[x] = sum & 0xFF;
                V[0xF] = (sum > 255) ? 1 : 0;
                break;
            }
            case 0x0005: { // subtracts Vy from Vx and stores in Vx with underflow. Flag Vf set to 1 if Vx > Vy and there was no underflow, 0 if not
                int vx = V[x];
                int vy = V[y];
                V[x] = (vx - vy) & 0xFF;
                V[0xF] = (vx >= vy) ? 1 : 0;
                break;
            }
            case 0x0007: { // subtracts Vx from Vy and stores in Vx with underflow. Flag Vf set to 1 if Vy > Vx and there was no underflow, 0 if not
                int vx = V[x];
                int vy = V[y];
                V[x] = (vy - vx) & 0xFF;
                V[0xF] = (vy >= vx) ? 1 : 0;
                break;
            }
            case 0x0006: { // Shifts Vx to the right by one bit
                int vy = V[y];
                int flag = vy & 1;
                V[x] = (vy >> 1) & 0xFF;
                V[0xF] = flag;
                break;
            }
            case 0x000E: { // Shifts Vx to the left by one bit
                int vy = V[y];
                int flag = (vy >> 7) & 1;
                V[x] = (vy << 1) & 0xFF;
                V[0xF] = flag;
                break;
            }
                /* Old behavior for bitshifts. Sets Vx to Vy, behavior is otherwise identical. Make a toggle.
            case 0x0006: // Shifts Vx to the right by one bit
                V[x] = V[y];
                V[0xF] = V[x] & 0x1;
                V[x] >>= 1;
                break;
            case 0x000E: // Shifts Vx to the left by one bit
                V[x] = V[y];
                V[0xF] = (V[x] & 0x80) >> 7;
                V[x] <<= 1;
                break;
                 */
            default:
                break;
        }
    }
    private void handleF(int opcode, int x) {
        switch(opcode & 0x00FF) {
            case 0x0007: // Sets Vx to delay timer
                V[x] = delayTimer;
                break;
            case 0x0015: // Sets delay timer to Vx
                delayTimer = V[x];
                break;
            case 0x0018: // Sets sound timer to Vx
                soundTimer = V[x];
                break;
            case 0x001E: // Adds Vx to I
                I += (V[x] & 0xFF);
                break;
            case 0x000A: // Halt until any key is pressed and store it in Vx
                boolean pressed = false;
                for(int i = 0; i < 16; i++) {
                    if(keys[i]) {
                        V[x] = i;
                        pressed = true;
                        break;
                    }
                }
                if(!pressed) pc -= 2;
                break;
            case 0x0029: // St
                I = 0x050 + ((V[x] & 0x0F) * 5);
                break;
            case 0x0033: // Convert VX into decimal digits and stores them into concurrent memory addresses
                int val = V[x] & 0xFF;

                memory[I] = (byte)(val / 100);
                memory[I + 1] = (byte)((val / 10) % 10);
                memory[I + 2] = (byte)(val % 10);
                break;
            case 0x0055: // Store everything from V0 to VX in memory;
                for(int i = 0; i <= x; i++) {
                    memory[I + i] = (byte)(V[i] & 0xFF);
                }
                break;
            case 0x0065: // Load memory into V0 through VX
                for(int i = 0; i <= x; i++) {
                    V[i] = memory[I + i] & 0xFF;
                }
                break;
            default:
                break;
        }
    }

    public void updateTimers() {
        if(delayTimer > 0) delayTimer--;
        if(soundTimer > 0) {
            soundTimer--;
            if(soundTimer == 1) {
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    public void setKey(int keyIndex, boolean isPressed) {
        if(keyIndex >= 0 && keyIndex < 16) keys[keyIndex] = isPressed;
    }
}
