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

import java.awt.*;
import javax.swing.*;

public class Display extends JPanel {

    private Graphics g;
    private int scale;
    private boolean[][] displayMatrix = new boolean[64][32];

    private final Color colorA = new Color(101,179,166);
    private final Color colorB = new Color(13,28,99);

    // constructor
    public Display(){
        setScale(Chip8.DEFAULT_SCALE);
    }

    public Display(int scale) {
        setScale(scale);
    }

    public void paintComponent(Graphics g){
        this.g = g;
        drawScreen();
    }

    public boolean changePixel(int x, int y){
        boolean isSet = displayMatrix[x][y];
        displayMatrix[x][y] = !isSet;
        return isSet;
    }

    private void drawScreen(){
        Color color;
        for (int x=0; x<64; x++){
            for (int y=0; y<32; y++){
                if(displayMatrix[x][y]) {
                    color = colorA;
                } else {
                    color = colorB;
                }
                drawPixel(color,scale,x,y);
            }
        }
    }

    public void clearScreen(){
        for (int x=0; x<64; x++){
            for (int y=0; y<32; y++){
                drawPixel(colorB,scale,x,y);
                displayMatrix[x][y] = false;
            }
        }
    }

    private void drawPixel(Color color, int scale, int x, int y){
        g.setColor(color);
        g.fillRect(scale*x,scale*y,scale,scale);
    }

    public int getScale(){
        return scale;
    }

    public void setScale(int newScale){
        scale = newScale;
    }
}
