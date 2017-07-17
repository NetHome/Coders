package nu.nethome.coders.decoders;

import nu.nethome.util.ps.*;
import nu.nethome.util.ps.BitString.Field;

import java.util.HashMap;
import java.util.Map;

/**
 * The PrologueDecoder parses a set of pulse lengths and decodes a protocol used
 * by Prologue-thermometers which is transmitted over 433MHz AM-modulated RF-signal.
 * The reception of the pulses may for example be made via the AudioProtocolSampler.
 * The NexaDecoder implements the ProtocolDecoder-interface and accepts the pulses
 * one by one. It contains a state machine, and when a complete message is decoded,
 * this is reported over the ProtocolDecoderSink-interface which is given at
 * construction.
 *
 * The protocol is space length encoded and sent with MSB first.
 * the protocol messages has the following layout:<br>
 *
 * h = Humidity
 * t = Temperature * 10
 * c = Channel 0 - 2
 * u = button
 * b = Battery (1 = low)
 * r = rolling id
 * i = id
 * <br>
 *  _Nyb 4_  ____Byte 3_____  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____  _S_<br>
 *  3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0   0<br>
 *  i i i i  r r r r r r r r  b u c c t t t t  t t t t t t t t  h h h h h h h h   x<br>
 *
 * @author Stefan
 *
 */
public class PrologueDecoder implements ProtocolDecoder {

    protected static final int IDLE = 0;
    protected static final int READING_MARK = 5;
    protected static final int READING_SPACE = 6;
    protected static final int REPEAT_SCAN = 10;
    protected static final int PROLOGUE_BIT_LENGTH = 36 + 1;
    private static final String PROTOCOL_NAME = "Prologue";
    public static final ProtocolInfo PROLOGUE_PROTOCOL_INFO = new ProtocolInfo(PROTOCOL_NAME, "Space Length", PROTOCOL_NAME, PROLOGUE_BIT_LENGTH, 1);

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
    private Map<String, Field> fields = new HashMap<String, Field>();

    public PrologueDecoder() {
        fields.put("Humidity", new Field(0, 8));
        fields.put("Temp", new Field(8, 12));
        fields.put("Channel", new Field(20, 2));
        fields.put("Button", new Field(22, 1));
        fields.put("Battery", new Field(23, 1));
        fields.put("RollingId", new Field(24, 8));
        fields.put("Id", new Field(32, 4));
    }

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
        binaryMessage.shiftRight(1); // last bit is ignored - parity?
        // Assure we get same data twice in a row as simple error detection strategy
        if (this.data.equals(lastParsedData)) {
            ProtocolMessage message = new ProtocolMessage(PROTOCOL_NAME, binaryMessage.extractInt(fields.get("Temp")), binaryMessage.extractInt(fields.get("Channel")), binaryMessage.toByteInts());
            for (String field : fields.keySet()) {
                message.addField(new FieldValue(field, binaryMessage.extractInt(fields.get(field))));
            }
            message.setRepeat(repeat - 1);
            m_Sink.parsedMessage(message);
        }
        lastParsedData.setValue(binaryMessage);
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
                    addBit(false);
                } else if (LONG_SPACE.matches(pulse)) {
                    state = READING_MARK;
                    addBit(true);
                } else {
                    quitParsing(pulse);
                }
                break;
            }
            case REPEAT_SCAN: {
                if (MARK.matches(pulse) && bitstate) {
                    // Ok read mark
                } else if (PREAMBLE_SPACE.matches(pulse) && !bitstate) {
                    data.clear();
                    repeat++;
                    state = READING_MARK;
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
            m_Sink.partiallyParsedMessage(String.format("Prologue Pulse: %g ms, State: %d", pulseLength, state), data.length());
        }
        state = IDLE;
        lastParsedData.clear();
    }
}
