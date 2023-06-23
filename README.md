```
      ███                                    ███    ██████             ███  
     ░░░                                    ███    ███░░███           ░░███ 
     █████  ██████   █████ █████  ██████   ███    ░███ ░░░  ████████   ░░███
    ░░███  ░░░░░███ ░░███ ░░███  ░░░░░███ ░███   ███████   ░░███░░███   ░███
     ░███   ███████  ░███  ░███   ███████ ░███  ░░░███░     ░███ ░███   ░███
     ░███  ███░░███  ░░███ ███   ███░░███ ░░███   ░███      ░███ ░███   ███ 
     ░███ ░░████████  ░░█████   ░░████████ ░░███  █████     ████ █████ ██░  
     ░███  ░░░░░░░░    ░░░░░     ░░░░░░░░   ░░░  ░░░░░     ░░░░ ░░░░░ ░░░   
 ███ ░███                                                                   
░░██████                                                                    
 ░░░░░░                                                                     
```

`java(fn)` is a lightweight library (on the order of hundreds of kb) with no dependencies, which adds a few additional functional tools to the java language, most notably a `Result` class for error handling without exceptions and a set of tuples that allow better readability in streams.

## Result

The `Result` class is an algebraic type that represents the possibly unsuccessful result of an operation.  Similar to an `Either` in many languages, a `Result` wraps a value of type ERR or of type OK, but never both.  `null` is a valid value, but the Result will either be an err or an ok, and the other value will be empty (similar to Optional.empty()).

```java
Try.get(() -> new URL(theUrl))                                // Result<Exception, URL>
    .asOk().flatMap(Try.Map(URL::openConnection))             // Result<Exception, Connection>
    .asOk().flatMap(Try.Map(URLConnection::getInputStream))   // Result<Exception, InputStream>
    .asOk().map(is -> new BufferedReader(new InputStreamReader(is)))  // Result<Exception, BufferedReader>
    .asOk().map(BufferedReader::lines)                        // Result<Exception, Stream<String>>
    .asErr().peek(ex -> System.err.println("An exception occurred trying to fetch the url."))
    .asErr().peek(Exception::printStackTrace)
    .asOk().opt()                                              // Optional<Stream<String>>
    .ifPresent(lines -> System.out.println("URL contents from demoResult: " + lines.collect(Collectors.joining("\n"))));

```

See more demo code [in the source tree](./src/test/java/org/javafn/demo).

The syntax and usage of the Result class is heavily inspired by Rust's Result type [https://doc.rust-lang.org/std/result/](https://doc.rust-lang.org/std/result/) and Scala's Either type (prior to v2.13) [https://www.scala-lang.org/api/2.12.11/scala/util/Either.html](https://www.scala-lang.org/api/2.12.11/scala/util/Either.html).

`java(fn)` also includes an `Either` class with many similar features.  The distinction is about semantics.  To a new Java developer unfamiliar with Haskell, "Result" conveys more meaning then "Either", and "asErr"/"asOk" is more intuitive than "asLeft"/"asRight".  `Either` is included because sometimes you don't want to apply "err/ok" semantics.  Rather you want to convey that a piece of data may be represented in one of two types, both equally valid.

Rust has the `?` operator, for which there is no java equivilent.

```rust
fn do_thing() -> Result<OkType, ErrType> {
  let intermediate = get_intermediate()?
  Ok(intermediate.toOkType())
}
```

The following is our proposed usage pattern.
 
```java
public Result<ErrType, OkType> do_thing() {
  final Foo intermediate;
  {
    final Result<ErrType, Foo> res = get_intermediate();
    if (res.isErr) return res.asErr().into();
    intermediate = res.asOk().get();
  }
  return Result.ok(intermediate.toOkType());
```

Notice that we handle the intermediate result in a scope block so the variable `res` lives a short life.  If the result is an error, we want to return it, but the type is not correct.  We have a `Result<ErrType, Foo>` but we want a `Result<ErrType, OkType>`, and the `into()` function on the error projection allows us to safely change the signature.  Otherwise, we get the intermediate result and begin using it.

The `Try` class can be used in streams to perform an operation that would normally throw an exception (which is not allowed in streams) and returns a `Result` instead.

## Tuples

`java(fn)` offers tuples similar to the Pair, Triplet, and Quartet type in [javatuples](https://www.javatuples.org/) that supports better fluent functional usage.  These are named `Pair`, `Trio`, and `Quad`.  We did not add tuples of 5 or more because we had to draw the line somewhere.

Compare

```java
IntStream.range(0, 10).mapToObj(i -> org.javatuples.Pair.with(i, i*2))
    .filter(pair -> pair.getValue0() % 2 == 0)
    .map(pair -> pair.getValue0() + pair.getValue1())
    .forEach(System.out::println);
```

versus

```java
IntStream.range(0, 10).mapToObj(i -> Pair.of(i, i*2))
    .filter(pair -> pair.filter1(i -> i % 2 == 0)
    .map(pair -> pair.map( (l, r) -> l + r))
    .forEach(System.out::println);
```

and even better

```java
IntStream.range(0, 10).mapToObj(i -> Pair.of(i, i*2))
    .filter(Pair.Filter1(i -> i % 2 == 0)
    .map(Pair.Map( (l, r) -> l + r))
    .forEach(System.out::println);
```

Notice that you get to name the parameters you're interested in, similar to destructuring in languages that support pattern matching.  `java(fn)`'s tuples contain two versions of most functions, lowercase methods and uppercase static methods.  The lowercase methods operate directly on the tuple, while the uppercase methods return a function that accepts a pair and operates on that.  The latter are useful in streams to avoid creating a variable named `pair` just to call a function on it.  The methods must be named differently because java can't have static and member functions with the same name.

```java
stream.map(pair -> pair.map( (l, r) -> ...));
stream.map(Pair.Map( (l, r) -> ...));
```

The `Trio` and `Quad` classes are similar to `Pair` but are not documented so we don't run the risk of them getting out of sync.  Refer to the documentation for `Pair` for all three tuples.

Another feature of the tuples is the ability to create chunks and sliding windows over arrays.

## Compatibility

This library is in use in several production projects that are frozen at Java 8 until RedHat drops support in favor of a newer Java version.  Therefore, this library must always be bytecode compatible with Java 8.  It should support newer versions with no modifications (I have used it for small projects using Java 17, but have done no major testing).  

## Contact

By email `org.javafn`at`javafn.org`
