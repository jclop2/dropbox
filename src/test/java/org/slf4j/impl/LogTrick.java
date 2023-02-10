package org.slf4j.impl;

import org.slf4j.Logger;

public class LogTrick {
	public static int setLogLevel(Logger log, int level) {
		final SimpleLogger logger = (SimpleLogger) log;
		final int previous = logger.currentLogLevel;
		logger.currentLogLevel = level;
		return previous;
	}
}
