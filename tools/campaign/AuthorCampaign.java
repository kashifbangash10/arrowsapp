import com.amowpuzzle.aitricker.domain.generator.*;
import com.amowpuzzle.aitricker.domain.model.*;
import com.amowpuzzle.aitricker.domain.engine.*;
import java.nio.file.*;
import java.util.*;
/** Offline authoring only. The app loads the committed coordinates, never these candidate seeds. */
public class AuthorCampaign {
 static Set<Integer> shapes=Set.of(15,20,25,30,40,45,49,50);
 public static void main(String[] args)throws Exception{
  StringBuilder kt=new StringBuilder("package com.amowpuzzle.aitricker.data.level\n\nimport com.amowpuzzle.aitricker.domain.model.*\n\n/** Fixed, solver-checked campaign paths. Authored offline by tools/campaign/AuthorCampaign.java.\n * Stable arrow IDs and tail-to-head grid coordinates; no runtime generation or fallback. */\ninternal object FixedCampaignLevels {\n    fun level(id: Int): Level = when (id) {\n");
  for(int id=1;id<=50;id++)if(!shapes.contains(id))kt.append("        "+id+" -> level"+id+"()\n");
  kt.append("        else -> error(\"No fixed campaign level $id\")\n    }\n\n");
  StringBuilder report=new StringBuilder("id,difficulty,arrows,openings,rounds,cols,rows,candidate\n");
  for(int id=1;id<=50;id++){
   if(shapes.contains(id))continue;
   Difficulty difficulty=id<=5?Difficulty.EASY:id<=20?Difficulty.NORMAL:id<=35?Difficulty.HARD:Difficulty.EXPERT;
   int cols,rows,target,minCount,minDepth,maxDepth,minFree,maxFree,maxLength;float turn;
   if(id<=5){cols=6;rows=7;target=id+5;minCount=target;minDepth=new int[]{1,2,2,3,3}[id-1];maxDepth=new int[]{2,3,4,4,5}[id-1];minFree=new int[]{4,3,3,2,2}[id-1];maxFree=new int[]{6,5,4,4,3}[id-1];maxLength=id<=2?3:4;turn=0.08f+id*0.035f;}
   else if(id<=20){int step=id-6;cols=9+step/5;rows=11+step/5;target=14+step;minCount=target-1;minDepth=4+step/4;maxDepth=minDepth+3;minFree=2;maxFree=4;maxLength=5;turn=0.28f+step*.007f;}
   else if(id<=35){int step=id-21;cols=13+step/5;rows=16+step/4;target=35+step;minCount=target-2;minDepth=9+step/4;maxDepth=minDepth+4;minFree=1;maxFree=3;maxLength=6;turn=.40f+step*.006f;}
   else {int step=id-36;cols=17+step/7;rows=22+step/6;target=68+step;minCount=target-5;minDepth=14+step/5;maxDepth=minDepth+6;minFree=1;maxFree=2;maxLength=7;turn=.48f+step*.004f;}
   Level chosen=null;SolutionResult solution=null;int selected=-1;
   for(int attempt=0;attempt<180;attempt++){
    int seedId=id+attempt*101;
    LevelSpec spec=new LevelSpec(seedId,cols,rows,difficulty,target,minCount,minFree,maxFree,minDepth,.35f,2,maxLength,turn,12);
    Level candidate=LevelGenerator.INSTANCE.generate(spec);
    SolutionResult r=PuzzleSolver.INSTANCE.solve(candidate,candidate.getArrows());
    if(r.getSolvable()&&candidate.getArrowCount()>=minCount&&candidate.getArrowCount()<=target&&r.getInitiallyFree()>=minFree&&r.getInitiallyFree()<=maxFree&&r.getMaxDependencyDepth()>=minDepth&&r.getMaxDependencyDepth()<=maxDepth){chosen=candidate;solution=r;selected=attempt;break;}
   }
   if(chosen==null)throw new IllegalStateException("No qualifying level "+id+"; review constraints, do not use fallback.");
   String row=String.format(Locale.ROOT,"%d,%s,%d,%d,%d,%d,%d,%d",id,difficulty,chosen.getArrowCount(),solution.getInitiallyFree(),solution.getMaxDependencyDepth(),cols,rows,selected);
   System.out.println(row);report.append(row).append('\n');
   kt.append("    // "+chosen.getArrowCount()+" arrows, "+solution.getInitiallyFree()+" openings, "+solution.getMaxDependencyDepth()+" dependency rounds.\n");
   kt.append("    private fun level"+id+"() = Level(\n        id = "+id+", cols = "+cols+", rows = "+rows+", difficulty = Difficulty."+difficulty+",\n        presentation = LevelPresentation.DARK_WHITE,\n        arrows = listOf(\n");
   for(ArrowPath a:chosen.getArrows()){
    List<Cell> c=a.getCells();List<Cell> corners=new ArrayList<>();corners.add(c.get(0));
    for(int j=1;j<c.size()-1;j++){Cell p=c.get(j-1),q=c.get(j),r=c.get(j+1);if(q.getCol()-p.getCol()!=r.getCol()-q.getCol()||q.getRow()-p.getRow()!=r.getRow()-q.getRow())corners.add(q);}
    corners.add(c.get(c.size()-1));kt.append("            path("+a.getId()+", Direction."+a.getExit());for(Cell p:corners)kt.append(", "+p.getCol()+", "+p.getRow());kt.append("),\n");
   }
   kt.append("        )\n    )\n\n");
  }
  kt.append("    private fun path(id: Int, exit: Direction, vararg xy: Int): ArrowPath {\n        require(xy.size >= 4 && xy.size % 2 == 0)\n        val corners = xy.toList().chunked(2).map { Cell(it[0], it[1]) }\n        val cells = mutableListOf(corners.first())\n        for ((from, to) in corners.zipWithNext()) {\n            require((from.col == to.col) != (from.row == to.row))\n            val dx = (to.col - from.col).coerceIn(-1, 1)\n            val dy = (to.row - from.row).coerceIn(-1, 1)\n            var cursor = from\n            while (cursor != to) {\n                cursor = Cell(cursor.col + dx, cursor.row + dy)\n                cells.add(cursor)\n            }\n        }\n        return ArrowPath(id, cells, exit)\n    }\n}\n");
  Files.writeString(Path.of("app/src/main/java/com/amowpuzzle/aitricker/data/level/FixedCampaignLevels.kt"),kt.toString());
  Files.createDirectories(Path.of("artifacts/campaign"));Files.writeString(Path.of("artifacts/campaign/authored-recipes.csv"),report.toString());
 }
}
