package com.tc.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiIOExceptionHandler {

  private final List<Throwable> exceptions = new ArrayList<>();

  public void doSafely(SafeRunnable safeRunnable) {
    try {
      safeRunnable.run();
    } catch (MultiIOException e) {
      exceptions.addAll(Arrays.asList(e.getSuppressed()));
    } catch (Exception e) {
      exceptions.add(e);
    }
  }

  public void done(String errorMsg) throws IOException {
    if (!exceptions.isEmpty()) {
      MultiIOException ioException = new MultiIOException(errorMsg);
      exceptions.forEach(ioException::addSuppressed);
      exceptions.clear();
      throw ioException;
    }
  }

  public void addAsSuppressedTo(Throwable t) {
    exceptions.forEach(t::addSuppressed);
    exceptions.clear();
  }

  static class MultiIOException extends IOException {
    MultiIOException(String message) {
      super(message);
    }
  }

  @FunctionalInterface
  public interface SafeRunnable {
    void run() throws Exception;
  }

}
