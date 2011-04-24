package org.brekka.commons.tapestry.base;

import org.brekka.commons.lang.ErrorCode;
import org.brekka.commons.lang.ErrorCode.Utils;

import org.apache.commons.lang.ArrayUtils;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.ioc.Messages;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.ExceptionReporter;
import org.apache.tapestry5.services.Request;

public abstract class AbstractExceptionReport extends CommonPageSupport implements ExceptionReporter {
    
    @Inject
    private ComponentResources resources;
    
    
    @Inject
    @Symbol(SymbolConstants.PRODUCTION_MODE)
    private boolean productionMode;
    
    
    @SuppressWarnings("unused")
    @Inject
    @Property(write = false)
    private Request request;
    
    @SuppressWarnings("unused")
    @Inject
    @Symbol(SymbolConstants.TAPESTRY_VERSION)
    @Property(write = false)
    private String tapestryVersion;

    @SuppressWarnings("unused")
    @Inject
    @Symbol(SymbolConstants.APPLICATION_VERSION)
    @Property(write = false)
    private String applicationVersion;
    
    
    @SuppressWarnings("unused")
    @Property
    private Throwable exception;
    
    @Property
    private ErrorCode errorCode;
    
    @SuppressWarnings("unused")
    @Property
    private String errorSpecificMessage;
    
    public void onActivate() {
        onActivate(null);
    }
    
    public void onActivate(String errorCode) {
        if (errorCode != null) {
            this.errorCode = Utils.parseCode(errorCode);
        }
        
        if (this.errorCode != null) {
            // Attempt to resolve the error specific message from the messages file
            Messages messages = resources.getMessages();
            String key = "Error." + this.errorCode.toString() + ".message";
            if (messages.contains(key)) {
                this.errorSpecificMessage = messages.get(key);
            }
        }
    }
    
    public Object[] onPassivate() {
        Object[] retVal = ArrayUtils.EMPTY_OBJECT_ARRAY;
        if (errorCode != null) {
            retVal = new Object[] { errorCode.toString() };
        }
        return retVal;
    }
    
    public void reportException(Throwable exception) {
        this.exception = exception;
    }
    
    /**
     * Initialize the error code (if any). If an error code is present, this will be used to attempt to locate a
     * problem specified error message.
     * 
     * @param errorCode the error code.
     */
    public void initErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Determines whether details of the error should be shown, returning true if they should.
     * The default logic is based on the production status of the system and whether the current
     * visitor is an administrator as determined by {@link #isAdministrator()}.
     * 
     * If the system is not in production mode, a detailed error report will be shown.
     * @return
     */
    public boolean isShowDetails() {
        boolean show = (!productionMode || isAdministrator());
        return show;
    }
    
    /**
     * Determines if the current visitor is a systems administrator or someone who should be allowed
     * to view the exception details.
     * 
     * Should be overridden by the implementor to provide in-production viewing of exception details
     * for privileged visitors.
     * 
     * @return true if the visitor is an administrator.
     */
    protected boolean isAdministrator() {
        return false;
    }
}
