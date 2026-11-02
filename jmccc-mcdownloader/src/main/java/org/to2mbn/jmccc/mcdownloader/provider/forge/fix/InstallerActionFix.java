package org.to2mbn.jmccc.mcdownloader.provider.forge.fix;

import java.io.File;
import java.util.function.Predicate;

public final class InstallerActionFix {

    private InstallerActionFix() {
    }

    public static boolean run(Object action, Class<?> actionType, File target, File installer) throws Exception {
        try {
            return (Boolean) actionType.getMethod("run", File.class, File.class).invoke(action, target, installer);
        } catch (NoSuchMethodException e) {
            Predicate<String> optionals = value -> true;
            try {
                return (Boolean) actionType.getMethod("run", File.class, Predicate.class, File.class).invoke(action, target, optionals, installer);
            } catch (NoSuchMethodException ex) {
                return (Boolean) actionType.getMethod("run", File.class, Predicate.class).invoke(action, target, optionals);
            }
        }
    }
}
