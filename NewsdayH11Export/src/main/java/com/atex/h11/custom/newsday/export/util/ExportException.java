package com.atex.h11.custom.newsday.export.util;

public class ExportException extends Exception {
	
	// need to declare
	private static final long serialVersionUID = 1L;

	public ExportException() {
		super();
	}
	
	public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
