/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stestr@nethome.nu>
 *
 * This file is part of OpenNetHome.
 *
 * OpenNetHome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenNetHome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
* Copyright (C) 2009 Stefan Strömberg
*
* Project: NetHome
*
* This source is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE.
*
*/

package nu.nethome.coders.encoders;

import nu.nethome.coders.decoders.NexaDecoder;
import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

import java.util.ArrayList;

/**
 * The NexaEncoder is an encoder for the RF-protocol used by NEXA products
 * such as remote switches, dimmers, PIR-detectors and so on. The NexaEncoder
 * can take the data from a protocol message and encode that into a sequence
 * of pulse lengths which may be played by RF-transmitting hardware for instance
 * via the AudioPulsePlayer.
 *
 * @author Stefan
 */
@Plugin
public class NexaEncoder implements ProtocolEncoder {

    public int repeatCount = 5;
    private char address = 'A';
    private int button = 1;
    private int command = 1;
    protected int onCommandValue;
    protected int offCommandValue;

    // Get the pulse lengths from the decoder
    protected int LONG_MARK = NexaDecoder.NEXA_LONG_MARK.length();
    protected int SHORT_MARK = NexaDecoder.NEXA_SHORT_MARK.length();
    protected int LONG_SPACE = NexaDecoder.NEXA_LONG_SPACE.length();
    protected int SHORT_SPACE = NexaDecoder.NEXA_SHORT_SPACE.length();
    protected int REPEAT = NexaDecoder.NEXA_REPEAT.length();

    public NexaEncoder() {
        setup();
    }

    protected void setup() {
        offCommandValue = 0x14 << 17;
        onCommandValue = 0x54 << 17;
    }

    /**
     * Encodes the current message according to the following protocol:
     * <p/>
     * s = Start bit = 0
     * b = Button number
     * a = Address
     * o = On/Off-bit
     * <p/>
     * ____Byte 2_____  ____Byte 1_____  ____Byte 0_____  _S_
     * 7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0   0
     * x o x 1 x 1 x x  x b x b x b x b  x a x a x a x a   s
     */
    public int[] encode() {
        ArrayList<Integer> result = new ArrayList<Integer>();
        long messageTemplate = 0;
        long message = 0;

        // encode message
        int address = ((int) this.address) - ((int) 'A');
        int button = this.button - 1;
        messageTemplate |= (address << 1) & 0x02;
        messageTemplate |= (address << 2) & 0x08;
        messageTemplate |= (address << 3) & 0x20;
        messageTemplate |= (address << 4) & 0x80;

        messageTemplate |= (button << 9) & 0x200;
        messageTemplate |= (button << 10) & 0x800;
        messageTemplate |= (button << 11) & 0x2000;
        messageTemplate |= (button << 12) & 0x8000;

        messageTemplate |= (command == 0) ? offCommandValue : onCommandValue;

        // Start actually encoding the data pulses
        for (int i = 0; i < repeatCount; i++) {
            message = messageTemplate;

            // 25 bits of data including start bit
            for (int j = 0; j < 25; j++) {
                if ((message & 1) == 1) {
                    result.add(LONG_MARK);
                    result.add(SHORT_SPACE);
                } else {
                    result.add(SHORT_MARK);
                    result.add(LONG_SPACE);
                }
                message >>= 1;
            }
            int last = result.size() - 1;
            // Add the repeat delay to the last space period
            result.set(last, result.get(last) + REPEAT);
        }
        int resultArray[] = new int[result.size()];
        for (int i = 0; i < result.size(); i++) {
            resultArray[i] = result.get(i);
        }
        return resultArray;
    }

    /**
     * Get the address in the character form A-P
     *
     * @return address
     */
    public char getAddress() {
        return address;
    }

    /**
     * Set the address in the character form A-P
     *
     * @param address 'A'-'P'
     */
    public void setAddress(char address) {
        address = Character.toUpperCase(address);
        if ((address < 'A') || (address > 'P')) {
            return;
        }
        this.address = address;
    }

    /**
     * Set the address in the numeric form
     *
     * @param address 0-15
     */
    public void setAddress(int address) {
        if ((address < 0) || (address > 15)) {
            return;
        }
        this.address = "ABCDEFGHIJKLMNOP".charAt(address);
    }

    /**
     * Get the button number
     *
     * @return button 1-16
     */
    public int getButton() {
        return button;
    }

    /**
     * Set the button number to activate
     *
     * @param button 1-16
     */
    public void setButton(int button) {
        if ((button < 1) || (button > 16)) {
            return;
        }
        this.button = button;
    }

    /**
     * Gett the current on/off command. 0 = off, 1= on
     *
     * @return 0 = off, 1= on
     */
    public int getCommand() {
        return command;
    }

    /**
     * Set the on/off command. 0 = off, 1 = on
     *
     * @param command
     */
    public void setCommand(int command) {
        if ((command < 0) || (command > 1)) {
            return;
        }
        this.command = command;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    /**
     * Set the number of times the message should be repeated
     *
     * @param repeatCount 1-100
     */
    public void setRepeatCount(int repeatCount) {
        if ((repeatCount < 1) || (repeatCount > 100)) {
            return;
        }
        this.repeatCount = repeatCount;
    }

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("Nexa", "Mark Length", "Nexa", 25, 5);
    }

    /**
     * This class was built using another model than the ProtocolEncoder interface, so the implementation
     * is a bit odd
     */
    public int[] encode(Message message, Phase phase) throws BadMessageException {
        if (Phase.FIRST == phase) {
            return new int[0];
        }
        for (FieldValue field : message.getFields()) {
            if (field.getName().equals("Command")) {
                setCommand(field.getValue());
            } else if (field.getName().equals("Button")) {
                setButton(field.getValue());
            } else if (field.getName().equals("HouseCode")) {
                setAddress(field.getValue());
            } else {
                throw new BadMessageException(field);
            }
        }
        setRepeatCount(1);
        return encode();
    }

    @Override
    public int modulationFrequency(Message message) {
        return 0;
    }

    /**
     * Build a correct protocol message for the Nexa protocol
     * @param command 0 or 1 meaning off or on
     * @param button which button is pressed 1-16
     * @param address which address (or house code) to send to 0-15
     * @return a message with the specified parameters
     */
    public static Message buildMessage(int command, int button, int address) {
        ProtocolMessage result = new ProtocolMessage("Nexa", command, address, 0);
        result.addField(new FieldValue("Command", command));
        result.addField(new FieldValue("Button", button));
        result.addField(new FieldValue("HouseCode", address));
        return result;
    };
}

