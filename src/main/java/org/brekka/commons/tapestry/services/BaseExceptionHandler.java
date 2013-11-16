package org.brekka.commons.tapestry.services;

import static java.lang.String.format;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.internal.services.PageResponseRenderer;
import org.apache.tapestry5.internal.services.RequestPageCache;
import org.apache.tapestry5.internal.structure.Page;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.ioc.internal.OperationException;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.runtime.ComponentEventException;
import org.apache.tapestry5.services.ExceptionReporter;
import org.apache.tapestry5.services.RequestExceptionHandler;
import org.apache.tapestry5.services.RequestGlobals;
import org.apache.tapestry5.services.Response;
import org.brekka.commons.lang.ErrorCode;
import org.brekka.commons.lang.ErrorCoded;
import org.brekka.commons.tapestry.CommonsTapestryErrorCode;
import org.brekka.commons.tapestry.CommonsTapestryException;
import org.brekka.commons.tapestry.base.AbstractExceptionReport;
import org.brekka.commons.tapestry.error.ErrorCodeReceiver;
import org.slf4j.Logger;

/**
 * <p>
 * Replacement for the standard Tapestry exception handler. Retains the functionality of the original but provides a number
 * of extension points that allow extending classes to tweak what happens when an exception is encountered.
 * </p>
 * <p>
 * To use this class you will need to register it in your module class. For example:
 * </p>
 * <pre>
 * public class AppModule {
 *     public static void bind(ServiceBinder binder) {
 *         binder.bind(RequestExceptionHandler.class, BaseExceptionHandler.class).withId("AppExceptionHandler");
 *     }
 *     
 *     public static void contributeAlias(@Local RequestExceptionHandler handler,
 *                                        Configuration<AliasContribution<?>> configuration) {
 *         configuration.add(AliasContribution.create(RequestExceptionHandler.class, handler));
 *     }
 * }
 * </pre>
 * <p>
 * It is however recommended that you create a subclass as there will invariably be the need to use one or more
 * of the extension points.
 * </p>
 * 
 * @author Andrew Taylor
 */
public class BaseExceptionHandler implements RequestExceptionHandler {
    /**
     * Set of error codes that denote a bad request.
     */
    private static final Set<CommonsTapestryErrorCode> BAD_URL_CODES = Collections.unmodifiableSet(
        EnumSet.of(CommonsTapestryErrorCode.CT404, CommonsTapestryErrorCode.CT601, CommonsTapestryErrorCode.CT602)
    );

    private final RequestPageCache pageCache;

    private final PageResponseRenderer renderer;

    private final Logger logger;

    private final String pageName;

    private final RequestGlobals requestGlobals;
    
    private final Response response;

    public BaseExceptionHandler(RequestPageCache pageCache, PageResponseRenderer renderer, Logger logger,
                @Inject @Symbol(SymbolConstants.EXCEPTION_REPORT_PAGE) String pageName, RequestGlobals requestGlobals, Response response) {
        this.pageCache = pageCache;
        this.renderer = renderer;
        this.logger = logger;
        this.pageName = pageName;
        this.response = response;
        this.requestGlobals = requestGlobals;
    }

    /**
     * Handles all exceptions that Tapestry encounters and cannot handle itself. Will normally show an error page.
     * 
     * This method is deliberately marked final as there is no point overriding it. If you don't like what it does, just
     * implement a {@link RequestExceptionHandler} from scratch.
     */
    public final void handleRequestException(Throwable exception) throws IOException {
        String requestPath = requestGlobals.getRequest().getPath();
        
        // Determines whether a custom response is required for this exception/path.
        ErrorResponseType errorResponseType = determineResponse(requestPath, exception);
        int responseCode = 0;
        Page page = null;
        switch (errorResponseType) {
            case UNSUPPORTED_METHOD:
                responseCode = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
                // No page
                break;
            case ACCESS_DENIED:
                responseCode = HttpServletResponse.SC_FORBIDDEN;
                page = prepareAccessDeniedPage(exception, pageCache);
                break;
            case INVALID_URL:
                // For now treat this like a page not found.
            case NOT_FOUND:
                responseCode = HttpServletResponse.SC_NOT_FOUND;
                page = prepareNotFoundPage(exception, pageCache);
                break;
            case SERVICE_UNAVAILABLE:
                responseCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
                page = prepareErrorPage(exception, pageCache);
                break;
            case SYSTEM_ERROR:
                responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                logSystemError(exception);
                response.setHeader("X-Tapestry-ErrorMessage", InternalUtils.toMessage(exception));
                page = prepareErrorPage(exception, pageCache);
                break;
        }
        
        // TAP5-233: Make sure the client knows that an error occurred.
        response.setStatus(responseCode);
        if (page != null) {
            renderer.renderPageResponse(page);
        }
    }
    
    /**
     * Return the error page, performing some initialization of the component itself.
     * @param exception
     * @param pageCache
     * @return
     */
    protected Page prepareErrorPage(Throwable exception, RequestPageCache pageCache) {
        Page page = pageCache.get(pageName);
        // Let the page set up for the new exception (standard Tapestry functionality).
        ExceptionReporter rootComponent = (ExceptionReporter) page.getRootComponent();
        rootComponent.reportException(exception);
        
        // Attempt to identify and set the error code on  the page if available.
        if (rootComponent instanceof ErrorCodeReceiver) {
            ErrorCodeReceiver exceptionReport = (ErrorCodeReceiver) rootComponent;
            ErrorCode ec = identifyErrorCode(exception);
            exceptionReport.initErrorCode(ec);
        }
        return page;
    }
    
    /**
     * Return a specific page for when a resource is not found. By default just returns the error page which we know
     * exists.
     * @param exception
     * @param pageCache
     * @return
     */
    protected Page prepareNotFoundPage(Throwable exception, RequestPageCache pageCache) {
        return prepareErrorPage(exception, pageCache);
    }
    
    /**
     * Return a specific page for when access is denied. By default just returns the error page which we know
     * exists.
     * @param exception
     * @param pageCache
     * @return
     */
    protected Page prepareAccessDeniedPage(Throwable exception, RequestPageCache pageCache) {
        return prepareErrorPage(exception, pageCache);
    }
    
    /**
     * Determines the type of response that should be generated for the given exception. The default implementation
     * just assumes a system error.
     * 
     * @param requestPath
     * @param exception
     * @param response
     * @return
     */
    protected ErrorResponseType determineResponse(String requestPath, Throwable exception) {
        if (exception instanceof OperationException) {
            exception = exception.getCause();
        }
        ErrorResponseType errorResponseType = ErrorResponseType.SYSTEM_ERROR;
        CommonsTapestryException commonsTapestryException = findCommonsTapestryException(exception);
        if (commonsTapestryException != null) {
            ErrorCode errorCode = commonsTapestryException.getErrorCode();
        	if (errorCode == CommonsTapestryErrorCode.CT404) {
        	    errorResponseType = ErrorResponseType.NOT_FOUND;
        	} else if (BAD_URL_CODES.contains(errorCode)) {
                /*
                 * Bad encoding of request URL based on old links picked up by
                 * the GoogleBot. Just return a 404.
                 */
                errorResponseType = ErrorResponseType.INVALID_URL;
            }
        } else if (exception instanceof ComponentEventException) {
            Throwable cause = exception.getCause();
            if (cause != null
                    && cause.getClass() == RuntimeException.class
                    && cause.getMessage() != null
                    && cause.getMessage().contains("Forms require that the request method be POST")) {
                /*
                 * This rule is somewhat tied to the implementation internals of Tapestry, but thats what you get when
                 * a system uses plain old RuntimeException...
                 */
                errorResponseType = ErrorResponseType.UNSUPPORTED_METHOD;
            }
        } else if (exception instanceof NullPointerException 
                && requestPath.contains("/assets/virtual/")) {
            /*
             * The Tapestry class 'VirtualAssetStreamerImpl' responsible for generating the
             * combined JavaScript asset file doesn't handle old links properly, throwing a NPE when the
             * asset cannot be found in the local cache.
             * 
             * Until that gets fixed, this special case is in place to avoid the error getting logged,
             * returning 404 instead, which is more correct.
             */
            errorResponseType = ErrorResponseType.NOT_FOUND;
        }
        return errorResponseType;
    }

    protected void logSystemError(Throwable exception) {
        String referer = requestGlobals.getRequest().getHeader("Referer");
        String userAgent = requestGlobals.getRequest().getHeader("User-Agent");
        String remoteAddr = requestGlobals.getHTTPServletRequest().getRemoteAddr();
        String visitorIdentity = getVisitorIdentity();
        HttpServletRequest request = requestGlobals.getHTTPServletRequest();
        String query = request.getQueryString();
        StringBuffer url = request.getRequestURL();
        if (query != null) {
            url.append("?");
            url.append(query);
        }
        
        // Log out the error
        logError(logger, exception, visitorIdentity, url.toString(), referer, userAgent, remoteAddr);
    }
    
    /**
     * Writes the error to the logging subsystem.
     * @param logger
     * @param exception
     * @param visitorIdentity
     * @param url
     * @param referer
     * @param userAgent
     * @param remoteAddr
     */
    protected void logError(Logger logger, Throwable exception, String visitorIdentity, String url, String referer,
            String userAgent, String remoteAddr) {
        String message = format("Request by '%s' %n" +
                "  URL:        %s%n" +
                "  Referer:    %s%n" +
                "  User Agent: %s%n" +
                "  Client IP:  %s", visitorIdentity, url, referer, userAgent, remoteAddr);
        logger.error(message, exception);
    }
    
    /**
     * Identify any error code from the exception. The default looks for exceptions that implement ErrorCoded but
     * this method could equally be enhanced to detect other types of exception and return an error code for that.
     * @param exception the exception to inspect.
     * @return the error code if one can be determined, otherwise just null.
     */
    protected ErrorCode identifyErrorCode(Throwable exception) {
        ErrorCode ec = null;
        if (exception instanceof ErrorCoded) {
            ErrorCoded errorCoded = (ErrorCoded) exception;
            ec = errorCoded.getErrorCode();
        }
        return ec;
    }
    
    /**
     * Default implementation attempts to identify the user via the {@link HttpServletRequest#getUserPrincipal()} method.
     * Can be overridden to support alternative security implementations.
     * @return the name of the visitor.
     */
    protected String getVisitorIdentity() {
        String username = "guest";
        Principal userPrincipal = requestGlobals.getHTTPServletRequest().getUserPrincipal();
        if (userPrincipal != null) {
            username = userPrincipal.getName();
        }
        return username;
    }
    
    
    /**
     * @param exception
     * @return
     */
    private static CommonsTapestryException findCommonsTapestryException(Throwable exception) {
        while (exception != null) {
            if (exception instanceof CommonsTapestryException) {
                return (CommonsTapestryException) exception;
            }
            exception = exception.getCause();
        }
        return null;
    }
    
    /**
     * Determines how the error should be reported to the user. Certain exceptions thrown may indicate that access denied or
     * that a resource could not be found. 
     */
    protected enum ErrorResponseType {
        /**
         * Return a system error page with response code 500.
         */
        SYSTEM_ERROR,
        /**
         * Service is currently unavailable.
         */
        SERVICE_UNAVAILABLE,
        /**
         * Returns a page not found with reponse code 404.
         */
        NOT_FOUND,
        /**
         * Returns an access denied page with response code 403.
         */
        ACCESS_DENIED,
        /**
         * The URL was bad.
         */
        INVALID_URL,
        /**
         * Method not supported, will responde with code 405.
         */
        UNSUPPORTED_METHOD,
    }

}
