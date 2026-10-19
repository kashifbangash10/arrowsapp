import com.amowpuzzle.aitricker.data.level.LevelCatalog;
import com.amowpuzzle.aitricker.data.level.CampaignChapters;
import com.amowpuzzle.aitricker.data.level.ExpertExpansionLevels;
import com.amowpuzzle.aitricker.data.level.MasterCampaignLevels;
import com.amowpuzzle.aitricker.data.level.GrandmasterCampaignLevels;
import com.amowpuzzle.aitricker.data.level.AdvancedCampaignLevels;
import com.amowpuzzle.aitricker.data.level.ApexCampaignLevels;
import com.amowpuzzle.aitricker.domain.engine.PuzzleSolver;
import com.amowpuzzle.aitricker.domain.engine.SolutionResult;
import com.amowpuzzle.aitricker.domain.model.ArrowPath;
import com.amowpuzzle.aitricker.domain.model.Cell;
import com.amowpuzzle.aitricker.domain.model.Level;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Export actual compiled Kotlin levels, never a parallel reconstruction of the data. */
public class CampaignExport {
    private static String quoted(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String levelName(int id) {
        if (id >= 1001) return ApexCampaignLevels.INSTANCE.name(id);
        if (id >= 501) return AdvancedCampaignLevels.INSTANCE.name(id);
        if (id >= 251) return GrandmasterCampaignLevels.INSTANCE.name(id);
        if (id >= 101) return MasterCampaignLevels.INSTANCE.name(id);
        if (id >= 51) return ExpertExpansionLevels.INSTANCE.name(id);
        return switch (id) {
            case 6 -> "Dense maze";
            case 15 -> "Heart";
            case 20 -> "Diamond";
            case 25 -> "Lightning";
            case 30 -> "Star";
            case 40 -> "Butterfly";
            case 45 -> "Rocket";
            case 49 -> "Dog";
            case 50 -> "Guitar";
            default -> "Winding board";
        };
    }

    public static void main(String[] args) throws Exception {
        int totalLevels = LevelCatalog.TOTAL_LEVELS;
        StringBuilder json = new StringBuilder("{\"levelCount\":").append(totalLevels).append(",\"levels\":[\n");
        for (int id = 1; id <= totalLevels; id++) {
            Level level = LevelCatalog.INSTANCE.level(id);
            SolutionResult solution = PuzzleSolver.INSTANCE.solve(level, level.getArrows());
            if (!solution.getSolvable() || !PuzzleSolver.INSTANCE.verify(level, solution.getMoveSequence())) {
                throw new IllegalStateException("Level " + id + " does not have a valid complete solution");
            }
            int occupiedCells = level.getArrows().stream().mapToInt(arrow -> arrow.getCells().size()).sum();
            int turns = level.getArrows().stream().mapToInt(ArrowPath::getTurnCount).sum();
            int longest = level.getArrows().stream().mapToInt(arrow -> arrow.getCells().size()).max().orElse(0);
            if (id > 1) json.append(",\n");
            json.append("{\"id\":").append(level.getId())
                .append(",\"name\":").append(quoted(levelName(id)))
                .append(",\"category\":").append(quoted(CampaignChapters.INSTANCE.getAll().get(CampaignChapters.INSTANCE.indexOf(id)).getName()))
                .append(",\"cols\":").append(level.getCols()).append(",\"rows\":").append(level.getRows())
                .append(",\"difficulty\":\"").append(level.getDifficulty().name()).append('"')
                .append(",\"presentation\":\"").append(level.getPresentation().name()).append('"')
                .append(",\"movementRule\":\"").append(level.getMovementRule().name()).append('"')
                .append(",\"arrowCount\":").append(level.getArrowCount())
                .append(",\"occupiedCells\":").append(occupiedCells)
                .append(",\"gridDensity\":").append((double) occupiedCells / (level.getCols() * level.getRows()))
                .append(",\"totalTurns\":").append(turns)
                .append(",\"longestArrowCells\":").append(longest)
                .append(",\"initiallyFree\":").append(solution.getInitiallyFree())
                .append(",\"dependencyRounds\":").append(solution.getMaxDependencyDepth())
                .append(",\"solutionIds\":[");
            boolean first = true;
            for (ArrowPath arrow : solution.getMoveSequence()) {
                if (!first) json.append(',');
                first = false;
                json.append(arrow.getId());
            }
            json.append("],\"arrows\":[");
            first = true;
            for (ArrowPath arrow : level.getArrows()) {
                if (!first) json.append(',');
                first = false;
                json.append("{\"id\":").append(arrow.getId()).append(",\"exit\":\"")
                    .append(arrow.getExit().name()).append("\",\"cells\":[");
                boolean firstCell = true;
                for (Cell cell : arrow.getCells()) {
                    if (!firstCell) json.append(',');
                    firstCell = false;
                    json.append('[').append(cell.getCol()).append(',').append(cell.getRow()).append(']');
                }
                json.append("]}");
            }
            json.append("]}");
        }
        json.append("\n]}\n");
        Path output = Path.of(args.length == 0 ? "artifacts/campaign/catalog.json" : args[0]);
        if (output.getParent() != null) Files.createDirectories(output.getParent());
        Files.writeString(output, json.toString(), StandardCharsets.UTF_8);
        System.out.println("Exported " + totalLevels + " solved levels to " + output);
    }
}
