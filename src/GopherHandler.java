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

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class GopherHandler extends SimpleChannelUpstreamHandler {
	  
    private static final Logger logger = Logger.getLogger(
    		GopherHandler.class.getName());

	private IGopherRequestHandler _requestHandler;
    
    public GopherHandler(IGopherRequestHandler requestHandler) {
    	_requestHandler = requestHandler;
	}

	public void handleUpstream(
            ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent) {
            logger.debug(e.toString());
        }
        super.handleUpstream(ctx, e);
    }

    public void messageReceived(
            ChannelHandlerContext ctx, MessageEvent e) {
    	
        // Try to get the message  
        if (!(e.getMessage() instanceof String)) {
        	logger.fatal("MessageEvent.getMessage() not a string");
        	return;
        }
        
        // Get Selector
        String selector = (String) e.getMessage();
        String queryString = null;
        // If the selector have <TAB>
		int indexTAB = selector.indexOf("\t");
		if (indexTAB != -1) {
			queryString = selector.substring(indexTAB + 1).trim();
			selector = selector.substring(0, indexTAB).trim();
		}
		logger.info("[Selector : '" + selector + "']");
		logger.info("[Query String : '" + queryString + "']");
        
        // Process Gopher Request
        GopherTransactionResult result;
        result = _requestHandler.process(selector, queryString);
        
        ChannelFuture writerFuture = result.processChannel(e.getChannel(), queryString);
        // Close the connection when the whole content is written out.
        writerFuture.addListener(ChannelFutureListener.CLOSE);
    } 

	@Override
    public void channelDisconnected(ChannelHandlerContext ctx,
            ChannelStateEvent e) throws Exception {
    }

    public void exceptionCaught(
            ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.error("Unexpected exception from downstream.",
                e.getCause());
        e.getChannel().close();
    }
}
