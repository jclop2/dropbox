package com.fathzer.soft.jclop.dropbox.swing;

import java.util.Locale;

import net.astesana.ajlib.utilities.LocalizationData;

public class MessagePack {
	public static final String DEFAULT_BUNDLE_NAME = "com.fathzer.soft.jclop.dropbox.swing.messages"; //$NON-NLS-1$
	public static LocalizationData INSTANCE = new LocalizationData(DEFAULT_BUNDLE_NAME);
	
	public static String getString(String key, Locale locale) {
		return INSTANCE.getString(key, locale);
	}
}
