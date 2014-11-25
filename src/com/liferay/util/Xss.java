/**
 * Copyright (c) 2000-2005 Liferay, LLC. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.util;

import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.EncodingException;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

/**
 * <a href="Xss.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @author Clarence Shen
 * @version $Revision: 1.3 $
 */
public class Xss {

    public static final String XSS_REGEXP_PATTERN = GetterUtil.getString( SystemProperties.get( Xss.class.getName() + ".regexp.pattern" ) );
    public static final Boolean ESAPI_VALIDATION = GetterUtil.getBoolean( SystemProperties.get( Xss.class.getName() + ".ESAPI.validation" ), true );

    /**
     * Removes from the given text possible XSS hacks
     *
     * @param text
     * @return
     * @deprecated Is recommended to use instead methods like URIHasXSS or ParamsHaveXSS and handle properly a possible XSS attack
     */
    public static String strip ( String text ) {
        if ( text == null ) {
            return null;
        }
        return RegEX.replace( text, "", XSS_REGEXP_PATTERN );
    }

    /**
     * Checks into the request query string for possible XSS hacks and return true if any possible XSS fragment is found
     *
     * @param request
     * @return true if any possible XSS fragment is found
     */
    @SuppressWarnings ("unchecked")
    public static boolean ParamsHaveXSS ( HttpServletRequest request ) {
        return ParamsHaveXSS( request.getQueryString() );
    }

    /**
     * Checks into a given query string for possible XSS hacks and return true if any possible XSS fragment is found
     *
     * @param queryString
     * @return true if any possible XSS fragment is found
     */
    @SuppressWarnings ("unchecked")
    public static boolean ParamsHaveXSS ( String queryString ) {

        if ( ESAPI_VALIDATION ) {
            if ( queryString != null && !queryString.isEmpty() ) {

                //Canonicalizes input before validation to prevent bypassing filters with encoded attacks.
                queryString = ESAPI.encoder().canonicalize( queryString, false );

                //Validate the query string
                return !ESAPI.validator().isValidInput( "URLContext", queryString, "HTTPQueryString", queryString.length(), true );
            }

            return false;

        } else {
            queryString = UtilMethods.decodeURL( queryString );
            return RegEX.contains( queryString, XSS_REGEXP_PATTERN );
        }
    }

    /**
     * Checks in the given uri for possible XSS hacks and return true if any possible XSS fragment is found
     *
     * @param uri
     * @return true if any possible XSS fragment is found
     */
    public static boolean URIHasXSS ( String uri ) {

        if ( uri == null ) {
            return false;
        }

        if ( ESAPI_VALIDATION ) {

            /*
             Verify if the given URI have parameters, if it have them apply the security check to both, the
             URI and the parameters.
             */
            String finalURI = uri;
            String queryString = null;
            if ( uri.contains( "?" ) ) {
                finalURI = uri.substring( 0, uri.indexOf( "?" ) );
                queryString = uri.substring( uri.indexOf( "?" ) + 1, uri.length() );
            }

            //Validate the URI
            boolean isValid = ESAPI.validator().isValidInput( "URLContext", finalURI, "HTTPURI", uri.length(), false );
            //Validate the query string if present
            if ( isValid && queryString != null ) {
                return ParamsHaveXSS( queryString );
            }

            return !isValid;
        } else {
            return RegEX.contains( uri, XSS_REGEXP_PATTERN );
        }
    }

    /**
     * Checks in the given url for possible XSS hacks and return true if any possible XSS fragment is found
     *
     * @param url
     * @return true if any possible XSS fragment is found
     * @deprecated Use instead individually URIHasXSS and ParamsHaveXSS
     */
    public static boolean URLHasXSS ( String url ) {

        if ( url == null ) {
            return false;
        }
        if ( ESAPI_VALIDATION ) {

            boolean isValid;

            //Verify if the given value is a query string or a URI
            if ( !url.startsWith( "/" ) || url.contains( "&" ) || url.contains( "=" ) ) {
                if ( url.contains( "=" ) ) {
                    isValid = ESAPI.validator().isValidInput( "URLContext", url, "HTTPQueryString", url.length(), false );
                } else {
                    isValid = ESAPI.validator().isValidInput( "URLContext", url, "HTTPParameterValue", url.length(), false );
                }
            } else {
                isValid = ESAPI.validator().isValidInput( "URLContext", url, "HTTPURI", url.length(), false );
            }

            return !isValid;
        } else {
            return RegEX.contains( url, XSS_REGEXP_PATTERN );
        }
    }

    public static String encodeForURL ( String value ) throws EncodingException {
        return value != null ? ESAPI.encoder().encodeForURL( value ) : "";
    }

    public static String escapeHTMLAttrib ( String value ) {
        return value != null ? ESAPI.encoder().encodeForHTMLAttribute( value ) : "";
    }


    public static String unEscapeHTMLAttrib ( String value ) {
        return value != null ? ESAPI.encoder().decodeForHTML( value ) : "";
    }

}