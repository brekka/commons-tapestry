package org.brekka.commons.tapestry.services;

import static org.brekka.commons.tapestry.CommonsTapestryErrorCode.CT601;
import static org.brekka.commons.tapestry.CommonsTapestryErrorCode.CT602;

import java.util.BitSet;

import org.apache.commons.lang.StringUtils;
import org.apache.tapestry5.internal.services.URLEncoderImpl;
import org.apache.tapestry5.services.URLEncoder;
import org.brekka.commons.tapestry.CommonsTapestryException;

/**
 * Near-identical copy of {@link URLEncoderImpl} that simply allows the safe list to be specified during construction.
 * 
 * Also converts spaces to plus (+) and back. Any normal plusses will be encoded in the ususal way.
 * 
 * @author Andrew Taylor
 */
public class CustomURLEncoderImpl implements URLEncoder {
    private static final String ENCODED_NULL = "$N";
    private static final String ENCODED_BLANK = "$B";

    /**
     * Bit set indicating which character are safe to pass through (when encoding or decoding) as-is. All other
     * characters are encoded as a kind of unicode escape.
     */
    private final BitSet safe = new BitSet(512);

    
    public CustomURLEncoderImpl(String... charLists) {
        for (String charList : charLists) {
            markSafe(charList);
        }
    }
    
    /**
     * Default behaviour exactly the same as {@link URLEncoderImpl}
     */
    public CustomURLEncoderImpl() {
        this (
            "abcdefghijklmnopqrstuvwxyz",
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            "01234567890-_.:" 
        );
    }

    private void markSafe(String s) {
        for (int i = 0; i < s.length(); i++) {
            if ("+".equals(s)) {
                // Plus is reserved for space substitution
                break;
            }
            safe.set(s.codePointAt(i));
        }
    }

    public String encode(String input) {
        if (input == null)
            return ENCODED_NULL;

        if (input.equals(""))
            return ENCODED_BLANK;

        boolean dirty = false;

        int length = input.length();

        StringBuilder output = new StringBuilder(length * 2);

        for (int i = 0; i < length; i++) {
            char ch = input.charAt(i);

            if (ch == ' ') {
                // Convert space to plus
                output.append('+');
                dirty = true;
                continue;
            }
            if (ch == '$') {
                output.append("$$");
                dirty = true;
                continue;
            }

            int chAsInt = ch;

            if (safe.get(chAsInt)) {
                output.append(ch);
                continue;
            }

            output.append(String.format("$%04x", chAsInt));
            dirty = true;
        }

        return dirty ? output.toString() : input;
    }

    public String decode(String input) {
    	assert input != null;

        if (input.equals(ENCODED_NULL))
            return null;

        if (input.equals(ENCODED_BLANK))
            return "";

        boolean dirty = false;

        int length = input.length();

        StringBuilder output = new StringBuilder(length * 2);

        for (int i = 0; i < length; i++) {
            char ch = input.charAt(i);
            
            if (ch == '$') {
                dirty = true;

                if (i + 1 < length && input.charAt(i + 1) == '$') {
                    output.append('$');
                    i++;

                    dirty = true;
                    continue;
                }

                if (i + 4 < length) {
                    String hex = input.substring(i + 1, i + 5);

                    try {
                        int unicode = Integer.parseInt(hex, 16);

                        output.append((char) unicode);
                        i += 4;
                        dirty = true;
                        continue;
                    } catch (NumberFormatException ex) {
                        // Ignore.
                    }
                }

                throw new CommonsTapestryException(CT602,
                        "Input string '%s' is not valid; the '$' character at position %d should be followed by another '$' or a four digit hex number (a unicode value).",
                                        input, i + 1);
            }
            if (ch == '+') {
                // Convert plus to space
                ch = ' ';
                dirty = true;
            } else {
                int codePoint = input.codePointAt(i);
                if (!safe.get(codePoint)) {
                    throw new CommonsTapestryException(CT601,
                            "Input string '%s' is not valid; the character '%s' at position %d is not valid.", 
                            input, StringUtils.substring(input, i, i + 1), i + 1);
                }
            }

            output.append(ch);
        }

        return dirty ? output.toString() : input;
    }
}
