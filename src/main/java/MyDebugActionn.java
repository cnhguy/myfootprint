import com.intellij.execution.*;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class MyDebugActionn extends AnAction {

    public MyDebugActionn() {
        super("","start FootPrint", AllIcons.General.IjLogo);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        RunnerAndConfigurationSettings config = RunManagerImpl.getInstanceEx(e.getProject())
                .getSelectedConfiguration();
        System.out.println(config.getName());

        executeConfiguration(config, MyExecutor.getDebugExecutorInstance());
    }

    private static void executeConfiguration(@NotNull RunnerAndConfigurationSettings configuration,
                                       @NotNull Executor executor) {
        ExecutionEnvironmentBuilder builder;
        try {
            builder = ExecutionEnvironmentBuilder.create(executor, configuration);
            builder.runner(new MyProgramRunner());
        }
        catch (ExecutionException e) {
            return;
        }

        ProgramRunnerUtil.executeConfiguration(builder
                .contentToReuse(null)
                .dataContext(null)
                .activeTarget()
                .build(), true, true);
    }
}
