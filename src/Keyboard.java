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

import javax.swing.*;
import java.awt.event.ActionEvent;

public class Keyboard extends JComponent{

    private Display display;

    private String keyboardType; //Types: Qwerty, Qwertz, Azerty
    public boolean[] keysPressed = new boolean[16];

    // inner class to handle keyboard presses
    class keyboardAction extends AbstractAction {

        // key is a hexadecimal number from 0 to F
        int key;
        // press tells whether it's a button press or release
        boolean press;

        // constructor
        public keyboardAction(int key, boolean press){
            this.key = key;
            this.press = press;
        }

        public void actionPerformed(ActionEvent e) {
            keysPressed[key] = press;
        }

    }


    // constructors
    public Keyboard(Display display){
        this.display = display;
        setupKeyboard(Chip8.DEFAULT_KEYBOARD_TYPE);
    }

    public Keyboard(Display display, String keyboardType){
        this.display = display;
        // default is Qwerty
        if(keyboardType.equals("Qwertz") | keyboardType.equals("Azerty")) {
            setupKeyboard(keyboardType);
        } else {
            setupKeyboard("Qwerty");
        }
    }

    // sets keyboard up as Qwerty, Qwertz or Azerty (possible arguments for keyboardType)
    public void setupKeyboard(String keyboardType){

        this.keyboardType = keyboardType;

        Action press0 = new keyboardAction(0x0, true);
        Action release0 = new keyboardAction(0x0, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("X"),"press0");
        display.getActionMap().put("press0",press0);
        display.getInputMap().put(KeyStroke.getKeyStroke("released X"),"release0");
        display.getActionMap().put("release0",release0);


        Action press1 = new Keyboard.keyboardAction(0x1, true);
        Action release1 = new keyboardAction(0x1, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("1"),"press1");
        display.getActionMap().put("press1",press1);
        display.getInputMap().put(KeyStroke.getKeyStroke("released 1"),"release1");
        display.getActionMap().put("release1",release1);


        Action press2 = new Keyboard.keyboardAction(0x2, true);
        Action release2 = new keyboardAction(0x2, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("2"),"press2");
        display.getActionMap().put("press2",press2);
        display.getInputMap().put(KeyStroke.getKeyStroke("released 2"),"release2");
        display.getActionMap().put("release2",release2);


        Action press3 = new Keyboard.keyboardAction(0x3, true);
        Action release3 = new keyboardAction(0x3, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("3"),"press3");
        display.getActionMap().put("press3",press3);
        display.getInputMap().put(KeyStroke.getKeyStroke("released 3"),"release3");
        display.getActionMap().put("release3",release3);


        Action press4 = new Keyboard.keyboardAction(0x4, true);
        Action release4 = new keyboardAction(0x4, false);
        if(keyboardType.equals("Qwerty") | keyboardType.equals("Qwertz")) {
            display.getInputMap().put(KeyStroke.getKeyStroke("Q"), "press4");
            display.getInputMap().put(KeyStroke.getKeyStroke("released Q"), "release4");
        } else {
            display.getInputMap().put(KeyStroke.getKeyStroke("A"), "press4");
            display.getInputMap().put(KeyStroke.getKeyStroke("released A"), "release4");
        }
        display.getActionMap().put("press4", press4);
        display.getActionMap().put("release4", release4);


        Action press5 = new Keyboard.keyboardAction(0x5, true);
        Action release5 = new keyboardAction(0x5, false);
        if(keyboardType.equals("Qwerty") | keyboardType.equals("Qwertz")) {
            display.getInputMap().put(KeyStroke.getKeyStroke("W"), "press5");
            display.getInputMap().put(KeyStroke.getKeyStroke("released W"), "release5");
        } else {
            display.getInputMap().put(KeyStroke.getKeyStroke("Z"), "press5");
            display.getInputMap().put(KeyStroke.getKeyStroke("released Z"), "release5");
        }
        display.getActionMap().put("press5", press5);
        display.getActionMap().put("release5", release5);


        Action press6 = new Keyboard.keyboardAction(0x6, true);
        Action release6 = new keyboardAction(0x6, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("E"),"press6");
        display.getActionMap().put("press6",press6);
        display.getInputMap().put(KeyStroke.getKeyStroke("released E"),"release6");
        display.getActionMap().put("release6",release6);


        Action press7 = new Keyboard.keyboardAction(0x7, true);
        Action release7 = new keyboardAction(0x7, false);
        if(keyboardType.equals("Qwerty") | keyboardType.equals("Qwertz")) {
            display.getInputMap().put(KeyStroke.getKeyStroke("A"), "press7");
            display.getInputMap().put(KeyStroke.getKeyStroke("released A"), "release7");
        } else {
            display.getInputMap().put(KeyStroke.getKeyStroke("Q"), "press7");
            display.getInputMap().put(KeyStroke.getKeyStroke("released Q"), "release7");
        }
        display.getActionMap().put("press7", press7);
        display.getActionMap().put("release7", release7);


        Action press8 = new Keyboard.keyboardAction(0x8, true);
        Action release8 = new keyboardAction(0x8, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("S"),"press8");
        display.getActionMap().put("press8",press8);
        display.getInputMap().put(KeyStroke.getKeyStroke("released S"),"release8");
        display.getActionMap().put("release8",release8);


        Action press9 = new Keyboard.keyboardAction(0x9, true);
        Action release9 = new keyboardAction(0x9, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("D"),"press9");
        display.getActionMap().put("press9",press9);
        display.getInputMap().put(KeyStroke.getKeyStroke("released D"),"release9");
        display.getActionMap().put("release9",release9);


        Action pressA = new Keyboard.keyboardAction(0xa, true);
        Action releaseA = new keyboardAction(0xa, false);
        if (keyboardType.equals("Qwerty")) {
            display.getInputMap().put(KeyStroke.getKeyStroke("Z"), "pressA");
            display.getInputMap().put(KeyStroke.getKeyStroke("released Z"), "releaseA");
        } else if (keyboardType.equals("Qwertz")) {
            display.getInputMap().put(KeyStroke.getKeyStroke("Y"), "pressA");
            display.getInputMap().put(KeyStroke.getKeyStroke("released Y"), "releaseA");
        } else {
            display.getInputMap().put(KeyStroke.getKeyStroke("W"), "pressA");
            display.getInputMap().put(KeyStroke.getKeyStroke("released W"), "releaseA");
        }
        display.getActionMap().put("pressA", pressA);
        display.getActionMap().put("releaseA", releaseA);


        Action pressB = new Keyboard.keyboardAction(0xb, true);
        Action releaseB = new keyboardAction(0xb, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("C"),"pressB");
        display.getActionMap().put("pressB",pressB);
        display.getInputMap().put(KeyStroke.getKeyStroke("released C"),"releaseB");
        display.getActionMap().put("releaseB",releaseB);


        Action pressC = new Keyboard.keyboardAction(0xc, true);
        Action releaseC = new keyboardAction(0xc, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("4"),"pressC");
        display.getActionMap().put("pressC",pressC);
        display.getInputMap().put(KeyStroke.getKeyStroke("released 4"),"releaseC");
        display.getActionMap().put("releaseC",releaseC);


        Action pressD = new Keyboard.keyboardAction(0xd, true);
        Action releaseD = new keyboardAction(0xd, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("R"),"pressD");
        display.getActionMap().put("pressD",pressD);
        display.getInputMap().put(KeyStroke.getKeyStroke("released R"),"releaseD");
        display.getActionMap().put("releaseD",releaseD);


        Action pressE = new Keyboard.keyboardAction(0xe, true);
        Action releaseE = new keyboardAction(0xe, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("F"),"pressE");
        display.getActionMap().put("pressE",pressE);
        display.getInputMap().put(KeyStroke.getKeyStroke("released F"),"releaseE");
        display.getActionMap().put("releaseE",releaseE);


        Action pressF = new Keyboard.keyboardAction(0xf, true);
        Action releaseF = new keyboardAction(0xf, false);
        display.getInputMap().put(KeyStroke.getKeyStroke("V"),"pressF");
        display.getActionMap().put("pressF",pressF);
        display.getInputMap().put(KeyStroke.getKeyStroke("released V"),"releaseF");
        display.getActionMap().put("releaseF",releaseF);

    }

    public String getKeyboardType(){
        return keyboardType;
    }

}
