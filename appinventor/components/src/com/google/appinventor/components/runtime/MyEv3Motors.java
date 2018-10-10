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

    private static TimerInternal timerInternal;

    private class RotateInDurationCommand {
        MyEv3Motors motors;
        int         power;
        int         milliseconds;
        boolean     userBrake;

        public RotateInDurationCommand(MyEv3Motors motors, int power, int milliseconds, boolean userBrake) {
            this.motors = motors;
            this.power = power;
            this.milliseconds = milliseconds;
            this.userBrake = userBrake;
        }
    }

    private static final List<RotateInDurationCommand> rotateInDurationCommands = new ArrayList<RotateInDurationCommand>();

    /**
     * Creates a new Ev3Motors component.
     *
     * @param container
     */
    public MyEv3Motors(ComponentContainer container) {
        super(container);
        if (timerInternal == null) {
            timerInternal = new TimerInternal(new AlarmHandler() {
                @Override
                public void alarm() {
                    if (rotateInDurationCommands.isEmpty()) {
                        timerInternal.Enabled(false);
                    } else {

                        RotateInDurationCommand command = rotateInDurationCommands.get(0);
                        rotateInDurationCommands.remove(0);

                        command.motors.RotateInDuration(command.power, command.milliseconds, command.userBrake);
                        timerInternal.Interval(command.milliseconds);
                    }
                }
            }, false, 1000);
        }
    }


    @SimpleFunction(description = "Rotate the motors in a period of time")
    public void RotateInDurationAfter(int power, int milliseconds, boolean useBrake) {
        rotateInDurationCommands.add(new RotateInDurationCommand(this, power, milliseconds, useBrake));
        if (!timerInternal.Enabled()) {
            timerInternal.Enabled(true);
        }
    }
}
