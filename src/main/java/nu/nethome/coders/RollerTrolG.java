package nu.nethome.coders;

import nu.nethome.coders.decoders.RollerTrolGDecoder;
import nu.nethome.util.ps.BitString;
import nu.nethome.util.ps.ProtocolInfo;
import nu.nethome.util.ps.PulseLength;

public class RollerTrolG {
    public static final String ROLLER_TROL_G_PROTOCOL_NAME = "RollerTrolG";
    public static final int PROTOCOL_BIT_LENGTH = 40;
    public static final ProtocolInfo ROLLERTROL_PROTOCOL_INFO = new ProtocolInfo(ROLLER_TROL_G_PROTOCOL_NAME, "Mark Length", "RollerTrol", PROTOCOL_BIT_LENGTH, 1);

    public static final int COMMAND_STOP = 0x55;
    public static final int COMMAND_UP = 0x11;
    public static final int COMMAND_UP_END = 0x1E;
    public static final int COMMAND_DOWN = 0x33;
    public static final int COMMAND_DOWN_END = 0x3C;
    public static final int COMMAND_LEARN = 0xCC;

    public static final PulseLength LONG_PREAMBLE_MARK =
            new PulseLength(RollerTrolGDecoder.class, "LONG_PREAMBLE_MARK", 5170, 4000, 5900);
    public static final PulseLength LONG_PREAMBLE_SPACE =
            new PulseLength(RollerTrolGDecoder.class, "LONG_PREAMBLE_SPACE", 1665, 1000, 2000);
    public static final PulseLength SHORT =
            new PulseLength(RollerTrolGDecoder.class, "SHORT", 360, 200, 500);
    public static final PulseLength LONG =
            new PulseLength(RollerTrolGDecoder.class, "LONG", 770, 600, 900);
    public static final PulseLength REPEAT_SPACE =
            new PulseLength(RollerTrolGDecoder.class, "REPEAT_SPACE", 9100, 8000, 12000);

    public static final BitString.Field COMMAND = new BitString.Field(0, 8);
    public static final BitString.Field CHANNEL = new BitString.Field(8, 4);
    public static final BitString.Field ADDRESS = new BitString.Field(12, 28);
}
