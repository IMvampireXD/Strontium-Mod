package name.modid.optimization;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Lambda-backed listener construction for hot event registration paths.
 * Unlike reflective/generated listener factories this creates no adapter
 * classes and keeps construction allocation-free after class linking.
 */
public final class FastEventFactory {
	private FastEventFactory() {
	}

	public static <T> Consumer<T> listener(Consumer<? super T> callback) {
		return Objects.requireNonNull(callback, "callback")::accept;
	}

	public static <T> Supplier<T> constructor(Supplier<? extends T> factory) {
		return Objects.requireNonNull(factory, "factory")::get;
	}
}
