package nu.nethome.coders.decoders;

import nu.nethome.coders.RollerTrolG;
import nu.nethome.util.ps.*;

public class PrologueDecoder implements ProtocolDecoder {

    protected static final int IDLE = 0;
    protected static final int READING_MARK = 5;
    protected static final int READING_SPACE = 6;
    protected static final int REPEAT_SCAN = 10;
    protected static final int PROLOGUE_BIT_LENGTH = 36;

    public static final ProtocolInfo PROLOGUE_PROTOCOL_INFO = new ProtocolInfo("Prologue", "Space Length", "Prologue", PROLOGUE_BIT_LENGTH, 1);

    public static final PulseLength PREAMBLE_SPACE =
            new PulseLength(PrologueDecoder.class, "PREAMBLE_SPACE", 8770, 7000, 10000);
    public static final PulseLength LONG_SPACE =
            new PulseLength(PrologueDecoder.class, "LONG_SPACE", 3880, 3000, 5000);
    public static final PulseLength SHORT_SPACE =
            new PulseLength(PrologueDecoder.class, "SHORT_SPACE", 1930, 1000, 2999);
    public static final PulseLength MARK =
            new PulseLength(PrologueDecoder.class, "MARK", 500, 300, 700);

    protected ProtocolDecoderSink m_Sink = null;
    BitString data = new BitString();
    BitString lastParsedData = new BitString();
    protected int state = IDLE;
    private int repeat = 0;

    public void setTarget(ProtocolDecoderSink sink) {
        m_Sink = sink;
    }

    public ProtocolInfo getInfo() {
        return PROLOGUE_PROTOCOL_INFO;
    }

    protected void addBit(boolean b) {
        data.addLsb(b);
        if (data.length() == PROLOGUE_BIT_LENGTH) {
            decodeMessage(data);
        }
    }

    public void decodeMessage(BitString binaryMessage) {
//        int channel = binaryMessage.extractInt(RollerTrolG.CHANNEL);

        ProtocolMessage message = new ProtocolMessage(RollerTrolG.ROLLER_TROL_G_PROTOCOL_NAME, 0, 0, data.toByteInts());

//        message.addField(new FieldValue(RollerTrolG.COMMAND_NAME, command));
        message.setRepeat(repeat);
        if (data.equals(lastParsedData)) {
            m_Sink.parsedMessage(message);
        }
        lastParsedData.setValue(data);
        state = REPEAT_SCAN;
    }

    public int parse(double pulse, boolean bitstate) {
        switch (state) {
            case IDLE: {
                if (PREAMBLE_SPACE.matches(pulse) && !bitstate) {
                    data.clear();
                    repeat = 0;
                    state = READING_MARK;
                }
                break;
            }
            case READING_MARK: {
                if (MARK.matches(pulse)) {
                    state = READING_SPACE;
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case READING_SPACE: {
                if (SHORT_SPACE.matches(pulse)) {
                    state = READING_MARK;
                    addBit(true);
                } else if (LONG_SPACE.matches(pulse)) {
                    state = READING_MARK;
                    addBit(false);
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case REPEAT_SCAN: {
                if (PREAMBLE_SPACE.matches(pulse) && !bitstate) {
                    data.clear();
                    state = READING_MARK;
                }
                break;
            }
        }
        return state;
    }

    private void quitParsing(double pulseLength) {
        if (data.length() > 5) {
            m_Sink.partiallyParsedMessage(String.format("Prologue Pulse: %g ms, State: %d", pulseLength, state), data.length());
        }
        state = IDLE;
        lastParsedData.clear();
    }
}
