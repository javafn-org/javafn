package org.javafn.result;

import org.javafn.utils.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collector;

public record ResultBatch<ERR, OK>(List<ERR> errors, List<OK> oks) {

    public ResultBatch {
        errors = errors == null ? List.of() : List.copyOf(errors);
        oks = oks == null ? List.of() : List.copyOf(oks);
    }

    public static <E, O> ResultBatch<E, O> empty() { return new ResultBatch<>(List.of(), List.of()); }

    @SafeVarargs
    public static <E, O> ResultBatch<E, O> of(final Result<E, O>... results) {
        return Arrays.stream(results).collect(collector());
    }

    public static <E, O> Collector<Result<E, O>, ?, ResultBatch<E, O>> collector() {
        return Collector.<Result<E, O>, Acc<E, O>, ResultBatch<E, O>>of(
                Acc::new, Acc::add, Acc::merge, Acc::finish);
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasOks() { return !oks.isEmpty(); }
    public boolean onlyErrors() { return oks.isEmpty(); }
    public boolean onlyOks() { return errors.isEmpty(); }
    public boolean hasBoth() { return !errors.isEmpty() && ! oks.isEmpty(); }

    public int size() { return errors.size() + oks.size(); }

    public <E, O> ResultBatch<E, O> map(final Function<ERR, E> errFn, final Function<OK, O> okFn) {
        return new ResultBatch<>(
                errors.stream().map(errFn).toList(),
                oks.stream().map(okFn).toList());
    }

    public <Z> ResultBatch<Z, OK> mapErrs(final Function<ERR, Z> fn) {
        return new ResultBatch<>(errors.stream().map(fn).toList(), oks);
    }

    public <Z> ResultBatch<ERR, Z> mapOks(final Function<OK, Z> fn) {
        return new ResultBatch<>(errors, oks.stream().map(fn).toList());
    }

    public Result<List<ERR>, List<OK>> fold() {
        return errors.isEmpty()
                ? Result.ok(oks)
                : Result.err(errors);
    }

    public <Z> Z reduce(final BiFunction<List<ERR>, List<OK>, Z> fn) {
        return fn.apply(errors, oks);
    }

    public void use(final BiConsumer<List<ERR>, List<OK>> fn) {
        fn.accept(errors, oks);
    }

    public ResultBatch<ERR, OK> append(final ResultBatch<ERR, OK> that) {
        return new ResultBatch<>(
                Data.append(errors, that.errors),
                Data.append(oks, that.oks));
    }

    private record Acc<E, O>(ArrayList<E> errors, ArrayList<O> oks) {
        Acc() { this(new ArrayList<>(), new ArrayList<>()); }
        void add(final Result<E, O> result) { result.use(errors::add, oks::add); }
        Acc<E, O> merge(final Acc<E, O> other) {
            errors.addAll(other.errors);
            oks.addAll(other.oks);
            return this;
        }
        ResultBatch<E, O> finish() { return new ResultBatch<>(errors, oks); }
    }
}
