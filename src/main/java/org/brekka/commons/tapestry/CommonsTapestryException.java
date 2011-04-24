package org.brekka.commons.tapestry;

import org.brekka.commons.lang.BaseException;
import org.brekka.commons.lang.ErrorCode;

public class CommonsTapestryException extends BaseException {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = 8191048604901260486L;

	public CommonsTapestryException(ErrorCode errorCode, String message,
			Object... messageArgs) {
		super(errorCode, message, messageArgs);
	}

	public CommonsTapestryException(ErrorCode errorCode, Throwable cause,
			String message, Object... messageArgs) {
		super(errorCode, cause, message, messageArgs);
	}
	
}
