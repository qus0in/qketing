package kr.noco.qticket.e2e;

import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

public final class PlaywrightExtension implements BeforeEachCallback, AfterEachCallback,
        InvocationInterceptor, ParameterResolver {

    private static final Namespace NAMESPACE = Namespace.create(PlaywrightExtension.class);
    private static final String SESSION = "browserSession";

    @Override
    public void beforeEach(ExtensionContext context) {
        store(context).put(SESSION, BrowserSession.launch());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        BrowserSession session = session(context);
        if (session != null) session.close();
        store(context).remove(SESSION);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> method, ExtensionContext context) throws Throwable {
        try {
            invocation.proceed();
        } catch (Throwable failure) {
            captureFailure(context, failure);
            throw failure;
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameter, ExtensionContext context) {
        return parameter.getParameter().getType() == BrowserSession.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter, ExtensionContext context) {
        return session(context);
    }

    private void captureFailure(ExtensionContext context, Throwable failure) {
        try {
            session(context).screenshot(context.getRequiredTestMethod().getName());
        } catch (RuntimeException screenshotFailure) {
            failure.addSuppressed(screenshotFailure);
        }
    }

    private BrowserSession session(ExtensionContext context) {
        return store(context).get(SESSION, BrowserSession.class);
    }

    private Store store(ExtensionContext context) {
        return context.getStore(NAMESPACE);
    }
}
