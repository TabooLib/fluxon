package org.tabooproject.fluxon.jsr223;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;

/**
 * 将 Writer 包装为 OutputStream
 * 用于将 ScriptContext 的 Writer 转换为 Environment 可用的 PrintStream
 */
class WriterOutputStream extends OutputStream {

    private final Writer writer;
    private final CharsetDecoder decoder;
    private final ByteBuffer inputBuffer;
    private final CharBuffer outputBuffer;

    /**
     * 使用 UTF-8 编码创建
     */
    WriterOutputStream(Writer writer) {
        this(writer, StandardCharsets.UTF_8);
    }

    /**
     * 使用指定编码创建
     */
    WriterOutputStream(Writer writer, Charset charset) {
        this.writer = writer;
        this.decoder = charset.newDecoder();
        // 缓冲区大小：足够处理大多数场景
        this.inputBuffer = ByteBuffer.allocate(128);
        this.outputBuffer = CharBuffer.allocate(128);
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int remaining = len;
        int offset = off;
        while (remaining > 0) {
            int chunk = Math.min(remaining, inputBuffer.remaining());
            inputBuffer.put(b, offset, chunk);
            offset += chunk;
            remaining -= chunk;
            // 解码并写入
            processInput(false);
        }
    }

    /**
     * 处理输入缓冲区中的字节
     */
    private void processInput(boolean endOfInput) throws IOException {
        inputBuffer.flip();
        while (true) {
            CoderResult result = decoder.decode(inputBuffer, outputBuffer, endOfInput);
            flushOutput();
            if (result.isUnderflow()) {
                break;
            }
            if (result.isOverflow()) {
                continue;
            }
            if (result.isError()) {
                result.throwException();
            }
        }
        inputBuffer.compact();
    }

    /**
     * 将输出缓冲区的内容写入 Writer
     */
    private void flushOutput() throws IOException {
        outputBuffer.flip();
        if (outputBuffer.hasRemaining()) {
            writer.write(outputBuffer.toString());
        }
        outputBuffer.clear();
    }

    @Override
    public void flush() throws IOException {
        processInput(false);
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        processInput(true);
        // 最终刷新解码器
        decoder.flush(outputBuffer);
        flushOutput();
        writer.close();
    }
}
