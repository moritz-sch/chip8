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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;

public class Chip8 {

    public final static int DEFAULT_SCALE = 10;
    public final static String DEFAULT_KEYBOARD_TYPE = "Qwerty";
    public final static byte DEFAULT_CYCLE_TIME = 2;
    public final static int DEFAULT_INSTRUCTIONS_PER_TIMER_CYCLE = 9;

    private final ImageIcon icon = new ImageIcon("chip8icon.png");

    File openedFile;
    Memory memory = new Memory();
    Display display = new Display();
    Keyboard keyboard = new Keyboard(display);
    CPU cpu = new CPU(memory, display, keyboard);

    private byte cycleTime = DEFAULT_CYCLE_TIME;
    private int instructionsPerTimerCycle = DEFAULT_INSTRUCTIONS_PER_TIMER_CYCLE;

    private boolean isPaused = false;
    private boolean isFileLoaded = false;
    private boolean printInstructions = false;

    private boolean fileChooserOpen = false;

    // the interface is used to update the memory window after an instruction
    // the window is an observer, this class the subject
    interface instructionExecutedListener {
        void instructionWasExecuted();
    }

    // the list of listeners (which will only contain the memory window if it is open)
    private ArrayList<instructionExecutedListener> listeners = new ArrayList<>();

    public void addListener(instructionExecutedListener toAdd){
        listeners.add(toAdd);
    }

    public void notifyListeners(){
        for (instructionExecutedListener listener : listeners){
            listener.instructionWasExecuted();
        }
    }


    // open a CHIP-8 program
    private void openFile(File file){
        openedFile = file;
        memory = new Memory(openedFile);
        cpu = new CPU(memory, display, keyboard, cpu.getOriginalShiftInstructions(), cpu.getOriginalReadWriteMemoryInstructions(), cpu.getOriginalJumpWithOffsetInstructions());

        isFileLoaded = true;
    }

    private void printInstructionInfo(int[] instruction){
        System.out.printf("%04x",instruction[2]);
        System.out.print("      ");
        System.out.printf("%02x",instruction[0]);
        System.out.print(" ");
        System.out.printf("%02x",instruction[1]);
        System.out.print("    " + cpu.getMnemonicFromInstruction(instruction) + "\n");
    }

    private int[] executeOneInstruction(){
        int[] lastInstruction = cpu.executeOneInstruction();
        notifyListeners();
        return lastInstruction;
    }

    private void go() throws Exception{

        JFrame frame = new JFrame("CHIP-8 Interpreter");
        frame.setIconImage(icon.getImage());
        // the displayBox is needed to keep the image centered even if the window is wider than it
        Box displayBox = new Box(BoxLayout.Y_AXIS);

        // lots of inner classes for menu actions

        // open files
        class OpenFileListener implements ActionListener{

            public void actionPerformed(ActionEvent e){
                fileChooserOpen = true;
                JFileChooser openFileChooser = new JFileChooser();
                openFileChooser.showOpenDialog(frame);
                File file = openFileChooser.getSelectedFile();
                if(file != null){
                    openFile(file);
                    display.clearScreen();
                    display.repaint();
                }
                fileChooserOpen = false;
            }
        }

        // reset
        class ResetListener implements ActionListener{
            public void actionPerformed(ActionEvent e){
                if(isFileLoaded) {
                    openFile(openedFile);
                    display.clearScreen();
                    display.repaint();
                }
            }
        }

        // pause
        class PauseListener implements ActionListener {
            public void actionPerformed(ActionEvent e){
                isPaused = !isPaused;
            }
        }

        // exit program
        class ExitListener implements ActionListener {
            public void actionPerformed(ActionEvent e){
                System.exit(0);
            }
        }

        // change keyboard type
        class KeyboardTypeListener implements ActionListener {
            String keyboardType;

            public KeyboardTypeListener (String keyboardType){
                this.keyboardType = keyboardType;
            }

            public void actionPerformed(ActionEvent e){
                keyboard.setupKeyboard(keyboardType);
            }
        }

        // change scale
        class ScaleListener implements ActionListener {
            int scale;

            public ScaleListener (int scale){
                this.scale = scale;
            }

            public void actionPerformed(ActionEvent e){
                display.setScale(scale);
                Dimension dimension = new Dimension(64*scale,32*scale);
                display.setPreferredSize(dimension);
                display.setMinimumSize(dimension);
                display.setMaximumSize(dimension);
                displayBox.setPreferredSize(dimension);
                displayBox.setMinimumSize(dimension);
                displayBox.setMaximumSize(dimension);
                frame.pack();
            }
        }

        // change speed
        class SpeedListener implements ActionListener {
            byte newCycleTime;

            public SpeedListener (byte newCycleTime){
                this.newCycleTime = newCycleTime;
            }

            public void actionPerformed(ActionEvent e){
                cycleTime = newCycleTime;
            }
        }

        // select type for ambiguous shift instructions
        class ShiftInstructionsTypeListener implements ActionListener {
            public void actionPerformed(ActionEvent e){
                cpu.setOriginalShiftInstructions(!cpu.getOriginalShiftInstructions());
            }
        }

        // select type for ambiguous memory read/write instructions
        class ReadWriteMemoryInstructionsTypeListener implements ActionListener {
            public void actionPerformed(ActionEvent e){
                cpu.setOriginalReadWriteMemoryInstructions(!cpu.getOriginalReadWriteMemoryInstructions());
            }
        }

        // select type for ambiguous jump with offset instruction
        class JumpWithOffsetInstructionTypeListener implements ActionListener {
            public void actionPerformed(ActionEvent e){
                cpu.setOriginalJumpWithOffsetInstructions(!cpu.getOriginalJumpWithOffsetInstructions());
            }
        }

        // toggle printing instructions during execution
        class PrintInstructionsListener implements ActionListener {
            public void actionPerformed(ActionEvent e){
                printInstructions = !printInstructions;
            }
        }

        // show memory
        class ShowRegistersAndMemoryListener implements ActionListener, instructionExecutedListener {
            JFrame memoryFrame ;
            JEditorPane memoryPane;
            JScrollPane scrollPane;

            public ShowRegistersAndMemoryListener(){
                memoryFrame = new JFrame("Memory");
                memoryPane = new JEditorPane();
                scrollPane = new JScrollPane(memoryPane);

                memoryFrame.setIconImage(icon.getImage());
                memoryFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                memoryPane.setEditable(false);
                memoryPane.setContentType("text/html");
                updateMemoryPaneContent();

                memoryFrame.getContentPane().add(BorderLayout.CENTER,scrollPane);
            }

            @Override
            public void instructionWasExecuted(){
                updateMemoryPaneContent();
            }

            // not a good way to do this, just passing a huge string to another method
            // but it does the job
            private String memoryPaneContent(){
                String contents = "<html><head>";
                contents += "<style>" +
                        "td, th {" +
                        "padding: 4px;" +
                        "}" +
                        "</style></head>";
                contents += "<body style=\"margin-left:10px; margin-right:10px; text-align:center; \">";
                contents += "<h2>Registers</h2>";
                contents += cpu.registersTable();
                contents += "<hr><h2>Memory</h2>";
                contents += memory.memoryTable();
                contents += "<hr></body></html>";
                return contents;
            }

            private void updateMemoryPaneContent(){
                memoryPane.setText(memoryPaneContent());
            }

            public void actionPerformed(ActionEvent e){
                if(isFileLoaded & isPaused) {
                    updateMemoryPaneContent();

                    memoryFrame.setSize(700, 800);
                    memoryFrame.setLocationRelativeTo(frame);
                    memoryFrame.setVisible(true);
                }
            }
        }

        // print memory
        class PrintRegistersAndMemoryListener implements ActionListener {
            public void actionPerformed(ActionEvent e){
                if(isFileLoaded & isPaused) {
                    cpu.printRegisters();
                    memory.printMemory(0x200, memory.getMemoryUsed());
                }
            }
        }

        // step
        class StepListener implements ActionListener {
            // this executes one instruction and prints the memory
            public void actionPerformed(ActionEvent e){
                if(isFileLoaded & isPaused) {
                    int[] lastInstruction = executeOneInstruction();

                    System.out.print("EXECUTED    ");
                    printInstructionInfo(lastInstruction);
                    System.out.print("NEXT        ");
                    printInstructionInfo(cpu.getNextInstruction());
                    System.out.println();

                    // if the instruction affects the graphics, we redraw the screen
                    if (lastInstruction[0] == 0) {
                        if (lastInstruction[1] == 0xe0) {
                            display.repaint();
                        }
                    } else if ((lastInstruction[0] & 0xf0) == 0xd0) {
                        display.repaint();
                    }
                }
            }
        }

        // step and print registers and memory
        class StepAndPrintRegistersAndMemoryListener implements ActionListener {
            // this executes one instruction and prints the memory
            public void actionPerformed(ActionEvent e){
                if(isFileLoaded & isPaused) {
                    int[] lastInstruction = executeOneInstruction();

                    System.out.print("EXECUTED    ");
                    printInstructionInfo(lastInstruction);
                    System.out.print("NEXT        ");
                    printInstructionInfo(cpu.getNextInstruction());
                    System.out.println();

                    // if the instruction affects the graphics, we redraw the screen
                    if (lastInstruction[0] == 0) {
                        if (lastInstruction[1] == 0xe0) {
                            display.repaint();
                        }
                    } else if ((lastInstruction[0] & 0xf0) == 0xd0) {
                        display.repaint();
                    }
                    cpu.printRegisters();
                    memory.printMemory(0x200, memory.getMemoryUsed());
                }
            }
        }

        // about window
        class AboutListener implements ActionListener {

            JFrame aboutFrame = new JFrame("About");

            // inner class for the license button
            class LicenseButtonListener implements  ActionListener {

                public void actionPerformed(ActionEvent e){
                    JFrame licenseFrame = new JFrame("License");
                    licenseFrame.setIconImage(icon.getImage());
                    licenseFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                    JEditorPane licensePane = new JEditorPane();
                    licensePane.setEditable(false);
                    licensePane.setContentType("text/plain");

                    try {
                        Reader fileRead = new FileReader("License.txt");
                        licensePane.read(fileRead,null);
                    }
                    catch (Exception exception) {
                        licensePane.setText("LICENSE FILE NOT FOUND");
                    }

                    JScrollPane scrollPane = new JScrollPane(licensePane);

                    licenseFrame.getContentPane().add(BorderLayout.CENTER,scrollPane);
                    licenseFrame.setSize(440,520);
                    licenseFrame.setLocationRelativeTo(aboutFrame);
                    licenseFrame.setVisible(true);
                }
            }

            public void actionPerformed(ActionEvent e){
                aboutFrame.setIconImage(icon.getImage());
                aboutFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                JEditorPane aboutPane = new JEditorPane();
                aboutPane.setEditable(false);
                aboutPane.setContentType("text/html");
                aboutPane.setText("<html>" +
                        "<body style=\"margin-left:10px; margin-right:10px; text-align:center; \">" +
                        "<h2>CHIP-8 Interpreter</h2>" +
                        "<p>2021, by MS</p>" +
                        "</html>");

                JPanel buttonPanel = new JPanel();
                JButton licenseButton = new JButton("License");
                buttonPanel.add(licenseButton);

                licenseButton.addActionListener(new LicenseButtonListener());

                aboutFrame.getContentPane().add(BorderLayout.CENTER,aboutPane);
                aboutFrame.getContentPane().add(BorderLayout.SOUTH,buttonPanel);

                aboutFrame.setSize(300,200);
                aboutFrame.setLocationRelativeTo(frame);
                aboutFrame.setVisible(true);
            }
        }

        // info window
        class InfoListener implements ActionListener {

            public void actionPerformed(ActionEvent e){
                JFrame infoFrame = new JFrame("Manual");
                infoFrame.setIconImage(icon.getImage());
                infoFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                JEditorPane infoPane = new JEditorPane();
                infoPane.setEditable(false);
                infoPane.setContentType("text/html");

                try {
                    Reader fileRead = new FileReader("help.html");
                    infoPane.read(fileRead,null);
                }
                catch (Exception exception) {
                    infoPane.setText("MANUAL FILE NOT FOUND");
                }

                JScrollPane scrollPane = new JScrollPane(infoPane);

                infoFrame.getContentPane().add(BorderLayout.CENTER,scrollPane);
                infoFrame.setSize(480,480);
                infoFrame.setLocationRelativeTo(frame);
                infoFrame.setVisible(true);
            }

        }

        // end of inner classes

        // menu bar
        JMenuBar menuBar = new JMenuBar();

        // file menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem openFileItem = new JMenuItem("Open file");
        openFileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openFileItem.addActionListener(new OpenFileListener());
        JMenuItem resetItem = new JMenuItem("Reset");
        resetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        resetItem.addActionListener(new ResetListener());
        JCheckBoxMenuItem pauseCheckbox = new JCheckBoxMenuItem("Pause",isPaused);
        pauseCheckbox.setAccelerator(KeyStroke.getKeyStroke(' '));
        pauseCheckbox.addActionListener(new PauseListener());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(new ExitListener());

        fileMenu.add(openFileItem);
        fileMenu.add(resetItem);
        fileMenu.add(pauseCheckbox);
        fileMenu.add(exitItem);

        // scale menu
        JMenu scaleMenu = new JMenu("Scale");
        ButtonGroup scaleButtons = new ButtonGroup();
        JRadioButtonMenuItem scale1Button = new JRadioButtonMenuItem("1x",display.getScale() == 1);
        JRadioButtonMenuItem scale2Button = new JRadioButtonMenuItem("2x",display.getScale() == 2);
        JRadioButtonMenuItem scale3Button = new JRadioButtonMenuItem("3x",display.getScale() == 3);
        JRadioButtonMenuItem scale4Button = new JRadioButtonMenuItem("4x",display.getScale() == 4);
        JRadioButtonMenuItem scale6Button = new JRadioButtonMenuItem("6x",display.getScale() == 6);
        JRadioButtonMenuItem scale8Button = new JRadioButtonMenuItem("8x",display.getScale() == 8);
        JRadioButtonMenuItem scale10Button = new JRadioButtonMenuItem("10x",display.getScale() == 10);
        JRadioButtonMenuItem scale12Button = new JRadioButtonMenuItem("12x",display.getScale() == 12);
        JRadioButtonMenuItem scale16Button = new JRadioButtonMenuItem("16x",display.getScale() == 16);
        scaleButtons.add(scale1Button);
        scaleMenu.add(scale1Button);
        scaleButtons.add(scale2Button);
        scaleMenu.add(scale2Button);
        scaleButtons.add(scale3Button);
        scaleMenu.add(scale3Button);
        scaleButtons.add(scale4Button);
        scaleMenu.add(scale4Button);
        scaleButtons.add(scale6Button);
        scaleMenu.add(scale6Button);
        scaleButtons.add(scale8Button);
        scaleMenu.add(scale8Button);
        scaleButtons.add(scale10Button);
        scaleMenu.add(scale10Button);
        scaleButtons.add(scale12Button);
        scaleMenu.add(scale12Button);
        scaleButtons.add(scale16Button);
        scaleMenu.add(scale16Button);

        scale1Button.addActionListener(new ScaleListener(1));
        scale2Button.addActionListener(new ScaleListener(2));
        scale3Button.addActionListener(new ScaleListener(3));
        scale4Button.addActionListener(new ScaleListener(4));
        scale6Button.addActionListener(new ScaleListener(6));
        scale8Button.addActionListener(new ScaleListener(8));
        scale10Button.addActionListener(new ScaleListener(10));
        scale12Button.addActionListener(new ScaleListener(12));
        scale16Button.addActionListener(new ScaleListener(16));

        // speed menu
        JMenu speedMenu = new JMenu("Speed");
        ButtonGroup speedButtons = new ButtonGroup();
        JRadioButtonMenuItem fastButton = new JRadioButtonMenuItem("Fast",cycleTime==1);
        JRadioButtonMenuItem normalButton = new JRadioButtonMenuItem("Normal",cycleTime==2);
        JRadioButtonMenuItem slowButton = new JRadioButtonMenuItem("Slow",cycleTime==3);
        JRadioButtonMenuItem verySlowButton = new JRadioButtonMenuItem("Very slow",cycleTime==5);
        speedButtons.add(fastButton);
        speedMenu.add(fastButton);
        speedButtons.add(normalButton);
        speedMenu.add(normalButton);
        speedButtons.add(slowButton);
        speedMenu.add(slowButton);
        speedButtons.add(verySlowButton);
        speedMenu.add(verySlowButton);

        fastButton.addActionListener(new SpeedListener((byte) 1));
        normalButton.addActionListener(new SpeedListener((byte) 2));
        slowButton.addActionListener(new SpeedListener((byte) 3));
        verySlowButton.addActionListener(new SpeedListener((byte) 5));

        // controls menu
        JMenu controlsMenu = new JMenu("Controls");
        ButtonGroup controlButtons = new ButtonGroup();
        JRadioButtonMenuItem qwertyButton = new JRadioButtonMenuItem("Qwerty",keyboard.getKeyboardType().equals("Qwerty"));
        JRadioButtonMenuItem qwertzButton = new JRadioButtonMenuItem("Qwertz",keyboard.getKeyboardType().equals("Qwertz"));
        JRadioButtonMenuItem azertyButton = new JRadioButtonMenuItem("Azerty",keyboard.getKeyboardType().equals("Azerty"));
        controlButtons.add(qwertyButton);
        controlsMenu.add(qwertyButton);
        controlButtons.add(qwertzButton);
        controlsMenu.add(qwertzButton);
        controlButtons.add(azertyButton);
        controlsMenu.add(azertyButton);

        qwertyButton.addActionListener(new KeyboardTypeListener("Qwerty"));
        qwertzButton.addActionListener(new KeyboardTypeListener("Qwertz"));
        azertyButton.addActionListener(new KeyboardTypeListener("Azerty"));

        // cpu menu
        JMenu cpuMenu = new JMenu("CPU");
        JCheckBoxMenuItem shiftInstructionsTypeCheckbox = new JCheckBoxMenuItem("Original shift instructions",cpu.getOriginalShiftInstructions());
        JCheckBoxMenuItem readWriteMemoryInstructionsTypeCheckbox = new JCheckBoxMenuItem("Original read/write memory instructions",cpu.getOriginalReadWriteMemoryInstructions());
        JCheckBoxMenuItem jumpWithOffsetInstructionTypeCheckbox = new JCheckBoxMenuItem("Original jump with offset instruction",cpu.getOriginalJumpWithOffsetInstructions());

        shiftInstructionsTypeCheckbox.addActionListener(new ShiftInstructionsTypeListener());
        readWriteMemoryInstructionsTypeCheckbox.addActionListener(new ReadWriteMemoryInstructionsTypeListener());
        jumpWithOffsetInstructionTypeCheckbox.addActionListener(new JumpWithOffsetInstructionTypeListener());

        cpuMenu.add(shiftInstructionsTypeCheckbox);
        cpuMenu.add(readWriteMemoryInstructionsTypeCheckbox);
        cpuMenu.add(jumpWithOffsetInstructionTypeCheckbox);

        // debug menu
        JMenu debugMenu = new JMenu("Debug");
        JCheckBoxMenuItem printInstructionsCheckbox = new JCheckBoxMenuItem("Print instructions",printInstructions);
        printInstructionsCheckbox.setAccelerator(KeyStroke.getKeyStroke('p')); //case-sensitive...
        JMenuItem stepItem = new JMenuItem("Step");
        stepItem.setAccelerator(KeyStroke.getKeyStroke('n')); //case-sensitive...
        JMenuItem showMemoryItem = new JMenuItem("Show registers and memory");
        showMemoryItem.setAccelerator(KeyStroke.getKeyStroke('j')); //case-sensitive
        JMenuItem printMemoryItem = new JMenuItem("Print registers and memory");
        printMemoryItem.setAccelerator(KeyStroke.getKeyStroke('k')); //case-sensitive
        JMenuItem stepAndPrintMemoryItem = new JMenuItem("Step and print registers and memory");
        stepAndPrintMemoryItem.setAccelerator((KeyStroke.getKeyStroke('m'))); //case-sensitive

        printInstructionsCheckbox.addActionListener(new PrintInstructionsListener());
        stepItem.addActionListener(new StepListener());
        ShowRegistersAndMemoryListener showRegistersAndMemoryListener = new ShowRegistersAndMemoryListener();
        addListener(showRegistersAndMemoryListener);
        showMemoryItem.addActionListener(showRegistersAndMemoryListener);
        printMemoryItem.addActionListener(new PrintRegistersAndMemoryListener());
        stepAndPrintMemoryItem.addActionListener(new StepAndPrintRegistersAndMemoryListener());

        debugMenu.add(printInstructionsCheckbox);
        debugMenu.add(stepItem);
        debugMenu.add(showMemoryItem);
        debugMenu.add(printMemoryItem);
        debugMenu.add(stepAndPrintMemoryItem);

        // help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem infoItem = new JMenuItem("Info");
        JMenuItem aboutItem = new JMenuItem("About");

        infoItem.addActionListener(new InfoListener());
        aboutItem.addActionListener(new AboutListener());

        helpMenu.add(infoItem);
        helpMenu.add(aboutItem);


        // add items to menu bar
        menuBar.add(fileMenu);
        menuBar.add(scaleMenu);
        menuBar.add(speedMenu);
        menuBar.add(controlsMenu);
        menuBar.add(cpuMenu);
        menuBar.add(debugMenu);
        menuBar.add(helpMenu);

        frame.setJMenuBar(menuBar);

        // setting the window size
        Dimension dimension = new Dimension(64*display.getScale(),32*display.getScale());
        // for some reasons the dimensions have to be set HERE!
        // if it is done inside the display class in the method setScale the panel will not be centered!
        display.setPreferredSize(dimension);
        display.setMinimumSize(dimension);
        display.setMaximumSize(dimension);
        displayBox.setPreferredSize(dimension);
        displayBox.setMinimumSize(dimension);
        displayBox.setMaximumSize(dimension);

        displayBox.add(display);

        frame.getContentPane().add(BorderLayout.CENTER, displayBox);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        int i = 0;

        // fetch, decode, execute loop
        while(true) {
            i++;
            if(!isPaused & !fileChooserOpen & isFileLoaded) {
                // we don't use the method executeOneInstruction of this class since the
                // memory window shouldn't be updated while running the interpreter normally,
                // only when using step
                int[] lastInstruction = cpu.executeOneInstruction();

                if (printInstructions){
                    printInstructionInfo(lastInstruction);
                }

                // if the instruction affects the graphics, we redraw the screen
                if (lastInstruction[0] == 0) {
                    if (lastInstruction[1] == 0xe0) {
                        display.repaint();
                    }
                } else if ((lastInstruction[0] & 0xf0) == 0xd0) {
                    display.repaint();
                }
            }
            Thread.sleep(cycleTime);
            if(i == instructionsPerTimerCycle){
                i = 0;
                cpu.decrementTimers();
            }
        }
    }

    public static void main(String[] Args) throws Exception{
        Chip8 chip8 = new Chip8();
        chip8.go();
    }

}