package cyue.idea.plugins.bunyan;

public class LineCut {

  protected final StringBuilder sb = new StringBuilder(1000);

  public String feed(String in) {
    String out = null;

    int lePos = in.lastIndexOf('\n');
    if (lePos == in.length() - 1) {
      // exact line, very okay
      if (sb.length() > 0) {
        out = sb.append(in).toString();
        sb.setLength(0);
      } else {
        out = in;
      }
    } else if (lePos < 0) {
      // no line end for now
      sb.append(in);
      out = null;
    } else {
      // new incomplete line found
      sb.append(sb.substring(0, lePos + 1));
      out = sb.toString();
      sb.setLength(0);
      sb.append(sb.substring(lePos + 1));
    }

    return out;
  }

}
