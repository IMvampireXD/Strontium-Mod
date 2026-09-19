package name.modid.optimization;

import java.util.function.Supplier;

public final class LazyValue<T> implements Supplier<T> {
	private Supplier<? extends T> factory;
	private T value;

	public LazyValue(Supplier<? extends T> factory) {
		this.factory = factory;
	}

	@Override
	public synchronized T get() {
		if (factory != null) {
			value = factory.get();
			factory = null;
		}
		return value;
	}
}
