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

import nu.nethome.coders.decoders.RisingSunDecoder;
import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

import java.util.ArrayList;

@Plugin
public class RisingSunEncoder implements ProtocolEncoder{
	
	public int repeatCount = 5;
	private int channel = 1;
	private int button = 1;
	private int command = 1;
	protected int onCommandValue;
	protected int offCommandValue;
	
	// Get the pulse lengths from the decoder
	protected int LONG_MARK = RisingSunDecoder.RISING_SUN_LONG_MARK.length(); 
	protected int SHORT_MARK = RisingSunDecoder.RISING_SUN_SHORT_MARK.length();
	protected int LONG_SPACE = RisingSunDecoder.RISING_SUN_LONG_SPACE.length(); 
	protected int SHORT_SPACE = RisingSunDecoder.RISING_SUN_SHORT_SPACE.length();
	protected int REPEAT = RisingSunDecoder.RISING_SUN_REPEAT.length();
	protected static int BUTTON_MAP[] = {0x54, 0x51, 0x45, 0x15};

	public RisingSunEncoder() {
		setup();
	}
	
	protected void setup() {
		offCommandValue = 0x15 << 17;
		onCommandValue = 0x55 << 17;
	}

	/**
	 * s = Start bit = 0<br>
	 * a = Channel 1 not selected<br>
	 * b = Channel 2 not selected<br>
	 * c = Channel 3 not selected<br>
	 * d = Channel 4 not selected<br>
	 * e = Button 1 not pressed<br>
	 * f = Button 2 not pressed<br>
	 * g = Button 3 not pressed<br>
	 * h = Button 4 not pressed<br>
	 * o = On/Off-bit<br>
	 * <br>
	 *  ____Byte 2_____  ____Byte 1_____  ____Byte 0_____  _S_<br>
	 *  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0  7 6 5 4 3 2 1 0   0<br>
	 *  x o x x x x x x  x h x g x f x e  x d x c x b x a   s<br>
	 *
	 */
	public int[] encode() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		long messageTemplate = 0;
		long message = 0;
		
		// encode message
		int sendChannel = this.channel - 1;
		int sendButton = this.button - 1;
		messageTemplate |= BUTTON_MAP[sendChannel] << 1;
		messageTemplate |= BUTTON_MAP[sendButton] << 9;

		messageTemplate |= (command == 0) ? offCommandValue : onCommandValue;

		// Start actually encoding the data pulses
		for (int i = 0; i < repeatCount; i++) {
			message = messageTemplate;
			
			// 25 bits of data including start bit
			for (int j = 0; j < 25; j++) {
				if ((message & 1) == 1) {
					result.add(LONG_MARK);
					result.add(SHORT_SPACE);
				}
				else {
					result.add(SHORT_MARK);
					result.add(LONG_SPACE);
				}
				message >>= 1;
			}
			int last = result.size() - 1;
			// Add the repeat delay as the last space period
			result.set(last, REPEAT);
		}
		int resultArray[] = new int[result.size()];
		for (int i = 0; i < result.size(); i++) {
			resultArray[i] = result.get(i);
		}
		return resultArray;
	}

	public int getChannel() {
		return channel;
	}

	public void setChannel(int channel) {
		if ((channel < 1) || (channel > 4)) {
			return;
		}
		this.channel = channel;
	}

	public int getButton() {
		return button;
	}

	public void setButton(int deviceCode) {
		if ((deviceCode < 1) || (deviceCode > 4)) {
			return;
		}
		button = deviceCode;
	}

	public int getCommand() {
		return command;
	}

	public void setCommand(int command) {
		if ((command < 0) || (command > 1)) {
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

    public ProtocolInfo getInfo() {
        return new ProtocolInfo("RisingSun", "Mark Length", "RisingSun", 25, 5);
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
            } else if (field.getName().equals("Channel")) {
                setChannel(field.getValue());
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
     * @param channel which address (or house code) to send to 0-15
     * @return a message with the specified parameters
     */
    public static Message buildMessage(int command, int button, int channel) {
        ProtocolMessage result = new ProtocolMessage("RisingSun", command, channel, 0);
        result.addField(new FieldValue("Command", command));
        result.addField(new FieldValue("Button", button));
        result.addField(new FieldValue("Channel", channel));
        return result;
    };

}


