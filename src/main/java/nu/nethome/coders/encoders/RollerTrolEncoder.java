package nu.nethome.coders.encoders;

import nu.nethome.coders.RollerTrol;
import nu.nethome.util.ps.*;

import static nu.nethome.coders.RollerTrol.*;

/**
 *
 */
public class RollerTrolEncoder implements ProtocolEncoder {

    @Override
    public ProtocolInfo getInfo() {
        return ROLLERTROL_PROTOCOL_INFO;
    }

    @Override
    public int[] encode(Message message, Phase phase) throws BadMessageException {
        int houseCode = 0;
        int deviceCode = 0;
        int command = 0;
        for (FieldValue f : message.getFields()) {
            if (f.getName().equals(HOUSE_CODE_NAME)) {
                houseCode = f.getValue();
            }
            if (f.getName().equals(DEVICE_CODE_NAME)) {
                deviceCode = f.getValue();
            }
            if (f.getName().equals(COMMAND_NAME)) {
                command = f.getValue();
            }
        }
        return encode(houseCode, deviceCode, command);
    }

    private int[] encode(int houseCode, int deviceCode, int command) {
        int[] result = new int[4];
        result[0] = RollerTrol.LONG_PREAMBLE_MARK.length();
        result[1] = RollerTrol.LONG_PREAMBLE_SPACE.length();
        result[2] = RollerTrol.SHORT_PREAMBLE_MARK.length();
        result[3] = RollerTrol.SHORT.length();
        return result;
    }

    @Override
    public int modulationFrequency(Message message) {
        return 0;
    }

    public static Message buildMessage(int command, int houseCode, int deviceCode) {
        ProtocolMessage result = new ProtocolMessage(ROLLER_TROL_PROTOCOL_NAME, command, deviceCode, 0);
        result.addField(new FieldValue(COMMAND_NAME, command));
        result.addField(new FieldValue(HOUSE_CODE_NAME, houseCode));
        result.addField(new FieldValue(DEVICE_CODE_NAME, deviceCode));
        return result;
    }


}
