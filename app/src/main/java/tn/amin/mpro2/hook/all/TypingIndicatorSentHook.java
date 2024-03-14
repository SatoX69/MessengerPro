package tn.amin.mpro2.hook.all;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import tn.amin.mpro2.constants.OrcaClassNames;
import tn.amin.mpro2.hook.BaseHook;
import tn.amin.mpro2.hook.HookId;
import tn.amin.mpro2.hook.HookTime;
import tn.amin.mpro2.hook.listener.HookListenerResult;
import tn.amin.mpro2.hook.unobfuscation.OrcaUnobfuscator;
import tn.amin.mpro2.orca.OrcaGateway;

public class TypingIndicatorSentHook extends BaseHook {
    @Override
    public HookId getId() {
        return HookId.TYPING_INDICATOR_SEND;
    }

    @Override
    public HookTime getHookTime() {
        return HookTime.AFTER_DEOBFUSCATION;
    }

    @Override
    protected Set<XC_MethodHook.Unhook> injectInternal(OrcaGateway gateway) {
        Class<?> TypingIndicatorDispatcher = gateway.unobfuscator.getClass(OrcaUnobfuscator.CLASS_TYPING_INDICATOR_DISPATCHER);

        if (TypingIndicatorDispatcher == null)
            throw new RuntimeException(OrcaUnobfuscator.CLASS_TYPING_INDICATOR_DISPATCHER + " is null");


        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        final Class<?> MailboxSDKJNI = XposedHelpers.findClass(OrcaClassNames.MAILBOX_SDK_JNI, gateway.classLoader);
        unhooks.addAll(XposedBridge.hookAllMethods(MailboxSDKJNI, "dispatchVOOOOZ", wrap(new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1].getClass().getName().equals(OrcaClassNames.MAILBOX) &&
                        (Boolean.TRUE.equals(param.args[param.args.length-1]))
                ) {
                    notifyListenersWithResult((listener) -> ((TypingIndicatorSentListener) listener).onTypingIndicatorSent());
                    boolean allowTypingIndicator = !getListenersReturnValue().isConsumed || (Boolean) getListenersReturnValue().value;
                    if (!allowTypingIndicator) {
                        param.args[param.args.length-1] = Boolean.FALSE;
                    }
                }
            }
        })));

        Method dispatchTypingIndicator = TypingIndicatorDispatcher.getMethods()[0];
        unhooks.add(XposedBridge.hookMethod(dispatchTypingIndicator, wrap(new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                notifyListenersWithResult((listener) -> ((TypingIndicatorSentListener) listener).onTypingIndicatorSent());
                boolean allowTypingIndicator = !getListenersReturnValue().isConsumed || (Boolean) getListenersReturnValue().value;
                if (!allowTypingIndicator) {
                    param.setResult(null);
                }
            }
        })));

        return unhooks;
    }

    public interface TypingIndicatorSentListener {
        HookListenerResult<Boolean> onTypingIndicatorSent();
    }
}
