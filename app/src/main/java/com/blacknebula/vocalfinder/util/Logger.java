package com.blacknebula.vocalfinder.util;

import android.util.Log;

import com.google.common.base.Optional;

public class Logger {

    final static private String NAME = "vocalFinder";

    public enum Type {
        VOCAL_FINDER;

        @Override
        public String toString() {
            return String.format("[%s]", super.toString());    //To change body of overridden methods use File | Settings | File Templates.
        }

    }

    private enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    public static void debug(Type logType, String message, Object... args) {
        log(Level.DEBUG, logType, Optional.<Throwable>absent(), message, args);
    }

    public static void warn(Type logType, String message, Object... args) {
        log(Level.WARN, logType, Optional.<Throwable>absent(), message, args);
    }

    public static void info(Type logType, String message, Object... args) {
        log(Level.INFO, logType, Optional.<Throwable>absent(), message, args);
    }

    public static void error(Type logType, Throwable ex) {
        log(Level.ERROR, logType, Optional.of(ex), "");
    }

    public static void error(Type logType, String message, Object... args) {
        log(Level.ERROR, logType, Optional.<Throwable>absent(), message, args);
    }

    public static void error(Type logType, Throwable ex, String message, Object... args) {
        log(Level.ERROR, logType, Optional.of(ex), message, args);
    }

    private static void log(Level level, Type logType, Optional<Throwable> ex, String message, Object... args) {
        try {
            final String formattedMessage = formatMessage(String.format("%s ", logType.toString()) + message, args);
            switch (level) {
                case DEBUG:
                    if (Log.isLoggable(NAME, Log.DEBUG))
                        Log.d(NAME, formattedMessage);
                    break;
                case INFO:
                    if (Log.isLoggable(NAME, Log.INFO))
                        Log.i(NAME, formattedMessage);
                    break;
                case WARN:
                    if (Log.isLoggable(NAME, Log.WARN))
                        Log.w(NAME, formattedMessage);
                    break;
                case ERROR:
                    if (ex.isPresent())
                        Log.e(NAME, formattedMessage, ex.get());
                    else
                        Log.e(NAME, formattedMessage);
                    break;
            }
        } catch (Exception e) {
            Log.e(NAME, "LOG exception while logging data", e);
        }
    }

    private static String formatMessage(String message, Object... args) {
        try {
            return String.format(message, args);
        } catch (Exception ex) {
            Log.e(NAME, "LOG error on formatting the message=" + message, ex);
            return "";
        }
    }
}