package com.qian.wesmile.proxy;

import com.alibaba.fastjson.JSON;
import com.qian.wesmile.annotation.JsonBody;
import com.qian.wesmile.annotation.ParamName;
import com.qian.wesmile.annotation.RelativePath;
import com.qian.wesmile.model.result.APIResult;
import com.qian.wesmile.model.result.AccessToken;
import com.qian.wesmile.request.AbstractHttpRequester;
import com.qian.wesmile.request.DefalutHttpRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class APIInvocationHandler implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger(APIInvocationHandler.class);

    private static String GET_ACCESS_TOKEN_URL_PATTERN = "%s/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";
    protected String domain;
    protected String appid;
    protected String appSecret;
    private static AccessToken accessToken;

    private AbstractHttpRequester httpRequester;

    public APIInvocationHandler(String domain, String appid, String appSecret, AbstractHttpRequester httpRequester) {
        this.domain = domain;
        this.appid = appid;
        this.appSecret = appSecret;
        this.httpRequester = httpRequester;
    }

    public APIInvocationHandler(String domain, String appid, String appSecret) {
        this.domain = domain;
        this.appid = appid;
        this.appSecret = appSecret;
        this.httpRequester = new DefalutHttpRequester();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        checkAnnotation(method);
        String result = doAPIRequest(method, args);
        Class<?> returnType = method.getReturnType();
        if (void.class == returnType) {
            return null;
        }
        try {
            //TODO 这里无法正常解析出数据到底是返回一个属性是空的对象还是直接招聘运行时异常有等商榷
            APIResult apiResult = JSON.parseObject(result, APIResult.class);
            if (apiResult.success()) {
                return JSON.parseObject(result, returnType);
            } else {
                throw new RuntimeException(result + " can't parse to {}" + returnType);
            }
        } catch (Exception e) {
            throw new RuntimeException(result + " can't parse to {}" + returnType);
        }
    }

    private void checkAnnotation(Method method) {
        if (!method.isAnnotationPresent(RelativePath.class)) {
            throw new RuntimeException(method.toString() + " must with annotation ＠RelativePath ");
        }
    }

    //    protected String doRequest(String url,){
    //
    //    }

    /**
     * 把通过url地址传递的参数拼接到url后面,自动处理accessToken的值
     *
     * @param annotation
     * @param parameters
     * @param paramsValue
     * @return
     */
    public String getUrlWithAddressParams(RelativePath annotation, Parameter[] parameters,
                                          Object[] paramsValue) {
        StringBuffer sb = new StringBuffer();
        sb.append(domain);
        sb.append(annotation.value());
        sb.append("?access_token=");
        sb.append(getAccessToken());
        sb.append("&");
        for (int i = 0; i < parameters.length; i++) {
            Object value = paramsValue[i];
            Parameter p = parameters[i];
            //参数非空且不是请求体中传递的参数
            if (value != null && !p.isAnnotationPresent(JsonBody.class)) {
                ParamName pAnnotation = p.getAnnotation(ParamName.class);
                sb.append(pAnnotation.value());
                sb.append("=");
                sb.append(paramsValue[i]);
            }
            sb.append("&");
        }
        return sb.toString();

    }

    private String getAccessToken() {
        if (accessToken == null || accessToken.isExpire()) {
            return getAccessTokenByRequest().getAccessToken();
        }

        return accessToken.getAccessToken();
        //                  return "27_R-y1HA9fcQAtdEBjFD6TxPhi_3ujSqjhgRdmuFvrdE16hBspqNWP6PWro4kLFDMI4x1HsKBwLHI7K9V" +
        //          "-jiFazAKGCqE-f5WlUX1yPuODlXmcLxkIPcAKIu--IO3yFGuw0EhR1fj-a72UeLzQTKMeACAXOQ";

    }

    private AccessToken getAccessTokenByRequest() {
        String url = String.format(GET_ACCESS_TOKEN_URL_PATTERN, domain, appid, appSecret);
        String result = httpRequester.doRequest(url, null);
        AccessToken accessToken = JSON.parseObject(result, AccessToken.class);
        if (accessToken.getAccessToken() == null || accessToken.getAccessToken().length() == 0) {
            throw new RuntimeException("can't parse access token from " + result);
        }
        APIInvocationHandler.accessToken = accessToken;
        return accessToken;
    }

    private String doAPIRequest(Method method, Object[] args) {

        RelativePath annotation = method.getAnnotation(RelativePath.class);
        Parameter[] parameters = method.getParameters();

        String url = getUrlWithAddressParams(annotation, parameters, args);

        String s = "";
        if (args != null && args.length > 0) {
            Class<? extends Parameter> aClass = parameters[0].getClass();
            if (!aClass.isPrimitive()) {
                s = JSON.toJSONString(args[0]);
            }
        }

        String result = httpRequester.doRequest(url, s);
        return result;
    }
}
