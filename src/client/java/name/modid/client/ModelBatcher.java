package name.modid.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Groups submitted model payloads by render state so a renderer can bind a
 * material once and draw all compatible models in one batch.
 */
public final class ModelBatcher<K, V> {
	private final Map<K, ArrayList<V>> batches = new HashMap<>();

	public void submit(K renderState, V model) {
		batches.computeIfAbsent(renderState, ignored -> new ArrayList<>()).add(model);
	}

	public int batchCount() {
		return batches.size();
	}

	public int modelCount() {
		int count = 0;
		for (List<V> models : batches.values()) {
			count += models.size();
		}
		return count;
	}

	public void forEachBatch(Consumer<? super List<V>> consumer) {
		for (List<V> models : batches.values()) {
			consumer.accept(models);
		}
	}

	public void clear() {
		for (ArrayList<V> models : batches.values()) {
			models.clear();
		}
		batches.clear();
	}
}
