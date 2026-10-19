import com.amowpuzzle.aitricker.data.level.LevelCatalog;
import com.amowpuzzle.aitricker.domain.model.ArrowPath;
import com.amowpuzzle.aitricker.domain.model.Cell;
import com.amowpuzzle.aitricker.domain.model.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Stable, ordered fingerprints of every shipped field used by Levels 2-50. */
public class LevelFingerprint {
    public static void main(String[] args) throws Exception {
        StringBuilder report = new StringBuilder();
        for (int id = 2; id <= 50; id++) {
            Level level = LevelCatalog.INSTANCE.level(id);
            StringBuilder canonical = new StringBuilder();
            canonical.append(level.getId()).append('|').append(level.getCols()).append('|')
                .append(level.getRows()).append('|').append(level.getDifficulty().name()).append('|')
                .append(level.getStartingLives()).append('|').append(level.getStartingHints()).append('\n');
            for (ArrowPath arrow : level.getArrows()) {
                canonical.append(arrow.getId()).append(':').append(arrow.getExit().name()).append(':');
                for (Cell cell : arrow.getCells()) canonical.append(cell.getCol()).append(',').append(cell.getRow()).append(';');
                canonical.append('\n');
            }
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
            report.append(id).append(' ').append(hash).append('\n');
        }
        if (args.length == 0) System.out.print(report);
        else Files.writeString(Path.of(args[0]), report.toString(), StandardCharsets.UTF_8);
    }
}
