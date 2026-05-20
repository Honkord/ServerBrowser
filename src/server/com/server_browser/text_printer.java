/**
 * Server Browser version 1.0 revision 0
 * Copyright (C) 2026 Honkord
 *
 * See docs/LICENSE.md. 
 */

package com.server_browser;

import java.io.*;

public final class text_printer {
	public enum format {
		LOG,
		ERROR
	}

	public static void print(format f, String str) {
		switch (f) {
			case LOG:
				System.out.println("[O][\033[29mLOG\033[0m][ " + str + " ]");
				break;
			case ERROR:
				System.err.println("[X][\033[31mERROR\033[0m][ " + str + " ]");
				break;
		}
	}
}
