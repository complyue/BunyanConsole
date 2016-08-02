package cyue.idea.plugins.bunyan;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;


public class StreamDecoder {

  protected final String linePrefix;
  protected InputStream in;
  protected CharsetDecoder decoder;
  protected byte[] bytes;
  protected int bp;
  protected CharBuffer chars;
  protected StringBuilder str;

  public StreamDecoder(InputStream in, String linePrefix) {
    this.linePrefix = linePrefix;
    this.in = in;
    this.decoder = Charset.defaultCharset().newDecoder();
    this.bytes = new byte[2000];
    this.bp = 0;
    this.chars = CharBuffer.allocate(2000); // chars should be large enough for all possible bytes
    this.str = new StringBuilder(5000);
  }

  public String readOut() throws IOException, InterruptedException {
    // TODO implement linePrefix semantics

    str.setLength(0); // clear str buffer

    while (true) { // loop regarding buffer capacity

      while (in.available() > 0) { // loop regarding input data and buffer capacity
        int b = in.read();
        if (b < 0) break;
        bytes[bp++] = (byte) b;
        if (bp >= bytes.length) {
          // buf full
          break;
        }
      }
      if (bp <= 0) {
        // non read
        break;
      }

      // decode bytes to chars
      ByteBuffer bb = ByteBuffer.wrap(bytes, 0, bp);
      chars.clear();
      decoder.decode(bb, chars, false);
      chars.flip();
      str.append(chars.toString());
      // need to keep remaining bytes as incomplete byte sequence may occur
      if (bb.hasRemaining()) {
        bb.compact();
        bp = bb.limit();
      } else {
        bp = 0;
      }

      // wait io cycle
      if (in.available() <= 0) {
        Thread.sleep(1L);
      }

    }

    if (str.length() <= 0) {
      // nothing read
      return null;
    }

    return str.toString();
  }

  public static void main(String... args) throws Exception {
    String[] cmd = new String[]{"bunyan"};
    if (args.length > 0) {
      cmd = args;
    }

    Process p = Runtime.getRuntime().exec(cmd);
    Writer sink = new OutputStreamWriter(p.getOutputStream());
    StreamDecoder out = new StreamDecoder(p.getInputStream(), "");
    StreamDecoder err = new StreamDecoder(p.getErrorStream(), "");

    BufferedReader sin = new BufferedReader(new InputStreamReader(System.in));
    for (String line = sin.readLine(); line != null; line = sin.readLine()) {

      sink.write(line);
      sink.write("\n");
      sink.flush();
      Thread.sleep(1L);

      for (String seg = err.readOut(); seg != null; seg = err.readOut()) {
        System.err.print("\n*-err-* <<<");
        System.err.print(seg);
        System.err.println(">>>*-err-*\n");
      }
      for (String seg = out.readOut(); seg != null; seg = out.readOut()) {
        System.err.print("\n*-out-* <<<");
        System.err.print(seg);
        System.err.println(">>>*-out-*\n");
      }

    }
  }

}
