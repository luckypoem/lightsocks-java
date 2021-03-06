package com.lightsocks.socks5.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import com.lightsocks.socks5.bean.DstServer;

public class CRequestHandler extends ChannelHandlerAdapter {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
		DstServer server = getDstServer(ctx, msg);
		if (server == null) {
			ctx.close();
			return;
		}
		ctx.pipeline().replace(this, "myHandler", new CLeftHandler(server));
	}

	/**
	 * +----+-----+-------+------+----------+----------+ 
	 * |VER | CMD | RSV | ATYP | DST.ADDR   | DST.PORT | 
	 * +----+-----+-------+------+----------+----------+
	 * | 1  | 1   | X'00' | 1  | Variable   | 2        |
	 * +----+-----+-------+------+----------+----------+
	 * 
	 * @param ctx
	 * @param msg
	 * @return
	 */
	private DstServer getDstServer(ChannelHandlerContext ctx, Object msg) {
		ByteBuf buf = ((ByteBuf) msg);
		byte v = buf.readByte();
		if (v != 5) {
			buf.release();
			return null;
		}
		buf.skipBytes(2); // skip cmd and rsv
		byte atyp = buf.readByte();
		DstServer server = new DstServer();
		server.setAtyp(atyp);
		byte[] addr = null;
		if (atyp == 1) { // ipv4
			addr = new byte[4];
			buf.readBytes(addr);
		} else if (atyp == 4) { // ipv6
			addr = new byte[6];
			buf.readBytes(addr);
		} else if (atyp == 3) { // domain
			byte length = buf.readByte();
			addr = new byte[length + 1];
			addr[0] = length;
			buf.readBytes(addr, 1, length);
		}
		server.setAddr(addr);
		byte[] port = new byte[2];
		buf.readBytes(port);
		server.setPort(port);
		buf.release();
		return server;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}
