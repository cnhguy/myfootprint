import com.intellij.execution.executors.DefaultDebugExecutor;
import org.jetbrains.annotations.NonNls;

public class MyExecutor extends DefaultDebugExecutor{
    @NonNls
    public static final String EXECUTOR_ID = "MY_EXECUTOR";
}
