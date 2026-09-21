package name.modid.optimization;

import java.util.concurrent.atomic.LongAdder;

public final class PacketTrafficMetrics {
	public static final PacketTrafficMetrics GLOBAL = new PacketTrafficMetrics();
	private static final LongAdder inbound = new LongAdder();
	private static final LongAdder outbound = new LongAdder();
	private static final LongAdder inboundRaw = new LongAdder();
	private static final LongAdder outboundRaw = new LongAdder();
	private static final LongAdder inboundPackets = new LongAdder();
	private static final LongAdder outboundPackets = new LongAdder();
	private final PacketClassDiagnostics diagnostics = new PacketClassDiagnostics();
	private static final LongAdder inboundWindow = new LongAdder();
	private static final LongAdder outboundWindow = new LongAdder();
	private static final LongAdder inboundRawWindow = new LongAdder();
	private static final LongAdder outboundRawWindow = new LongAdder();
	private static volatile long windowStarted = System.nanoTime();
	private static volatile long inboundRate;
	private static volatile long outboundRate;
	private static volatile long inboundRawRate;
	private static volatile long outboundRawRate;

	public void recordInbound(int transmitted, int raw) {
		inbound.add(transmitted);
		inboundRaw.add(raw);
		inboundWindow.add(transmitted);
		inboundRawWindow.add(raw);
		refreshWindow();
	}

	public void recordOutbound(int transmitted, int raw) {
		outbound.add(transmitted);
		outboundRaw.add(raw);
		outboundWindow.add(transmitted);
		outboundRawWindow.add(raw);
		refreshWindow();
	}

	public void recordInbound(Object packet, int transmitted, int raw) {
		recordInbound(transmitted, raw);
		diagnostics.record(packet, transmitted, raw);
	}

	public void recordOutbound(Object packet, int transmitted, int raw) {
		recordOutbound(transmitted, raw);
		diagnostics.record(packet, transmitted, raw);
	}

	public long inbound() { return inbound.sum(); }
	public long outbound() { return outbound.sum(); }
	public long inboundRaw() { return inboundRaw.sum(); }
	public long outboundRaw() { return outboundRaw.sum(); }
	public PacketClassDiagnostics diagnostics() { return diagnostics; }

	public void recordInboundPacket(Object packet) {
		recordInbound(packet, 1, 0);
	}

	public void recordOutboundPacket(Object packet) {
		recordOutbound(packet, 1, 0);
	}

	public void recordInboundPacketOnly(Object packet) {
		inboundPackets.increment();
		inboundWindow.increment();
		diagnostics.record(packet, 0, 0);
	}

	public void recordOutboundPacketOnly(Object packet) {
		outboundPackets.increment();
		outboundWindow.increment();
		diagnostics.record(packet, 0, 0);
	}

	public long inboundPackets() { return inboundPackets.sum(); }
	public long outboundPackets() { return outboundPackets.sum(); }

	public Statistics snapshot() {
		return new Statistics(inbound(), outbound(), inboundRaw(), outboundRaw());
	}

	public record Statistics(long inbound, long outbound, long inboundRaw, long outboundRaw) {
		public double inboundSavings() {
			return inboundRaw == 0 ? 0.0 : 1.0 - ((double) inbound / inboundRaw);
		}

		public double outboundSavings() {
			return outboundRaw == 0 ? 0.0 : 1.0 - ((double) outbound / outboundRaw);
		}
	}

	public long inboundPerSecond() {
		refreshWindow();
		return inboundRate;
	}

	public long outboundPerSecond() {
		refreshWindow();
		return outboundRate;
	}

	public long inboundRawPerSecond() {
		refreshWindow();
		return inboundRawRate;
	}

	public long outboundRawPerSecond() {
		refreshWindow();
		return outboundRawRate;
	}

	private synchronized void refreshWindow() {
		long now = System.nanoTime();
		if (now - windowStarted >= 1_000_000_000L) {
			long elapsed = Math.max(1L, now - windowStarted);
			inboundRate = inboundWindow.sum() * 1_000_000_000L / elapsed;
			outboundRate = outboundWindow.sum() * 1_000_000_000L / elapsed;
			inboundRawRate = inboundRawWindow.sum() * 1_000_000_000L / elapsed;
			outboundRawRate = outboundRawWindow.sum() * 1_000_000_000L / elapsed;
			inboundWindow.reset();
			outboundWindow.reset();
			inboundRawWindow.reset();
			outboundRawWindow.reset();
			windowStarted = now;
		}
	}
}
