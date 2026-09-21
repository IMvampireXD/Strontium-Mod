package name.modid.optimization;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Maps repeated immutable payloads to compact template references. References
 * are only emitted after a literal has been observed, so an unknown reference
 * can always request a literal fallback.
 */
public final class LiteralTemplateMapper {
	private final BoundedCache<Long, byte[]> templates;
	private final int maximumTemplates;
	private long nextId = 1;

	public LiteralTemplateMapper(int maximumTemplates) {
		if (maximumTemplates < 1) {
			throw new IllegalArgumentException("maximumTemplates must be positive");
		}
		this.maximumTemplates = maximumTemplates;
		templates = new BoundedCache<>(maximumTemplates);
	}

	public synchronized Template encode(byte[] payload) {
		byte[] copy = payload.clone();
		long id = digestId(copy);
		byte[] previous = templates.get(id);
		if (previous != null && Arrays.equals(previous, copy)) {
			return new Template(id, true, new byte[0]);
		}
		if (previous != null || id == 0) {
			do {
				id = nextId++;
			} while (id == 0 || templates.get(id) != null);
		}
		templates.put(id, copy);
		return new Template(id, false, copy);
	}

	public synchronized byte[] resolve(long id) {
		byte[] value = templates.get(id);
		return value == null ? null : value.clone();
	}

	public synchronized void remember(long id, byte[] payload) {
		if (id <= 0 || payload == null) {
			throw new IllegalArgumentException("invalid template");
		}
		templates.put(id, payload.clone());
	}

	public synchronized void clear() {
		templates.clear();
		nextId = 1;
	}

	public synchronized int size() {
		return templates.size();
	}

	public record Template(long id, boolean reference, byte[] literal) {
		public Template {
			literal = literal == null ? new byte[0] : literal.clone();
		}

		@Override
		public byte[] literal() {
			return literal.clone();
		}
	}

	private static long digestId(byte[] payload) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
			long id = 0;
			for (int index = 0; index < Long.BYTES; index++) {
				id = (id << 8) | (digest[index] & 0xffL);
			}
			return id & Long.MAX_VALUE;
		} catch (NoSuchAlgorithmException exception) {
			throw new AssertionError(exception);
		}
	}
}
