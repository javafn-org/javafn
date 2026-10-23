
s/\.asErr\(\)\.map\(/.mapErr(/g
s/\.asOk\(\)\.map\(/.map(/g

s/\.asErr\(\)\.peek\(/.ifErr(/g
s/\.asOk\(\)\.peek\(/.ifOk(/g

s/\.asErr\(\)\.opt\(\)/.optErr\(\)/g
s/\.asOk\(\)\.opt\(\)/.opt\(\)/g

s/\.asErr\(\)\.filter\(/.filterErr(/g
s/\.asOk\(\)\.filter\(/.filter(/g

s/\.asOk\(\)\.flatMap\(/.flatMap(/g
s/\.asErr\(\)\.flatMap\(/.flatRecover(/g

s/\.asErr\(\)\.filterMap\(/.recoverIf(/g
s/\.asOk\(\)\.filterMap\(/.failIf(/g

s/\.asErr\(\)\.get\b/.expectErr/g
s/\.asOk\(\)\.get\b/.expectOk/g

# Static helper classes were renamed.
s/\bErr\.Map\b/Errs.map/g
s/\bErr\.Filter\b/Errs.filter/g
s/\bErr\.Peek\b/Errs.ifErr/g
s/\bErr\.FilterMap\b/Errs.recoverIf/g
s/\bErr\.Swap\b/Errs.swap/g

s/\bOk\.Map\b/Oks.map/g
s/\bOk\.Filter\b/Oks.filter/g
s/\bOk\.Peek\b/Oks.ifOk/g
s/\bOk\.FlatMap\b/Oks.flatMap/g
s/\bOk\.FilterMap\b/Oks.failIf/g
s/\bOk\.Swap\b/Oks.swap/g

# Top-level function helpers renamed or changed to method references.
s/\bResult\.Peek\b/Result::consume/g
s/\bResult\.MapResult\b/Result::map/g
s/\bResult\.Reduce\b/Result::reduce/g

s/\bResult\.Err\.\b/Errs./g
s/\bResult\.Ok\.\b/Oks./g
s/\bErr\./Errs./g
s/\bOk\./Oks./g

# Result constructors and specialized types
s/\bResult\.errList\b/Result.err/g
s/Result<[^,]+,\s*([^>]+)>/Result<\1>/g
s/IntResult<[^>]+>/IntResult/g
s/LongResult<[^>]+>/LongResult/g
s/DoubleResult<[^>]+>/DoubleResult/g

# Try utility renaming
s/\bTry\.get\b/Try.toGet/g
s/\bTry\.run\b/Try.to/g
s/\bTry\.Accept\b/Try.toUse/g
s/\bTry\.Map\b/Try.toMap/g

# Pattern matching transition (Caution: structural change)
s/\.isErr\(\)/ instanceof Err<?,?> e/g
s/\.isOk\(\)/ instanceof Ok<?> o/g
s/\.isErr\b/ instanceof Err<?,?> e/g
s/\.isOk\b/ instanceof Ok<?> o/g
