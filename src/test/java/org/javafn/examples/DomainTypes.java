package org.javafn.examples;

import java.io.Closeable;

public class DomainTypes {

    public static class Foo {
        public Foo() {}
        public Foo(final Object... ignored) {}
        public void doFooThings() {}
        public Foo updateState() { return this; }
        public Bar safeBarGenerate(final Object... ignored) { return new Bar(); }
        public boolean isStateValid() { return true; }
        public Closeable open() { return () -> {}; }
        public String name() { return ""; }
        public void rollback() { }
    }
    public static class Bar {
        public void doBarThings() { }
    }
    public static class HttpResponse {
        public HttpResponse code(int ignored) { return this; }
        public HttpResponse entity(Object ignored) { return this; }
    }
}
