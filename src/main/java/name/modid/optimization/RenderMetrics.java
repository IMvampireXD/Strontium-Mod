package name.modid.optimization;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class RenderMetrics {
	private final AtomicLong cullingNanos = new AtomicLong();
	private final AtomicInteger renderedBlockEntities = new AtomicInteger();
	private final AtomicInteger skippedBlockEntities = new AtomicInteger();
	private final AtomicInteger renderedEntities = new AtomicInteger();
	private final AtomicInteger skippedEntities = new AtomicInteger();
	private final AtomicInteger visibleRenderedBlockEntities = new AtomicInteger();
	private final AtomicInteger visibleSkippedBlockEntities = new AtomicInteger();
	private final AtomicInteger visibleRenderedEntities = new AtomicInteger();
	private final AtomicInteger visibleSkippedEntities = new AtomicInteger();
	private final AtomicLong lastCullingPassNanos = new AtomicLong();
	private final AtomicLong cullingPasses = new AtomicLong();
	private final AtomicLong lastFrameResetNanos = new AtomicLong();

	public void recordCulling(long nanos) {
		cullingNanos.set(nanos);
		lastCullingPassNanos.set(System.nanoTime());
		cullingPasses.incrementAndGet();
	}

	public void setBlockEntities(int rendered, int skipped) {
		renderedBlockEntities.set(rendered);
		skippedBlockEntities.set(skipped);
	}

	public void setEntities(int rendered, int skipped) {
		renderedEntities.set(rendered);
		skippedEntities.set(skipped);
	}

	public void beginRenderPass() {
		long now = System.nanoTime();
		long previous = lastFrameResetNanos.get();
		if (now - previous < 10_000_000L
				|| !lastFrameResetNanos.compareAndSet(previous, now)) {
			return;
		}
		visibleRenderedBlockEntities.set(renderedBlockEntities.get());
		visibleSkippedBlockEntities.set(skippedBlockEntities.get());
		visibleRenderedEntities.set(renderedEntities.get());
		visibleSkippedEntities.set(skippedEntities.get());
		renderedBlockEntities.set(0);
		skippedBlockEntities.set(0);
		renderedEntities.set(0);
		skippedEntities.set(0);
	}

	public void recordEntityRendered() {
		renderedEntities.incrementAndGet();
	}

	public void recordEntitySkipped() {
		skippedEntities.incrementAndGet();
	}

	public void recordBlockEntityRendered() {
		renderedBlockEntities.incrementAndGet();
	}

	public void recordBlockEntitySkipped() {
		skippedBlockEntities.incrementAndGet();
	}

	public String cullingTime() {
		long nanos = cullingNanos.get();
		if (nanos == 0) {
			return "0.000";
		}
		return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000_000.0);
	}

	public long cullingPasses() {
		return cullingPasses.get();
	}

	public long cullingAgeMillis() {
		long timestamp = lastCullingPassNanos.get();
		return timestamp == 0 ? -1 : Math.max(0, (System.nanoTime() - timestamp) / 1_000_000L);
	}

	public int renderedBlockEntities() {
		return visibleRenderedBlockEntities.get();
	}

	public int skippedBlockEntities() {
		return visibleSkippedBlockEntities.get();
	}

	public int renderedEntities() {
		return visibleRenderedEntities.get();
	}

	public int skippedEntities() {
		return visibleSkippedEntities.get();
	}
}
