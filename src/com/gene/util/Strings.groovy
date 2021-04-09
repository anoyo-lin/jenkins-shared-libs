package com.gene.util

public class Strings {
    /**
    * trim the script and remove spaces from the beginning of each line.
    */
    public static String trimAndShift(String s) {
        s = s.trim().replaceAll(/\n\s+/, "\n")
        return removeFirstLineIfEmpty(s)
    }

    /**
    * prepare CMD commands each taking own line, as in 
    * c:\DIR\git.exe ...
    */
    public static String escapeForCmd(String s) {
        // Protect percent sign % against early consumption by CMD as in curl --write-out "%{http_code}".
        //
        // Use quoted strings for uniform escape rules (applied once against Groovy interpolation and another against the RegEx
        // interpreter). This seems easier than slashy Groovy strings that do not seem to allow visible representation of newlines.
        return s.replaceAll(/%/, "%%")

    }

    /**
    * Prepare possibly multi-line fold of a single CMD command, with the 
    * argument in single quotes, as in 
    *           c:\cygwin64\bin\bash.exe -c '...'
    **/
    public static String escapeForCmdFoldInSingleQuotes(String s) {
        // protect pipe characters | against CMD to keep them within the
        // context of quotes sh.exe -c '...' or powershell -Command ... 
        //
        // Protect line breaks. Honour trailing balckslash as a line continuations.
        return escapeForCmd(s).replaceAll(/\|/, "^|").replaceAll(/\n\s*/, "; ^\r\n ").replaceAll(/\\; \^\r\n/. "^\r\n")
    }

    public static String removeFirstLineIfEmpty(String s) {
        if (s.startWith("\n")) {
            return s.substring(1)
        }
        return s
    }

    public static String removeFirstLine(String s) {
        return s.replaceFirst(/^[^\n]*\n/, "")
    }

    public static String removeCarriageReturns(String s) {
        return s.split("\n").collect{ it.replace(/\r$/, "") }.join("\n")
    }
}