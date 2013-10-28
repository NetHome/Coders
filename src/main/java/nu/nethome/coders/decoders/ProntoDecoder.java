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

package nu.nethome.coders.decoders;

import nu.nethome.util.plugin.Plugin;
import nu.nethome.util.ps.*;

import java.util.LinkedList;


/**
 * The PRONTO-format is named after a manufacturer of generic remote controls. It is a format
 * for describing raw IR-protocol messages. This decoder does not really decode the signal, 
 * it formats the raw sampled pulses in the pronto-format.
 * 
 * @author Stefan Str�mberg
 */
@Plugin
public class ProntoDecoder implements ProtocolDecoder {
	protected static final int IDLE = 0;
	protected static final int SAMPLING = 1;
	private static final int MIN_MESSAGELENGTH = 10;

	protected int m_State = IDLE;
	
	/** Modulation frequency set to use when calculating the burst length numbers */
	protected double m_Freq = 0;

	/** 
	 * Modulation frequency actually used when calculating the burst length numbers
	 * Because of rounding this is not exactly same as the set frequency 
	 */
	protected double m_UsedFreq = 0;
	
	
	/** Constant for the pronto remote's internal clock frequency */
	protected double m_ProntoFreqConstant = .241246;

	/** Minimum time (in uS) to regard as space between protocol messages */ 
	protected int m_LeadOutMinTime = 9000;
	
	/** Amount of time (in uS) that has to be added to bursts to compensate for slow 
	 * reaction in the receivers 
	 */
	protected double m_PulseWidthModification = 0;

	protected ProtocolDecoderSink m_Sink = null;

	LinkedList<Integer> m_Bursts = new LinkedList<Integer>();
	
	public void setTarget(ProtocolDecoderSink sink) {
		m_Sink = sink;
	}

    public ProntoDecoder() {
        this.setModulationFrequency(40000);
    }
	
	public ProtocolInfo getInfo() {
		return new ProtocolInfo("Pronto", "None", "Pronto", 0, 5);
	}

	public void addBurst(double burstUs, boolean state) {
		// Adjust for imperfections in receiver
		burstUs = state ? burstUs - m_PulseWidthModification  : 
			burstUs + m_PulseWidthModification;
		
		int prontoBurst = (int)(0.5 + (burstUs * m_UsedFreq) / 1000000.0);
		
		// Trim if burst is VERY long
		if (prontoBurst > 0xFFFF) prontoBurst = 0xFFFF;
		
		// Add burst to burst pairs
		m_Bursts.add(prontoBurst);
		
		// Check if this was the Lead Out burst pair
		if (burstUs > m_LeadOutMinTime) {
			// Ok, this is possibly the end of the sequence
			// Check how long it is
			if (m_Bursts.size() < MIN_MESSAGELENGTH) {
				// This was to short to be a real message, it is probably noise - ignore it
				m_Bursts.clear();
				m_State = IDLE;
				return;
			}
			if ((m_Bursts.size() & 1) == 1) {
				// Something is wrong, there should be an even number of bursts to form
				// burst pairs. Add a dummy burst to be able to go on.
				m_Bursts.add(prontoBurst);
			}
			// Add leading 0 to signal this is a sampled signal
			String prontoMessage = "0000";
			// Add the burst frequency
			// No rounding here, Pronto takes pure integer part
			Integer prontoFreq = (int)(1000000.0 /(m_Freq * m_ProntoFreqConstant));
			prontoMessage += String.format(" %04x", prontoFreq);
			// Add zero to signal that we have no one time bursts
			prontoMessage += " 0000";
			// Add number of burst pairs in signal
			prontoMessage += String.format(" %04x", m_Bursts.size() / 2);
			// Add the burst pairs
			for (int burst : m_Bursts) {
				prontoMessage += String.format(" %04x", burst);
			}
			// Create a report of the message
			ProtocolMessage message = new ProtocolMessage("Pronto", 0, m_Bursts.size(), 1);
			message.setRawMessageByteAt(0, m_Bursts.size());
			message.addField(new FieldValue("Message", prontoMessage));
			// Report the parsed message
			m_Sink.parsedMessage(message);
			m_Bursts.clear();
			m_State = IDLE;
		}
	}
	
	/* (non-Javadoc)
	 * @see ssg.ir.IRDecoder#parse(java.lang.Double)
	 */
	public int parse(double pulse, boolean state) {
		switch (m_State) {
		case IDLE: {
			if ((pulse < m_LeadOutMinTime) && (pulse > 0)) {
				m_State = SAMPLING;
				addBurst(pulse, state);
			}
		}
		break;

		case SAMPLING: {
			addBurst(pulse, state);
		}
		break;
		}
        return m_State;
	}

	public double getModulationFrequency() {
		return m_Freq;
	}

	public void setModulationFrequency(double freq) {
		m_Freq = freq;
		Integer prontoFreq = (int)(1000000.0 /(m_Freq * m_ProntoFreqConstant));
		m_UsedFreq = 1000000.0 /(prontoFreq * m_ProntoFreqConstant);
	}

	public int getLeadOutTime() {
		return m_LeadOutMinTime;
	}

	public void setLeadOutTime(int leadOutMinTime) {
		m_LeadOutMinTime = leadOutMinTime;
	}

	public double getPulseWidthModification() {
		return m_PulseWidthModification;
	}

	public void setPulseWidthModification(double pulseWidthModification) {
		m_PulseWidthModification = pulseWidthModification;
	}
}

