import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class MyRunConfigurationFactory extends ConfigurationFactory {


    public MyRunConfigurationFactory(@NotNull ConfigurationType type) {
        super(type);
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new MyRunConfiguration(project, this, "my_run_configuration");
    }
}
