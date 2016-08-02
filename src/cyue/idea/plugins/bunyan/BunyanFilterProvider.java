package cyue.idea.plugins.bunyan;

import com.intellij.execution.filters.ConsoleInputFilterProvider;
import com.intellij.execution.filters.InputFilter;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class BunyanFilterProvider implements ConsoleInputFilterProvider {

  @NotNull
  @Override
  public InputFilter[] getDefaultFilters(@NotNull Project project) {
    return new InputFilter[]{new BunyanFilter()};
  }

}
