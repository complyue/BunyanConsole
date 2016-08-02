package cyue.idea.plugins.bunyan;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PipeProcessing {

  protected final String[] cmd;
  protected final String linePrefix, errLinePrefix;
  protected final int maxRetry;

  public PipeProcessing(String[] cmd, String linePrefix, String errLinePrefix, int maxRetry) {
    this.cmd = cmd;
    this.linePrefix = linePrefix;
    this.errLinePrefix = errLinePrefix;
    this.maxRetry = maxRetry;
  }

  protected Process p;
  protected Writer out;
  protected StreamDecoder in, err;

  public List<Pair<String, ConsoleViewContentType>> convert(
    String seg, ConsoleViewContentType consoleViewContentType
  ) throws IOException, InterruptedException {

    if (null != this.p && !this.p.isAlive()) {
      new Throwable("piping process died").printStackTrace();
      this.p = null;
    }
    if (null == this.p) {
      this.p = Runtime.getRuntime().exec(this.cmd);
      this.out = new OutputStreamWriter(p.getOutputStream());
      this.in = new StreamDecoder(p.getInputStream(), linePrefix);
      this.err = new StreamDecoder(p.getErrorStream(), errLinePrefix);
    }

    ArrayList<Pair<String, ConsoleViewContentType>> result = new ArrayList<>(3);

    // make sure no previous back pressure to block writes below
    pump(this.err, result, consoleViewContentType);
    pump(this.in, result, consoleViewContentType);

    // write out data
    this.out.write(seg);
    this.out.flush();

    int tryCnt = 0;
    while (true) {

      // wait io
      Thread.sleep(tryCnt);
      // read result
      pump(this.err, result, consoleViewContentType);
      pump(this.in, result, consoleViewContentType);

      if (result.size() > 0) {
        // got sth, assume all have been converted
        return result;
      }

      if (++tryCnt >= this.maxRetry) {
        // no luck with the processor
        new Throwable("Processing failed for [" + seg + "]").printStackTrace();
        this.p.destroy(); // kill it
        this.p.waitFor();
        return null;
      }

    }
  }

  protected static void pump(
    StreamDecoder in, List<Pair<String, ConsoleViewContentType>> out,
    ConsoleViewContentType consoleViewContentType
  ) throws IOException, InterruptedException {
    for (String seg = in.readOut(); seg != null; seg = in.readOut()) {
      out.add(new Pair<>(seg, consoleViewContentType));
    }
  }

}
