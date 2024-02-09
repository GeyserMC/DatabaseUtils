package org.geysermc.databaseutils.processor.action;

import java.util.Set;

public final class ActionRegistry {
    private static final Set<Action> REGISTERED_ACTIONS = Set.of(
            new FindByAction(), new ExistsByAction(), new SaveAction(), new UpdateAction(), new DeleteAction());

    public static Action actionMatching(String name) {
        for (Action action : REGISTERED_ACTIONS) {
            if (action.actionPattern().matcher(name).matches()) {
                return action;
            }
        }
        return null;
    }
}
