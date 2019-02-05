import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.openapi.project.Project;

public class MyDebugProcessEvents extends DebugProcessImpl {
    protected MyDebugProcessEvents(Project project) {
        super(project);
    }
}
