import com.intellij.execution.*;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import org.jetbrains.annotations.NotNull;

public class MyDebugAction extends AnAction {

    public MyDebugAction() {
        super("","start FootPrint", AllIcons.General.IjLogo);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        RunnerAndConfigurationSettings config = RunManagerImpl.getInstanceEx(e.getProject())
                .getSelectedConfiguration();
        System.out.println(config.getName());

//        System.out.println(e.getData(LangDataKeys.PSI_FILE));

//        System.out.println("------");
//        int i = 0;
//        for (Executor executor : ExecutorRegistry.getInstance().getRegisteredExecutors()) {
//            System.out.println(i + " : " + executor.getId());
//            i++;
//        }
//        System.out.println("------");

        this.executeConfiguration(config, MyExecutor.getMyExecutorInstance());
    }

    private static void executeConfiguration(@NotNull RunnerAndConfigurationSettings configuration,
                                       @NotNull Executor executor) {
        ExecutionEnvironmentBuilder builder;
        try {
            builder = ExecutionEnvironmentBuilder.create(executor, configuration);
        }
        catch (ExecutionException e) {
//            LOG.error(e);
            return;
        }

        ProgramRunnerUtil.executeConfiguration(builder
                .contentToReuse(null)
                .dataContext(null)
                .activeTarget()
                .build(), true, true);
    }
}
