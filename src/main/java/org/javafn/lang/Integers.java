/*
 * Copyright (c) 1994, 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.javafn.lang;

import org.javafn.result.IntResult;
import org.javafn.result.Result;

/**
 * The {@code Integers} class provides non-throwing versions
 * of the methods in {@code Integer}.
 * Implementation and documentation was taken from the existing codebase
 * and modified to change method signatures and behaviors as necessary.
 */
public class Integers {

	static IntResult<String> forInputString(String s) {
		return IntResult.err("For input string: \"" + s + "\"");
	}

	/**
	 * Parses the string argument as a signed integer in the radix
	 * specified by the second argument. The characters in the string
	 * must all be digits of the specified radix (as determined by
	 * whether {@link java.lang.Character#digit(char, int)} returns a
	 * nonnegative value), except that the first character may be an
	 * ASCII minus sign {@code '-'} ({@code '\u005Cu002D'}) to
	 * indicate a negative value or an ASCII plus sign {@code '+'}
	 * ({@code '\u005Cu002B'}) to indicate a positive value. The
	 * resulting integer value is returned.
	 *
	 * <p>An IntResult.Err is returned
	 * if any of the following situations occurs:
	 * <ul>
	 * <li>The first argument is {@code null} or is a string of
	 * length zero.
	 *
	 * <li>The radix is either smaller than
	 * {@link java.lang.Character#MIN_RADIX} or
	 * larger than {@link java.lang.Character#MAX_RADIX}.
	 *
	 * <li>Any character of the string is not a digit of the specified
	 * radix, except that the first character may be a minus sign
	 * {@code '-'} ({@code '\u005Cu002D'}) or plus sign
	 * {@code '+'} ({@code '\u005Cu002B'}) provided that the
	 * string is longer than length 1.
	 *
	 * <li>The value represented by the string is not a value of type
	 * {@code int}.
	 * </ul>
	 *
	 * <p>Examples:
	 * <blockquote><pre>
	 * parseInt("0", 10) returns ok(0)
	 * parseInt("473", 10) returns ok(473)
	 * parseInt("+42", 10) returns ok(42)
	 * parseInt("-0", 10) returns ok(0)
	 * parseInt("-FF", 16) returns ok(-255)
	 * parseInt("1100110", 2) returns ok(102)
	 * parseInt("2147483647", 10) returns ok(2147483647)
	 * parseInt("-2147483648", 10) returns ok(-2147483648)
	 * parseInt("2147483648", 10) returns err
	 * parseInt("99", 8) returns err
	 * parseInt("Kona", 10) returns err
	 * parseInt("Kona", 27) returns ok(411787)
	 * </pre></blockquote>
	 *
	 * @param      s   the {@code String} containing the integer
	 *                  representation to be parsed
	 * @param      radix   the radix to be used while parsing {@code s}.
	 * @return     an IntResult.Ok wrapping the integer represented by the string argument in the
	 *             specified radix or an IntResult.Err with the error message that would be
	 *             used in the NumberFormatException constructor.
	 */
	public static IntResult<String> parseInt(String s, int radix)
	{
		/*
		 * WARNING: This method may be invoked early during VM initialization
		 * before IntegerCache is initialized. Care must be taken to not use
		 * the valueOf method.
		 */

		if (s == null) {
			return IntResult.err("null");
		}

		if (radix < Character.MIN_RADIX) {
			return IntResult.err("radix " + radix +
					" less than Character.MIN_RADIX");
		}

		if (radix > Character.MAX_RADIX) {
			return IntResult.err("radix " + radix +
					" greater than Character.MAX_RADIX");
		}

		int result = 0;
		boolean negative = false;
		int i = 0, len = s.length();
		int limit = -java.lang.Integer.MAX_VALUE;
		int multmin;
		int digit;

		if (len > 0) {
			char firstChar = s.charAt(0);
			if (firstChar < '0') { // Possible leading "+" or "-"
				if (firstChar == '-') {
					negative = true;
					limit = java.lang.Integer.MIN_VALUE;
				} else if (firstChar != '+')
					return forInputString(s);

				if (len == 1) // Cannot have lone "+" or "-"
					return forInputString(s);
				i++;
			}
			multmin = limit / radix;
			while (i < len) {
				// Accumulating negatively avoids surprises near MAX_VALUE
				digit = Character.digit(s.charAt(i++),radix);
				if (digit < 0) {
					return forInputString(s);
				}
				if (result < multmin) {
					return forInputString(s);
				}
				result *= radix;
				if (result < limit + digit) {
					return forInputString(s);
				}
				result -= digit;
			}
		} else {
			return forInputString(s);
		}
		return IntResult.ok(negative ? result : -result);
	}


	/**
	 * Parses the string argument as a signed decimal integer. The
	 * characters in the string must all be decimal digits, except
	 * that the first character may be an ASCII minus sign {@code '-'}
	 * ({@code '\u005Cu002D'}) to indicate a negative value or an
	 * ASCII plus sign {@code '+'} ({@code '\u005Cu002B'}) to
	 * indicate a positive value. The resulting integer value is
	 * returned, exactly as if the argument and the radix 10 were
	 * given as arguments to the {@link #parseInt(java.lang.String,
	 * int)} method.
	 *
	 * @param s    a {@code String} containing the {@code int}
	 *             representation to be parsed
	 * @return     an IntResult.ok wrapping the integer value represented
	 *             by the argument in decimal or an IntResult.err
	 *             with the error message that would be
	 *             used in the NumberFormatException constructor.
	 */
	public static IntResult<String> parseInt(String s) {
		return parseInt(s,10);
	}

	/**
	 * Parses the string argument as an unsigned integer in the radix
	 * specified by the second argument.  An unsigned integer maps the
	 * values usually associated with negative numbers to positive
	 * numbers larger than {@code MAX_VALUE}.
	 *
	 * The characters in the string must all be digits of the
	 * specified radix (as determined by whether {@link
	 * java.lang.Character#digit(char, int)} returns a nonnegative
	 * value), except that the first character may be an ASCII plus
	 * sign {@code '+'} ({@code '\u005Cu002B'}). The resulting
	 * integer value is returned.
	 *
	 * <p>An IntResult.Err is returned if any of the following situations occurs:
	 * <ul>
	 * <li>The first argument is {@code null} or is a string of
	 * length zero.
	 *
	 * <li>The radix is either smaller than
	 * {@link java.lang.Character#MIN_RADIX} or
	 * larger than {@link java.lang.Character#MAX_RADIX}.
	 *
	 * <li>Any character of the string is not a digit of the specified
	 * radix, except that the first character may be a plus sign
	 * {@code '+'} ({@code '\u005Cu002B'}) provided that the
	 * string is longer than length 1.
	 *
	 * <li>The value represented by the string is larger than the
	 * largest unsigned {@code int}, 2<sup>32</sup>-1.
	 *
	 * </ul>
	 *
	 *
	 * @param      s   the {@code String} containing the unsigned integer
	 *                  representation to be parsed
	 * @param      radix   the radix to be used while parsing {@code s}.
	 * @return     an IntResult.Ok wrapping the integer represented by the string argument in the
	 *             specified radix or an IntResult.Err with the error message that would be
	 *             used in the NumberFormatException constructor.
	 * @since 1.8
	 */
	public static IntResult<String> parseUnsignedInt(String s, int radix) {
		if (s == null)  {
			return IntResult.err("null");
		}

		int len = s.length();
		if (len > 0) {
			char firstChar = s.charAt(0);
			if (firstChar == '-') {
				return IntResult.err(String.format("Illegal leading minus sign " +
						"on unsigned string %s.", s));
			} else {
				if (len <= 5 || // Integer.MAX_VALUE in Character.MAX_RADIX is 6 digits
						(radix == 10 && len <= 9) ) { // Integer.MAX_VALUE in base 10 is 10 digits
					return parseInt(s, radix);
				} else {
					long ell = Long.parseLong(s, radix);
					if ((ell & 0xffff_ffff_0000_0000L) == 0) {
						return IntResult.ok((int) ell);
					} else {
						return IntResult.err(String.format("String value %s exceeds " +
								"range of unsigned int.", s));
					}
				}
			}
		} else {
			return forInputString(s);
		}
	}

	/**
	 * Parses the string argument as an unsigned decimal integer. The
	 * characters in the string must all be decimal digits, except
	 * that the first character may be an an ASCII plus sign {@code
	 * '+'} ({@code '\u005Cu002B'}). The resulting integer value
	 * is returned, exactly as if the argument and the radix 10 were
	 * given as arguments to the {@link
	 * #parseUnsignedInt(java.lang.String, int)} method.
	 *
	 * @param s   a {@code String} containing the unsigned {@code int}
	 *            representation to be parsed
	 * @return     an IntResult.Ok wrapping the integer represented by the string argument in
	 *             decimal or an IntResult.Err with the error message that would be
	 *             used in the NumberFormatException constructor.
	 * @since 1.8
	 */
	public static IntResult<String>  parseUnsignedInt(String s) {
		return parseUnsignedInt(s, 10);
	}

	/**
	 * Returns an {@code Integer} object holding the value
	 * extracted from the specified {@code String} when parsed
	 * with the radix given by the second argument. The first argument
	 * is interpreted as representing a signed integer in the radix
	 * specified by the second argument, exactly as if the arguments
	 * were given to the {@link #parseInt(java.lang.String, int)}
	 * method. The result is a Result.Ok wrapping an {@code Integer} object that
	 * represents the integer value specified by the string
	 * or a Result.Err wrapping the error message sent to the
	 * NumberFormatException's constructor.
	 *
	 * @param      s   the string to be parsed.
	 * @param      radix the radix to be used in interpreting {@code s}
	 * @return     a Result.Ok wrapping the {@code Integer} object holding the value
	 *             represented by the string argument in the specified
	 *             radix or a Result.Err if the Integer version of this method
	 *             would have thrown an exception.
	 */
	public static Result<String, java.lang.Integer> valueOf(String s, int radix) {
		return parseInt(s,radix).asOk().mapToObj(java.lang.Integer::valueOf);
	}

	/**
	 * Returns an {@code Integer} object holding the
	 * value of the specified {@code String}. The argument is
	 * interpreted as representing a signed decimal integer, exactly
	 * as if the argument were given to the {@link
	 * #parseInt(java.lang.String)} method. The result is an
	 * {@code Integer} object that represents the integer value
	 * specified by the string.
	 *
	 * @param      s   the string to be parsed.
	 * @return     a Result.Ok wrapping the {@code Integer} object holding the value
	 *             represented by the string argument or a Result.Err if the Integer
	 *             version of this method would have thrown an exception.
	 */
	public static Result<String, java.lang.Integer> valueOf(String s) {
		return parseInt(s, 10).asOk().mapToObj(java.lang.Integer::valueOf);
	}
	/**
	 * Decodes a {@code String} into an {@code Integer}.
	 * Accepts decimal, hexadecimal, and octal numbers given
	 * by the following grammar:
	 *
	 * <blockquote>
	 * <dl>
	 * <dt><i>DecodableString:</i>
	 * <dd><i>Sign<sub>opt</sub> DecimalNumeral</i>
	 * <dd><i>Sign<sub>opt</sub></i> {@code 0x} <i>HexDigits</i>
	 * <dd><i>Sign<sub>opt</sub></i> {@code 0X} <i>HexDigits</i>
	 * <dd><i>Sign<sub>opt</sub></i> {@code #} <i>HexDigits</i>
	 * <dd><i>Sign<sub>opt</sub></i> {@code 0} <i>OctalDigits</i>
	 *
	 * <dt><i>Sign:</i>
	 * <dd>{@code -}
	 * <dd>{@code +}
	 * </dl>
	 * </blockquote>
	 *
	 * <i>DecimalNumeral</i>, <i>HexDigits</i>, and <i>OctalDigits</i>
	 * are as defined in section 3.10.1 of
	 * <cite>The Java&trade; Language Specification</cite>,
	 * except that underscores are not accepted between digits.
	 *
	 * <p>The sequence of characters following an optional
	 * sign and/or radix specifier ("{@code 0x}", "{@code 0X}",
	 * "{@code #}", or leading zero) is parsed as by the {@code
	 * Integer.parseInt} method with the indicated radix (10, 16, or
	 * 8).  This sequence of characters must represent a positive
	 * value or a {@link NumberFormatException} will be thrown.  The
	 * result is negated if first character of the specified {@code
	 * String} is the minus sign.  No whitespace characters are
	 * permitted in the {@code String}.
	 *
	 * @param     nm the {@code String} to decode.
	 * @return    an {@code Integer} object holding the {@code int}
	 *             value represented by {@code nm}
	 * @exception NumberFormatException  if the {@code String} does not
	 *            contain a parsable integer.
	 * @see java.lang.Integer#parseInt(java.lang.String, int)
	 */
	public static Result<String, java.lang.Integer> decode(String nm) {
		int radix = 10;
		int index = 0;
		boolean negative = false;
//		Integer result;

		if (nm.length() == 0)
			return Result.err("Zero length string");
		char firstChar = nm.charAt(0);
		// Handle sign, if present
		if (firstChar == '-') {
			negative = true;
			index++;
		} else if (firstChar == '+')
			index++;

		// Handle radix specifier, if present
		if (nm.startsWith("0x", index) || nm.startsWith("0X", index)) {
			index += 2;
			radix = 16;
		} else if (nm.startsWith("#", index)) {
			index++;
			radix = 16;
		} else if (nm.startsWith("0", index) && nm.length() > 1 + index) {
			index++;
			radix = 8;
		}

		if (nm.startsWith("-", index) || nm.startsWith("+", index))
			return Result.err("Sign character in wrong position");

		final boolean isNegativeFinal = negative;
		final int indexFinal = index;
		return valueOf(nm.substring(index), radix)
				.asOk().map(result -> isNegativeFinal ? java.lang.Integer.valueOf(-result.intValue()) : result)
				.asErr().flatMap(e -> valueOf(
						isNegativeFinal ? ("-" + nm.substring(indexFinal)) : nm.substring(indexFinal)
				));
	}

}
