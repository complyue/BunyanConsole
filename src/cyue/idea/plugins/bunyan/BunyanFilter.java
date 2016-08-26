
package cyue.idea.plugins.bunyan;

import com.intellij.execution.filters.InputFilter;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class BunyanFilter implements InputFilter {

  /**
   * Kudos: http://stackoverflow.com/a/38073998/6394508
   */
  public static Path lookForProgramInPath(String desiredProgram) {
    ProcessBuilder pb = new ProcessBuilder(File.pathSeparatorChar == ';' ? "where" : "which", desiredProgram);
    Path foundProgram = null;
    try {
      Process proc = pb.start();
      int errCode = proc.waitFor();
      if (errCode == 0) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
          foundProgram = Paths.get(reader.readLine());
        }
      }
    } catch (IOException | InterruptedException ex) {
    }
    return foundProgram;
  }

  public static final String[] PREFERED_BUNYAN_LOCS = new String[]{
      "/usr/local/bin/bunyan"
  };

  private static Path bunyanPath = null;

  public static String findBunyan() {
    // resolve from system PATH if no valid location cached
    if (bunyanPath == null) {
      Path p = lookForProgramInPath("bunyan");
      if (p != null) {
        p = p.toAbsolutePath();
        if (Files.isExecutable(p)) {
          bunyanPath = p;
          return p.toString();
        }
      }
    }
    // it may disappear over time, so check cached path every time
    if (bunyanPath != null && Files.isExecutable(bunyanPath)) {
      return bunyanPath.toString();
    }
    // try some predefined locations, if still not resolved
    // this works around OSX for its enhanced security resetting PATH of Application executable
    for (String tryPath : PREFERED_BUNYAN_LOCS) {
      try {
        Path p = Paths.get(tryPath);
        if (Files.isExecutable(p)) {
          bunyanPath = p;
          return tryPath;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    // fall back to try some luck for possible magic that may exists
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

