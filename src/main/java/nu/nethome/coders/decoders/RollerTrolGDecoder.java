package nu.nethome.coders.decoders;

import nu.nethome.coders.RollerTrol;
import nu.nethome.coders.RollerTrolG;
import nu.nethome.util.ps.*;

/**
 *
 */
public class RollerTrolGDecoder  implements ProtocolDecoder {

    protected static final int IDLE = 0;
    protected static final int READING_LONG_PREAMBLE_SPACE = 2;
    protected static final int READING_PREAMBLE_MARK = 3;
    protected static final int READING_MARK = 5;
    protected static final int READING_SHORT_SPACE = 6;
    protected static final int READING_LONG_SPACE = 7;
    protected static final int REPEAT_SCAN = 10;


    public static final BitString.Field BYTE4 = new BitString.Field(32, 8);
    public static final BitString.Field BYTE3 = new BitString.Field(24, 8);
    public static final BitString.Field BYTE2 = new BitString.Field(16, 8);
    public static final BitString.Field BYTE1 = new BitString.Field(8, 8);
    public static final BitString.Field BYTE0 = new BitString.Field(0, 8);


    protected ProtocolDecoderSink m_Sink = null;
    BitString data = new BitString();
    protected int state = IDLE;
    private int repeat = 0;

    public void setTarget(ProtocolDecoderSink sink) {
        m_Sink = sink;
    }

    public ProtocolInfo getInfo() {
        return RollerTrolG.ROLLERTROL_PROTOCOL_INFO;
    }

    protected void addBit(boolean b) {
        data.addLsb(b);
        if (data.length() == RollerTrolG.PROTOCOL_BIT_LENGTH) {
            decodeMessage(data);
        }
    }

    public void decodeMessage(BitString binaryMessage) {
        int channel = binaryMessage.extractInt(RollerTrolG.CHANNEL);
        int address = binaryMessage.extractInt(RollerTrolG.ADDRESS);
        int command = binaryMessage.extractInt(RollerTrolG.COMMAND);
        int bytes[] = new int[5];
        bytes[0] = binaryMessage.extractInt(BYTE4);
        bytes[1] = binaryMessage.extractInt(BYTE3);
        bytes[2] = binaryMessage.extractInt(BYTE2);
        bytes[3] = binaryMessage.extractInt(BYTE1);
        bytes[4] = binaryMessage.extractInt(BYTE0);
        ProtocolMessage message = new ProtocolMessage(RollerTrolG.ROLLER_TROL_G_PROTOCOL_NAME, command, channel, 5);
        message.setRawMessageByteAt(0, bytes[0]);
        message.setRawMessageByteAt(1, bytes[1]);
        message.setRawMessageByteAt(2, bytes[2]);
        message.setRawMessageByteAt(3, bytes[3]);
        message.setRawMessageByteAt(4, bytes[4]);

        message.addField(new FieldValue(RollerTrolG.COMMAND_NAME, command));
        message.addField(new FieldValue(RollerTrolG.CHANNEL_NAME, channel));
        message.addField(new FieldValue(RollerTrolG.ADDRESS_NAME, address));
        message.setRepeat(repeat);
        m_Sink.parsedMessage(message);
        state = REPEAT_SCAN;
    }

    public int parse(double pulse, boolean bitstate) {
        switch (state) {
            case IDLE: {
                if (RollerTrolG.LONG_PREAMBLE_MARK.matches(pulse) && bitstate) {
                    data.clear();
                    repeat = 0;
                    state = READING_LONG_PREAMBLE_SPACE;
                }
                break;
            }
            case READING_LONG_PREAMBLE_SPACE: {
                if (RollerTrolG.LONG_PREAMBLE_SPACE.matches(pulse)) {
                    state = READING_MARK;
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case READING_MARK: {
                if (RollerTrolG.SHORT.matches(pulse)) {
                    state = READING_LONG_SPACE;
                    addBit(false);
                } else if (RollerTrolG.LONG.matches(pulse)) {
                    state = READING_SHORT_SPACE;
                    addBit(true);
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case READING_SHORT_SPACE: {
                if (RollerTrolG.SHORT.matches(pulse)) {
                    state = READING_MARK;
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case READING_LONG_SPACE: {
                if (RollerTrolG.LONG.matches(pulse)) {
                    state = READING_MARK;
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case REPEAT_SCAN: {
                if (RollerTrolG.REPEAT_SPACE.matches(pulse)) {
                    state = READING_PREAMBLE_MARK;
                } else {
                    state = IDLE;
                }
                break;
            }
            case READING_PREAMBLE_MARK: {
                if (RollerTrolG.LONG_PREAMBLE_MARK.matches(pulse) && bitstate) {
                    state = READING_LONG_PREAMBLE_SPACE;
                    data.clear();
                    repeat++;
                } else {
                    state = IDLE;
                }
                break;
            }
        }
        return state;
    }

    private void quitParsing(double pulseLength) {
        if (data.length() > 5) {
            m_Sink.partiallyParsedMessage(String.format("RollerTrol Pulse: %g ms, State: %d", pulseLength, state), data.length());
        }
        state = IDLE;
    }
}
