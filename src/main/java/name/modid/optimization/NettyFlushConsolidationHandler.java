package name.modid.optimization;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Coalesces bursts of outbound flushes on the channel event loop. It never
 * moves work between threads and always flushes at the configured bound.
 */
public final class NettyFlushConsolidationHandler extends ChannelDuplexHandler {
	private final int maxPendingWrites;
	private int pendingWrites;
	private boolean flushScheduled;

	public NettyFlushConsolidationHandler(int maxPendingWrites) {
		if (maxPendingWrites < 2) {
			throw new IllegalArgumentException("maxPendingWrites must be at least two");
		}
		this.maxPendingWrites = maxPendingWrites;
	}

	@Override
	public void write(ChannelHandlerContext context, Object message, ChannelPromise promise) {
		pendingWrites++;
		context.write(message, promise);
		if (pendingWrites >= maxPendingWrites) {
			flushNow(context);
		} else if (!flushScheduled) {
			flushScheduled = true;
			context.executor().execute(() -> {
				if (flushScheduled) {
					flushNow(context);
				}
			});
		}
	}

	@Override
	public void flush(ChannelHandlerContext context) {
		flushNow(context);
	}

	private void flushNow(ChannelHandlerContext context) {
		pendingWrites = 0;
		flushScheduled = false;
		context.flush();
	}

	@Override
	public void channelInactive(ChannelHandlerContext context) throws Exception {
		if (pendingWrites > 0) {
			flushNow(context);
		}
		super.channelInactive(context);
	}
}
