package ru.yandex.market.graphouse.cacher;

import org.apache.http.util.ByteArrayBuffer;
import ru.yandex.market.graphouse.Metric;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"/>
 * @date 14/02/15
 */
public class MetricInputStream extends InputStream {

    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private List<Metric> metrics;

    private ByteArrayBuffer buffer = new ByteArrayBuffer(1000);
    private int rowNum = 0;
    private int rowPosition = 0;


    public MetricInputStream(List<Metric> metrics) {
        this.metrics = metrics;
    }

    @Override
    public int read() throws IOException {
        if (!ensureBuffer()) {
            return -1;
        }
        return buffer.byteAt(rowPosition++);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (!ensureBuffer()) {
            return -1;
        }
        int read = Math.min(len, buffer.length() - rowPosition);
        System.arraycopy(buffer.buffer(), rowPosition, b, off, read);
        rowPosition += read;
        return read;
    }

    /**
     * @return false если данные закончились
     */
    private boolean ensureBuffer() {
        if (rowPosition >= buffer.length()) {
            readRow();
        }
        return rowPosition < buffer.length();
    }

    private void readRow() {
        if (rowNum >= metrics.size()) {
            return;
        }

        rowPosition = 0;
        buffer.clear();

        Metric metric = metrics.get(rowNum);

        metric.getMetricDescription().writeName(buffer);
        buffer.append('\t');
        appendBytes(Double.toString(metric.getValue()).getBytes());
        buffer.append('\t');
        append(getTimestampSeconds(metric.getTime()));
        buffer.append('\t');
        appendBytes(dateFormat.format(metric.getTime()).getBytes());
        buffer.append('\t');
        append(metric.getUpdated());
        buffer.append('\n');
        rowNum++;
    }

    private void append(int data) {
        appendBytes(Integer.toString(data).getBytes());
    }

    private void appendBytes(byte[] bytes) {
        buffer.append(bytes, 0, bytes.length);
    }

    private static int getTimestampSeconds(Date date) {
        return (int) (TimeUnit.MILLISECONDS.toSeconds(date.getTime()));
    }
}