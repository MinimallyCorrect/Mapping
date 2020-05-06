package dev.minco.mapping.util;

public final class Throw {
	private Throw() {}

	public static RuntimeException sneaky(Throwable throwable) {
		throw sneakyThrowConvert(throwable);
	}

	@SuppressWarnings("unchecked")
	private static <T extends RuntimeException> RuntimeException sneakyThrowConvert(Throwable throwable) {
		throw (T) throwable;
	}
}
