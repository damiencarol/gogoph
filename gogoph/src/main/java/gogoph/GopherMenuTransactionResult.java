/*
    GOGOPH - Modern Gopher Server easy to manage.
    Copyright (C) 2012  Damien CAROL

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package gogoph;

import java.util.ArrayList;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

public class GopherMenuTransactionResult extends GopherTransactionResult {
	
	public ArrayList<GopherDirectoryEntity> selectors;

	public GopherMenuTransactionResult(ArrayList<GopherDirectoryEntity> tab) {
		selectors = tab;
	}

	public ChannelFuture processChannel(Channel channel, String queryString) {
		ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
		for (GopherDirectoryEntity item : selectors) {
			buf.writeBytes(item.getType().getBytes(ascii)); // data
			buf.writeBytes(item.getUsername().getBytes(ascii)); // data
			
			buf.writeByte( (byte)0x09 );  // <TAB>
			buf.writeBytes(item.getSelector().getBytes(ascii)); // data
			
			buf.writeByte( (byte)0x09 );  // <TAB>
			buf.writeBytes(item.getHost().getBytes(ascii)); // data
			
			buf.writeByte( (byte)0x09 );  // <TAB>
			buf.writeBytes(new Integer(item.getPort()).toString().getBytes(ascii)); // data
			
			buf.writeByte( (byte)0x0D );  // <CR>
			buf.writeByte( (byte)0x0A );  // <LF>
		}
		
		buf.writeByte( (byte)0x2E );  // .
		buf.writeByte( (byte)0x0D );  // <CR>
		buf.writeByte( (byte)0x0A );  // <LF>
		
		return channel.write(buf);
	}
}
