/*
This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import java.util.Timer;
//import java.util.TimerTask;

public class CPU {
    Memory memory;
    Display display;
    Keyboard keyboard;

    // index register
    // index and pc are 16-bit so short would suffice but would entail a lot of casting
    private int index;

    // program counter
    private int pc = 0x200;
    // stores the current program counter value when we execute an instruction
    // just for debugging purposes
    private int oldpc;

    // variable registers (8 bit)
    private int[] variableRegisters = new int[16];

    // timers
    private byte delayTimer = 0;
    private byte soundTimer = 0;

    private Timer timer = new Timer();

    // variables to determine which type of shift instructions (8xy6 and 8xye), respectively
    // read from memory and write to memory instructions (fx55 and fx65 are used),
    // respectively jump with offset (bnnn)
    private boolean originalShiftInstructions = true;
    private boolean originalReadWriteMemoryInstructions = true;
    private boolean originalJumpWithOffsetInstructions = true;

    // constructors
    public CPU(Memory memory, Display display, Keyboard keyboard) {
        this.memory = memory;
        this.display = display;
        this.keyboard = keyboard;

        // the timers are decremented every 17ms which is about 60 times per second
        /*timer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                decrementTimers();
            }
        }, 0L, 17L);*/
    }

    public CPU(Memory memory, Display display, Keyboard keyboard, boolean originalShiftInstructions, boolean originalReadWriteMemoryInstructions, boolean originalJumpWithOffsetInstructions) {
        this.memory = memory;
        this.display = display;
        this.keyboard = keyboard;

        this.originalShiftInstructions = originalShiftInstructions;
        this.originalReadWriteMemoryInstructions = originalReadWriteMemoryInstructions;
        this.originalJumpWithOffsetInstructions = originalJumpWithOffsetInstructions;

        // the timers are decremented every 17ms which is about 60 times per second
        /*timer.scheduleAtFixedRate(new TimerTask(){
            @Override
            public void run(){
                decrementTimers();
            }
        }, 0L, 17L);*/
    }


    // methods

    // executes one instruction and returns the executed instruction
    // as an array containing the high and low byte
    public int[] executeOneInstruction(){

        // fetch instruction
        int instructionHigh = memory.readMemory(pc);
        int instructionLow = memory.readMemory(pc + 1);

        int a = (instructionHigh & 0xf0) / 0x10;
        int x = instructionHigh & 0x0f;
        int y = (instructionLow & 0xf0) / 0x10;
        int n = instructionLow & 0x0f;
        int nn = instructionLow & 0xff;
        int nnn = (256 * x) + nn;

        // program counter is already set to point at the next instruction
        oldpc = pc;
        pc += 2;

        // decode the current instruction and execute it
        switch(a){
            case 0x0:
                switch(nnn){
                    case 0x0e0:
                        display.clearScreen();
                        break;
                    case 0x0ee:
                        pc = memory.stack.pop();
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal instruction.");
                }
                break;

            case 0x1:
                pc = nnn;
                break;

            case 0x2:
                memory.stack.push(pc);
                pc = nnn;
                break;

            case 0x3:
                if(nn == (variableRegisters[x] & 0xff)){
                    pc += 2;
                }
                break;

            case 0x4:
                if(nn != (variableRegisters[x] & 0xff)){
                    pc += 2;
                }
                break;

            case 0x5:
                if(n == 0){
                    if (variableRegisters[x] == variableRegisters[y]){
                        pc += 2;
                    }
                } else{
                    throw new IllegalArgumentException("Illegal instruction.");
                }
                break;

            case 0x6:
                variableRegisters[x] = nn;
                break;

            case 0x7:
                variableRegisters[x] = (variableRegisters[x] + nn) & 0xff;
                break;

            case 0x8:
                switch(n){
                    case 0x0:
                        variableRegisters[x] = variableRegisters[y];
                        break;

                    case 0x1:
                        variableRegisters[x] = variableRegisters[x] | variableRegisters[y];
                        break;

                    case 0x2:
                        variableRegisters[x] = variableRegisters[x] & variableRegisters[y];
                        break;

                    case 0x3:
                        variableRegisters[x] = variableRegisters[x] ^ variableRegisters[y];
                        break;

                    case 0x4:
                        variableRegisters[x] = variableRegisters[x] + variableRegisters[y];
                        if(variableRegisters[x] > 255){
                            variableRegisters[x] = variableRegisters[x] & 0xff;
                            variableRegisters[0xf] = 1;
                        } else{
                            variableRegisters[0xf] = 0;
                        }
                        break;

                    case 0x5:
                        variableRegisters[x] = variableRegisters[x] - variableRegisters[y];
                        if(variableRegisters[x] > 0){
                            variableRegisters[0xf] = 1;
                        } else{
                            variableRegisters[0xf] = 0;
                            variableRegisters[x] = variableRegisters[x] & 0xff;
                        }
                        break;

                    case 0x6:
                        // ambiguous instruction!
                        if(originalShiftInstructions) {
                            // the bit that gets shifted out gets written to VF
                            variableRegisters[0xf] = variableRegisters[y] & 1;
                            variableRegisters[x] = variableRegisters[y] >> 1;
                        } else{
                            variableRegisters[0xf] = variableRegisters[x] & 1;
                            variableRegisters[x] = variableRegisters[x] >> 1;
                        }
                        break;

                    case 0x7:
                        variableRegisters[x] = variableRegisters[y] - variableRegisters[x];
                        if(variableRegisters[x] > 0){
                            variableRegisters[0xf] = 1;
                        } else{
                            variableRegisters[0xf] = 0;
                            variableRegisters[x] = variableRegisters[x] & 0xff;
                        }
                        break;

                    case 0xe:
                        // ambiguous instruction!
                        if(originalShiftInstructions) {
                            // the bit that gets shifted out gets written to VF
                            variableRegisters[0xf] = variableRegisters[y] >> 7;
                            // we need to take care that it's still a byte
                            variableRegisters[x] = (variableRegisters[y] << 1) & 0xff;
                        } else{
                            variableRegisters[0xf] = variableRegisters[x] >> 7;
                            variableRegisters[x] = (variableRegisters[x] << 1) & 0xff;
                        }
                        break;

                    default:
                        throw new IllegalStateException("Illegal instruction.");
                }
                break;

            case 0x9:
                if(n == 0){
                    if (variableRegisters[x] != variableRegisters[y]){
                        pc += 2;
                    }
                } else{
                    throw new IllegalArgumentException("Illegal instruction.");
                }
                break;

            case 0xa:
                index = nnn;
                break;

            case 0xb:
                if(originalJumpWithOffsetInstructions) {
                    pc = (nnn + variableRegisters[0]) & 0xfff;
                } else {
                    pc = (nnn + variableRegisters[x]) & 0xfff;
                }
                break;

            case 0xc:
                variableRegisters[x] = ((int) (256 * Math.random())) & nn;
                break;

            case 0xd:
                int xPosition = variableRegisters[x] & 0x3f;
                int yPosition = variableRegisters[y] & 0x1f;

                variableRegisters[0xf] = 0;

                for (int row = 0; row < n; row++) {
                    if (yPosition + row > 31) {
                        break;
                    } else {
                        int p = 128;
                        for (int column = 0; column < 8; column++) {
                            if (xPosition + column > 63) {
                                break;
                            } else {
                                if ((memory.readMemory(index + row) & p) == p) {
                                    boolean switchedOff = display.changePixel(xPosition + column, yPosition + row);
                                    if (switchedOff) {
                                        variableRegisters[0xf] = 1;
                                    }
                                }
                                p /= 2;
                            }
                        }
                    }
                }
                break;

            case 0xe:
                switch(nn){
                    case 0x9e:
                        if(variableRegisters[x] >= 0 & variableRegisters[x] < 16) {
                            if (keyboard.keysPressed[variableRegisters[x]]) {
                                pc += 2;
                            }
                        } else {
                            throw new IllegalArgumentException("Trying to get input from a non-existing key.");
                        }
                        break;

                    case 0xa1:
                        if(!keyboard.keysPressed[variableRegisters[x]]){
                            pc += 2;
                        }
                        break;
                }
                break;

            case 0xf:
                switch(nn){
                    case 0x07:
                        variableRegisters[x] = delayTimer & 0xff;
                        break;

                    case 0x0a:
                        for (int i = 0; i < 16; i++) {
                            if(keyboard.keysPressed[i]){
                                variableRegisters[x] = i;
                                pc += 2;
                                break;
                            }
                        }
                        pc -= 2;
                        break;

                    case 0x15:
                        delayTimer = (byte) variableRegisters[x];
                        break;

                    case 0x18:
                        soundTimer = (byte) variableRegisters[x];
                        break;

                    case 0x1e:
                        index = index + variableRegisters[x];
                        if(index > 0xfff){
                            variableRegisters[0xf] = 1;
                            index = index & 0xfff;
                        }
                        break;

                    case 0x29:
                        index = 0x50 + (5 * (variableRegisters[x] & 0xf));
                        break;

                    case 0x33:
                        byte onesDigit = (byte) (variableRegisters[x] % 10);
                        byte tensDigit = (byte) ((variableRegisters[x] / 10) % 10);
                        byte hundredsDigit = (byte) ((variableRegisters[x] / 100) % 10);
                        byte[] digitsArray = {hundredsDigit, tensDigit, onesDigit};

                        memory.writeToMemory(index,digitsArray,true);
                        break;

                    case 0x55:
                        // ambiguous instruction!
                        for(int i=0; i<=x; i++){
                            memory.writeByteToMemory(index + i, (byte) variableRegisters[i],true);
                        }
                        if(originalReadWriteMemoryInstructions) {
                            index = index + x + 1;
                        }
                        break;

                    case 0x65:
                        // ambiguous instruction!
                        for(int i=0; i<=x; i++){
                            variableRegisters[i] = memory.readMemory(index+i) & 0xff;
                        }
                        if(originalReadWriteMemoryInstructions) {
                            index = index + x + 1;
                        }
                        break;
                }
                break;

            default:
                throw new IllegalStateException("Illegal instruction.");
        }

        return (new int[]{0x10 * a + x, nn, oldpc});

    }

    public String getMnemonicFromInstruction(int[] instruction){

        int a = (instruction[0] & 0xf0) / 0x10;
        int x = instruction[0] & 0x0f;
        int y = (instruction[1] & 0xf0) / 0x10;
        int n = instruction[1] & 0x0f;
        int nn = instruction[1] & 0xff;
        int nnn = (256 * x) + nn;

        switch(a){
            case 0x0:
                switch(nnn){
                    case 0x0e0:
                        return "CLS";
                    case 0x0ee:
                        return "RET";
                    default:
                        return "Illegal instruction";
                }

            case 0x1:
                return ("JP " + Integer.toHexString(nnn));

            case 0x2:
                return ("CALL " + Integer.toHexString(nnn));

            case 0x3:
                return ("SE V" + Integer.toHexString(x) + ", " + Integer.toHexString(nn));

            case 0x4:
                return ("SNE V" + Integer.toHexString(x) + ", " + Integer.toHexString(nn));

            case 0x5:
                if(n == 0){
                    return ("SE V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y));
                } else{
                    return "Illegal instruction";
                }

            case 0x6:
                return ("LD V" + Integer.toHexString(x) + ", " + Integer.toHexString(nn));

            case 0x7:
                return ("ADD V" + Integer.toHexString(x) + ", " + Integer.toHexString(nn));

            case 0x8:
                switch(n){
                    case 0x0:
                        return ("LD V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y));

                    case 0x1:
                        return ("OR V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y));

                    case 0x2:
                        return ("AND V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y));

                    case 0x3:
                        return ("XOR V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y));

                    case 0x4:
                        return ("ADD V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y));

                    case 0x5:
                        return ("SUB V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y));

                    case 0x6:
                        return ("SHR V" + Integer.toHexString(x));

                    case 0x7:
                        return ("SUBN V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y));

                    case 0xe:
                        return ("SHL V" + Integer.toHexString(x));

                    default:
                        return "Illegal instruction";
                }

            case 0x9:
                if(n == 0){
                    return ("SNE V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y));
                } else{
                    return "Illegal instruction";
                }

            case 0xa:
                return ("LD I, " + Integer.toHexString(nnn));

            case 0xb:
                return ("JP V0, " + Integer.toHexString(nnn));

            case 0xc:
                return ("RND V" + Integer.toHexString(x) + ", " + Integer.toHexString(nn));

            case 0xd:
                return ("DRW V" + Integer.toHexString(x) + ", V" + Integer.toHexString(y) + ", " + Integer.toHexString(n));

            case 0xe:
                switch(nn){
                    case 0x9e:
                        return ("SKP V" + Integer.toHexString(x));

                    case 0xa1:
                        return ("SKNP V" + Integer.toHexString(x));
                }

            case 0xf:
                switch(nn){
                    case 0x07:
                        return ("LD V" + Integer.toHexString(x) + ", DT");

                    case 0x0a:
                        return ("LD V" + Integer.toHexString(x) + ", K");

                    case 0x15:
                        return ("LD DT, V" + Integer.toHexString(x));

                    case 0x18:
                        return ("LD ST, V" + Integer.toHexString(x));

                    case 0x1e:
                        return ("ADD I, V" + Integer.toHexString(x));

                    case 0x29:
                        return ("LD F, V" + Integer.toHexString(x));

                    case 0x33:
                        return ("LD B, V" + Integer.toHexString(x));

                    case 0x55:
                        return ("LD [I], V" + Integer.toHexString(x));

                    case 0x65:
                        return ("LD V" + Integer.toHexString(x) + ", [I]");
                }

            default:
                return "Illegal instruction";
        }

    }

    public boolean getOriginalShiftInstructions(){
        return originalShiftInstructions;
    }

    public void setOriginalShiftInstructions(boolean b){
        originalShiftInstructions = b;
    }

    public boolean getOriginalReadWriteMemoryInstructions(){
        return originalReadWriteMemoryInstructions;
    }

    public void setOriginalReadWriteMemoryInstructions(boolean b){
        originalReadWriteMemoryInstructions = b;
    }

    public boolean getOriginalJumpWithOffsetInstructions(){
        return originalJumpWithOffsetInstructions;
    }

    public void setOriginalJumpWithOffsetInstructions(boolean b){
        originalJumpWithOffsetInstructions = b;
    }

    public void decrementTimers(){
        if(delayTimer != 0){
            delayTimer--;
        }

        if(soundTimer != 0){
            soundTimer--;
        }
    }

    public int[] getNextInstruction(){
        return (new int[]{memory.readMemory(pc) & 0xff,memory.readMemory(pc + 1) & 0xff,pc});
    }

    public void printRegisters(){
        System.out.println("REGISTERS");
        System.out.print("Index  ");
        System.out.printf("%4x",index);
        System.out.println();
        System.out.print("PC     ");
        System.out.printf("%4x",pc);
        System.out.println();
        for(int i=0; i<16; i++){
            System.out.print("V");
            System.out.printf("%1x",i);
            System.out.print("       ");
            System.out.printf("%2x",variableRegisters[i]);
            System.out.println();
        }
        System.out.println();
    }

    public String registersTable(){
        String table = "<table>";
        table += "<tr><td width=\"50\" style=\"color:#8E30BB\">I</td><td width=\"50\">" + Integer.toHexString(index & 0xffff) + "</td><td width=\"50\" style=\"color:#8E30BB\">PC</td><td width=\"50\">" + Integer.toHexString(pc & 0xffff) + "</td></tr>";
        for(int i=0; i<16; i+=2) {
            table += "<tr><td style=\"color:#8E30BB\">V" + Integer.toHexString(i) + "</td><td>" + Integer.toHexString(variableRegisters[i] & 0xff) + "</td><td style=\"color:#8E30BB\">V" + Integer.toHexString(i + 1) + "</td><td>" + Integer.toHexString(variableRegisters[i + 1] & 0xff) + "</td></tr>";
        }
        table += "</table>";

        return table;
    }

}