package org.seleniumhq.selenium.client.logging;

import com.thoughtworks.selenium.Selenium;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements the behavior of the logging {@link Selenium} object.
 *
 * @author Kohsuke Kawaguchi
 */
public class LoggingSeleniumProxy implements InvocationHandler, LoggingSelenium {
    private Selenium base;
    private Logger logger = Logger.getLogger(LoggingSeleniumProxy.class.getName());
    private Level level = Level.INFO;
    private String id = Integer.toHexString(hashCode());

    public LoggingSeleniumProxy(Selenium base) {
        this.base = base;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<?> decl = method.getDeclaringClass();

        if (decl==Object.class || decl==LoggingSelenium.class) {
            // handle it by ourselves
            return method.invoke(this,args);
        }

        // otherwise forward it to the base object
        StringBuilder buf = new StringBuilder();
        try {
            buf.append(id).append(": ");
            buf.append(method.getName());
            for (int i=0; i<args.length; i++) {
                buf.append(i==0?'(':',');
                appendValue(args[i], buf);
            }
            buf.append(')');

            Object r = method.invoke(base, args);

            // report the return value if the method can return a value.
            if (method.getReturnType()!=void.class) {
                buf.append(" -> ");
                appendValue(r,buf);
            }
            logger.log(level,buf.toString());

            return r;
        } catch (InvocationTargetException e) {
            Throwable target = e.getTargetException();
            logger.log(level,buf.toString(),target);
            throw target;   // unwrap exception
        }
    }

    private void appendValue(Object o, StringBuilder buf) {
        if (o instanceof String)
            buf.append('"').append(o).append('"');
        else
            buf.append(o);
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogLevel(Level level) {
        this.level = level;
    }

    public Level getLogLevel() {
        return level;
    }

    public Selenium getBaseDriver() {
        return base;
    }

    public void setBaseDriver(Selenium selenium) {
        this.base = selenium;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Logging Selenium driver around "+base;
    }
}
