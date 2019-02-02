import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class MySettingsEditor extends SettingsEditor<MyRunConfiguration> {

    public MySettingsEditor() {
        super();
    }

    @Override
    protected void resetEditorFrom(@NotNull MyRunConfiguration s) {

    }

    @Override
    protected void applyEditorTo(@NotNull MyRunConfiguration s) throws ConfigurationException {

    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.BLUE);
        panel.setOpaque(true);
        return panel;
    }
}
