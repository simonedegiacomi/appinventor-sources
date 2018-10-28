package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.TimerInternal;

import java.util.ArrayList;
import java.util.List;


@DesignerComponent(version = YaVersion.EV3_MOTORS_COMPONENT_VERSION,
        description = "Improved EV3 Motor",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/legoMindstormsEv3.png")
@SimpleObject(external = true)
public class MyEv3Motors extends Ev3Motors {

    private static final int TACHO_POOL_DELAY = 200;
    private static final int TACHO_TRESHOLD = 20;

    private static TimerInternal timerInternal;

    private abstract class RotateAfterCommand {
        MyEv3Motors motors;
        int         power;
        boolean     useBrake;

        public RotateAfterCommand(MyEv3Motors motors, int power, boolean useBrake) {
            this.motors = motors;
            this.power = power;
            this.useBrake = useBrake;
        }
    }

    private class RotateInDurationCommand extends RotateAfterCommand {
        int milliseconds;

        public RotateInDurationCommand(MyEv3Motors motors, int power, int milliseconds, boolean userBrake) {
            super(motors, power, userBrake);
            this.milliseconds = milliseconds;
        }
    }

    private class RotateInTachoCountsAfter extends RotateAfterCommand {
        int tachoCount;

        public RotateInTachoCountsAfter(MyEv3Motors motors, int power, boolean userBrake, int tachoCount) {
            super(motors, power, userBrake);
            this.tachoCount = tachoCount;
        }
    }


    private static final List<RotateAfterCommand> rotateCommands = new ArrayList<RotateAfterCommand>();

    private static RotateInTachoCountsAfter lastTachoScheduled = null;

    /**
     * Creates a new Ev3Motors component.
     *
     * @param container
     */
    public MyEv3Motors(ComponentContainer container) {
        super(container);
        if (timerInternal == null) {
            createTimer();
        }
    }


    @SimpleFunction(description = "Rotate the motors in a period of time")
    public void RotateInDurationAfter(int power, int milliseconds, boolean useBrake) {
        rotateCommands.add(new RotateInDurationCommand(this, power, milliseconds, useBrake));
        if (!timerInternal.Enabled()) {
            timerInternal.Enabled(true);
        }
    }

    @SimpleFunction(description = "Rotate the motors in a period of time")
    public void RotateInTachoCountsAfter(int power, int tacho, boolean useBrake) {
        rotateCommands.add(new RotateInTachoCountsAfter(this, power, useBrake, tacho));
        if (!timerInternal.Enabled()) {
            timerInternal.Enabled(true);
        }
    }

    private void createTimer() {
        timerInternal = new TimerInternal(new AlarmHandler() {
            @Override
            public void alarm() {
                if (lastTachoScheduled != null) { // Waiting for tacho based command
                    if (Math.abs(lastTachoScheduled.tachoCount - GetTachoCount()) <= TACHO_TRESHOLD) { // Have we reached the tacho target?
                        lastTachoScheduled = null;
                    }
                }

                if (lastTachoScheduled == null) {
                    // If we aren't waiting for tacho and the time is up, we need to stop the timer or start a new command
                    if (rotateCommands.isEmpty()) {
                        timerInternal.Enabled(false);
                    } else {
                        startNextCommand();
                    }
                }
            }
        }, false, 1000);
    }

    private void startNextCommand() {
        RotateAfterCommand command = rotateCommands.get(0);
        rotateCommands.remove(0);

        if (command instanceof RotateInDurationCommand) {
            RotateInDurationCommand durationCommand = (RotateInDurationCommand) command;

            command.motors.RotateInDuration(command.power, durationCommand.milliseconds, command.useBrake);
            timerInternal.Interval(durationCommand.milliseconds);

        } else if (command instanceof RotateInTachoCountsAfter) {
            RotateInTachoCountsAfter tachoCommand = (RotateInTachoCountsAfter) command;

            ResetTachoCount();
            command.motors.RotateInTachoCounts(tachoCommand.power, tachoCommand.tachoCount, tachoCommand.useBrake);
            timerInternal.Interval(TACHO_POOL_DELAY);
            lastTachoScheduled = tachoCommand;

        }
    }


}
