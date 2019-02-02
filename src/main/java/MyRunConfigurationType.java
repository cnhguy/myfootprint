import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MyRunConfigurationType extends ConfigurationTypeBase {

    public MyRunConfigurationType() {
        super("my_run_configuration_id",
                "my_run_configuration_displayname",
                "my_run_configuration_description",
                AllIcons.General.Balloon);
        addFactory(new MyRunConfigurationFactory(this));
    }





}
