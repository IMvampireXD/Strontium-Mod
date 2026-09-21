package name.modid.optimization;

import java.nio.ByteBuffer;

public final class StrontiumOptimizations implements AutoCloseable {
	public static final int CHUNK_BUFFER_SIZE = 256 * 1024;
	private final ByteBufferPool bufferPool = new ByteBufferPool(CHUNK_BUFFER_SIZE, 64);
	private final ChunkUpdateScheduler chunkScheduler = new ChunkUpdateScheduler();
	private final BoundedCache<Long, Boolean> visibilityCache = new BoundedCache<>(4096);
	private final TextureAnimationCache textureAnimations = new TextureAnimationCache(1024);
	private final BackgroundThrottle backgroundThrottle = new BackgroundThrottle();
	private final UniformCache uniforms = new UniformCache();
	private final VertexDeduplicator vertices = new VertexDeduplicator(16384);
	private final PacketSerializationCache packets = new PacketSerializationCache();
	private final FastTextureUpload textureUpload = new FastTextureUpload(64 * 1024);
	private final NoiseGenerationScheduler noise = new NoiseGenerationScheduler(chunkScheduler);
	private final DeferredLightUpdates lights = new DeferredLightUpdates();
	private final StringDeduplicator strings = new StringDeduplicator();
	private final RenderMetrics metrics = new RenderMetrics();
	private final ChunkDeliveryScheduler chunkDelivery = new ChunkDeliveryScheduler(4096);
	private final SharedChunkPacketCache sharedChunks = new SharedChunkPacketCache(2048);
	private final DelayedChunkCache delayedChunks = new DelayedChunkCache(
			Integer.getInteger("strontium.dccSizeLimit", 256),
			Double.parseDouble(System.getProperty("strontium.dccDistance", "16")),
			Long.getLong("strontium.dccTimeout", 30_000L));
	private final AdaptiveChunkLoadBudget chunkBudget = new AdaptiveChunkLoadBudget(1, 8, 33.3);
	private final CompactPacketRegistry compactPackets = new CompactPacketRegistry();
	private final OrderedPacketBatcher packetBatcher = new OrderedPacketBatcher();
	private final PacketTrafficMetrics traffic = PacketTrafficMetrics.GLOBAL;
	private final EncodedPacketTransport encodedTransport = new EncodedPacketTransport();
	private final LiteralTemplateMapper packetTemplates = new LiteralTemplateMapper(1024);
	private final IdleGate transportIdleGate = new IdleGate(1_000_000_000L, 30_000_000_000L);
	private final DistanceUpdatePolicy distanceUpdates = new DistanceUpdatePolicy(32, 192, 4);
	private final CameraMathCache cameraMath = new CameraMathCache();
	private final DirtyRegionBatch dirtyRegions = new DirtyRegionBatch();
	private final RenderStateCache<Long, Object> renderStates = new RenderStateCache<>(4096);
	private final ParticleBudget particles = new ParticleBudget(16, 512);
	private final PathfindingCache<Long, Object> paths = new PathfindingCache<>(1024);
	private final EntityCollisionBroadphase collisionBroadphase = new EntityCollisionBroadphase();
	private final NativeCollisionBackend nativeCollision = new NativeCollisionBackend();
	private final UniformLocationCache uniformLocations = new UniformLocationCache();
	private final VramDebugTracker vram = new VramDebugTracker();

	public ByteBuffer acquireChunkBuffer() {
		return bufferPool.acquire();
	}

	public void releaseChunkBuffer(ByteBuffer buffer) {
		bufferPool.release(buffer);
	}

	public ChunkUpdateScheduler chunks() {
		return chunkScheduler;
	}

	public TextureAnimationCache textureAnimations() {
		return textureAnimations;
	}

	public BackgroundThrottle backgroundThrottle() {
		return backgroundThrottle;
	}

	public UniformCache uniforms() {
		return uniforms;
	}

	public VertexDeduplicator vertices() {
		return vertices;
	}

	public PacketSerializationCache packets() {
		return packets;
	}

	public FastTextureUpload textureUpload() {
		return textureUpload;
	}

	public NoiseGenerationScheduler noise() {
		return noise;
	}

	public DeferredLightUpdates lights() {
		return lights;
	}

	public StringDeduplicator strings() {
		return strings;
	}

	public RenderMetrics metrics() {
		return metrics;
	}

	public ChunkDeliveryScheduler chunkDelivery() { return chunkDelivery; }
	public SharedChunkPacketCache sharedChunks() { return sharedChunks; }
	public DelayedChunkCache delayedChunks() { return delayedChunks; }
	public AdaptiveChunkLoadBudget chunkBudget() { return chunkBudget; }
	public CompactPacketRegistry compactPackets() { return compactPackets; }
	public OrderedPacketBatcher packetBatcher() { return packetBatcher; }
	public PacketTrafficMetrics traffic() { return traffic; }
	public EncodedPacketTransport encodedTransport() { return encodedTransport; }
	public LiteralTemplateMapper packetTemplates() { return packetTemplates; }
	public IdleGate transportIdleGate() { return transportIdleGate; }
	public DistanceUpdatePolicy distanceUpdates() { return distanceUpdates; }
	public CameraMathCache cameraMath() { return cameraMath; }
	public DirtyRegionBatch dirtyRegions() { return dirtyRegions; }
	public RenderStateCache<Long, Object> renderStates() { return renderStates; }
	public ParticleBudget particles() { return particles; }
	public PathfindingCache<Long, Object> paths() { return paths; }
	public EntityCollisionBroadphase collisionBroadphase() { return collisionBroadphase; }
	public NativeCollisionBackend nativeCollision() { return nativeCollision; }
	public UniformLocationCache uniformLocations() { return uniformLocations; }
	public VramDebugTracker vram() { return vram; }

	public boolean cachedVisibility(long key, java.util.function.BooleanSupplier calculation) {
		return visibilityCache.getOrCompute(key, ignored -> calculation.getAsBoolean());
	}

	public void clearFrameCaches() {
		visibilityCache.clear();
	}

	@Override
	public void close() {
		chunkScheduler.close();
		bufferPool.clear();
		visibilityCache.clear();
		uniforms.invalidate();
		vertices.clear();
		packets.clear();
		textureUpload.clear();
		lights.clear();
		strings.clear();
		sharedChunks.clear();
		delayedChunks.clear();
		textureAnimations.clear();
		packetTemplates.clear();
		dirtyRegions.clear();
		renderStates.invalidate();
		paths.invalidate();
		uniformLocations.clear();
	}
}
