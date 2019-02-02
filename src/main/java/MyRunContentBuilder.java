import com.intellij.execution.ExecutionResult;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunContentBuilder;
import org.jetbrains.annotations.NotNull;

public class MyRunContentBuilder extends RunContentBuilder {

    public MyRunContentBuilder(@NotNull ExecutionResult executionResult, @NotNull ExecutionEnvironment environment) {
        super(executionResult, environment);
    }
}
