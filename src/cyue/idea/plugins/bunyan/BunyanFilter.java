
package cyue.idea.plugins.bunyan;

import com.intellij.execution.filters.InputFilter;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;


public class BunyanFilter implements InputFilter {

  protected final static ThreadLocal<PipeProcessing> pp = new ThreadLocal<PipeProcessing>() {
    @Override
    protected PipeProcessing initialValue() {
      return new PipeProcessing(new String[]{
        "bunyan"//, "--color"
      }, "", "[bunyan-error] ", 30);
    }
  };

  @Nullable
  @Override
  public List<Pair<String, ConsoleViewContentType>> applyFilter(String s, ConsoleViewContentType consoleViewContentType) {
    if (ConsoleViewContentType.NORMAL_OUTPUT == consoleViewContentType
      || ConsoleViewContentType.ERROR_OUTPUT == consoleViewContentType) {

      try {
        return pp.get().convert(s, consoleViewContentType);
      } catch (Exception e) {
        // reflect error info
        StringWriter st = new StringWriter();
        PrintWriter pw = new PrintWriter(st);
        e.printStackTrace(pw);
        pw.flush();
        return Arrays.asList(
          new Pair<>(s, consoleViewContentType),
          new Pair<>(st.toString(), consoleViewContentType)
        );
      }

    }

    // others, no treatment
    return null;
  }

}

