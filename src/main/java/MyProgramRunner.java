import com.intellij.debugger.DebugEnvironment;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.DefaultDebugEnvironment;
import com.intellij.debugger.engine.*;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationListener;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.intellij.debugger.engine.managerThread.DebuggerCommand;
import com.intellij.debugger.impl.*;
import com.intellij.debugger.jdi.DecompiledLocalVariable;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.debugger.ui.breakpoints.BreakpointManager;
import com.intellij.debugger.ui.breakpoints.FieldBreakpoint;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.WatchedRootsProvider;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.*;
import com.intellij.psi.search.FileNameIndexServiceImpl;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.xdebugger.*;
import com.intellij.xdebugger.impl.WatchesManagerState;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.jetbrains.sa.jdi.VirtualMachineImpl;
import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.request.*;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

//public class MyProgramRunner extends DefaultJavaProgramRunner{
public class MyProgramRunner extends GenericDebuggerRunner {

    private static final boolean ourInitializationOk;
    private static Class<?> ourSlotInfoClass;
    private static Constructor<?> slotInfoConstructor;
    private static Class<?> ourGetValuesClass;
    private static Method ourEnqueueMethod;
    private static Method ourWaitForReplyMethod;

    static {
        boolean success = false;
        try {
            ourSlotInfoClass = Class.forName("com.sun.tools.jdi.JDWP$StackFrame$GetValues$SlotInfo");
            slotInfoConstructor = ourSlotInfoClass.getDeclaredConstructor(int.class, byte.class);
            slotInfoConstructor.setAccessible(true);

            ourGetValuesClass = Class.forName("com.sun.tools.jdi.JDWP$StackFrame$GetValues");
            ourEnqueueMethod = findMethod(ourGetValuesClass, "enqueueCommand");
            ourEnqueueMethod.setAccessible(true);
            ourWaitForReplyMethod = findMethod(ourGetValuesClass, "waitForReply");
            ourWaitForReplyMethod.setAccessible(true);

            success = true;
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
        ourInitializationOk = success;
    }

    private static Method findMethod(Class aClass, String methodName) throws NoSuchMethodException {
        for (Method method : aClass.getDeclaredMethods()) {
            if (methodName.equals(method.getName())) {
                return method;
            }
        }
        throw new NoSuchMethodException(aClass.getName() + "." + methodName);
    }

    private static Object createSlotInfoArray(Collection<DecompiledLocalVariable> vars) throws Exception {
        final Object arrayInstance = Array.newInstance(ourSlotInfoClass, vars.size());

        int idx = 0;
        for (DecompiledLocalVariable var : vars) {
            final Object info = slotInfoConstructor.newInstance(var.getSlot(), (byte)var.getSignature().charAt(0));
            Array.set(arrayInstance, idx++, info);
        }

        return arrayInstance;
    }

    public MyProgramRunner() {
        super();
//        System.out.println("my program runner constructor");
    }

    public static final String RUNNER_ID = "MyDebugRunner";

    @Override
    @NotNull
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        boolean r =  executorId.equals(MyExecutor.EXECUTOR_ID) && profile instanceof ModuleRunProfile
                && !(profile instanceof RunConfigurationWithSuppressedDefaultDebugAction);
//        System.out.println(executorId + ":" + r);
        return r;
    }

    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment env) throws ExecutionException {
        RunContentDescriptor descriptor = super.doExecute(state, env);
//        Content content = descriptor.getAttachedContent();
//        System.out.println(content.getTabName());
//        XDebugSessionImpl k;

        return descriptor;
    }

    @Nullable
    @Override
    protected RunContentDescriptor attachVirtualMachine(RunProfileState state,
                                                        @NotNull ExecutionEnvironment env,
                                                        RemoteConnection connection,
                                                        long pollTimeout) throws ExecutionException {
//        System.out.println("my attach virtual machine");
        DebugEnvironment environment = new DefaultDebugEnvironment(env, state, connection, pollTimeout);
        final DebuggerSession debuggerSession = DebuggerManagerEx.getInstanceEx(env.getProject()).attachVirtualMachine(environment);




        BreakpointManager breakpointManager = DebuggerManagerEx.getInstanceEx(env.getProject())
                .getBreakpointManager();
        ProjectFileIndex.getInstance(env.getProject()).iterateContent(new ContentIterator() {
            @Override
            public boolean processFile(@NotNull VirtualFile virtualFile) {
                if (!virtualFile.isDirectory()) {
                    PsiFile file = PsiManager.getInstance(env.getProject()).findFile(virtualFile);
                    Document document = PsiDocumentManager.getInstance(env.getProject())
                            .getDocument(file);
                    if (file.getFileType() instanceof JavaFileType) {
                        file.accept(new JavaRecursiveElementWalkingVisitor() {
                            @Override
                            public void visitElement(PsiElement element) {
//                                System.out.println(element);
                                super.visitElement(element);
                            }

                            @Override
                            public void visitLocalVariable(PsiLocalVariable variable) {
                                System.out.println(variable);

//                                FieldBreakpoint breakpoint = breakpointManager.addFieldBreakpoint
//                                        (document, document.getLineNumber(1));
//                                System.out.println(breakpoint);
                                super.visitLocalVariable(variable);
                            }
                        });
                    }
                }
                return true;
            }
        });


//        DebuggerManagerEx.getInstanceEx(env.getProject()).getBreakpointManager()
//                .addFieldBreakpoint()
//        DebuggerManagerImpl
//        FieldBreakpoint fb;


        if (debuggerSession == null) {
            return null;
        }

        debuggerSession.getContextManager().addListener(new DebuggerContextListener() {
            private int c = 0;
            @Override
            public void changeEvent(@NotNull DebuggerContextImpl newContext, DebuggerSession.Event event) {
                System.out.println(c + ":" + event.toString());
                c++;
                if (event == DebuggerSession.Event.PAUSE
                        || event == DebuggerSession.Event.CONTEXT
                        || event == DebuggerSession.Event.REFRESH
                        || event == DebuggerSession.Event.REFRESH_WITH_STACK
                        && debuggerSession.isPaused()) {


                    final SuspendContextImpl newSuspendContext = newContext.getSuspendContext();
                    final StackFrameProxyImpl sfProxy = newContext.getFrameProxy();
                    if (newSuspendContext != null && event == DebuggerSession.Event.REFRESH_WITH_STACK) {
//                    if (newSuspendContext != null) {
                        newSuspendContext
                                .getDebugProcess()
                                .getManagerThread()
                                .invokeCommand(
                                        new DebuggerCommand() {
                            @Override
                            public void action() {
                                try {
//                                    System.out.println("action");
                                    StackFrame frame = sfProxy.getStackFrame();
                                    List<LocalVariable> visibleVariables = frame.visibleVariables();

                                    Collection<DecompiledLocalVariable> vars = new ArrayList<>();
                                    int slot = 0;
                                    for (LocalVariable visibleVariable:visibleVariables) {
                                        List<String>  names = new ArrayList<>();
                                        names.add(visibleVariable.name());
                                        vars.add(new DecompiledLocalVariable(slot, visibleVariable.isArgument(),
                                                visibleVariable.signature(), names));
                                        slot++;
                                    }

//                                    vars.forEach(d -> System.out.println(d.getDefaultName()));

                                    Field frameIdField = frame.getClass().getDeclaredField("id");
                                    frameIdField.setAccessible(true);
                                    Object frameId = frameIdField.get(frame);

                                    VirtualMachine vm = frame.virtualMachine();
                                    Method stateMethod = vm.getClass().getDeclaredMethod("state");
                                    stateMethod.setAccessible(true);

                                    Object slotInfoArray = createSlotInfoArray(vars);

                                    Object packetStream;
                                    Object vmState = stateMethod.invoke(vm);
                                    synchronized(vmState) {
                                        packetStream = ourEnqueueMethod.invoke(null, vm, frame.thread(), frameId, slotInfoArray);
                                    }

                                    Object reply = ourWaitForReplyMethod.invoke(null, vm, packetStream);
                                    Field valuesField = reply.getClass().getDeclaredField("values");
                                    valuesField.setAccessible(true);

                                    Value[] values = (Value[]) valuesField.get(reply);
                                    if (vars.size() != values.length) {
                                        throw new InternalException("Wrong number of values returned from target VM");
                                    }

                                    int idx = 0;
                                    for (DecompiledLocalVariable var : vars) {
                                        Value value = values[idx];
                                        String valueAsString = null;
                                        //Type valueType = (null == value) ? null : value.type();
                                        if (null != value) {
                                            valueAsString = value.toString();
//                                            if (value instanceof ArrayReferenceImpl & var.getSignature().equals("[Ljava/lang/String;"))
//                                            {
//                                                ArrayReferenceImpl valueAsArray = (ArrayReferenceImpl)value;
//                                                List<StringReferenceImpl> arrayValues = (List<StringReferenceImpl>) valueAsArray.getValues();
//                                                valueAsString = "[";
//                                                for (StringReferenceImpl arrayValue:arrayValues) {
//                                                    valueAsString += arrayValue + ",";
//                                                }
//                                                if (arrayValues.size() > 0) { // remove last comma
//                                                    valueAsString = valueAsString.substring(0, valueAsString.length() - 1);
//                                                }
//                                                valueAsString += "]";
//                                            }
                                        }

                                        System.out.println(var.getDisplayName() + ":" + valueAsString);
                                        //msg.append(var.getSlot() + ":" + var.getName() + ":" + valueType + ":" + valueAsString + ":" + var.getSignature() + "\n");

                                        idx++;
                                    }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void commandCancelled() {

                            }
                        });
                    }
                }
            }
        });


//        addListenersDebugSession(debuggerSession);

        final DebugProcessImpl debugProcess = debuggerSession.getProcess();

        addListenersDebugProcess(debuggerSession);

        return XDebuggerManager.getInstance(env.getProject()).startSession(env, new XDebugProcessStarter() {
            @Override
            @NotNull
            public XDebugProcess start(@NotNull XDebugSession session) {
                XDebugSessionImpl sessionImpl = (XDebugSessionImpl)session;
                ExecutionResult executionResult = debugProcess.getExecutionResult();
                sessionImpl.addExtraActions(executionResult.getActions());
                if (executionResult instanceof DefaultExecutionResult) {
                    sessionImpl.addRestartActions(((DefaultExecutionResult)executionResult).getRestartActions());
                }
                return JavaDebugProcess.create(session, debuggerSession);
            }
        }).getRunContentDescriptor();
    }

    private void addListenersDebugSession(DebuggerSession debuggerSession) {
//        debuggerSession.getXDebugSession().addSessionListener(new XDebugSessionListener() {
//            @Override
//            public void sessionPaused() {
//                System.out.println("sessionPaused");
//            }
//
//            @Override
//            public void sessionResumed() {
//                System.out.println("sessionResumed");
//            }
//
//            @Override
//            public void sessionStopped() {
//                System.out.println("sessionStopped");
//            }
//
//            @Override
//            public void stackFrameChanged() {
//                System.out.println("stackFrameChanged");
//            }
//
//            @Override
//            public void beforeSessionResume() {
//                System.out.println("beforeSessionResume");
//            }
//
//            @Override
//            public void settingsChanged() {
//                System.out.println("settingsChanged");
//            }
//        });
    }

    private void addListenersDebugProcess(DebuggerSession debuggerSession) {
        DebugProcessImpl debugProcess = debuggerSession.getProcess();

//        debugProcess.addProcessListener(new ProcessListener() {
//            @Override
//            public void startNotified(@NotNull ProcessEvent processEvent) {
//                System.out.println("startNotified");
//            }
//
//            @Override
//            public void processTerminated(@NotNull ProcessEvent processEvent) {
//                System.out.println("processTerminated");
//            }
//
//            @Override
//            public void processWillTerminate(@NotNull ProcessEvent processEvent, boolean b) {
//                System.out.println("processWillTerminate");
//            }
//
//            @Override
//            public void onTextAvailable(@NotNull ProcessEvent processEvent, @NotNull Key key) {
//                System.out.println("onTextAvailable");
//            }
//        });

        debugProcess.addDebugProcessListener(new DebugProcessListener() {
/*            @Override
//            public void connectorIsReady() {
//                System.out.println("connectorIsReady");
//            }
//
//            @Override
//            public void paused(@NotNull SuspendContext suspendContext) {
//                System.out.println("paused");
//            }
//
//            @Override
//            public void resumed(SuspendContext suspendContext) {
//                System.out.println("resumed");
//            }
//
//            @Override
//            public void processDetached(@NotNull DebugProcess process, boolean closedByUser) {
//                System.out.println("processDetached");
//            }
//
//            @Override
//            public void processAttached(@NotNull DebugProcess process) {
//                System.out.println("processAttached");
//            }
//
//            @Override
//            public void attachException(RunProfileState state, ExecutionException exception, RemoteConnection remoteConnection) {
//                System.out.println("attachException");
//            }*/

            @Override
            public void threadStarted(@NotNull DebugProcess proc, ThreadReference thread) {
                System.out.println("threadStarted");
//                System.out.println(thread.isAtBreakpoint());
                proc.getManagerThread().invokeCommand(new DebuggerCommand() {
                    @Override
                    public void action() {
                        thread.suspend();
                        VirtualMachine vm;
                        try {
                            while(thread.frameCount() == 0) {
                                thread.resume();
                                thread.suspend();
                            }
                            StackFrame f = thread.frame(0);

                            System.out.println();
                            System.out.println("VIRTUAL MACHINE:" + f.virtualMachine());
                            vm = f.virtualMachine();
                            System.out.println("can request monitor events:" + vm
                                    .canRequestMonitorEvents());

                            EventRequestManager eventRequestManager = vm.eventRequestManager();


                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                        String[] excludes = {"java.*", "javax.*", "sun.*", "com.sun.*"};
                        EventThread eventThread = new EventThread((VirtualMachineProxyImpl) proc
                                .getVirtualMachineProxy(), excludes, new PrintWriter (System
                                .out));
                        eventThread.setEventRequests(true);
                        eventThread.start();
                        thread.resume();
                    }

                    @Override
                    public void commandCancelled() {}
                });

            }

//            @Override
//            public void threadStopped(@NotNull DebugProcess proc, ThreadReference thread) {
//                System.out.println("threadStopped");
//            }
        });
//        debugProcess.addEvaluationListener(new EvaluationListener() {
//            @Override
//            public void evaluationStarted(SuspendContextImpl suspendContext) {
//                System.out.println("evaluationStarted");
//            }
//
//            @Override
//            public void evaluationFinished(SuspendContextImpl suspendContext) {
//                System.out.println("evaluationFinished");
//            }
//        });
    }

}
