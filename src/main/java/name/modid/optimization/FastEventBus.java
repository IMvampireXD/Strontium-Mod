package name.modid.optimization;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Typed event dispatch avoids Method.invoke and class generation. Subscribers
 * can be method references or lambdas and are invoked directly.
 */
public final class FastEventBus<T> {
	private final CopyOnWriteArrayList<Consumer<? super T>> listeners = new CopyOnWriteArrayList<>();

	public void subscribe(Consumer<? super T> listener) {
		listeners.add(FastEventFactory.listener(listener));
	}

	public void unsubscribe(Consumer<? super T> listener) {
		listeners.remove(listener);
	}

	public void post(T event) {
		for (Consumer<? super T> listener : listeners) {
			listener.accept(event);
		}
	}
}
