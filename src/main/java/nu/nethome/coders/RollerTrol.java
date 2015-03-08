package nu.nethome.coders;

import nu.nethome.coders.decoders.RollerTrolDecoder;
import nu.nethome.util.ps.BitString;
import nu.nethome.util.ps.ProtocolInfo;
import nu.nethome.util.ps.PulseLength;

/**
 * Constants for the RollerTrol protocol
 *
 * My current understanding of the protocol is:
 *
 * h = House Code
 * d = Device Code (Channel)
 * c = Command
 * s = Check Sum
 *
 * Message is sent LSB first
 *
 * ____Byte 4_____  ____Byte 3_____  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____
 * 7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0
 * s s s s s s s s  0 0 0 0 0 0 0 1  c c c c d d d d  h h h h h h h h  h h h h h h h h
 */


public class RollerTrol {
    public static final String ROLLER_TROL_PROTOCOL_NAME = "RollerTrol";
    public static final ProtocolInfo ROLLERTROL_PROTOCOL_INFO = new ProtocolInfo(ROLLER_TROL_PROTOCOL_NAME, "Mark Length", "RollerTrol", 40, 1);
    public static final int MESSAGE_BIT_LENGTH = 40;

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
    // These are the fields in the binary message
    public static final BitString.Field HOUSE_CODE = new BitString.Field(0, 16);
    public static final BitString.Field DEVICE_CODE = new BitString.Field(16, 4);
    public static final BitString.Field COMMAND = new BitString.Field(20, 4);
    public static final BitString.Field CHECK_SUM = new BitString.Field(32, 8);
    public static final BitString.Field CONSTANT_FIELD = new BitString.Field(24, 8);

    // Message field names
    public static final String HOUSE_CODE_NAME = "HouseCode";
    public static final String COMMAND_NAME = "Command";
    public static final String DEVICE_CODE_NAME = "DeviceCode";

    // Commands
    public static final int UP = 12;
    public static final int STOP = 5;
    public static final int DOWN = 1;
    public static final int REVERSE = 8;
    public static final int LIMIT = 3;
    public static final int CONFIRM = 4;
    public static final int ALL_CHANNELS = 15;

    public static int calculateChecksum(BitString binaryMessage) {
        int calculatedCheckSum = binaryMessage.extractInt(RollerTrolDecoder.BYTE0) +
                binaryMessage.extractInt(RollerTrolDecoder.BYTE1) +
                binaryMessage.extractInt(RollerTrolDecoder.BYTE2) +
                binaryMessage.extractInt(RollerTrolDecoder.BYTE3);
        calculatedCheckSum = (((calculatedCheckSum / 256) + 1) * 256 + 1) - calculatedCheckSum;
        return calculatedCheckSum;
    }
}
