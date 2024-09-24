package test.advanced;

import java.util.UUID;
import org.geysermc.databaseutils.meta.Entity;
import org.geysermc.databaseutils.meta.Index;
import org.geysermc.databaseutils.meta.Key;
import org.geysermc.databaseutils.meta.Length;

@Index(columns = {"c"})
@Entity("hello")
public record TestEntity(
        @Key int a, @Key @Length(max = 50) String b, @Length(max = 10) String c, @Length(max = 16) UUID d) {}