package org.javafn.examples;

public class DomainTypes {

    public static class Foo {
        public Foo() {}
        public Foo(final String ignored) {}
        public void doFooThings() {}
        public Foo updateState() { return this; }
        public Bar safeBarGenerate(final String ignored) { return new Bar(); }
    }
    public static class Bar { }
}
