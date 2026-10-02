package com.wontlost.ckeditor.handler;

import com.wontlost.ckeditor.event.EditorErrorEvent.EditorError;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Logger;

/**
 * {@link ErrorHandler#logging(Logger)} 的实现：按严重级别写日志，不终止传播。
 *
 * <p>独立成类而不用 lambda，是为了兼顾两点（issue #137）：</p>
 * <ul>
 *   <li><b>可序列化</b>：{@link Logger} 不可序列化，序列化时只写 logger 名称，
 *       反序列化后按名称重新取回；</li>
 *   <li><b>运行时持有强引用</b>：JUL 的 LogManager 只弱引用 logger，若只保存名称、用时再
 *       {@code Logger.getLogger(name)}，调用方没有其它强引用时 logger 可能被回收，
 *       取回的是丢失了 handler / level 配置的新实例。</li>
 * </ul>
 * <p>匿名 logger（名称为 null）无法按名取回，此时实例不可序列化。</p>
 */
final class LoggingErrorHandler implements ErrorHandler {

    private final String loggerName;
    private transient Logger logger;

    LoggingErrorHandler(Logger logger) {
        this.logger = logger;
        this.loggerName = logger.getName();
    }

    @Override
    public boolean handleError(EditorError error) {
        switch (error.getSeverity()) {
            case WARNING:
                logger.warning(() -> String.format("[%s] %s", error.getCode(), error.getMessage()));
                break;
            case ERROR:
                logger.severe(() -> String.format("[%s] %s", error.getCode(), error.getMessage()));
                break;
            case FATAL:
                logger.severe(() -> String.format("FATAL [%s] %s\n%s",
                    error.getCode(), error.getMessage(), error.getStackTrace()));
                break;
        }
        return false; // Continue propagation
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (loggerName == null) {
            throw new java.io.NotSerializableException("anonymous Logger cannot be serialized");
        }
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.logger = Logger.getLogger(loggerName);
    }
}
