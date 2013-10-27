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

package nu.nethome.coders.encoders;

import nu.nethome.util.ps.ProtocolEncoder;

import java.util.ArrayList;
import java.util.Collection;

public class Encoders {
    public static Collection<Class<? extends ProtocolEncoder>> getAllTypes() {
        Collection<Class<? extends ProtocolEncoder>> result = new ArrayList<Class<? extends ProtocolEncoder>>();
        result.add(DeltronicEncoder.class);
        result.add(EmotivaEncoder.class);
        result.add(NexaEncoder.class);
        result.add(NexaFireEncoder.class);
        result.add(NexaLEncoder.class);
        result.add(ProntoEncoder.class);
        result.add(RisingSunEncoder.class);
        result.add(WavemanEncoder.class);
        result.add(X10Encoder.class);
        result.add(ZhejiangEncoder.class);
        return result;
    }
}
