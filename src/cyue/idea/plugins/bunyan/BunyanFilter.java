
package cyue.idea.plugins.bunyan;

import com.intellij.execution.filters.InputFilter;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class BunyanFilter implements InputFilter {

  public static String findBunyan() {
    for (String tryPath : new String[]{"/usr/local/bin/bunyan"}) {
      try {
        if (Files.isExecutable(Paths.get(tryPath))) {
          return tryPath;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return "bunyan";
  }

  protected final static ThreadLocal<PipeProcessing> pp = new ThreadLocal<PipeProcessing>() {
    @Override
    protected PipeProcessing initialValue() {
      return new PipeProcessing(new String[]{
          findBunyan() //, "--color"
      }, "", "[bunyan-error] ", 30);
    }
  };

  protected LineCut cutOut = new LineCut(), cutErr = new LineCut();

  @Nullable
  @Override
  public List<Pair<String, ConsoleViewContentType>> applyFilter(String s, ConsoleViewContentType consoleViewContentType) {

    LineCut lc;

    if (ConsoleViewContentType.NORMAL_OUTPUT == consoleViewContentType) {
      lc = cutOut;
    } else if (ConsoleViewContentType.ERROR_OUTPUT == consoleViewContentType) {
      lc = cutErr;
    } else {
      // others, no treatment
      return null;
    }

    try {
      String l = lc.feed(s);
      if (l == null) {
        // no complete line present, cork output
        return Collections.emptyList();
      }
      return pp.get().convert(l, consoleViewContentType);
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

}

