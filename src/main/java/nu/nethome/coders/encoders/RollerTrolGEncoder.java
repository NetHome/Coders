package nu.nethome.coders.encoders;

import nu.nethome.coders.RollerTrolG;
import nu.nethome.util.ps.*;

import static nu.nethome.coders.RollerTrolG.*;

/**
 *
 */
public class RollerTrolGEncoder implements ProtocolEncoder {

    public static final int PREAMBLE_LENGTH = 2;
    public static final int CONSTANT_FIELD_VALUE = 1;

    @Override
    public ProtocolInfo getInfo() {
        return ROLLERTROL_PROTOCOL_INFO;
    }

    @Override
    public int[] encode(Message message, Phase phase) throws BadMessageException {
        int address = 0;
        int channel = 0;
        int command = 0;
        for (FieldValue f : message.getFields()) {
            if (RollerTrolG.ADDRESS_NAME.equals(f.getName())) {
                address = f.getValue();
            }
            if (RollerTrolG.CHANNEL_NAME.equals(f.getName())) {
                channel = f.getValue();
            }
            if (RollerTrolG.COMMAND_NAME.equals(f.getName())) {
                command = f.getValue();
            }
        }
        return encode(address, channel, command);
    }

    private int[] encode(int address, int channel, int command) {
        int[] result = new int[PREAMBLE_LENGTH + RollerTrolG.PROTOCOL_BIT_LENGTH * 2 + 1];
        result[0] = RollerTrolG.LONG_PREAMBLE_MARK.length();
        result[1] = RollerTrolG.LONG_PREAMBLE_SPACE.length();
        BitString message = new BitString(RollerTrolG.PROTOCOL_BIT_LENGTH);
        message.insert(RollerTrolG.COMMAND, command);
        message.insert(RollerTrolG.ADDRESS, address);
        message.insert(RollerTrolG.CHANNEL, channel);
        int resultPosition = PREAMBLE_LENGTH;
        for (int i = 0; i < RollerTrolG.PROTOCOL_BIT_LENGTH; i++) {
            if (message.getBit(PROTOCOL_BIT_LENGTH - i - 1)) {
                result[resultPosition++] = LONG.length();
                result[resultPosition++] = SHORT.length();
            } else {
                result[resultPosition++] = SHORT.length();
                result[resultPosition++] = LONG.length();
            }
        }
        result[resultPosition] = RollerTrolG.REPEAT_SPACE.length();
        return result;
    }

    @Override
    public int modulationFrequency(Message message) {
        return 0;
    }

    public static Message buildMessage(int command, int address, int channel) {
        ProtocolMessage result = new ProtocolMessage(RollerTrolG.ROLLER_TROL_G_PROTOCOL_NAME, command, channel, 0);
        result.addField(new FieldValue(COMMAND_NAME, command));
        result.addField(new FieldValue(ADDRESS_NAME, address));
        result.addField(new FieldValue(CHANNEL_NAME, channel));
        return result;
    }


}
