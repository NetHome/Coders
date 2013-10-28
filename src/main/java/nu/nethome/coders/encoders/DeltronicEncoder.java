/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stefangs@nethome.nu>
 *
 * This file is part of OpenNetHome (http://www.nethome.nu).
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

package nu.nethome.coders.encoders;

import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

import java.util.ArrayList;

/**
 * The DeltronicEncoder is an encoder for the RF-protocol used by 
 * Deltronic products such as remote switches. The DeltronicEncoder
 * can take the data from a protocol message and encode that into a sequence
 * of pulse lengths which may be played by RF-transmitting hardware for instance 
 * via the AudioPulsePlayer.
 * 
 * @author Stefan
 */
@Plugin
public class DeltronicEncoder implements ProtocolEncoder {
	
	public int repeatCount = 3;
	private int address = 0;
	private int command = 1;
	private int button = 0;

	protected static final int DELTRONIC_HEADER_MARK = 330 - 60;
	protected static final int DELTRONIC_HEADER_SPACE = 830 + 60;
	protected static final int DELTRONIC_LONG_MARK = 835 - 60;
	protected static final int DELTRONIC_SHORT_MARK = 280 - 60;
	protected static final int DELTRONIC_LONG_SPACE = 845 + 60;
	protected static final int DELTRONIC_SHORT_SPACE = 300 + 60;
	protected static final int DELTRONIC_INTER_SPACE = 850 + 60;
	protected static final int DELTRONIC_REPEAT = 8810 + 60;
	
	protected static final int MESSAGE_LENGTH = 12;
	protected static final int HIGH_BIT = 1 << (MESSAGE_LENGTH - 1);
	
	static final int s_Buttons[] = {0x20, 0x10, 0x04, 0x08};

	/**
	 * Encodes the current message according to the following protocol:
	 *
	 * a = Address
	 * A = Button A
	 * B = Button B
	 * C = Button C
	 * D = Button D
	 * f = Off-bit
	 * n = On-bit
	 * 
	 *  ____Byte 1_____  ____Byte 0_____
	 *          3 2 1 0  7 6 5 4 3 2 1 0
	 *          a a a a  a a A B D C f n
	 */
	public int[] encode() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		long message = 0;
		
		// encode message
		long messageTemplate = 0x00000000;
		messageTemplate |= (command == 1) ? 0x01 : 0x02;
		messageTemplate |= s_Buttons[button];
		messageTemplate |= (address << 6);
		
		// Start encoding the data pulses
		for (int i = 0; i < repeatCount; i++) {
			message = messageTemplate;

			// Encode header
			result.add(DELTRONIC_HEADER_MARK);
			result.add(DELTRONIC_HEADER_SPACE);

			// Encode message bits
			for (int j = 0; j < (MESSAGE_LENGTH); j++) {
				if ((message & HIGH_BIT) == HIGH_BIT) {
					result.add(DELTRONIC_SHORT_MARK);
					result.add(DELTRONIC_LONG_SPACE);
				}
				else {
					result.add(DELTRONIC_LONG_MARK);
					result.add(DELTRONIC_SHORT_SPACE);
				}
				result.add(DELTRONIC_SHORT_MARK);
				result.add(DELTRONIC_INTER_SPACE);
				message <<= 1;
			}
			// Add the repeat delay
			int last = result.size() - 1;
			// Add the repeat delay to the last space period
			result.set(last, DELTRONIC_REPEAT);
		}
		int resultArray[] = new int[result.size()];
		for (int i = 0; i < result.size(); i++) {
			resultArray[i] = result.get(i);
		}
		return resultArray;
	}

	public int getAddress() {
		return address;
	}

	public void setAddress(int address) {
		if ((address >= (1<<6)) || (address < 0)) {
			return;
		}
		this.address = address;
	}

	public int getCommand() {
		return command;
	}

	public void setCommand(int command) {
		if ((command > 1) || (command < 0)) {
			return;
		}
		this.command = command;
	}

	public int getRepeatCount() {
		return repeatCount;
	}

	public void setRepeatCount(int repeatCount) {
		if ((repeatCount < 1) || (repeatCount > 100)) {
			return;
		}
		this.repeatCount = repeatCount;
	}

	public int getButton() {
		return button;
	}

	public void setButton(int button) {
		if ((button < 0) || (button > 3)) {
			return;
		}
		this.button = button;
	}

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("Deltronic", "Space Length", "Deltronic", 12, 5);
    }

    public int[] encode(Message message, Phase phase) throws BadMessageException {
        if (Phase.FIRST == phase) {
            return new int[0];
        }
        for (FieldValue field : message.getFields()) {
            if (field.getName().equals("Command")) {
                setCommand(field.getValue());
            } else if (field.getName().equals("Button")) {
                setButton(field.getValue());
            } else if (field.getName().equals("Address")) {
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

    public static Message buildCommandMessage(boolean on, int button, int address) {
        ProtocolMessage result = new ProtocolMessage("Deltronic", on ? 1 :0, address, 0);
        result.addField(new FieldValue("Command", on ? 1 : 0));
        result.addField(new FieldValue("Button", button));
        result.addField(new FieldValue("Address", address));
        return result;
    };
}

