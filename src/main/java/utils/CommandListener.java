package utils;

import org.lwjgl.input.Keyboard;

public class CommandListener {
    public static Command getCommand() {
        while(true) {
            if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
                return Command.MOVE_DOWN;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
                return Command.MOVE_UP;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
                return Command.MOVE_LEFT;
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
                return Command.MOVE_RIGHT;
            }
        }
    }
}
