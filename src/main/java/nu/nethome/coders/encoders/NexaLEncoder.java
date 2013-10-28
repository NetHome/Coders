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
 * The NexaLEncoder is an encoder for the learning code RF-protocol used by 
 * NEXA products such as remote switches, dimmers, PIR-detectors and so on. 
 * NEXA uses two different protocols, the Code Switch-protocol which is 
 * encoded by the NexaEncoder and the Learning Code-Protocol. The NexaLEncoder
 * can take the data from a protocol message and encode that into a sequence
 * of pulse lengths which may be played by RF-transmitting hardware for instance 
 * via the AudioPulsePlayer.
 * 
 * @author Stefan
 *
 */
@Plugin
public class NexaLEncoder implements ProtocolEncoder{
	
	public int repeatCount = 5;
	private long m_Address = 0;
	private int m_Command = 1;
	private int m_Button = 1;
	private int m_DimLevel = -1;

	protected static final int NEXA_HEADER_MARK = 290; // 240; // O340
	protected static final int NEXA_HEADER_SPACE = 2615; // O2545
	protected static final int NEXA_MARK = 250; // 220; // O280
	protected static final int NEXA_LONG_SPACE = 1100; // 1120;
	protected static final int NEXA_SHORT_SPACE = 280; // O250
	protected static final int NEXA_LONG_INTER_SPACE = 1120;
	protected static final int NEXA_SHORT_INTER_SPACE = 325; // O340
	protected static final int NEXA_REPEAT = 9600;

	protected static final int MESSAGE_LENGTH = 32;
	protected static final int MESSAGE_LENGTH_DIM = 36;
	
	/**
	 * a = Address
	 * s = On/Off/dim-bit. This "bit" can actually have three states! The protocol supports 1, 0 and tri state on 
	 *     all message bits. Usually only two of the states are used, but in this case the third state is used
	 *     to signal that there is extra dim level information at the end of the message. 
	 * g = Group bit
	 * b = Button
	 * d = Dim Level
	 * 
	 *  ____Byte 3_____  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____  __NibbleX__
	 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0    3 2 1 0
	 *  a a a a a a a a  a a a a a a a a  a a a a a a a a  a a g s b b b b    d d d d
	 */

	public int[] encode() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		long message = 0;
		boolean doDim = (m_DimLevel != -1);
		int messageLength = doDim ? MESSAGE_LENGTH_DIM : MESSAGE_LENGTH;
		
		// encode message
		long messageTemplate = 0;
		messageTemplate |= (m_Command << 4);
		messageTemplate |= (m_Address << 6);
		messageTemplate |= ((m_Button - 1) & 0x0F);
		messageTemplate |= ((m_Button - 1) & 0x10) << 1;
		if (doDim) {
			messageTemplate = (messageTemplate << 4) + m_DimLevel;
		}
		
		// Start encoding the data pulses
		for (int i = 0; i < repeatCount; i++) {
			long mask = 1L << (messageLength - 1);
			message = messageTemplate;

			// Encode header
			result.add(NEXA_HEADER_MARK);
			result.add(NEXA_HEADER_SPACE);

			// 32 or 36 bits of data
			for (int j = 0; j < messageLength; j++) {

				result.add(NEXA_MARK);
				if (doDim && (j == 27) && ((message & mask) == mask)) {
					// Encode "tri state"
					// This is the special third state bit which is used to signal that there is dim level
					// information at the end of the message
					result.add(NEXA_SHORT_SPACE);
					result.add(NEXA_MARK);
					result.add(NEXA_SHORT_INTER_SPACE);
				} else if ((message & mask) == mask) {
					// Encode a "1"
					result.add(NEXA_LONG_SPACE);
					result.add(NEXA_MARK);
					result.add(NEXA_SHORT_INTER_SPACE);
				}
				else {
					// Encode a "0"
					result.add(NEXA_SHORT_SPACE);
					result.add(NEXA_MARK);
					result.add(NEXA_LONG_INTER_SPACE);
				}
				mask >>= 1;
			}
			//Add trailing mark
			result.add(NEXA_MARK);

			// Add the repeat delay
			result.add(NEXA_REPEAT);
		}
		int resultArray[] = new int[result.size()];
		for (int i = 0; i < result.size(); i++) {
			resultArray[i] = result.get(i);
		}
		return resultArray;
	}

	/**
	 * Get the address (see {@link nu.nethome.coders.encoders.NexaLEncoder#setAddress} )
	 * @return Address
	 */
	public int getAddress() {
		return (int)m_Address;
	}

	/**
	 * Set the address of the message. Each sender has a unique address which is a 26 bit
	 * long integer. The receivers are dynamically programmed which sender address they
	 * shall respond to.
	 * @param address (0 - 67 108 863)
	 */
	public void setAddress(int address) {
		if ((address >= (1<<26)) || (address < 0)) {
			return;
		}
		m_Address = address;
	}

	/**
	 * Get the message command, see {@link nu.nethome.coders.encoders.NexaLEncoder#setCommand} )
	 * @return
	 */
	public int getCommand() {
		return m_Command;
	}

	/**
	 * Set the message command, which basically is just "on" or "off" encoded as 0, and 1 respectively
	 * @param command 0 or 1
	 */
	public void setCommand(int command) {
		if ((command > 1) || (command < 0)) {
			return;
		}
		m_Command = command;
	}

	/**
	 * Get number of repeats, see {@link nu.nethome.coders.encoders.NexaLEncoder#setRepeatCount}
	 * @return number of repeats
	 */
	public int getRepeatCount() {
		return repeatCount;
	}

	/**
	 * Set the number of times the message shall be repeated in the encoded list. Default value 
	 * is 5 times, which is the amount the Nexa remote controls use.
	 * @param repeatCount, 1-100
	 */
	public void setRepeatCount(int repeatCount) {
		if ((repeatCount < 1) || (repeatCount > 100)) {
			return;
		}
		this.repeatCount = repeatCount;
	}

	/**
	 * Get the button, see {@link nu.nethome.coders.encoders.NexaLEncoder#setButton}
	 * @return Button
	 */
	public int getButton() {
		return m_Button;
	}

	/**
	 * Set the id of the button that is pressed on the remote. A remote can have up to 32 buttons.
	 * @param button 1-32 
	 */
	public void setButton(int button) {
		if ((button > 33) || (button < 1)) {
			return;
		}
		m_Button = button;
	}

	/**
	 * Get current dim level. -1 if no dim is applied.
	 * @return dim level
	 */
	public int getDimLevel() {
		return m_DimLevel;
	}

	/**
	 * Specify an absolute dim level. 0 is the lowest light level (not off) and 14 is highest, 
	 * 15 seem to be some kind of half dim level? -1 means no dim level (current dim level
	 * in the device unaffected) 
	 * @param mDimLevel 0-15
	 */
	public void setDimLevel(int mDimLevel) {
		if ((mDimLevel < -1) || (mDimLevel > 15)) {
			return;
		}
		m_DimLevel = mDimLevel;
	}

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("NexaL", "Space Length", "Nexa", 32, 5);
    }

    public int[] encode(Message message, Phase phase) throws BadMessageException {
        if (Phase.FIRST == phase) {
            return new int[0];
        }
        setDimLevel(-1);
        for (FieldValue field : message.getFields()) {
            if (field.getName().equals("Command")) {
                setCommand(field.getValue());
            } else if (field.getName().equals("Button")) {
                setButton(field.getValue());
            } else if (field.getName().equals("Address")) {
                setAddress(field.getValue());
            } else if (field.getName().equals("DimLevel")) {
                setDimLevel(field.getValue());
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
     * @param button which button is pressed 1-32
     * @param address which address (or house code) to send to 0-15
     * @return a message with the specified parameters
     */
    public static Message buildMessage(int command, int button, int address) {
        ProtocolMessage result = new ProtocolMessage("Nexa", command, address, 0);
        result.addField(new FieldValue("Command", command));
        result.addField(new FieldValue("Button", button));
        result.addField(new FieldValue("Address", address));
        return result;
    };

    /**
     * Build a correct protocol message for the NexaL protocol with an absolute dim level specified
     * @param dimLevel Specifies the absolute dim level of the lamp 0-15
     * @param button which button is pressed 1-32
     * @param address which address to send to 0 - 67 108 863
     * @return a message with the specified parameters
     */
    public static Message buildDimMessage(int dimLevel, int button, int address) {
        ProtocolMessage result = new ProtocolMessage("NexaL", 1, address, 0);
        result.addField(new FieldValue("Command", 1));
        result.addField(new FieldValue("Button", button));
        result.addField(new FieldValue("Address", address));
        result.addField(new FieldValue("DimLevel", dimLevel));
        return result;
    };
}

