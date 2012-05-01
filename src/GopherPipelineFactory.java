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

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;

public class GopherPipelineFactory implements ChannelPipelineFactory {

	private IGopherRequestHandler _requestHandler;

	public GopherPipelineFactory(IGopherRequestHandler requestHandler) {
		_requestHandler = requestHandler;
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = pipeline();

		pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192,
				Delimiters.lineDelimiter()));

		pipeline.addLast("decoder", new StringDecoder());
		// pipeline.addLast("encoder", new GopherEncoder());

		// Add business logic.
		// Please note we create a handler for every new channel
		// because it has state-full properties.
		pipeline.addLast("handler", new GopherHandler(_requestHandler));

		return pipeline;
	}
}
