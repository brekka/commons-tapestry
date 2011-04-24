package org.brekka.commons.tapestry;

import org.brekka.commons.lang.ErrorCode;
import org.brekka.commons.tapestry.services.CustomURLEncoderImpl;

public enum CommonsTapestryErrorCode implements ErrorCode {

    /**
     * General purpose error code denoting something could not be found.
     */
    CT404,
    
    /**
     * Thrown by {@link CustomURLEncoderImpl} when it encounters a URL with invalid characters.
     */
    CT601,
    
    /**
     * Thrown by {@link CustomURLEncoderImpl} when it encounters a URL with a '$' that is not escaped correctly, ie
     * is invalid
     */
    CT602,
    ;
    
    private static final Area AREA = Utils.createArea("CT");
    private int number = 0;

    public int getNumber() {
        return (this.number == 0 ? this.number = ErrorCode.Utils.extractErrorNumber(name(), getArea()) : this.number);
    }
    public Area getArea() { return AREA; }
}
