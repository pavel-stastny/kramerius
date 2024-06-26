package cz.incad.kramerius.auth.thirdparty.shibb.cdk;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

public class CDKSecuredChannelHTTPProxyRequest implements InvocationHandler {

    private HttpServletRequest reqest;
    private Principal principal;
    private Map<String, String> headers;
    private String ipAddress;

    CDKSecuredChannelHTTPProxyRequest(HttpServletRequest request, Principal principal, Map<String, String> headers,
            String ipAddress) {
        super();
        this.reqest = request;
        this.principal = principal;
        this.headers = headers;
        this.ipAddress = ipAddress;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (method.getName().equals("getUserPrincipal")) {
            return this.principal;
        } else if (method.getName().equals("getHeader")) {
            if (args.length == 1) {
                // this.reqest.getHeaderNames()
                return this.headers.get(args[0].toString());
            } else {
                throw new UnsupportedOperationException("unsupported");
            }
        } else if (method.getName().equals("getHeaderNames")) {
            ListEnumeration enumeration = new ListEnumeration(new ArrayList<>(headers.keySet()));
            return enumeration;
        } else if (method.getName().equals("getRemoteAddr")) {
            return this.ipAddress;
        } else {
            return method.invoke(this.reqest, args);
        }
    }

    public static HttpServletRequest newInstance(HttpServletRequest reqest, Principal principal,
            Map<String, String> headers, String ipAddress) {
        return (HttpServletRequest) java.lang.reflect.Proxy.newProxyInstance(
                CDKSecuredChannelHTTPProxyRequest.class.getClassLoader(),
                new Class[] { ServletRequest.class, HttpServletRequest.class },
                new CDKSecuredChannelHTTPProxyRequest(reqest, principal, headers, ipAddress));
    }
}
