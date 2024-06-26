package test.advanced;

import java.util.UUID;
import org.geysermc.databaseutils.meta.Entity;
import org.geysermc.databaseutils.meta.Index;
import org.geysermc.databaseutils.meta.Key;

@Index(columns = {"c"})
@Entity("hello")
public record TestEntity(@Key int a, @Key String b, String c, UUID d) {
}