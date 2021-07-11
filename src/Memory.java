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

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;

public class Memory {

    private final int memorySize = 4096;
    private byte[] memory = new byte[memorySize];

    private final int[] intFont;
    {
        intFont = new int[]{0xf0, 0x90, 0x90, 0x90, 0xf0, //0
                            0x20, 0x60, 0x20, 0x20, 0x70, //1
                            0xf0, 0x10, 0xf0, 0x80, 0xf0, //2
                            0xf0, 0x10, 0xf0, 0x10, 0xf0, //3
                            0x90, 0x90, 0xf0, 0x10, 0x10, //4
                            0xf0, 0x80, 0xf0, 0x10, 0xf0, //5
                            0xf0, 0x80, 0xf0, 0x90, 0xf0, //6
                            0xf0, 0x10, 0x20, 0x40, 0x40, //7
                            0xf0, 0x90, 0xf0, 0x90, 0xf0, //8
                            0xf0, 0x90, 0xf0, 0x10, 0xf0, //9
                            0xf0, 0x90, 0xf0, 0x90, 0x90, //A
                            0xe0, 0x90, 0xe0, 0x90, 0xe0, //B
                            0xf0, 0x80, 0x80, 0x80, 0xf0, //C
                            0xe0, 0x90, 0x90, 0x90, 0xe0, //D
                            0xf0, 0x80, 0xf0, 0x80, 0xf0, //E
                            0xf0, 0x80, 0xf0, 0x80, 0x80  //F
        };
    }

    private int memoryUsed = 0x9f;

    private final byte[] font = intArrayToByteArray(intFont);

    public Deque<Integer> stack = new ArrayDeque<>();

    // Constructors
    public Memory() {
        writeToMemory(0x50,font,true);
    }

    public Memory(File romFile) {
        this();
        loadRom(romFile);
    }


    // Methods
    public void clearMemory(){
        for(int i=0x200; i<0xFFF; i++){
            writeByteToMemory(i,(byte) 0,false);
            memoryUsed = 0x9f;
        }
    }

    public void loadRom(File romFile){
        try {
            clearMemory();
            byte[] romArray = Files.readAllBytes(romFile.toPath());
            writeToMemory(0x200,romArray,true);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void writeByteToMemory(int address, byte toEnter, boolean log){
        if(address > memorySize | address < 0){
            throw new IllegalArgumentException("Trying to write outside of memory.");
        } else{
            memory[address] = toEnter;
            if(log) {
                memoryUsed = Math.max(address, memoryUsed);
            }
        }
    }

    public void writeToMemory(int address, byte[] toEnter, boolean log){
        if(toEnter.length + address > memorySize | address < 0){
            throw new IllegalArgumentException("Trying to write outside of memory.");
        } else{
            int i = address;
            for(byte entry : toEnter){
                memory[i] = entry;
                i++;
            }
            if(log) {
                memoryUsed = Math.max(toEnter.length + address, memoryUsed);
            }
        }
    }

    public byte readMemory(int address){
        if(address > memorySize | address < 0){
            throw new IllegalArgumentException("Trying to read outside of memory.");
        } else{
            return memory[address];
        }
    }

    // for debugging
    public void printMemory(){
        System.out.println("MEMORY");
        for(int i = 0; i<memorySize; i++){
            if(i % 16 == 0){
                System.out.println();
                System.out.printf("%02x",i/16);
                System.out.print("   ");
            } else if(i % 8 == 0){
                System.out.print("   ");
            } else {
                System.out.print(" ");
            }
            System.out.printf("%02x",memory[i]);
        }
    }

    public void printMemory(int from, int to){
        // we want multiples of 16
        int from16 = from - (from % 16);
        int to16 = to - (to % 16) + 16;

        System.out.println("MEMORY");
        for(int i = from16; i<to16; i++){
            if(i % 16 == 0){
                System.out.println();
                System.out.printf("%02x",i/16);
                System.out.print("   ");
            } else if(i % 8 == 0){
                System.out.print("   ");
            } else {
                System.out.print(" ");
            }
            System.out.printf("%02x",memory[i]);
        }
        System.out.println("\n");
    }

    public String memoryTable(){
        int to = memoryUsed;
        int to16 = to - (to % 16) + 16;
        int i = 0x200;

        String table = "<table>";
        while(i < to16){
            table += "<tr><td style=\"color:#8E30BB\">" + Integer.toHexString(i) + "</td>";
            for(int j=0; j<16; j++){
                table += "<td>" + Integer.toHexString(memory[i + j] & 0xff) + "</td>";
            }
            table += "</tr>";
            i += 16;
        }
        table += "</table>";
        return table;
    }

    public static byte[] intArrayToByteArray(int[] toConvert){
        byte[] a = new byte[toConvert.length];
        int i = 0;
        for(int x : toConvert){
            a[i] = (byte) x;
            i++;
        }
        return a;
    }

    public int getMemoryUsed (){
        return memoryUsed;
    }

}
