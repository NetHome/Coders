package nu.nethome.coders;

import nu.nethome.coders.decoders.RollerTrolDecoder;
import nu.nethome.util.ps.BitString;
import nu.nethome.util.ps.ProtocolInfo;
import nu.nethome.util.ps.PulseLength;

/**
 *
 */
public class RollerTrol {
    public static final String ROLLER_TROL_PROTOCOL_NAME = "RollerTrol";
    public static final ProtocolInfo ROLLERTROL_PROTOCOL_INFO = new ProtocolInfo(ROLLER_TROL_PROTOCOL_NAME, "Mark Length", "RollerTrol", 40, 1);
    // This are the pulse length constants for the protocol. The default values may
    // be overridden by system properties
    public static final PulseLength LONG_PREAMBLE_MARK =
            new PulseLength(RollerTrolDecoder.class, "LONG_PREAMBLE_MARK", 4885, 4000, 5000);
    public static final PulseLength LONG_PREAMBLE_SPACE =
            new PulseLength(RollerTrolDecoder.class, "LONG_PREAMBLE_SPACE", 2450, 2300, 2600);
    public static final PulseLength SHORT_PREAMBLE_MARK =
            new PulseLength(RollerTrolDecoder.class, "SHORT_PREAMBLE_MARK", 1650, 1500, 1800);
    public static final PulseLength SHORT =
            new PulseLength(RollerTrolDecoder.class, "SHORT", 300, 200, 490);
    public static final PulseLength LONG =
            new PulseLength(RollerTrolDecoder.class, "LONG", 600, 500, 800);
    public static final PulseLength SHORT_MARK =
            new PulseLength(RollerTrolDecoder.class, "SHORT_MARK", 500, 300, 700);
    public static final PulseLength SPACE =
            new PulseLength(RollerTrolDecoder.class, "SPACE", 1000, 800, 1200);
    public static final BitString.Field HOUSE_CODE = new BitString.Field(0, 16);
    public static final BitString.Field DEVICE_CODE = new BitString.Field(16, 4);
    public static final String HOUSE_CODE_NAME = "HouseCode";
    public static final String COMMAND_NAME = "Command";
    public static final String DEVICE_CODE_NAME = "DeviceCode";
}
