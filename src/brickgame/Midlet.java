package brickgame;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;

public final class Midlet extends MIDlet implements CommandListener {
    private final Command playCommand = new Command("Play", Command.OK, 1);
    private final Command exitCommand = new Command("Exit", Command.EXIT, 2);

    private Display display;
    private List machineList;
    private BrickCanvas canvas;
    private int selectedMachine;

    public Midlet() {
    }

    protected void startApp() {
        display = Display.getDisplay(this);
        if (machineList == null) {
            createMachineList();
        }
        if (canvas == null) {
            canvas = new BrickCanvas(this);
        }
        canvas.start();
        if (display.getCurrent() == null) {
            display.setCurrent(machineList);
        }
    }

    protected void pauseApp() {
        if (canvas != null) {
            canvas.stop();
        }
    }

    protected void destroyApp(boolean unconditional) {
        if (canvas != null) {
            canvas.stop();
        }
    }

    private void createMachineList() {
        machineList = new List("Brick ROM cores", List.IMPLICIT);
        for (int i = 0; i < MachineProfile.ALL.length; i++) {
            machineList.append(MachineProfile.ALL[i].name, null);
        }
        machineList.setSelectCommand(playCommand);
        machineList.addCommand(exitCommand);
        machineList.setCommandListener(this);
    }

    public void commandAction(Command command, Displayable source) {
        if (command == exitCommand) {
            requestExit();
            return;
        }
        if (source == machineList && (command == playCommand || command == List.SELECT_COMMAND)) {
            int index = machineList.getSelectedIndex();
            if (index >= 0 && index < MachineProfile.ALL.length) {
                selectedMachine = index;
                canvas.loadMachine(index);
                display.setCurrent(canvas);
            }
        }
    }

    void showMachineList() {
        if (machineList == null || display == null) {
            return;
        }
        if (canvas != null) {
            canvas.enterIdle();
        }
        machineList.setSelectedIndex(selectedMachine, true);
        display.setCurrent(machineList);
    }

    void requestExit() {
        if (canvas != null) {
            canvas.stop();
        }
        notifyDestroyed();
    }
}
