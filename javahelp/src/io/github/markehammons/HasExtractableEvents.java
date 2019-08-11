package io.github.markehammons;

public interface HasExtractableEvents<T,U> {
    U extractFrom(T t);
}
